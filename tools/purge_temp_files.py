#!/usr/bin/env python3
"""
tools/purge_temp_files.py
-------------------------
Cleans up temporary scratch scripts, test java files, stray class files,
and temporary directories accumulated in the mod root folder.
"""

import os
import shutil

ROOT_TEMP_DIRS = [
    "assets", # scratch assets folder in root (real assets are in src/main/resources/assets)
    "_temp",
    "temp",
    "temp_extract",
    "temp_extract2",
    "temp_extract3",
    "temp_extract4",
    "temp_weapons",
    "test_gui",
    "com",
    "bin",
    "__pycache__"
]

ROOT_SCRATCH_FILES = [
    # Scratch Java files
    "DumpMethods.java",
    "Test.java",
    "TestTeleport.java",

    # Scratch images, logs, text
    "beacon_beam.png",
    "error.log",
    "how-to-save",

    # Scratch Python image/model/gui scripts from previous sessions
    "analyze_bg.py",
    "clean_bg.py",
    "create_advanced_effect_icons.py",
    "create_textures.py",
    "draw_gravity.py",
    "draw_radar.py",
    "find_sky.py",
    "fix_axe_texture.py",
    "fix_textures.py",
    "generate_sword_variations.py",
    "generate_ui_textures.py",
    "generate_weapons_textures.py",
    "layout_skill_tree.py",
    "make_silver.py",
    "mask_circle.py",
    "process_azure.py",
    "process_bigger.py",
    "process_evil_spear.py",
    "process_evil_spear2.py",
    "process_grand_sword.py",
    "process_highres.py",
    "process_jagged_edge.py",
    "process_middle.py",
    "process_pixel.py",
    "process_sword.py",
    "process_weapons2.py",
    "process_zsword.py",
    "process_zsword_32.py",
    "recolor_saber.py",
    "remove_bg_gammet.py",
    "remove_bg_ruby.py",
    "remove_bg_senzu.py",
    "remove_bg_zeni.py",
    "remove_bgs_properly.py",
    "restore_textures.py",
    "rotate_flip_axe.py",
    "scratch_823.py",
    "smart_bg_remover.py",
    "smart_bg_remover2.py",
    "test_gui.py",
    "update_jsons.py"
]

def purge():
    print("[*] Purging root temporary directories...")
    for d in ROOT_TEMP_DIRS:
        if os.path.exists(d) and os.path.isdir(d):
            try:
                shutil.rmtree(d, ignore_errors=True)
                print(f"  [-] Removed directory: {d}")
            except Exception as e:
                print(f"  [!] Error removing dir {d}: {e}")

    print("\n[*] Purging scratch root files...")
    for f in ROOT_SCRATCH_FILES:
        if os.path.exists(f) and os.path.isfile(f):
            try:
                os.remove(f)
                print(f"  [-] Removed file: {f}")
            except Exception as e:
                print(f"  [!] Error removing file {f}: {e}")

    print("\n[*] Purging stray .class files from root...")
    for f in os.listdir("."):
        if f.endswith(".class") and os.path.isfile(f):
            try:
                os.remove(f)
                print(f"  [-] Removed class file: {f}")
            except Exception as e:
                print(f"  [!] Error removing {f}: {e}")

    # Also clean pycache in tools
    tools_pycache = os.path.join("tools", "__pycache__")
    if os.path.exists(tools_pycache):
        shutil.rmtree(tools_pycache, ignore_errors=True)
        print(f"  [-] Removed: {tools_pycache}")

    print("\n[OK] Root folder cleanup complete!")

if __name__ == "__main__":
    purge()
