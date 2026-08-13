import os
from PIL import Image

def process_variations(input_path, out_dir):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    # Basic cleanup and sizing
    pixels = img.load()
    def flood_fill_transparent(img, start_pos, threshold=40):
        width, height = img.size
        pixels = img.load()
        start_color = pixels[start_pos]
        if start_color[3] == 0: return
        stack = [start_pos]
        visited = set()
        while stack:
            x, y = stack.pop()
            if (x, y) in visited: continue
            visited.add((x, y))
            current_color = pixels[x, y]
            if abs(current_color[0] - start_color[0]) < threshold and \
               abs(current_color[1] - start_color[1]) < threshold and \
               abs(current_color[2] - start_color[2]) < threshold and current_color[3] > 0:
                pixels[x, y] = (255, 255, 255, 0)
                if x > 0: stack.append((x - 1, y))
                if x < width - 1: stack.append((x + 1, y))
                if y > 0: stack.append((x, y - 1))
                if y < height - 1: stack.append((x, y + 1))
                
    flood_fill_transparent(img, (0, 0))
    flood_fill_transparent(img, (img.size[0]-1, 0))
    flood_fill_transparent(img, (0, img.size[1]-1))
    flood_fill_transparent(img, (img.size[0]-1, img.size[1]-1))

    # Base rotation to diagonal
    base_img = img.rotate(-45, resample=Image.Resampling.BICUBIC, expand=True)
    bbox = base_img.getbbox()
    if bbox: base_img = base_img.crop(bbox)
    width, height = base_img.size
    max_dim = max(width, height)
    square_img = Image.new("RGBA", (max_dim, max_dim), (255, 255, 255, 0))
    square_img.paste(base_img, ((max_dim - width) // 2, (max_dim - height) // 2))
    base_img = square_img.resize((64, 64), Image.Resampling.LANCZOS)
    
    # Solidify alpha
    pixels = base_img.load()
    for x in range(base_img.size[0]):
        for y in range(base_img.size[1]):
            r, g, b, a = pixels[x, y]
            if a > 128: pixels[x, y] = (r, g, b, 255)
            else: pixels[x, y] = (0, 0, 0, 0)

    # Variation 1: Original base diagonal
    v1 = base_img
    v1.save(os.path.join(out_dir, "sword_var_1.png"), "PNG")
    
    # Variation 2: Transposed (diagonal flip)
    v2 = base_img.transpose(Image.TRANSPOSE)
    v2.save(os.path.join(out_dir, "sword_var_2.png"), "PNG")
    
    # Variation 3: Anti-diagonal flip (Rotate 180 + Transpose)
    v3 = base_img.transpose(Image.ROTATE_180).transpose(Image.TRANSPOSE)
    v3.save(os.path.join(out_dir, "sword_var_3.png"), "PNG")
    
    # Variation 4: Left-Right flip (which breaks handle, but just in case)
    v4 = base_img.transpose(Image.FLIP_LEFT_RIGHT)
    v4.save(os.path.join(out_dir, "sword_var_4.png"), "PNG")

if __name__ == "__main__":
    orig_path = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\dabura_sword_1786597264913.png"
    out_dir = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0"
    process_variations(orig_path, out_dir)
