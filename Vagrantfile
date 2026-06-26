Vagrant.configure("2") do |config|
  config.vm.box = "debian/bookworm64"

  # Configure Bridged Networking to receive its own IP directly from the local router
  config.vm.network "public_network"

  # VM Specs per Phase 1 of README: 2 vCPUs, 2GB RAM
  config.vm.provider "virtualbox" do |vb|
    vb.name = "DJ-Midi-Watts-Cloud"
    vb.memory = "2048"
    vb.cpus = 2
  end

  # Phase 2 & 3: Provision Storage Layers and Tailscale VPN
  config.vm.provision "shell", inline: <<-SHELL
    export DEBIAN_FRONTEND=noninteractive
    echo "Updating system..."
    apt-get update -y
    
    echo "Installing Snap and Curl..."
    apt-get install -y snapd curl
    
    echo "Deploying Cloud Core System (Nextcloud)..."
    snap install core
    snap install nextcloud
    
    echo "Installing Tailscale VPN..."
    curl -fsSL https://tailscale.com/install.sh | sh
    
    echo "========================================================"
    echo "Provisioning Complete!"
    echo "Next steps:"
    echo "1. Type 'vagrant ssh' to connect to this VM."
    echo "2. Run 'sudo tailscale up' to authenticate the tunnel."
    echo "3. Access Nextcloud via the VM's assigned IP address."
    echo "========================================================"
  SHELL
end
