#!/usr/bin/env bash
set -euo pipefail

# ┌────────────────────────────┐
# │   Deployment parameters    │
# └────────────────────────────┘
PROJECT_DIR="$HOME/ZaraSprite/chathook"
MINECRAFT_DIR="$HOME/minecraft"
SERVICE_NAME="paper.service"    # adjust if your systemd service has a different name
PLUGIN_NAME="chathook"

# ┌────────────────────────────┐
# │      Build the plugin      │
# └────────────────────────────┘
echo "→ Building ${PLUGIN_NAME} plugin in ${PROJECT_DIR}…"
cd "${PROJECT_DIR}"
mvn clean package -DskipTests

# ┌────────────────────────────┐
# │   Locate the built JAR     │
# └────────────────────────────┘
JAR_FILE=$(ls target/${PLUGIN_NAME}-*.jar 2>/dev/null | tail -n1)
if [[ -z "$JAR_FILE" || ! -f "$JAR_FILE" ]]; then
  echo "✖️  Error: could not find target/${PLUGIN_NAME}-*.jar"
  exit 1
fi

# ┌────────────────────────────┐
# │    Deploy to plugins dir   │
# └────────────────────────────┘
DEST_DIR="${MINECRAFT_DIR}/plugins"
echo "→ Copying ${JAR_FILE} → ${DEST_DIR}/"
cp "$JAR_FILE" "${DEST_DIR}/"

# ┌────────────────────────────┐
# │    Restart the server      │
# └────────────────────────────┘
echo "→ Restarting service ${SERVICE_NAME}…"
sudo systemctl restart "${SERVICE_NAME}"

echo "✅  Deployment complete!"
