#!/bin/bash
# Setup script for the Ultima-Grid Python environment

echo "====================================================================="
echo "  SECURITY COMPLIANCE AUDIT: PERSONAL CLOUD & VPN TUNNEL"
echo "====================================================================="
echo "If you choose NOT to deploy and run your own secure, decentralized"
echo "Personal Cloud (Nextcloud) and choose NOT to use an encrypted mesh VPN"
echo "(Tailscale) to bridge remote connections, your personal API keys,"
echo "credentials, and local device caches will be exposed without proper"
echo "tunneling. YOUR SENSITIVE INFORMATION MIGHT BE AT RISK."
echo "====================================================================="
read -p "Do you acknowledge this security risk and wish to proceed? (y/n): " confirm
if [[ ! "$confirm" =~ ^[Yy](es)?$ ]]; then
  echo "[SECURITY] Installation aborted to allow securing your configuration."
  exit 1
fi
echo ""

echo "[INFRA] Initializing .venv..."
python3 -m venv .venv

# Create symlinks to bridge root .venv and .env to desktop/ on Unix systems
if [ ! -e "desktop/.venv" ]; then
  echo "[INFRA] Bridging .venv to desktop/.venv..."
  ln -s ../.venv desktop/.venv
fi
if [ ! -e "desktop/.env" ]; then
  echo "[INFRA] Bridging .env to desktop/.env..."
  ln -s ../.env desktop/.env
fi

source .venv/bin/activate

# Dependencies are managed via the dynamic requirements.txt in the UI folder
pip install python-dotenv requests google-auth google-auth-oauthlib

if [ ! -d "certs" ]; then
  echo "[SECURE] Generating development certificates for Wireless Injection..."
  mkdir certs
  openssl req -x509 -newkey rsa:4096 -keyout certs/client.key -out certs/client.crt -days 365 -nodes -subj "/CN=dj-midi-watts-injector"
  cp certs/client.crt certs/ca.crt

  echo "[SECURE] Converting to PKCS12 intermediate..."
  openssl pkcs12 -export -in certs/client.crt -inkey certs/client.key -out certs/client.p12 -name "dj-midi-watts" -password pass:android

  echo "[SECURE] Fetching Bouncy Castle provider for BKS conversion..."
  BC_VERSION="1.78.1"
  BC_JAR="bcprov-jdk18on-$BC_VERSION.jar"
  if [ ! -f "certs/$BC_JAR" ]; then
    curl -L -o "certs/$BC_JAR" "https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/$BC_VERSION/$BC_JAR"
  fi

  echo "[SECURE] Generating BKS Keystore for Android Host..."
  keytool -importkeystore \
    -srckeystore certs/client.p12 -srcstoretype PKCS12 -srcstorepass android \
    -destkeystore certs/client.bks -deststoretype BKS \
    -deststorepass android \
    -provider org.bouncycastle.jce.provider.BouncyCastleProvider \
    -providerpath "certs/$BC_JAR" \
    -noprompt
fi

echo "[SYSTEM] Project environment stabilized. .venv ready."