#!/usr/bin/env python3
"""
Dragon Block Arcane DBA — Build Script
Compiles the mod and produces dragonblockarcanedba.jar in the project root.
"""
import os
import sys
import shutil
import subprocess


def verify_java():
    """Verify that JAVA_HOME is set and points to a valid JDK installation."""
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        print("ERROR: JAVA_HOME is not set.")
        print("Please set JAVA_HOME to your JDK 25 installation directory.")
        print("Example (PowerShell): [Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\\Program Files\\Java\\jdk-25', 'User')")
        sys.exit(1)

    javac = os.path.join(java_home, "bin", "javac.exe")
    if not os.path.exists(javac):
        javac = os.path.join(java_home, "bin", "javac")
    if not os.path.exists(javac):
        print(f"ERROR: JAVA_HOME is set to '{java_home}' but no javac binary was found.")
        sys.exit(1)

    print(f"Using JAVA_HOME: {java_home}")


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


def run_build(clean=True):
    print("=========================================")
    print("Building Dragon Block Arcane DBA Mod Jar")
    print("  (Requires GeckoLib 5.5.3 at runtime)")
    print("=========================================")

    verify_java()

    gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
    if not os.path.exists(gradlew.replace("./", "")):
        print(f"ERROR: {gradlew} not found in the current directory.")
        sys.exit(1)

    tasks = ["build"]
    if clean:
        tasks.insert(0, "clean")

    print(f"Running Gradle tasks: {' '.join(tasks)}")
    try:
        result = subprocess.run([gradlew] + tasks + ["--stacktrace"], capture_output=False, text=True)
        if result.returncode != 0:
            print("ERROR: Gradle build failed! Check errors above.")
            sys.exit(result.returncode)
    except Exception as e:
        print(f"ERROR: Failed to run Gradle: {e}")
        sys.exit(1)

    # Locate output jar
    libs_dir = os.path.join("build", "libs")
    if not os.path.exists(libs_dir):
        print("ERROR: build/libs directory was not created. Build must have failed.")
        sys.exit(1)

    # Find the remapped jar (exclude -sources and -dev variants)
    target_jar = None
    for file in os.listdir(libs_dir):
        if file.startswith("dragonblockarcanedba-") and file.endswith(".jar"):
            if "-sources" not in file and "-dev" not in file:
                target_jar = os.path.join(libs_dir, file)
                break

    if not target_jar:
        print("ERROR: Could not locate the built mod jar in build/libs!")
        sys.exit(1)

    # Destination paths as requested
    mod_dir = os.path.abspath("mod")
    backup_dir = os.path.join(mod_dir, "backup")
    os.makedirs(backup_dir, exist_ok=True)

    jar_filename = os.path.basename(target_jar)
    dest_path = os.path.join(mod_dir, jar_filename)
    backup_path = os.path.join(backup_dir, jar_filename)

    print(f"Located built JAR: {target_jar}")
    
    # If the jar is already in the mod folder, move it to backup
    if os.path.exists(dest_path):
        print(f"Existing jar found in mod/. Backing up to: {backup_path}")
        try:
            if os.path.exists(backup_path):
                os.remove(backup_path) # Overwrite existing backup
            shutil.move(dest_path, backup_path)
        except Exception as e:
            print(f"Warning: Failed to back up existing jar: {e}")

    # Copy new jar to mod/
    print(f"Copying built jar to: {dest_path}")
    try:
        shutil.copy2(target_jar, dest_path)
        print("SUCCESS: Mod jar successfully built and placed in mod/ folder.")
    except Exception as e:
        print(f"ERROR: Failed to copy JAR: {e}")
        sys.exit(1)


if __name__ == "__main__":
    clean_build = "--no-clean" not in sys.argv
    nuke = "--nuke-caches" in sys.argv

    if nuke:
        nuke_caches()

    run_build(clean=clean_build)
