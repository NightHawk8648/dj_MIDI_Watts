import os
import sys
import subprocess
import argparse
from pathlib import Path
import concurrent.futures

LEGAL_PROMPT = """
================================================================================
⚠️  COPYRIGHT & LEGAL WARNING ⚠️
================================================================================
WARNING: The reproduction, distribution, and resale of copyrighted music without 
explicit authorization from the copyright holder is a violation of international 
copyright laws (e.g., DMCA). Penalties can include severe civil fines and 
criminal prosecution.

DJ MIDI Watts does not condone or facilitate piracy. This media conversion tool 
is strictly for fair-use analysis, original creation, and authorized remixing 
of media you legally own or have permission to use.
================================================================================
"""

def print_legal_prompt():
    print(LEGAL_PROMPT)
    print("Type 'I AGREE' to confirm you understand and accept full legal responsibility.")
    print("Or press Enter to cancel and exit.")
    
    response = input("> ")
    if response.strip() != "I AGREE":
        print("Consent not provided. Exiting.")
        sys.exit(1)
    print("Consent recorded. Proceeding...")

def check_ffmpeg():
    try:
        subprocess.run(["ffmpeg", "-version"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    except FileNotFoundError:
        print("ERROR: FFmpeg is not installed or not found in system PATH.")
        print("Please install FFmpeg to use this converter.")
        sys.exit(1)

def get_best_video_encoder():
    try:
        result = subprocess.run(["ffmpeg", "-encoders"], stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        encoders = result.stdout
        
        # Check AMD first since user specified it, then NVIDIA, then Intel
        if "h264_amf" in encoders:
            return "h264_amf"
        elif "h264_nvenc" in encoders:
            return "h264_nvenc"
        elif "h264_qsv" in encoders:
            return "h264_qsv"
        
        return "libx264" # CPU fallback
    except Exception:
        return "libx264"

def convert_single_file(input_path, output_dir, output_format, video_encoder):
    output_ext = f".{output_format.lower()}"
    output_path = output_dir / f"{input_path.stem}_converted{output_ext}"
    
    cmd = ["ffmpeg", "-i", str(input_path), "-y"]
    
    if output_format.lower() == "mp3":
        cmd.extend(["-b:a", "320k"])
    elif output_format.lower() == "wav":
        cmd.extend(["-c:a", "pcm_s16le", "-ar", "44100"])
    elif output_format.lower() == "flac":
        cmd.extend(["-c:a", "flac"])
    elif output_format.lower() == "mp4":
        if video_encoder != "libx264":
            # Hardware accelerated encode
            cmd.extend(["-c:v", video_encoder, "-preset", "fast", "-c:a", "aac", "-b:a", "192k"])
            # AMF uses -qp instead of -cq for constant quality in some versions, 
            # but -rc cqp -qp_p 22 is safer for AMF. 
            # To keep it generic across NVENC/AMF/QSV without breaking, we'll use basic high bitrate or default VBR
            cmd.extend(["-b:v", "5M"])
        else:
            # CPU fallback
            cmd.extend(["-c:v", "libx264", "-preset", "fast", "-crf", "22", "-c:a", "aac", "-b:a", "192k"])
    else:
        return f"Unsupported format: {output_format}"

    cmd.append(str(output_path))
    
    print(f"Converting {input_path.name} to {output_format.upper()}...")
    try:
        # Hide standard output unless there's an error
        subprocess.run(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, check=True, text=True)
        return f"Success: {input_path.name} -> {output_path.name}"
    except subprocess.CalledProcessError as e:
        return f"Failed: {input_path.name}. Error: {e.stderr.strip().splitlines()[-1] if e.stderr else 'Unknown Error'}"

def convert_media(input_target, output_format, output_dir=None):
    if not os.path.exists(input_target):
        print(f"Error: Target '{input_target}' not found.")
        return

    target_path = Path(input_target)
    
    if output_dir is None:
        out_dir = target_path if target_path.is_dir() else target_path.parent
    else:
        out_dir = Path(output_dir)
    
    out_dir.mkdir(parents=True, exist_ok=True)
    
    files_to_process = []
    if target_path.is_dir():
        valid_extensions = {".mp3", ".mp4", ".wav", ".flac", ".m4a", ".webm", ".mkv", ".ogg"}
        files_to_process = [f for f in target_path.iterdir() if f.is_file() and f.suffix.lower() in valid_extensions]
        if not files_to_process:
            print(f"No valid media files found in directory {target_path}")
            return
        print(f"Found {len(files_to_process)} media files in directory. Starting batch conversion...")
    else:
        files_to_process = [target_path]

    video_encoder = get_best_video_encoder()
    if output_format.lower() == "mp4":
        if video_encoder == "h264_amf":
            print("Hardware Acceleration: AMD AMF Enabled")
        elif video_encoder == "h264_nvenc":
            print("Hardware Acceleration: NVIDIA NVENC Enabled")
        elif video_encoder == "h264_qsv":
            print("Hardware Acceleration: Intel QSV Enabled")
        else:
            print("Hardware Acceleration: CPU Fallback (libx264)")

    # Use ThreadPoolExecutor for concurrent batch processing
    max_workers = min(4, os.cpu_count() or 1)
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        futures = [executor.submit(convert_single_file, f, out_dir, output_format, video_encoder) for f in files_to_process]
        for future in concurrent.futures.as_completed(futures):
            print(future.result())

def main():
    parser = argparse.ArgumentParser(description="DJ MIDI Watts Media Converter (Optimized)")
    parser.add_argument("input_target", help="Path to the source media file or directory for batch conversion")
    parser.add_argument("--format", choices=["mp3", "mp4", "wav", "flac"], required=True, help="Target conversion format")
    parser.add_argument("--output-dir", help="Optional output directory. Defaults to same directory as input.")
    parser.add_argument("--auto-agree", action="store_true", help="Automatically agree to legal terms (for UI automation)")
    
    args = parser.parse_args()
    
    if not args.auto_agree:
        print_legal_prompt()
        
    check_ffmpeg()
    convert_media(args.input_target, args.format, args.output_dir)

if __name__ == "__main__":
    main()
