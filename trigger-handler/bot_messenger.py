def send_to_bot(player, message):
    # Placeholder — simplest method is to write to a text file polled by Node bot
    with open("../ZaraSprite/outgoing.txt", "a", encoding="utf-8") as f:
        f.write(f"/tell {player} {message}\n")
