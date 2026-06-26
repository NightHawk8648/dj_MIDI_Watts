const { spawn, execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

// Load central path configuration
const configPath = path.join(__dirname, '..', 'config_paths.json');
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

console.log('🚀 Starting DJ MIDI WATTS Desktop Hub...');

// Helper to parse environment file
function parseEnvFile(filePath) {
    const env = {};
    if (!fs.existsSync(filePath)) return env;
    const content = fs.readFileSync(filePath, 'utf8');
    content.split(/\r?\n/).forEach(line => {
        const trimmed = line.trim();
        if (trimmed && !trimmed.startsWith('#')) {
            const parts = trimmed.split('=');
            if (parts.length >= 2) {
                const key = parts[0].trim();
                let val = parts.slice(1).join('=').trim();
                if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'"))) {
                    val = val.substring(1, val.length - 1);
                }
                env[key] = val;
            }
        }
    });
    return env;
}

// Helper to read WIF configuration
function readWifConfig(filePath) {
    if (!fs.existsSync(filePath)) return {};
    try {
        return JSON.parse(fs.readFileSync(filePath, 'utf8'));
    } catch (e) {
        return {};
    }
}

// Helper to retrieve secure secrets from the Windows Credential Vault
function getSecretFromVault(resource, username) {
    if (process.platform !== 'win32') return '';
    try {
        const cmd = `[Windows.Security.Credentials.PasswordVault, Windows.Security.Credentials, ContentType=WindowsRuntime] | Out-Null; $v = New-Object Windows.Security.Credentials.PasswordVault; Write-Host -NoNewline $v.Retrieve('${resource}', '${username}').Password`;
        const result = execSync(`powershell -Command "${cmd}"`, { stdio: ['ignore', 'pipe', 'ignore'] });
        return result.toString().trim();
    } catch (e) {
        return '';
    }
}

// Bootstrap Universal Environments
const projectRoot = path.resolve(__dirname, '..');
const rootDotEnv = parseEnvFile(path.join(projectRoot, '.env'));
const cloudEnv = parseEnvFile(path.join(projectRoot, 'cloud_env.env'));
const wifConfig = readWifConfig(path.join(projectRoot, 'wif-config.json'));

// Fetch Vault Keys if placeholder/missing
let ugS1 = rootDotEnv.UG_S1 || process.env.UG_S1 || '';
if (!ugS1 || ugS1 === 'S1_VOID') {
    ugS1 = getSecretFromVault('UltimaGrid', 'UG_S1');
}
let ugS2 = rootDotEnv.UG_S2 || process.env.UG_S2 || '';
if (!ugS2 || ugS2 === 'S2_VOID') {
    ugS2 = getSecretFromVault('UltimaGrid', 'UG_S2');
}

console.log('---------------------------------------------------------');
console.log(`[CONFIG] GCP Project ID  : ${cloudEnv.GCP_PROJECT_ID || 'dj-midi-watts'}`);
console.log(`[CONFIG] GCP Region      : ${cloudEnv.GCP_REGION || 'us-central1'}`);
console.log(`[CONFIG] WIF OAuth Client: ${wifConfig.attributeCondition || 'NOT_FOUND'}`);
console.log(`[CONFIG] Vault Credentials: ${ugS1 ? 'UG_S1 (VERIFIED)' : 'UG_S1 (MISSING)'} | ${ugS2 ? 'UG_S2 (VERIFIED)' : 'UG_S2 (MISSING)'}`);
console.log('---------------------------------------------------------');


// Platform / Device / Architecture detection
console.log(`💻 Detected platform: ${process.platform} (${process.arch})`);

let phpCmd = 'php';

// Verify PHP is available
try {
    execSync(`${phpCmd} -v`, { stdio: 'ignore' });
    console.log('✅ PHP Runtime found on PATH.');
} catch (e) {
    // Check custom WinGet path
    const localAppData = process.env.LOCALAPPDATA || (process.env.USERPROFILE ? path.join(process.env.USERPROFILE, 'AppData', 'Local') : '');
    const wingetPackagesDir = path.join(localAppData, 'Microsoft', 'WinGet', 'Packages');
    if (fs.existsSync(wingetPackagesDir)) {
        const pkgs = fs.readdirSync(wingetPackagesDir);
        const phpDir = pkgs.find(p => p.startsWith('PHP.PHP.'));
        if (phpDir) {
            const fallbackPath = path.join(wingetPackagesDir, phpDir, 'php.exe');
            if (fs.existsSync(fallbackPath)) {
                phpCmd = fallbackPath;
                console.log(`✅ PHP Runtime found at fallback path: ${phpCmd}`);
            }
        }
    }
    
    if (phpCmd === 'php') {
        console.error('❌ PHP is required for the local Desktop backend. Please install PHP.');
        process.exit(1);
    }
}

// Spawn PHP server on Port defined by Cloud Run or 8000
const port = process.env.PORT || 8000;
const host = process.env.K_SERVICE ? '0.0.0.0' : 'localhost';
const phpServer = spawn(phpCmd, ['-S', `${host}:${port}`, 'router.php'], {
    cwd: __dirname
});

phpServer.stdout.on('data', (data) => console.log(`[PHP] ${data}`));
phpServer.stderr.on('data', (data) => console.error(`[PHP Error] ${data}`));

console.log(`🌐 Web UI hosted at: http://${host}:${port}`);

// Attempt to find Google Chrome on Windows to load the unpacked extension automatically
let openedWithChrome = false;

if (process.env.K_SERVICE) {
    console.log('☁️ Running in Cloud Run environment. Skipping browser launch.');
    openedWithChrome = true; // Skip default browser fallback
} else if (process.platform === 'win32') {
    const chromePaths = [
        path.join(process.env.PROGRAMFILES || 'C:\\Program Files', 'Google', 'Chrome', 'Application', 'chrome.exe'),
        path.join(process.env['PROGRAMFILES(X86)'] || 'C:\\Program Files (x86)', 'Google', 'Chrome', 'Application', 'chrome.exe'),
        path.join(process.env.LOCALAPPDATA || path.join(process.env.USERPROFILE || 'C:\\', 'AppData', 'Local'), 'Google', 'Chrome', 'Application', 'chrome.exe')
    ];
    
    const chromeExe = chromePaths.find(p => p && fs.existsSync(p));
    if (chromeExe) {
        const extPath = path.resolve(__dirname, '../dj-midi-watts-extension');
        console.log(`🌐 Launching Chrome with unpacked extension loaded from: ${extPath}`);
        try {
            spawn(chromeExe, [
                `--load-extension=${extPath}`,
                `http://localhost:${port}`
            ], { detached: true, stdio: 'ignore' }).unref();
            openedWithChrome = true;
        } catch (err) {
            console.warn('⚠️ Failed to launch Chrome directly, falling back...');
        }
    }
}

if (!openedWithChrome) {
    console.log('🌐 Opening default system browser...');
    import('open').then(openModule => {
        openModule.default(`http://localhost:${port}`);
    }).catch(err => {
        console.error(`Failed to auto-open browser, please open http://localhost:${port} manually.`);
    });
}

// Clean up child processes on exit
process.on('SIGINT', () => {
    phpServer.kill();
    process.exit(0);
});
