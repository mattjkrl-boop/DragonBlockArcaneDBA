import zipfile
import os
from PIL import Image

def process_gui_image(img):
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    # Colors to replace (RGBA)
    c_bg = (198, 198, 198, 255)       # #C6C6C6 Vanilla grey background
    c_border_light = (255, 255, 255, 255) # #FFFFFF Top/Left border
    c_border_dark = (85, 85, 85, 255)     # #555555 Bottom/Right border
    c_slot_bg = (139, 139, 139, 255)      # #8B8B8B Slot inner background
    c_slot_dark = (55, 55, 55, 255)       # #373737 Slot inner top/left shadow
    
    # New Colors
    n_bg = (21, 24, 36, 255)          # #151824 Dark sci-fi background
    n_neon = (0, 255, 204, 255)       # #00FFCC Neon Cyan borders
    n_slot_bg = (10, 12, 14, 255)     # #0A0C0E Dark slot background
    n_slot_border = (0, 170, 170, 255) # #00AAAA Dimmer cyan for slot borders
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0: continue
            
            p = (r, g, b, a)
            
            # Approximate matching due to potential compression or slight variations
            def is_close(c1, c2, tol=5):
                return abs(c1[0]-c2[0])<=tol and abs(c1[1]-c2[1])<=tol and abs(c1[2]-c2[2])<=tol

            if is_close(p, c_bg):
                pixels[x, y] = n_bg
            elif is_close(p, c_border_light) or is_close(p, c_border_dark):
                # Only replace white if it's likely a border (we don't want to replace white pixels in icons if there are any)
                # But since it's a container background, it's mostly safe.
                pixels[x, y] = n_neon
            elif is_close(p, c_slot_bg):
                pixels[x, y] = n_slot_bg
            elif is_close(p, c_slot_dark):
                pixels[x, y] = n_slot_border

    return img

def main():
    jar_path = r'C:\Users\mattj\.gradle\caches\fabric-loom\26.2\minecraft-client.jar'
    out_dir = 'test_gui'
    os.makedirs(out_dir, exist_ok=True)
    
    with zipfile.ZipFile(jar_path, 'r') as z:
        for name in z.namelist():
            if 'gui/container/inventory.png' in name:
                z.extract(name, out_dir)
                img_path = os.path.join(out_dir, name)
                
                print(f"Processing {img_path}")
                img = Image.open(img_path)
                new_img = process_gui_image(img)
                out_path = os.path.join(out_dir, 'inventory_modified.png')
                new_img.save(out_path)
                print(f"Saved {out_path}")

if __name__ == '__main__':
    main()
