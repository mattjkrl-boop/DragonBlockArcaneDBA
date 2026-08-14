import os
import subprocess
import glob
import zipfile

cache_dir = r"C:\Users\carte\.gradle\caches\fabric-loom"
jars = glob.glob(os.path.join(cache_dir, "**", "*.jar"), recursive=True)

for jar in jars:
    if "sources" in jar or "javadoc" in jar: continue
    try:
        with zipfile.ZipFile(jar, 'r') as z:
            if "net/minecraft/client/renderer/LevelRenderer.class" in z.namelist():
                print(f"Found in {jar}")
                # extract it
                z.extract("net/minecraft/client/renderer/LevelRenderer.class", "temp")
                break
    except Exception as e:
        pass

if os.path.exists("temp/net/minecraft/client/renderer/LevelRenderer.class"):
    out = subprocess.check_output(["javap", "-p", "temp/net/minecraft/client/renderer/LevelRenderer.class"]).decode('utf-8', errors='ignore')
    for line in out.splitlines():
        if "renderSky(" in line:
            print("METHOD: " + line.strip())
