// DJ MIDI WATTS - Cloud & Sync Hosting Choice Selector
// Interactively prompts user to select a cloud deployment model

const readline = require('readline');
const fs = require('fs');
const path = require('path');

const configPath = path.join(__dirname, '..', 'hosting_config.json');

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
});

console.clear();
console.log('=====================================================================');
console.log('         DJ MIDI WATTS - CLOUD SYNC DEPLOYMENT ORCHESTRATOR          ');
console.log('=====================================================================');
console.log('To synchronize your hardware faders, playlists, and presets across  ');
console.log('devices (desktop, mobile app, extension), you must choose a sync host.');
console.log('');
console.log('Please select your preferred deployment configuration:');
console.log('');
console.log(' [1] Google Cloud Platform Client (Managed SaaS)');
console.log('     * Requires monthly GCP subscription/membership billing.');
console.log('     * Built-in serverless hosting, databases, and monitoring.');
console.log('     * Easiest zero-configuration deployment.');
console.log('');
console.log(' [2] Self-Hosted Cloud (Decentralized & Free)');
console.log('     * Fully open-source, private, and secure.');
console.log('     * Deployable locally via IntelliPHP, WSL2 (Ubuntu), or Node/NPM.');
console.log('     * Recommended to run on a personal server or mesh VPN tunnel.');
console.log('');

function promptChoice() {
    rl.question('Enter choice [1 or 2]: ', (answer) => {
        const choice = answer.trim();
        
        if (choice === '1') {
            console.log('\n--> Selected Option: Google Cloud Client (Managed)');
            console.log('    Ensure GCP billing is active and secrets/google-play-service.json is provisioned.');
            saveConfig('gcp_managed');
        } else if (choice === '2') {
            console.log('\n--> Selected Option: Self-Hosted Server (Local/Private)');
            console.log('    System will bind local port listeners using IntelliPHP or Node NPM.');
            console.log('    We highly recommend setting up Tailscale VPN to secure connection bridging.');
            saveConfig('self_hosted');
        } else {
            console.log('\n[ERROR] Invalid choice. Please enter 1 or 2.');
            promptChoice();
        }
    });
}

function saveConfig(mode) {
    const config = {
        deployment_mode: mode,
        timestamp: new Date().toISOString(),
        updated_by: 'cloud_host_selector'
    };
    
    fs.writeFileSync(configPath, JSON.stringify(config, null, 2), 'utf8');
    console.log(`\n[SUCCESS] Configuration saved to: desktop/hosting_config.json`);
    console.log('=====================================================================');
    rl.close();
}

promptChoice();
