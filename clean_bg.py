import os
from PIL import Image
import sys

def remove_specific_colors(img, target_colors, threshold=20):
    pixels = img.load()
    width, height = img.size
    for x in range(width):
        for y in range(height):
            c = pixels[x, y]
            if c[3] == 0:
                continue
            for t in target_colors:
                dist = abs(c[0]-t[0]) + abs(c[1]-t[1]) + abs(c[2]-t[2])
                if dist < threshold:
                    pixels[x, y] = (0, 0, 0, 0)
                    break
    return img

def process_image(input_path, output_path, is_saber=False, target_colors=[]):
    img = Image.open(input_path).convert("RGBA")
    
    img = remove_specific_colors(img, target_colors, threshold=25)
    
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
    
    # Resize cleanly without blurring transparent edges
    img = img.resize((64, 64), Image.Resampling.NEAREST)
    
    # Recolor saber
    if is_saber:
        pixels = img.load()
        for x in range(img.size[0]):
            for y in range(img.size[1]):
                r, g, b, a = pixels[x, y]
                if a > 0:
                    if r > b * 1.1 and g > b * 1.1 and (r + g) > 80:
                        gray = int((r + g + b) / 3)
                        dark_gray = int(gray * 0.3)
                        pixels[x, y] = (dark_gray, dark_gray, dark_gray, a)
                        
    img.save(output_path, "PNG")

if __name__ == "__main__":
    saber_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\jagged_edge_raw_1786602405303.png"
    saber_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\saber.png"
    
    grand_sword_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\media__1786634815225.jpg"
    grand_sword_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\grand_sword.png"
    
    # Saber checkerboard colors
    saber_targets = [(204,204,204), (255,255,255), (205,205,205), (254,254,254)]
    process_image(saber_orig, saber_out, is_saber=True, target_colors=saber_targets)
    
    # Grand Sword checkerboard colors (188,188,188) and the darker one probably around (160,160,160)
    # Let's inspect the original image to find the top two common gray colors near the corners.
    # Actually, we can just use the known values. 188 is one. The other is likely 170.
    gs_targets = [(189,189,189), (188,188,188), (170,170,170), (160,160,160), (150,150,150), (167,167,167)]
    process_image(grand_sword_orig, grand_sword_out, is_saber=False, target_colors=gs_targets)
