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


def download_better_player_model():
    """Download Better Player Model (BPM) from Modrinth into run/mods so animations execute."""
    mods_dir = os.path.join("run", "mods")
    os.makedirs(mods_dir, exist_ok=True)

    for file in os.listdir(mods_dir):
        if ("better" in file.lower() and "player" in file.lower() and "model" in file.lower()) and file.endswith(".jar"):
            return

    print("Better Player Model not found. Downloading via Modrinth API...")
    url = "https://api.modrinth.com/v2/project/betterplayermodel/version?loaders=%5B%22fabric%22%5D"
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
            print("Warning: Could not resolve a compatible Better Player Model version. Continuing without it.")
            return

        file_info = selected_version["files"][0]
        dest_file = os.path.join(mods_dir, file_info["filename"])

        print(f"Downloading Better Player Model {selected_version['version_number']}...")
        urllib.request.urlretrieve(file_info["url"], dest_file)
        print(f"Downloaded: {dest_file}")

    except Exception as e:
        print(f"Warning: Better Player Model download failed: {e}")


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


def sync_universal_animations():
    """
    Automatically syncs the Universal Player Animation Library into the Minecraft run configuration
    directory so Better Player Model (BPM) loads it immediately upon launch.
    """
    bpm_root = os.path.join("Animated", "UniversalAnimations", "BPM")
    if not os.path.exists(bpm_root):
        print("[!] Universal animations BPM folder not found at:", bpm_root)
        return

    dest_roots = [
        os.path.join("run", "config", "better_player_model", "custom"),
    ]

    appdata = os.environ.get("APPDATA")
    if appdata and os.path.exists(os.path.join(appdata, ".minecraft")):
        dest_roots.append(os.path.join(appdata, ".minecraft", "config", "better_player_model", "custom"))

    # Sync every race/model folder
    model_count = 0
    for model_folder in os.listdir(bpm_root):
        model_src = os.path.join(bpm_root, model_folder)
        if os.path.isdir(model_src):
            model_count += 1
            for droot in dest_roots:
                dst = os.path.join(droot, model_folder)
                try:
                    os.makedirs(dst, exist_ok=True)
                    shutil.copytree(model_src, dst, dirs_exist_ok=True)
                except Exception as e:
                    print(f"[!] Warning: Could not sync {model_folder} to {dst}: {e}")

    # Ensure built/default is also updated with Yardrat
    yardrat_src = os.path.join(bpm_root, "universal_humanoid")
    for b_dst in [
        os.path.join("run", "config", "better_player_model", "built", "default"),
        os.path.join(appdata, ".minecraft", "config", "better_player_model", "built", "default") if appdata and os.path.exists(os.path.join(appdata, ".minecraft")) else None
    ]:
        if b_dst:
            try:
                os.makedirs(b_dst, exist_ok=True)
                shutil.copytree(yardrat_src, b_dst, dirs_exist_ok=True)
            except Exception:
                pass

    print(f"[+] Auto-synced all {model_count} DBA race model packs to BPM custom and built/default folders")

    # Clean up accidental duplicate run/run directory
    run_run = os.path.join("run", "run")
    if os.path.exists(run_run):
        try:
            shutil.rmtree(run_run)
        except Exception:
            pass

    # Clear BPM cache so models recompile cleanly
    cache_dir = os.path.join("run", "config", "better_player_model", "cache")
    if os.path.exists(cache_dir):
        try:
            shutil.rmtree(cache_dir)
        except Exception:
            pass

    # Remove built-in anime / misc models if extracted
    stale_built = [
        os.path.join("run", "config", "better_player_model", "built", "wine_fox"),
        os.path.join("run", "config", "better_player_model", "built", "misc"),
    ]
    for p in stale_built:
        if os.path.exists(p):
            try:
                shutil.rmtree(p)
                print(f"[+] Cleaned up unwanted built-in model pack: {p}")
            except Exception:
                pass

    # Ensure blacklist.txt disables all built-in models from jar extraction
    blacklist_path = os.path.join("run", "config", "better_player_model", "blacklist.txt")
    if os.path.exists(blacklist_path):
        try:
            with open(blacklist_path, "r", encoding="utf-8", errors="ignore") as f:
                bl_text = f.read()
            if "assets/better_player_model/builtin/.*" not in bl_text:
                with open(blacklist_path, "a", encoding="utf-8") as f:
                    f.write("\n# Enabled Blacklist Rules for Dragon Block Arcane:\nassets/better_player_model/builtin/.*\nassets/better_player_model/builtin/default/.*\nassets/better_player_model/builtin/wine_fox/.*\nassets/better_player_model/builtin/misc/.*\n")
                print("[+] Updated BPM blacklist.txt with active rules")
        except Exception as e:
            print(f"[!] Warning: Could not update blacklist.txt: {e}")

    # Enforce default model and hide unwanted models in better_player_model-server.toml
    bpm_server_toml = os.path.join("run", "config", "better_player_model-server.toml")
    try:
        if os.path.exists(bpm_server_toml):
            with open(bpm_server_toml, "r", encoding="utf-8") as f:
                content = f.read()
            changed = False
            if 'DefaultModelId = "default"' in content:
                content = content.replace('DefaultModelId = "default"', 'DefaultModelId = "custom:universal_humanoid"')
                changed = True
            if 'ClientNotDisplayModels = []' in content:
                content = content.replace('ClientNotDisplayModels = []', 'ClientNotDisplayModels = ["built:wine_fox/.*", "built:misc/.*"]')
                changed = True
            if changed:
                with open(bpm_server_toml, "w", encoding="utf-8") as f:
                    f.write(content)
                print("[+] Configured BPM server default model and display filter")
    except Exception as e:
        print(f"[!] Warning: Could not configure BPM server toml: {e}")

    # Disable awkward HUD extra player puppet in better_player_model-client.toml
    bpm_client_toml = os.path.join("run", "config", "better_player_model-client.toml")
    try:
        if os.path.exists(bpm_client_toml):
            with open(bpm_client_toml, "r", encoding="utf-8") as f:
                c_content = f.read()
            if "DisablePlayerRender = false" in c_content:
                c_content = c_content.replace("DisablePlayerRender = false", "DisablePlayerRender = true")
                with open(bpm_client_toml, "w", encoding="utf-8") as f:
                    f.write(c_content)
                print("[+] Disabled BPM HUD extra player overlay")
    except Exception as e:
        print(f"[!] Warning: Could not configure BPM client toml: {e}")

    # Migrate any existing world saves to use custom:universal_humanoid
    old_tag = b'\x08\x00\x08model_id\x00\x07default'
    new_tag = b'\x08\x00\x08model_id\x00\x1acustom:universal_humanoid'
    import gzip
    for root, dirs, files in os.walk(os.path.join("run", "saves")):
        for f in files:
            if f.endswith(".dat") or f.endswith(".dat_old"):
                full = os.path.join(root, f)
                try:
                    with gzip.open(full, "rb") as fp:
                        d = fp.read()
                    if old_tag in d:
                        d = d.replace(old_tag, new_tag)
                        with gzip.open(full, "wb") as fp:
                            fp.write(d)
                except Exception:
                    pass


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
