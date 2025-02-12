import json
import socket
import threading
import sqlite3
import time
from e303 import send_sms

USERNAME = "sulemanali303"
PASSWORD = "Suleman@12"
class SMSClient:
    def __init__(self, server_host, server_port,send_delay):
        self.server_host = server_host
        self.server_port = server_port
        self.send_delay = send_delay
        self.socket = None
        self.modem = None
        self.queue = []
        self.lock = threading.Lock()
        self.running = True
        self.max_retries = 5  # Maximum number of retries per message
        self.init_db()

    def init_db(self):
        """Initialize the local database for logging messages."""
        conn = sqlite3.connect('client_messages.db')
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

    def add_to_db(self, server_message_id, content,message_subject, status, retries=0):
        """Add a new message to the local database."""
        conn = sqlite3.connect('client_messages.db')
        cursor = conn.cursor()
        cursor.execute('''
            INSERT INTO client_messages (server_message_id, content,subject, status, retries, timestamp, last_retry)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        ''', (server_message_id, content,message_subject, status, retries, int(time.time()), None))
        conn.commit()
        conn.close()

    def update_db_status(self, server_message_id, status, retries=None, last_retry=None):
        """Update the status and retry information of a message in the database."""
        conn = sqlite3.connect('client_messages.db')
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

    def connect_to_server(self):
        """Establish connection to the server's socket with retries."""
        while self.running:
            try:
                self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.socket.connect((self.server_host, self.server_port))
                print("Connected to server.")
                return True
            except Exception as e:
                print(f"Connection failed: {e}. Retrying in 5 seconds...")
                time.sleep(5)
        return False

    def receive_messages(self):
        """Listen for incoming messages from the server and add to queue."""
        while self.running:
            try:
                requestBody = self.socket.recv(102400).decode('utf-8')
                if not requestBody:
                    self.connect_to_server()
                    continue
                data = json.JSONDecoder().decode(requestBody)
                if not data:
                    print("Server disconnected. Reconnecting...")
                    self.connect_to_server()
                    continue
                message_id = data.get("message_id")
                content = data.get("message_content")
                message_subject = data.get("message_subject")
                with self.lock:
                    self.queue.append((message_id, content,message_subject))
                    self.add_to_db(message_id, content,message_subject, 'received')
            except Exception as e:
                print(f"Error receiving message: {e}. Reconnecting...")
                self.connect_to_server()

    def process_queue(self):
        """Process messages from the queue and send via GSM modem."""
        while self.running:
            if not self.queue:
                time.sleep(1)
                continue
            # Safely dequeue the message
            with self.lock:
                message = self.queue.pop(0) if self.queue else None
            if not message:
                continue
            message_id, content, message_subject = message
            # Parse content into number and text
            if message_subject and content:
                number = message_subject
                text = content
            else:
                print(f"Invalid message format: {message_subject,content}")
                self.update_db_status(message_id, 'invalid')
                self.send_ack(message_id,message_subject, 'invalid')
                continue
            # Check modem connection
            success = send_sms(number,text) == 200;
            if success:
                print(f"Message {message_id} delivered.")
                self.update_db_status(message_id, 'delivered')
                self.send_ack(message_id,number, 'delivered')
                time.sleep(10)
            else:
                # Handle retries
                conn = sqlite3.connect('client_messages.db')
                cursor = conn.cursor()
                cursor.execute('SELECT retries FROM client_messages WHERE server_message_id = ?', (message_id,))
                retries = cursor.fetchone()[0]
                conn.close()
                if retries >= self.max_retries:
                    print(f"Message {message_id} failed after max retries.")
                    self.update_db_status(message_id, 'failed', retries=retries+1, last_retry=int(time.time()))
                    self.send_ack(message_id, number,'failed')
                else:
                    print(f"Message {message_id} failed. Retrying...")
                    self.update_db_status(message_id, 'retrying', retries=retries+1, last_retry=int(time.time()))
                    with self.lock:
                        self.queue.append((message_id, content,number))  # Requeue
                    time.sleep(5)  # Delay before retry

    def send_ack(self, message_id,message_subject, status):
        """Send acknowledgment back to the server."""
        try:
            data = {
                "message_id": message_id,
                "message_status": status,
                "message_subject": message_subject
            }
            self.socket.sendall(json.dumps(data).encode('utf-8'))
        except Exception as e:
            print(f"Failed to send acknowledgment: {e}")

    def start(self):
        """Start the client threads."""
        if self.connect_to_server():
            # Start message receiver and processor threads
            threading.Thread(target=self.receive_messages, daemon=True).start()
            threading.Thread(target=self.process_queue, daemon=True).start()
            # Keep main thread alive
            while self.running:
                time.sleep(1)
if __name__ == "__main__":
    # Configuration (Adjust according to your setup)
    SERVER_HOST = "108.61.202.105"  # Server's IP address
    SERVER_PORT = 8085         # Server's socket port
    DELAY_SECONDS = 10           # Modem's baud rate
    client = SMSClient(SERVER_HOST, SERVER_PORT,DELAY_SECONDS)
    client.start()
