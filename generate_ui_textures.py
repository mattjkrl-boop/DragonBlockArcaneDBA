import zipfile
import os
from PIL import Image

def is_close(c1, c2, tol=5):
    return abs(c1[0]-c2[0])<=tol and abs(c1[1]-c2[1])<=tol and abs(c1[2]-c2[2])<=tol

def process_gui_image(img):
    img = img.convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    # Colors to replace (RGBA)
    c_bg = (198, 198, 198, 255)       # #C6C6C6 Vanilla grey background
    c_border_light = (255, 255, 255, 255) # #FFFFFF Top/Left border / Highlight
    c_border_dark = (85, 85, 85, 255)     # #555555 Bottom/Right border
    c_slot_bg = (139, 139, 139, 255)      # #8B8B8B Slot inner background
    c_slot_dark = (55, 55, 55, 255)       # #373737 Slot inner shadow
    
    # New Colors
    n_bg = (21, 24, 36, 255)          # #151824 Dark sci-fi background
    n_neon = (0, 255, 204, 255)       # #00FFCC Neon Cyan borders
    n_slot_bg = (10, 12, 14, 255)     # #0A0C0E Dark slot background
    n_slot_border = (0, 170, 170, 255) # #00AAAA Dimmer cyan for slot borders
    
    modified = False
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a < 250: continue # Only process solid or nearly solid pixels
            
            p = (r, g, b, a)
            
            if is_close(p, c_bg):
                pixels[x, y] = n_bg; modified = True
            elif is_close(p, c_border_light) or is_close(p, c_border_dark):
                # For #FFFFFF we must be careful not to overwrite transparent white or text, 
                # but since we filtered for a > 250 it's mostly solid highlights.
                pixels[x, y] = n_neon; modified = True
            elif is_close(p, c_slot_bg):
                pixels[x, y] = n_slot_bg; modified = True
            elif is_close(p, c_slot_dark):
                pixels[x, y] = n_slot_border; modified = True

    return img if modified else None

def main():
    jar_path = r'C:\Users\mattj\.gradle\caches\fabric-loom\26.2\minecraft-client.jar'
    out_dir = r'C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources'
    
    count = 0
    with zipfile.ZipFile(jar_path, 'r') as z:
        for name in z.namelist():
            if not name.endswith('.png'): continue
            
            # Categories of textures we want to theme
            valid_dirs = ['gui/container/', 'gui/sprites/container/', 'recipe_book', 
                          'gui/sprites/widget/', 'gui/sprites/world_list/', 
                          'gui/sprites/social_interactions/', 'gui/sprites/popup/', 
                          'gui/sprites/friends/', 'gui/sprites/tooltip/']
            
            # Specific full-screen backgrounds to replace completely
            bg_files = ['gui/inworld_menu_background.png', 'gui/inworld_menu_list_background.png', 
                        'gui/menu_background.png', 'gui/menu_list_background.png', 'gui/tab_header_background.png']
            
            dest_path = os.path.join(out_dir, name)
            
            try:
                if any(name.endswith(b) for b in bg_files):
                    # Replace these backgrounds entirely with a solid dark sci-fi color block (or gradient)
                    img = Image.new("RGBA", (64, 64), (21, 24, 36, 255))
                    os.makedirs(os.path.dirname(dest_path), exist_ok=True)
                    img.save(dest_path)
                    count += 1
                elif any(d in name for d in valid_dirs):
                    with z.open(name) as f:
                        img = Image.open(f)
                        new_img = process_gui_image(img)
                        if new_img:
                            os.makedirs(os.path.dirname(dest_path), exist_ok=True)
                            new_img.save(dest_path)
                            count += 1
            except Exception as e:
                print(f"Failed to process {name}: {e}")
                    
    print(f"Successfully processed and generated {count} GUI textures!")

if __name__ == '__main__':
    main()
