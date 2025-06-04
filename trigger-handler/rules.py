import re

TRIGGER_PATTERNS = [
    r"^/zarasprite\s+(.*)",
    r".*\bsprite\b.*",  # case-insensitive later
]

def is_trigger(message):
    for pattern in TRIGGER_PATTERNS:
        if re.search(pattern, message, re.IGNORECASE):
            return True
    return False
