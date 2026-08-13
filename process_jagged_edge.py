import os
from PIL import Image
import sys

sys.setrecursionlimit(20000)

def flood_fill_transparent(img, start_pos, threshold=40):
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
        
        if abs(current_color[0] - start_color[0]) < threshold and \
           abs(current_color[1] - start_color[1]) < threshold and \
           abs(current_color[2] - start_color[2]) < threshold and current_color[3] > 0:
           
            pixels[x, y] = transparent
            
            if x > 0: stack.append((x - 1, y))
            if x < width - 1: stack.append((x + 1, y))
            if y > 0: stack.append((x, y - 1))
            if y < height - 1: stack.append((x, y + 1))

def process_jagged_edge(input_path, output_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    print("Removing background...")
    flood_fill_transparent(img, (0, 0))
    flood_fill_transparent(img, (img.size[0]-1, 0))
    flood_fill_transparent(img, (0, img.size[1]-1))
    flood_fill_transparent(img, (img.size[0]-1, img.size[1]-1))
    
    # Crop away empty space to maximize size
    bbox = img.getbbox()
    if bbox:
        img = img.crop(bbox)
        
    # Make it square
    width, height = img.size
    max_dim = max(width, height)
    square_img = Image.new("RGBA", (max_dim, max_dim), (255, 255, 255, 0))
    x_offset = (max_dim - width) // 2
    y_offset = (max_dim - height) // 2
    square_img.paste(img, (x_offset, y_offset))
    img = square_img
    
    # Scale to 64x64 cleanly
    img = img.resize((64, 64), Image.Resampling.LANCZOS)
    
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
    print("Saved texture.")

if __name__ == "__main__":
    orig_path = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\jagged_edge_raw_1786602405303.png"
    out_path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\jagged_edge.png"
    process_jagged_edge(orig_path, out_path)
