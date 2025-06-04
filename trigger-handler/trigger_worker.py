import time
from rules import is_trigger
from gpt_client import call_gpt
from bot_messenger import send_to_bot
from config import FLASK_MESSAGE_LOG

def get_new_messages():
    with open(FLASK_MESSAGE_LOG, encoding="utf-8") as f:
        lines = f.readlines()
    # TODO: track last-read line to avoid reprocessing
    return [{"player": "Zarathale", "text": line.strip()} for line in lines]

def main():
    while True:
        messages = get_new_messages()
        for msg in messages:
            if is_trigger(msg["text"]):
                prompt = msg["text"]
                reply = call_gpt(prompt)
                send_to_bot(msg["player"], reply)
        time.sleep(5)

if __name__ == "__main__":
    main()
