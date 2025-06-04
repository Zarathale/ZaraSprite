from flask import Flask, request, jsonify

app = Flask(__name__)

@app.route("/receive", methods=["POST"])
def receive_chat():
    """
    Expects JSON payload:
        {
            "username": "<playerName>",
            "message": "<plain text message>"
        }

    1. Validate incoming JSON
    2. Ensure a player_profile exists (insert or update last_seen)
    3. Check for an active session (not archived, within timeout window)
       - If none, create a new session (and bump session_count)
    4. Insert the new message row, linked to that session_id
    5. Return { status: "received", session_id: <id> }
    """
    data = request.get_json(silent=True)
    if not data:
        return jsonify({"status": "error", "error": "Invalid JSON"}), 400

    username = data.get("username")
    message_text = data.get("message")

    if not username or not message_text:
        return jsonify({"status": "error", "error": "Missing username or message"}), 400

    now_iso = datetime.utcnow().isoformat()

    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()

    # ——————————————
    # 1. UPSERT player_profile
    # ——————————————
    cursor.execute(
        "SELECT username FROM player_profiles WHERE username = ?",
        (username,)
    )
    if cursor.fetchone() is None:
        # first time seeing this user
        cursor.execute(
            """
            INSERT INTO player_profiles (username, first_seen, last_seen)
            VALUES (?, ?, ?)
            """,
            (username, now_iso, now_iso)
        )
    else:
        # just update last_seen
        cursor.execute(
            "UPDATE player_profiles SET last_seen = ? WHERE username = ?",
            (now_iso, username)
        )

    # ——————————————
    # 2. FIND OR CREATE SESSION
    # ——————————————
    # Define the cutoff for “active” sessions
    cutoff = (datetime.utcnow() - timedelta(minutes=SESSION_TIMEOUT_MINUTES)).isoformat()

    cursor.execute(
        """
        SELECT session_id, start_time
          FROM sessions
         WHERE username = ?
           AND is_archived = 0
           AND datetime(start_time) >= ?
        ORDER BY start_time DESC
        LIMIT 1
        """,
        (username, cutoff)
    )
    row = cursor.fetchone()

    if row:
        # An active session already exists
        session_id = row[0]
    else:
        # No active session → create a new one
        cursor.execute(
            """
            INSERT INTO sessions (username, start_time)
            VALUES (?, ?)
            """,
            (username, now_iso)
        )
        session_id = cursor.lastrowid

        # Increment session_count in player_profiles
        cursor.execute(
            """
            UPDATE player_profiles
               SET session_count = session_count + 1
             WHERE username = ?
            """,
            (username,)
        )

    # ——————————————
    # 3. INSERT MESSAGE
    # ——————————————
    cursor.execute(
        """
        INSERT INTO messages (username, message, timestamp, session_id)
        VALUES (?, ?, ?, ?)
        """,
        (username, message_text, now_iso, session_id)
    )

    conn.commit()
    conn.close()

    return jsonify({"status": "received", "session_id": session_id}), 200


# ————————————————
# APPLICATION ENTRYPOINT
# ————————————————
if __name__ == "__main__":
    
    # Ensure data folder + tables exist before starting
    init_db()

    # Use Waitress to serve on 0.0.0.0:5000
    from waitress import serve
    serve(app, host="0.0.0.0", port=5000)