import os
import sqlite3
from flask import Flask, request, jsonify
from datetime import datetime, timedelta

app = Flask(__name__)

# ————————————————
# CONFIGURATION
# ————————————————
BASE_DIR = os.path.dirname(__file__)
DATA_DIR = os.path.abspath(os.path.join(BASE_DIR, '..', 'data'))
DATABASE_PATH = os.path.join(DATA_DIR, 'chat.db')

# Session timeout (minutes) and active‐window (hours) can be tweaked as needed
SESSION_TIMEOUT_MINUTES = 20
ACTIVE_WINDOW_HOURS = 48


# ————————————————
# SCHEMA SETUP
# ————————————————
def init_db():
    """
    Create (if not exists) the three tables:
    1. player_profiles
    2. sessions
    3. messages
    """
    os.makedirs(DATA_DIR, exist_ok=True)
    conn = sqlite3.connect(DATABASE_PATH)
    cursor = conn.cursor()

    # 1. player_profiles: one row per username
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS player_profiles (
            username TEXT PRIMARY KEY,
            first_seen TEXT,
            last_seen TEXT,
            session_count INTEGER DEFAULT 0,
            avg_session_length REAL DEFAULT 0,
            notes_json TEXT
        )
    """)

    # 2. sessions: each conversational session for a user
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS sessions (
            session_id       INTEGER PRIMARY KEY AUTOINCREMENT,
            username         TEXT,
            start_time       TEXT,
            end_time         TEXT,
            summary_text     TEXT,
            is_archived      INTEGER DEFAULT 0,
            FOREIGN KEY(username) REFERENCES player_profiles(username)
        )
    """)

    # 3. messages: every DM logged with a pointer to its session
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS messages (
            id           INTEGER PRIMARY KEY AUTOINCREMENT,
            username     TEXT,
            message      TEXT,
            timestamp    TEXT,
            session_id   INTEGER,
            FOREIGN KEY(session_id) REFERENCES sessions(session_id)
        )
    """)

    conn.commit()
    conn.close()

# ————————————————
# HEALTH CHECK ENDPOINT
# ————————————————
@app.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "ok"}), 200


# ————————————————
# RECEIVE_CHAT ENDPOINT
# ————————————————
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
