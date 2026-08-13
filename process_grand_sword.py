import os
from PIL import Image
import sys

sys.setrecursionlimit(20000)

def process_checkerboard(input_path, output_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    width, height = img.size
    pixels = img.load()
    
    # Collect colors from the edges to define the background
    bg_colors = []
    for x in range(width):
        bg_colors.append(pixels[x, 0])
        bg_colors.append(pixels[x, height-1])
    for y in range(height):
        bg_colors.append(pixels[0, y])
        bg_colors.append(pixels[width-1, y])
        
    # Find the most common colors on the edge
    color_counts = {}
    for c in bg_colors:
        # Round slightly to group compression artifacts
        r_c = (c[0]//5, c[1]//5, c[2]//5)
        color_counts[r_c] = color_counts.get(r_c, 0) + 1
        
    # The two most common colors are the checkerboard
    sorted_colors = sorted(color_counts.items(), key=lambda item: item[1], reverse=True)
    bg1 = sorted_colors[0][0]
    bg2 = sorted_colors[1][0] if len(sorted_colors) > 1 else bg1
    
    # Remove these colors
    threshold = 8 # strict threshold to avoid removing sword pixels
    for x in range(width):
        for y in range(height):
            c = pixels[x, y]
            r_c = (c[0]//5, c[1]//5, c[2]//5)
            # If color matches the rounded bg colors, make it transparent
            dist1 = abs(r_c[0]-bg1[0]) + abs(r_c[1]-bg1[1]) + abs(r_c[2]-bg1[2])
            dist2 = abs(r_c[0]-bg2[0]) + abs(r_c[1]-bg2[1]) + abs(r_c[2]-bg2[2])
            if dist1 < 3 or dist2 < 3:
                pixels[x, y] = (0, 0, 0, 0)
                
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
    orig_path = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\media__1786633691739.jpg"
    out_path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\grand_sword.png"
    process_checkerboard(orig_path, out_path)
