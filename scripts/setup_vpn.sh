#!/bin/bash
# DJ MIDI WATTS - VPN Tunnel Provisioner (Unix/macOS/Linux)
# Configures Tailscale mesh network for secure remote synchronization

echo -e "\033[36m=====================================================================\033[0m"
echo -e "\033[36;1m         DJ MIDI WATTS - SECURE MESH VPN PROVISIONING\033[0m"
echo -e "\033[36m=====================================================================\033[0m"

# Check if Tailscale is installed
if ! command -v tailscale >/dev/null 2>&1; then
    echo -e "\033[33m[INFO] Tailscale VPN is not detected on your system PATH.\033[0m"
    echo "       A secure VPN is required to sync your mobile Android app"
    echo "       with this desktop hub remotely when not on the same Wi-Fi."
    echo ""
    read -p "Would you like to install Tailscale? (y/N): " response
    if [[ "$response" =~ ^[Yy](es)?$ ]]; then
        if [[ "$OSTYPE" == "darwin"* ]]; then
            echo "Installing Tailscale via Homebrew..."
            brew install tailscale
        elif command -v apt-get >/dev/null 2>&1; then
            echo "Installing Tailscale via apt..."
            curl -fsSL https://tailscale.com/install.sh | sh
        else
            echo -e "\033[31m[ERROR] Package manager not recognized. Please install manually from: https://tailscale.com\033[0m"
        fi
    else
        echo -e "\033[33m[INFO] Setup skipped. Please set up your mesh VPN tunnel manually.\033[0m"
    fi
else
    echo -e "\033[32m[OK] Tailscale binary detected.\033[0m"
    tailscale status
    
    echo ""
    echo "To log in and link this device to your mesh network, run:"
    echo -e "  \033[36;1msudo tailscale up\033[0m"
    echo ""
    echo "To obtain the IP of your devices for sync, run:"
    echo -e "  \033[36mtailscale ip\033[0m"
fi

echo -e "\033[36m=====================================================================\033[0m"
