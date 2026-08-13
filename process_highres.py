import os
from PIL import Image
import sys

# Increase recursion depth just in case, though we use an iterative stack
sys.setrecursionlimit(20000)

def flood_fill_transparent(img, start_pos, threshold=40):
    width, height = img.size
    pixels = img.load()
    start_color = pixels[start_pos]
    
    # If the start pixel is already transparent, skip
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

def process_highres(input_path, output_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    # Flood fill from corners to properly remove bg without killing white inside the axe
    print("Removing background...")
    flood_fill_transparent(img, (0, 0))
    flood_fill_transparent(img, (img.size[0]-1, 0))
    flood_fill_transparent(img, (0, img.size[1]-1))
    flood_fill_transparent(img, (img.size[0]-1, img.size[1]-1))
    
    print("Transforming...")
    # Flip it
    img = img.transpose(Image.FLIP_LEFT_RIGHT)
    
    # Rotate by -40 degrees for tilt
    img = img.rotate(-40, resample=Image.Resampling.BICUBIC, expand=False)
    
    # Resize to 256x256
    img = img.resize((256, 256), Image.Resampling.LANCZOS)
    
    # Save
    img.save(output_path, "PNG")
    print("Saved fixed high-res texture.")

if __name__ == "__main__":
    orig_path = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\ox_kings_ax_centered_1786595743882.png"
    out_path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\ox_kings_ax.png"
    process_highres(orig_path, out_path)
