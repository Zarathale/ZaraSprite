from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/receive", methods=["POST"])
def receive_chat():
    data = request.get_json(silent=True)
    # Expecting JSON like: { "sender": "...", "message": "...", "type": "chat" }
    if not data or not all(k in data for k in ("sender", "message", "type")):
        return jsonify({"error": "invalid payload"}), 400

    sender = data["sender"]
    message = data["message"]
    msg_type = data["type"]

    # For now, just print to console
    print(f"[{msg_type.upper()}] {sender}: {message}")

    # Respond with a simple acknowledgment
    return jsonify({"status": "received"}), 200

if __name__ == "__main__":
    # Listen on all interfaces, port 5000
    app.run(host="0.0.0.0", port=5000)
