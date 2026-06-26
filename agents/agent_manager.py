import os
import sys
import subprocess
import re

# Ensure standard streams are configured correctly
if sys.platform.startswith('win'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
        sys.stderr.reconfigure(encoding='utf-8')
    except AttributeError:
        pass

def mask_value(val):
    if not val or val == 'VOID':
        return 'VOID'
    if len(val) <= 8:
        return '********'
    return val[:4] + '...' + val[-4:]

def get_env_variable(var_name, env_dict):
    # Try local dict first, then os.environ
    return env_dict.get(var_name) or os.environ.get(var_name)

def read_env_file(filepath):
    vars_dict = {}
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if not line or line.startswith('#'):
                    continue
                if '=' not in line:
                    continue
                name, val = line.split('=', 1)
                vars_dict[name.strip()] = val.strip().strip('"\'')
    return vars_dict

def update_env_file(filepath, new_vars):
    if not os.path.exists(filepath):
        # Create a new one
        with open(filepath, 'w', encoding='utf-8') as f:
            for k, v in new_vars.items():
                f.write(f'{k}="{v}"\n')
        return

    # Update existing lines
    lines = []
    updated = set()
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            stripped = line.strip()
            if stripped and not stripped.startswith('#') and '=' in stripped:
                name, val = stripped.split('=', 1)
                name = name.strip()
                if name in new_vars:
                    lines.append(f'{name}="{new_vars[name]}"\n')
                    updated.add(name)
                    continue
            lines.append(line)
    
    # Add new vars that weren't in the file
    with open(filepath, 'w', encoding='utf-8') as f:
        for line in lines:
            f.write(line)
        for k, v in new_vars.items():
            if k not in updated:
                f.write(f'{k}="{v}"\n')

def run_cmd(args):
    try:
        res = subprocess.run(args, capture_output=True, text=True, check=True)
        return res.stdout.strip()
    except subprocess.CalledProcessError as e:
        return None

def main():
    print("=====================================================================")
    print("        DJ MIDI WATTS - Google Cloud Agent & Setup Manager")
    print("=====================================================================")
    
    # Resolve Paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, os.pardir))
    
    desktop_env_path = os.path.join(project_root, 'desktop', '.env')
    root_env_path = os.path.join(project_root, '.env')
    user_env_path = os.path.join(project_root, 'user.env')
    
    # Read existing variables
    env_vars = {}
    env_vars.update(read_env_file(desktop_env_path))
    env_vars.update(read_env_file(root_env_path))
    env_vars.update(read_env_file(user_env_path))
    
    # Target Setup Configurations
    settings = {
        'GCP_PROJECT_ID': {'prompt': 'GCP Project ID', 'default': 'dj-midi-watts', 'is_secret': False},
        'UG_S1': {'prompt': 'Gemini AI Orchestration Key (UG_S1)', 'default': '', 'is_secret': True},
        'UG_S2': {'prompt': 'Google OAuth Client ID (UG_S2)', 'default': '', 'is_secret': True},
        'PLAY_API_KEY': {'prompt': 'Google Play API Key', 'default': '', 'is_secret': True},
        'CHROME_CLIENT_ID': {'prompt': 'Chrome Client ID', 'default': '', 'is_secret': False},
        'CHROME_CLIENT_SECRET': {'prompt': 'Chrome Client Secret', 'default': '', 'is_secret': True},
        'CHROME_WEBSTORE_TOKEN': {'prompt': 'Chrome Webstore Token', 'default': 'https://oauth2.googleapis.com/token', 'is_secret': False},
        'CODE_SIGN_CERT': {'prompt': 'Code Signing Certificate Path', 'default': 'certs/code-sign.pfx', 'is_secret': False},
        'CODE_SIGN_PASS': {'prompt': 'Code Signing Password', 'default': 'YOUR_CODE_SIGN_PASSWORD', 'is_secret': True},
        'UG_S3_KEYSTORE_PASS': {'prompt': 'Keystore Password (UG_S3_KEYSTORE_PASS)', 'default': 'android', 'is_secret': True},
    }
    
    final_configs = {}
    
    print("\n--- Gather Configurations ---")
    for key, spec in settings.items():
        existing = get_env_variable(key, env_vars)
        display_val = mask_value(existing) if spec['is_secret'] and existing else existing
        
        prompt_str = f"{spec['prompt']}"
        if display_val:
            prompt_str += f" [Current: {display_val}]"
        elif spec['default']:
            prompt_str += f" [Default: {spec['default']}]"
            
        val = input(f"{prompt_str}: ").strip()
        
        if not val:
            val = existing if existing else spec['default']
            
        final_configs[key] = val

    # Verify GCP configuration
    print("\n--- GCP Environment Verification Check ---")
    print(f"Setting active project context to '{final_configs['GCP_PROJECT_ID']}'...")
    
    # Attempt project config set
    res = run_cmd(['gcloud', 'config', 'set', 'project', final_configs['GCP_PROJECT_ID']])
    if res is not None:
        print("✅ GCP Project context configured.")
    else:
        print("⚠️ Warning: Could not configure gcloud project context. Ensure Google Cloud SDK is installed.")

    # Check active gcloud identity
    identity = run_cmd(['gcloud', 'config', 'get-value', 'account'])
    if identity:
        print(f"✅ Active GCP Identity discovered: {identity}")
    else:
        print("❌ Error: No active Google Cloud authentication found. Run 'gcloud auth login' first.")

    # Check Secret Manager API
    api_check = run_cmd(['gcloud', 'services', 'list', '--enabled', '--filter=name:secretmanager.googleapis.com', '--format=value(name)'])
    if api_check and 'secretmanager.googleapis.com' in api_check:
        print("✅ Secret Manager API is enabled.")
    else:
        print("⚠️ Warning: Secret Manager API is not enabled or could not be verified.")

    # Confirm Permissions Prior to Action
    print("\n=====================================================================")
    print("  CONFIRMATION OF PERMISSIONS")
    print("=====================================================================")
    print("Do you grant permission to:")
    print("1. Write setup variables into .env files")
    print("2. Cache secret variables securely into the OS Windows Credential Vault")
    print("=====================================================================")
    confirm = input("Confirm permissions and write environment? (y/N): ").strip().lower()
    
    if confirm not in ['y', 'yes']:
        print("❌ Action aborted by user. Settings have not been written.")
        sys.exit(0)
        
    print("\n▶️ Provisioning settings...")

    # Write Plaintext to user.env (restricted local access)
    update_env_file(user_env_path, final_configs)
    print(f"✅ Plaintext configuration saved locally to: user.env")

    # Cache securely in Windows Vault using the provision utility
    provision_script = os.path.join(project_root, 'scripts', 'provision_credentials.ps1')
    if os.path.exists(provision_script):
        # Trigger powershell execution
        ps_cmd = [
            'powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', provision_script,
            '-S1_Key', final_configs['UG_S1'],
            '-S2_Key', final_configs['UG_S2']
        ]
        try:
            subprocess.run(ps_cmd, check=True)
            print("✅ Secrets securely cached in Windows Credential Manager.")
        except subprocess.CalledProcessError as e:
            print(f"⚠️ Warning: Vault provisioning script returned error: {e}")
    else:
        print("⚠️ Warning: Provisioning script 'provision_credentials.ps1' not found. Skipping Vault cache.")

    # Mask sensitive variables for standard .env files
    masked_configs = final_configs.copy()
    for key, spec in settings.items():
        if spec['is_secret']:
            masked_configs[key] = mask_value(final_configs[key])

    # Write masked representations to public-facing files
    update_env_file(root_env_path, masked_configs)
    update_env_file(desktop_env_path, masked_configs)
    print("✅ Masked configuration stored in workspace .env and desktop/.env files.")
    print("\n🎉 Setup & Secure Provisioning Completed successfully!")

if __name__ == '__main__':
    main()
