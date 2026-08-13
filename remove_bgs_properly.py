import os
from PIL import Image
from rembg import remove

def process_with_rembg(input_path, output_path, is_saber=False):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    print("Removing background with rembg...")
    # rembg expects a PIL image or bytes. We can pass PIL image.
    img = remove(img)
    
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
    
    # Solidify alpha and apply custom coloring if it's saber
    pixels = img.load()
    for x in range(img.size[0]):
        for y in range(img.size[1]):
            r, g, b, a = pixels[x, y]
            if a > 128:
                if is_saber:
                    # Recolor gold to black for the Saber
                    # R > B and G > B and R+G > 80
                    if r > b * 1.1 and g > b * 1.1 and (r + g) > 80:
                        gray = int((r + g + b) / 3)
                        dark_gray = int(gray * 0.3)
                        pixels[x, y] = (dark_gray, dark_gray, dark_gray, 255)
                    else:
                        pixels[x, y] = (r, g, b, 255)
                else:
                    pixels[x, y] = (r, g, b, 255)
            else:
                pixels[x, y] = (0, 0, 0, 0)
                
    img.save(output_path, "PNG")
    print(f"Saved texture to {output_path}")

if __name__ == "__main__":
    saber_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\jagged_edge_raw_1786602405303.png"
    saber_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\saber.png"
    
    grand_sword_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\media__1786633691739.jpg"
    grand_sword_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\grand_sword.png"
    
    process_with_rembg(saber_orig, saber_out, is_saber=True)
    process_with_rembg(grand_sword_orig, grand_sword_out, is_saber=False)
