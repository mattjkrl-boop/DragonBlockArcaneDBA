import os, shutil

base_dir = os.path.abspath("Animated")
backups_dir = os.path.join(base_dir, "Backups")
core_backup_dir = os.path.join(backups_dir, "core_animations")
extra_backup_dir = os.path.join(backups_dir, "extra_animations")
bedrock_backup_dir = os.path.join(backups_dir, "universal_bedrock_export")

os.makedirs(core_backup_dir, exist_ok=True)
os.makedirs(extra_backup_dir, exist_ok=True)
os.makedirs(bedrock_backup_dir, exist_ok=True)

# 1. Backup 22 Core Animations from GoodAnimations
good_dir = os.path.join(base_dir, "GoodAnimations")
core_zips = [f for f in os.listdir(good_dir) if f.endswith(".zip")]
print(f"[*] Backing up {len(core_zips)} core animations to {core_backup_dir}...")
for f in core_zips:
    src = os.path.join(good_dir, f)
    dst = os.path.join(core_backup_dir, f)
    shutil.copy2(src, dst)

# 2. Backup extra animations from Animated/namekian#0000_*.zip with clean names
loose_zips = [f for f in os.listdir(base_dir) if f.startswith("namekian#0000") and f.endswith(".zip")]
print(f"[*] Processing {len(loose_zips)} loose AI animations into named extra backups...")
core_names = set(os.path.splitext(f)[0] for f in core_zips)

for f in loose_zips:
    src = os.path.join(base_dir, f)
    # determine clean name
    clean_name = f.replace("namekian#0000_", "").replace("namekian#0000.zip", "base_rig.zip")
    if clean_name == "namekian#0000":
        clean_name = "base_rig.zip"
    elif not clean_name.endswith(".zip"):
        clean_name += ".zip"
        
    stem = os.path.splitext(clean_name)[0]
    if stem not in core_names and stem != "base_rig":
        dst = os.path.join(extra_backup_dir, clean_name)
        shutil.copy2(src, dst)
        print(f"  [+] Extra backup: {f} -> {clean_name}")

# 3. Backup Universal Bedrock output
univ_dir = os.path.join(base_dir, "UniversalAnimations")
for item in ["manifest.json", "universal_skeleton.json", "UniversalAnimationConversionReport.json"]:
    p = os.path.join(univ_dir, item)
    if os.path.exists(p):
        shutil.copy2(p, os.path.join(bedrock_backup_dir, item))

bpm_anim = os.path.join(univ_dir, "BPM", "universal_humanoid", "animations", "main.animation.json")
if os.path.exists(bpm_anim):
    shutil.copy2(bpm_anim, os.path.join(bedrock_backup_dir, "main.animation.json"))

print("[OK] Backups successfully completed.")

# 4. Remove the loose namekian ZIP files from Animated/
print(f"[*] Cleaning up loose namekian ZIPs from {base_dir}...")
for f in loose_zips:
    p = os.path.join(base_dir, f)
    try:
        os.remove(p)
    except Exception as e:
        print(f"  [!] Error removing {p}: {e}")

# 5. Remove useless temporary folders
for temp_dir in [
    os.path.join(base_dir, ".extracted_stage"),
    os.path.join(good_dir, "BlockbenchImport")
]:
    if os.path.exists(temp_dir):
        print(f"[*] Removing temporary folder: {temp_dir}")
        shutil.rmtree(temp_dir, ignore_errors=True)

# 6. Remove obsolete ps1 scripts in Animated
for script in [
    os.path.join(base_dir, "animation-fix.ps1"),
    os.path.join(good_dir, "Prepare-Blockbench.ps1")
]:
    if os.path.exists(script):
        print(f"[*] Removing obsolete script: {script}")
        try:
            os.remove(script)
        except Exception as e:
            print(f"  [!] Error removing {script}: {e}")

print("[OK] Cleanup and reorganization complete!")
