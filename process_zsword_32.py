import os
from PIL import Image

def process_z_sword(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    
    # Remove white background (RGB > 235)
    pixels = img.load()
    width, height = img.size
    for x in range(width):
        for y in range(height):
            r, g, b, a = pixels[x, y]
            if r > 235 and g > 235 and b > 235:
                pixels[x, y] = (0, 0, 0, 0)
                
    # Crop
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        
    # Square
    width, height = img.size
    max_dim = max(width, height)
    square_img = Image.new("RGBA", (max_dim, max_dim), (255, 255, 255, 0))
    x_offset = (max_dim - width) // 2
    y_offset = (max_dim - height) // 2
    square_img.paste(img, (x_offset, y_offset))
    img = square_img
    
    # Resize to 32x32 exactly
    img = img.resize((32, 32), Image.Resampling.LANCZOS)
    
    # Solidify alpha
    pixels = img.load()
    for x in range(img.size[0]):
        for y in range(img.size[1]):
            r, g, b, a = pixels[x, y]
            if a > 128:
                pixels[x, y] = (r, g, b, 255)
            else:
                pixels[x, y] = (0, 0, 0, 0)
                
    img.save(output_path, "PNG")
    print(f"Saved {output_path} at 32x32")

if __name__ == "__main__":
    z_sword_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\z_sword_dbz_1786635751507.png"
    z_sword_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\z_sword.png"
    
    process_z_sword(z_sword_orig, z_sword_out)
