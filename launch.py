#!/usr/bin/env python3
"""
Dragon Block Arcane DBA — Launch Script
Builds the mod (if needed) and launches the Minecraft client via Gradle runClient.
Supports custom usernames, offline UUIDs, and local IP detection for LAN multiplayer.
"""
import os
import sys
import json
import shutil
import urllib.request
import subprocess
import socket


def verify_java():
    """Verify that JAVA_HOME is set and points to a valid JDK installation."""
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        print("ERROR: JAVA_HOME is not set.")
        print("Please set JAVA_HOME to your JDK 25 installation directory.")
        print("Example (PowerShell): [Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\\Program Files\\Java\\jdk-25', 'User')")
        sys.exit(1)

    java_exe = os.path.join(java_home, "bin", "java.exe")
    if not os.path.exists(java_exe):
        java_exe = os.path.join(java_home, "bin", "java")
    if not os.path.exists(java_exe):
        print(f"ERROR: JAVA_HOME is set to '{java_home}' but no java binary was found.")
        sys.exit(1)

    print(f"Using JAVA_HOME: {java_home}")


def save_username(file_path, name):
    """Save player name to persistent file."""
    try:
        os.makedirs(os.path.dirname(file_path), exist_ok=True)
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(name.strip())
    except Exception:
        pass


def resolve_username():
    """Determine the username from CLI args, prompt, or saved file."""
    saved_file = os.path.join("run", "player_name.txt")
    saved_name = "Player"
    if os.path.exists(saved_file):
        try:
            with open(saved_file, "r", encoding="utf-8") as f:
                content = f.read().strip()
                if content:
                    saved_name = content
        except Exception:
            pass

    # Check CLI arguments
    args = sys.argv[1:]
    for i, arg in enumerate(args):
        if arg in ("-u", "--username", "--name") and i + 1 < len(args):
            chosen = args[i + 1].strip()
            if chosen:
                save_username(saved_file, chosen)
                return chosen
        elif arg.startswith("--username="):
            chosen = arg.split("=", 1)[1].strip()
            if chosen:
                save_username(saved_file, chosen)
                return chosen
        elif arg.startswith("--name="):
            chosen = arg.split("=", 1)[1].strip()
            if chosen:
                save_username(saved_file, chosen)
                return chosen
        elif not arg.startswith("-") and arg not in ("build", "runClient"):
            # Positional argument (e.g. `python launch.py Bob`)
            save_username(saved_file, arg.strip())
            return arg.strip()

    # If interactive console and no CLI username specified, prompt user
    if sys.stdin.isatty():
        try:
            print(f"\nEnter player username (press Enter for '{saved_name}'): ", end="", flush=True)
            user_input = sys.stdin.readline().strip()
            if user_input:
                saved_name = user_input
                save_username(saved_file, saved_name)
        except (EOFError, KeyboardInterrupt):
            pass

    return saved_name


def get_local_ips():
    """Detect local LAN IPv4 addresses."""
    ips = set()
    try:
        hostname = socket.gethostname()
        for ip in socket.gethostbyname_ex(hostname)[2]:
            if not ip.startswith("127.") and not ip.startswith("169.254."):
                ips.add(ip)
    except Exception:
        pass
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        if not ip.startswith("127."):
            ips.add(ip)
        s.close()
    except Exception:
        pass
    return sorted(list(ips))


def download_modmenu():
    """Download ModMenu from Modrinth into the run/mods directory if not already present."""
    mods_dir = os.path.join("run", "mods")
    os.makedirs(mods_dir, exist_ok=True)

    for file in os.listdir(mods_dir):
        if file.lower().startswith("modmenu") and file.endswith(".jar"):
            return

    print("ModMenu not found. Downloading via Modrinth API...")
    url = "https://api.modrinth.com/v2/project/modmenu/version?loaders=%5B%22fabric%22%5D"
    headers = {"User-Agent": "DBA-Launcher/1.0"}

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=15) as response:
            versions = json.loads(response.read().decode())

        selected_version = None
        for v in versions:
            if "26.2" in v.get("game_versions", []):
                selected_version = v
                break

        if not selected_version and versions:
            selected_version = versions[0]

        if not selected_version:
            print("Warning: Could not resolve a compatible ModMenu version. Continuing without it.")
            return

        file_info = selected_version["files"][0]
        dest_file = os.path.join(mods_dir, file_info["filename"])

        print(f"Downloading ModMenu {selected_version['version_number']}...")
        urllib.request.urlretrieve(file_info["url"], dest_file)
        print(f"Downloaded: {dest_file}")

    except Exception as e:
        print(f"Warning: ModMenu download failed: {e}")


