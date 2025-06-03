from flask import Flask, request
import datetime
import os

app = Flask(__name__)

# (1) Ensure a “logs” folder exists
LOG_DIR = "logs"
os.makedirs(LOG_DIR, exist_ok=True)
LOG_FILE = os.path.join(LOG_DIR, "chat_messages.log")

@app.route("/chat", methods=["POST"])
def receive_chat():
    # (2) Expect JSON or form data; adjust if Theatria sends plain text
    data = None
    if request.is_json:
        data = request.get_json()
    else:
        # fallback to raw text or form‐encoded
        data = request.get_data(as_text=True)

    # (3) Timestamp and append to log file
    timestamp = datetime.datetime.now().isoformat(sep=" ", timespec="seconds")
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(f"[{timestamp}] {data}\n")

    return ("", 204)  # no content

if __name__ == "__main__":
    # (4) Listen on all interfaces so remote server can reach you
    app.run(host="0.0.0.0", port=5000, debug=True)
