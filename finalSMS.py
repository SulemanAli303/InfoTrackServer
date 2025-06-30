from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
from flask_cors import CORS
import sqlite3
import base64
import threading
import time
import datetime
app = Flask(__name__)
CORS(app)
socketio = SocketIO(app, cors_allowed_origins="*")

# Hardcoded authentication credentials
USERNAME = "sulemanali303"
PASSWORD = "Suleman@12"

# Message Queue
message_queue = []
lock = threading.Lock()


# **Database Setup**
def init_db():
    conn = sqlite3.connect('messages.db')
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            content TEXT,
            subject TEXT,
            status TEXT,
            timestamp INTEGER
        )
    ''')
    conn.commit()
    conn.close()
init_db()
# **Helper Functions**
def add_message_to_db(content, subject, status="pending"):
    conn = sqlite3.connect('messages.db')
    cursor = conn.cursor()
    cursor.execute("INSERT INTO messages (content, subject, status, timestamp) VALUES (?, ?, ?, ?)",
                   (content, subject, status, int(time.time())))
    message_id = cursor.lastrowid
    conn.commit()
    conn.close()
    return message_id
def update_message_status(message_id, status):
    conn = sqlite3.connect('messages.db')
    cursor = conn.cursor()
    cursor.execute("UPDATE messages SET status = ? WHERE id = ?", (status, message_id))
    conn.commit()
    conn.close()
def authenticate():
    auth_header = request.headers.get("Authorization")
    if not auth_header:
        return False
    try:
        auth_type, encoded_credentials = auth_header.split(" ")
        decoded_credentials = base64.b64decode(encoded_credentials).decode("utf-8")
        username, password = decoded_credentials.split(":")
        return username == USERNAME and password == PASSWORD
    except Exception:
        return False


# **HTTP Routes**
@app.route("/send-sms", methods=["POST"])
def send_sms():
    if not authenticate():
        return jsonify({"isSuccess": False, "message": "Not Authorized"}), 401
    data = request.json
    message_content = data.get("message")
    subject = data.get("to")
    if not message_content:
        return jsonify({"isSuccess": False, "message": "Missing message content"}), 400

    message_id = add_message_to_db(message_content, subject)

    with lock:
        if len(socketio.server.eio.sockets) > 0:
            socketio.emit("new_message", {"message_id": message_id, "content": message_content, "subject": subject})
            update_message_status(message_id, "sent_device")
        else:
            message_queue.append((message_id, message_content, subject))

    return jsonify({"isSuccess": True, "message": "Message received", "data": {"message_id": message_id}}), 200


@app.route("/update-sms", methods=["POST"])
def update_sms():
    if not authenticate():
        return jsonify({"isSuccess": False, "message": "Not Authorized"}), 401

    data = request.json
    message_id = data.get("message_id")
    message_status = data.get("message_status")

    if not message_id or not message_status:
        return jsonify({"isSuccess": False, "message": "Missing required fields"}), 400

    update_message_status(message_id, message_status)
    return jsonify({"isSuccess": True, "message": "Message status updated"}), 200


@app.route("/fetch-sms", methods=["GET"])
def fetch_sms():
    if not authenticate():
        return jsonify({"isSuccess": False, "message": "Not Authorized"}), 401
    now = int(time.time())  # Current UNIX timestamp
    today_start = int(datetime.datetime.combine(datetime.date.today(), datetime.time.min).timestamp())
    deviceId = request.args.get("deviceId", None)
    dateFrom = int(request.args.get("dateFrom", today_start))
    dateTo = int(request.args.get("dateTo", now))
    page = int(request.args.get("page", 1))
    limit = int(request.args.get("limit", 10))
    offset = (page - 1) * limit

    conn = sqlite3.connect("messages.db")
    cursor = conn.cursor()

    query = "SELECT COUNT(*) FROM messages WHERE timestamp BETWEEN ? AND ?"
    params = [dateFrom, dateTo]

    if deviceId:
        query += " AND content LIKE ?"
        params.append(f"%{deviceId}%")

    cursor.execute(query, params)
    total_record = cursor.fetchone()[0]

    query = "SELECT * FROM messages WHERE timestamp BETWEEN ? AND ?"
    if deviceId:
        query += " AND content LIKE ?"
    query += " ORDER BY id DESC LIMIT ? OFFSET ?"

    params.extend([limit, offset])
    cursor.execute(query, params)
    messages = [{"id": row[0], "content": row[1], "subject": row[2], "status": row[3], "timestamp": row[4]}
                for row in cursor.fetchall()]
    conn.close()
    return jsonify({
        "isSuccess": True,
        "message": "Messages fetched successfully",
        "data": {
            "messages": messages,
            "currentPage": page,
            "limit": limit,
            "totalRecord": total_record,
            "currentCount": len(messages),
        }
    })


# **Socket.IO Handlers**
@socketio.on("connect")
def handle_connect():
    print("Client connected")


@socketio.on("disconnect")
def handle_disconnect():
    print("Client disconnected")


@socketio.on("update_status")
def handle_update_status(data):
    print(data)
    message_id = data.get("message_id")
    status = data.get("message_status")
    if message_id and status:
        update_message_status(message_id, status)
        emit("status_updated", {"message_id": message_id, "message_status": status}, broadcast=True)


# **Background Task to Retry Queued Messages**
def retry_queued_messages():
    while True:
        with lock:
            for message in list(message_queue):
                message_id, message_content, subject = message
                current_time = int(time.time())
                conn = sqlite3.connect("messages.db")
                cursor = conn.cursor()
                cursor.execute("SELECT timestamp FROM messages WHERE id = ?", (message_id,))
                timestamp = cursor.fetchone()[0]
                conn.close()
                if current_time - timestamp > 1200:  # 20 minutes timeout
                    update_message_status(message_id, "timeout")
                    message_queue.remove(message)
                elif len(socketio.server.eio.sockets) > 0:
                    socketio.emit("new_message", {"message_id": message_id, "content": message_content, "subject": subject})
                    update_message_status(message_id, "sent_device")
                    message_queue.remove(message)

        time.sleep(10)


# **Start Background Threads**
threading.Thread(target=retry_queued_messages, daemon=True).start()

# **Run Flask & Socket.IO**
if __name__ == "__main__":
    socketio.run(app, host="0.0.0.0", port=8085, debug=True)
