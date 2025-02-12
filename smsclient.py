import socket
import json
import time
from e303 import send_sms  # Ensure this module is correctly implemented

# Huawei E303 Hi-Link modem configuration
SMS_API_URL = "http://192.168.8.1/api/sms/send-sms"

SERVER_HOST = "localhost"  # Replace with your actual server IP if needed
SERVER_PORT = 8086
RECONNECT_DELAY = 0.1  # Seconds to wait before retrying the connection

def connect_to_server():
    """Continuously connect to the socket server and listen for messages."""
    
    while True:
        client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            print(f"Connecting to server at {SERVER_HOST}:{SERVER_PORT}...")
            client_socket.connect((SERVER_HOST, SERVER_PORT))
            print("Connected to server.")

            while True:
                # Receive data from the server
                data = client_socket.recv(1024)
                if not data:
                    print("Server disconnected. Attempting to reconnect...")
                    break  # Exit the inner loop and reconnect

                # Decode and parse the received JSON message
                message = data.decode("utf-8")
                print(f"Received message: {message}")

                try:
                    message_data = json.loads(message)
                    to = message_data.get("to")
                    body = message_data.get("body")

                    if to and body:
                        # Send SMS using the Huawei modem
                        send_sms(str(to), str(body))
                    else:
                        print("Invalid message format. 'to' and 'body' are required.")

                except json.JSONDecodeError:
                    print("Failed to decode JSON message.")
        
        except (socket.error, ConnectionError) as e:
            print(f"Connection error: {e}. Retrying in {RECONNECT_DELAY} seconds...")
        
        finally:
            client_socket.close()
            time.sleep(RECONNECT_DELAY)  # Wait before reconnecting

if __name__ == "__main__":
    connect_to_server()
