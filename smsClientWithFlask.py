import json
import time
import threading
import sqlite3
from e303 import send_sms
import socketio  # Flask-SocketIO client

# Server Authentication
USERNAME = "sulemanali303"
PASSWORD = "Suleman@12"

class SMSClient:
    def __init__(self, server_url, send_delay):
        self.server_url = server_url
        self.send_delay = send_delay
        self.sio = socketio.Client()
        self.queue = []
        self.lock = threading.Lock()
        self.running = True
        self.max_retries = 5  # Max retries per message
        self.init_db()

        # Register event handlers
        self.sio.on("connect", self.on_connect)
        self.sio.on("disconnect", self.on_disconnect)
        self.sio.on("new_message", self.on_new_message)

    def init_db(self):
        """Initialize the local database for logging messages."""
        conn = sqlite3.connect("client_messages.db")
        cursor = conn.cursor()
        cursor.execute('''
            CREATE TABLE IF NOT EXISTS client_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                server_message_id INTEGER,
                content TEXT,
                subject TEXT,
                status TEXT,
                retries INTEGER DEFAULT 0,
                timestamp INTEGER,
                last_retry INTEGER
            )
        ''')
        conn.commit()
        conn.close()

    def add_to_db(self, server_message_id, content, subject, status, retries=0):
        """Add a new message to the local database."""
        conn = sqlite3.connect("client_messages.db")
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO client_messages (server_message_id, content, subject, status, retries, timestamp, last_retry)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (server_message_id, content, subject, status, retries, int(time.time()), None))
        conn.commit()
        conn.close()

    def update_db_status(self, server_message_id, status, retries=None, last_retry=None):
        """Update the status and retry information of a message in the database."""
        conn = sqlite3.connect("client_messages.db")
        cursor = conn.cursor()
        if retries is not None and last_retry is not None:
            cursor.execute('''
                UPDATE client_messages SET status = ?, retries = ?, last_retry = ? WHERE server_message_id = ?
            ''', (status, retries, last_retry, server_message_id))
        elif retries is not None:
            cursor.execute('''
                UPDATE client_messages SET status = ?, retries = ? WHERE server_message_id = ?
            ''', (status, retries, server_message_id))
        else:
            cursor.execute('''
                UPDATE client_messages SET status = ? WHERE server_message_id = ?
            ''', (status, server_message_id))
        conn.commit()
        conn.close()

    def on_connect(self):
        """Handle successful connection to the server."""
        print("Connected to server.")

    def on_disconnect(self):
        """Handle disconnection from the server."""
        print("Disconnected from server. Reconnecting...")
        while not self.sio.connected:
            try:
                self.sio.connect(self.server_url)
            except Exception as e:
                print(f"Reconnection failed: {e}. Retrying in 5 seconds...")
                time.sleep(5)

    def on_new_message(self, data):
        """Handle incoming messages from the server."""
        print(f"Received message from server: {data}")
        message_id = data.get("message_id")
        content = data.get("content")
        subject = data.get("subject")

        if message_id and content and subject:
            with self.lock:
                self.queue.append((message_id, content, subject))
                self.add_to_db(message_id, content, subject, "received")
        else:
            print("Invalid message format received.")

    def process_queue(self):
        """Process messages from the queue and send via GSM modem."""
        while self.running:
            if not self.queue:
                time.sleep(1)
                continue

            with self.lock:
                message = self.queue.pop(0) if self.queue else None

            if not message:
                continue

            message_id, content, subject = message
            number = subject

            if not number or not content:
                print(f"Invalid message format: {subject}, {content}")
                self.update_db_status(message_id, "invalid")
                self.send_ack(message_id, "invalid")
                continue

            success = send_sms(number, content) == 200
            if success:
                print(f"Message {message_id} delivered.")
                self.update_db_status(message_id, "delivered")
                self.send_ack(message_id, "delivered")
                time.sleep(10)
            else:
                conn = sqlite3.connect("client_messages.db")
                cursor = conn.cursor()
                cursor.execute("SELECT retries FROM client_messages WHERE server_message_id = ?", (message_id,))
                retries = cursor.fetchone()[0]
                conn.close()
                if retries >= self.max_retries:
                    print(f"Message {message_id} failed after max retries.")
                    self.update_db_status(message_id, "failed", retries=retries+1, last_retry=int(time.time()))
                    self.send_ack(message_id, "failed")
                else:
                    print(f"Message {message_id} failed. Retrying...")
                    self.update_db_status(message_id, "retrying", retries=retries+1, last_retry=int(time.time()))
                    with self.lock:
                        self.queue.append((message_id, content, number))
                    time.sleep(5)

    def send_ack(self, message_id, status):
        """Send acknowledgment back to the server."""
        try:
            self.sio.emit("update_status", {
                "message_id": message_id,
                "message_status": status
            })
        except Exception as e:
            print(f"Failed to send acknowledgment: {e}")

    def start(self):
        """Start the client and listen for messages."""
        try:
            self.sio.connect(self.server_url)
            threading.Thread(target=self.process_queue, daemon=True).start()
            self.sio.wait()
        except Exception as e:
            print(f"Connection error: {e}. Retrying...")
            self.on_disconnect()

if __name__ == "__main__":
    SERVER_URL = "http://localhost:8085"
    DELAY_SECONDS = 10  # Modem delay

    client = SMSClient(SERVER_URL, DELAY_SECONDS)
    client.start()