def download_geckolib():
    """Download GeckoLib from Modrinth into the run/mods directory if not already present."""
    mods_dir = os.path.join("run", "mods")
    os.makedirs(mods_dir, exist_ok=True)

    for file in os.listdir(mods_dir):
        if file.lower().startswith("geckolib") and file.endswith(".jar"):
            return

    print("GeckoLib not found. Downloading via Modrinth API...")
    url = "https://api.modrinth.com/v2/project/geckolib/version?loaders=%5B%22fabric%22%5D"
    headers = {"User-Agent": "DBA-Launcher/1.0"}

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=15) as response:
            versions = json.loads(response.read().decode())

        selected_version = None
        for v in versions:
            if "26.2" in v.get("game_versions", []):
                selected_version = v
                break

        if not selected_version and versions:
            selected_version = versions[0]

        if not selected_version:
            print("Warning: Could not resolve a compatible GeckoLib version. Continuing without it.")
            return

        file_info = selected_version["files"][0]
        dest_file = os.path.join(mods_dir, file_info["filename"])

        print(f"Downloading GeckoLib {selected_version['version_number']}...")
        urllib.request.urlretrieve(file_info["url"], dest_file)
        print(f"Downloaded: {dest_file}")

    except Exception as e:
        print(f"Warning: GeckoLib download failed: {e}")


def nuke_caches():
    """Delete stale Gradle/Loom caches to force a clean resolve."""
    for folder in [".gradle", ".fabric"]:
        path = os.path.abspath(folder)
        if os.path.exists(path):
            print(f"Cleaning cache: {path}")
            try:
                shutil.rmtree(path)
            except Exception as e:
                print(f"Warning: Could not remove {path}: {e}")


def launch_client():
    verify_java()
    download_modmenu()
    download_geckolib()

    username = resolve_username()
    local_ips = get_local_ips()
    primary_ip = local_ips[0] if local_ips else "192.168.x.x"

    print("\n" + "=" * 64)
    print(f"  Dragon Block Arcane DBA — Minecraft Launcher")
    print(f"  Active Profile: {username}")
    print("=" * 64)
    if local_ips:
        print("  Your Local Network IP Address(es) for LAN:")
        for ip in local_ips:
            print(f"    -> {ip}")
    print("----------------------------------------------------------------")
    print("  [HOW TO HOST A LAN GAME]")
    print("    1. Enter your singleplayer world.")
    print("    2. Press ESC -> 'Open to LAN' -> configure -> 'Start LAN World'.")
    print(f"    3. Share your IP and Port with friends (e.g. {primary_ip}:54321).")
    print("")
    print("  [HOW TO JOIN A LAN GAME]")
    print(f"    1. Each player should launch with their own name:")
    print(f"       python launch.py <PlayerName>")
    print(f"    2. Go to 'Multiplayer' -> 'Direct Connection'.")
    print(f"    3. Enter Host IP and Port (e.g. {primary_ip}:54321) -> 'Join Server'.")
    print("=" * 64 + "\n")

    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    if not os.path.exists(gradlew.replace("./", "")):
        print(f"ERROR: {gradlew} not found in the current directory.")
        sys.exit(1)

    # Build first if --build flag is passed
    if "--build" in sys.argv:
        print("Building mod before launching...")
        build_result = subprocess.run([gradlew, "build", "--stacktrace"], capture_output=False, text=True)
        if build_result.returncode != 0:
            print("ERROR: Build failed! Fix errors before launching.")
            sys.exit(build_result.returncode)

    gradle_cmd = [gradlew, "runClient", f"-Pusername={username}"]
    print(f"Starting Minecraft client as '{username}' via Gradle runClient...")
    try:
        result = subprocess.run(gradle_cmd, capture_output=False)
        sys.exit(result.returncode)
    except KeyboardInterrupt:
        print("\nClient terminated by user.")
        sys.exit(0)
    except Exception as e:
        print(f"ERROR: Failed to launch client: {e}")
        sys.exit(1)


if __name__ == "__main__":
    if "--nuke-caches" in sys.argv:
        nuke_caches()

    launch_client()
