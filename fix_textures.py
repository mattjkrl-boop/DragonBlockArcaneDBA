import rembg
from PIL import Image

def fix_image(input_path, output_path):
    print(f"Processing {input_path}...")
    img = Image.open(input_path)
    
    # Remove background using rembg
    out = rembg.remove(img)
    
    # Crop to bounding box
    bbox = out.getbbox()
    if bbox:
        out = out.crop(bbox)
        
    # Make square
    w, h = out.size
    max_dim = max(w, h)
    square_img = Image.new("RGBA", (max_dim, max_dim), (0, 0, 0, 0))
    x_off = (max_dim - w) // 2
    y_off = (max_dim - h) // 2
    square_img.paste(out, (x_off, y_off))
    
    # For weapons, they should be rotated to face top-right corner.
    # The Evil Spear image is pointing top-right.
    # The Brave Sword image is pointing top-right.
    # The Katana image is pointing top-right.
    # So they are already correctly oriented!
    
    # Resize to 32x32 for better detail than 16x16, but still pixelated
    final_img = square_img.resize((32, 32), Image.Resampling.NEAREST)
    
    final_img.save(output_path, "PNG")
    print(f"Saved {output_path}")

tasks = [
    ("temp_weapons/media__1786858341118.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\evil_spear.png"),
    ("temp_weapons/media__1786858341146.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\brave_sword.png"),
    ("temp_weapons/media__1786858341163.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\katana.png"),
    ("temp_weapons/media__1786858359223.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\blaster_ammo.png"),
]

for in_p, out_p in tasks:
    try:
        fix_image(in_p, out_p)
    except Exception as e:
        print(f"Failed {in_p}: {e}")
