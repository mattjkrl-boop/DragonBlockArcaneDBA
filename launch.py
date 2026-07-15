#!/usr/bin/env python3
"""
Dragon Block Arcane DBA — Launch Script
Builds the mod (if needed) and launches the Minecraft client via Gradle runClient.
Automatically downloads ModMenu into run/mods if not already present.
"""
import os
import sys
import json
import shutil
import urllib.request
import subprocess


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


def download_modmenu():
    """Download ModMenu from Modrinth into the run/mods directory if not already present."""
    mods_dir = os.path.join("run", "mods")
    os.makedirs(mods_dir, exist_ok=True)

    # Check if any modmenu jar is already downloaded
    for file in os.listdir(mods_dir):
        if file.lower().startswith("modmenu") and file.endswith(".jar"):
            print(f"ModMenu already present: {file}")
            return

    print("ModMenu not found. Downloading via Modrinth API...")
    url = "https://api.modrinth.com/v2/project/modmenu/version?loaders=%5B%22fabric%22%5D"
    headers = {"User-Agent": "DBA-Launcher/1.0"}

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=15) as response:
            versions = json.loads(response.read().decode())

        # Try to find a version matching minecraft 26.2
        selected_version = None
        for v in versions:
            if "26.2" in v.get("game_versions", []):
                selected_version = v
                break

        # Fallback to latest
        if not selected_version and versions:
            selected_version = versions[0]

        if not selected_version:
            print("Warning: Could not resolve a compatible ModMenu version. Continuing without it.")
            return

        file_info = selected_version["files"][0]
        download_url = file_info["url"]
        filename = file_info["filename"]
        dest_file = os.path.join(mods_dir, filename)

        print(f"Downloading ModMenu {selected_version['version_number']}...")
        urllib.request.urlretrieve(download_url, dest_file)
        print(f"Downloaded: {dest_file}")

    except Exception as e:
        print(f"Warning: ModMenu download failed: {e}")
        print("Continuing without ModMenu. You can install it manually in 'run/mods/'.")


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
    print("=========================================")
    print("Launching Dragon Block Arcane DBA Client")
    print("=========================================")

    verify_java()
    download_modmenu()

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

    print("Starting Minecraft client via runClient...")
    try:
        result = subprocess.run([gradlew, "runClient"], capture_output=False)
        sys.exit(result.returncode)
    except KeyboardInterrupt:
        print("\nClient terminated by user.")
        sys.exit(0)
    except Exception as e:
        print(f"ERROR: Failed to launch client: {e}")
        sys.exit(1)


if __name__ == "__main__":
    nuke = "--nuke-caches" in sys.argv

    if nuke:
        nuke_caches()

    launch_client()
