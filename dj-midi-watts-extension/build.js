const fs = require('fs');
const path = require('path');
const archiver = require('archiver');

const DIST_DIR = path.join(__dirname, 'dist');
const OUTPUT_ZIP = path.join(__dirname, 'dj-midi-watts-extension.zip');

// Files and folders to include
const INCLUDE_ASSETS = [
    'manifest.json',
    'background.js',
    'content.js',
    'popup.js',
    'popup.html',
    'rules.json',
    'README.md',
    '_metadata'
];

async function build() {
    console.log('Cleaning old build...');
    if (fs.existsSync(DIST_DIR)) {
        fs.rmSync(DIST_DIR, { recursive: true, force: true });
    }
    if (fs.existsSync(OUTPUT_ZIP)) {
        fs.rmSync(OUTPUT_ZIP, { force: true });
    }
    fs.mkdirSync(DIST_DIR, { recursive: true });

    console.log('Copying files to dist...');
    for (const asset of INCLUDE_ASSETS) {
        const sourcePath = path.join(__dirname, asset);
        const destPath = path.join(DIST_DIR, asset);
        
        if (fs.existsSync(sourcePath)) {
            const stats = fs.statSync(sourcePath);
            if (stats.isDirectory()) {
                fs.cpSync(sourcePath, destPath, { recursive: true });
            } else {
                fs.copyFileSync(sourcePath, destPath);
            }
            console.log(`Copied: ${asset}`);
        } else {
            console.warn(`Warning: Asset ${asset} not found, skipping.`);
        }
    }

    console.log('Creating zip archive...');
    const output = fs.createWriteStream(OUTPUT_ZIP);
    const archive = archiver('zip', { zlib: { level: 9 } });

    output.on('close', function() {
        console.log(`Archive created successfully: ${archive.pointer()} total bytes`);
    });

    archive.on('error', function(err) {
        throw err;
    });

    archive.pipe(output);
    archive.directory(DIST_DIR, false);
    await archive.finalize();
}

build().catch(err => {
    console.error('Build failed:', err);
    process.exit(1);
});
