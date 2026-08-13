import os
from PIL import Image
import sys

sys.setrecursionlimit(20000)

def flood_fill_transparent(img, start_pos, threshold=30):
    width, height = img.size
    pixels = img.load()
    start_color = pixels[start_pos]
    
    if start_color[3] == 0:
        return
        
    transparent = (255, 255, 255, 0)
    
    stack = [start_pos]
    visited = set()
    
    while stack:
        x, y = stack.pop()
        if (x, y) in visited:
            continue
        visited.add((x, y))
        
        current_color = pixels[x, y]
        
        # Check distance
        dist = abs(current_color[0] - start_color[0]) + abs(current_color[1] - start_color[1]) + abs(current_color[2] - start_color[2])
        if dist < threshold and current_color[3] > 0:
           
            pixels[x, y] = transparent
            
            if x > 0: stack.append((x - 1, y))
            if x < width - 1: stack.append((x + 1, y))
            if y > 0: stack.append((x, y - 1))
            if y < height - 1: stack.append((x, y + 1))

def remove_checkerboard(img):
    width, height = img.size
    pixels = img.load()
    
    # We'll flood fill from 0,0, assuming it's part of the background.
    # But checkerboards have 2 colors. So we flood fill twice.
    # First color:
    c1 = pixels[0, 0]
    # Find second color (adjacent checkerboard square, usually ~16px away or just look for the most common other color on the border)
    c2 = None
    for i in range(1, width):
        c = pixels[i, 0]
        dist = abs(c[0]-c1[0]) + abs(c[1]-c1[1]) + abs(c[2]-c1[2])
        if dist > 15: # Different color
            c2 = c
            break
            
    if not c2:
        c2 = c1 # fallback
        
    # Flood fill both colors from all corners
    for corner in [(0,0), (width-1,0), (0,height-1), (width-1,height-1)]:
        flood_fill_transparent(img, corner, threshold=40)
        
    # Also just scan the edges and flood fill anything that looks like c1 or c2
    for x in range(width):
        flood_fill_transparent(img, (x, 0), threshold=40)
        flood_fill_transparent(img, (x, height-1), threshold=40)
    for y in range(height):
        flood_fill_transparent(img, (0, y), threshold=40)
        flood_fill_transparent(img, (width-1, y), threshold=40)
        
    return img

def process_image(input_path, output_path, is_saber=False, is_checkerboard=False):
    img = Image.open(input_path).convert("RGBA")
    
    if is_checkerboard:
        img = remove_checkerboard(img)
    else:
        # Standard flood fill from corners for solid backgrounds
        width, height = img.size
        for corner in [(0,0), (width-1,0), (0,height-1), (width-1,height-1)]:
            flood_fill_transparent(img, corner, threshold=20)
    
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
    
    # Resize
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
    
    grand_sword_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\media__1786633691739.jpg"
    grand_sword_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\grand_sword.png"
    
    process_image(saber_orig, saber_out, is_saber=True, is_checkerboard=False)
    process_image(grand_sword_orig, grand_sword_out, is_saber=False, is_checkerboard=True)
