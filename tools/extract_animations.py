#!/usr/bin/env python3
"""
tools/extract_animations.py
---------------------------
Extracts embedded GLB animation files from source Animate Anything / GoodAnimations ZIP files
into a clean working/staging directory without modifying or deleting the original ZIPs.
"""

import os
import sys
import zipfile
import shutil
import argparse

def extract_animations(source_dir, output_stage_dir):
    """
    Extracts each <anim>.zip from source_dir into output_stage_dir/<anim>/
    and finds the embedded .glb file.
    
    Returns a dict mapping anim_id -> absolute path to .glb file.
    """
    source_dir = os.path.abspath(source_dir)
    output_stage_dir = os.path.abspath(output_stage_dir)
    
    if not os.path.exists(source_dir):
        raise FileNotFoundError(f"Source directory not found: {source_dir}")
        
    os.makedirs(output_stage_dir, exist_ok=True)
    
    zip_files = sorted([f for f in os.listdir(source_dir) if f.lower().endswith(".zip")])
    if not zip_files:
        print(f"[WARN] No .zip files found in {source_dir}")
        return {}
        
    print(f"[*] Found {len(zip_files)} animation ZIP files in {source_dir}")
    extracted_map = {}
    
    for zname in zip_files:
        anim_id = os.path.splitext(zname)[0]
        zpath = os.path.join(source_dir, zname)
        target_anim_dir = os.path.join(output_stage_dir, anim_id)
        os.makedirs(target_anim_dir, exist_ok=True)
        
        target_glb = os.path.join(target_anim_dir, f"{anim_id}.glb")
        
        # Check if already extracted and newer than zip
        if os.path.exists(target_glb) and os.path.getmtime(target_glb) >= os.path.getmtime(zpath):
            extracted_map[anim_id] = target_glb
            continue
            
        with zipfile.ZipFile(zpath, "r") as zf:
            glb_members = [m for m in zf.namelist() if m.lower().endswith(".glb")]
            if not glb_members:
                print(f"[WARN] No .glb found inside {zname}")
                continue
                
            # Usually only one GLB per animation ZIP
            primary_glb = glb_members[0]
            with zf.open(primary_glb) as src, open(target_glb, "wb") as dst:
                shutil.copyfileobj(src, dst)
                
            extracted_map[anim_id] = target_glb
            print(f"  [+] Extracted {anim_id} -> {os.path.basename(target_glb)} ({os.path.getsize(target_glb):,} bytes)")
            
    print(f"[OK] Successfully extracted {len(extracted_map)} animations into {output_stage_dir}")
    return extracted_map

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract GLB animations from ZIP files.")
    parser.add_argument("--source", default=os.path.join("Animated", "GoodAnimations"), help="Source ZIPs directory")
    parser.add_argument("--output", default=os.path.join("Animated", ".extracted_stage"), help="Staging output directory")
    args = parser.parse_args()
    
    extracted = extract_animations(args.source, args.output)
    print(f"Extracted {len(extracted)} animation files.")
