import os
from PIL import Image

def process_pixel_art(input_path, output_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    # Flip it
    img = img.transpose(Image.FLIP_LEFT_RIGHT)
    
    # Rotate it
    img = img.rotate(-35, resample=Image.Resampling.BICUBIC, expand=False)
    
    # Downscale to 32x32
    img = img.resize((32, 32), Image.Resampling.LANCZOS)
    
    pixels = img.getdata()
    new_pixels = []
    
    for p in pixels:
        r, g, b, a = p
        
        # Remove pixels that are almost pure white (background and inside hole)
        # and remove semi-transparent pixels to avoid fuzzy edges
        if a < 128 or (r > 240 and g > 240 and b > 240):
            new_pixels.append((0, 0, 0, 0))
        else:
            new_pixels.append((r, g, b, 255))
            
    img.putdata(new_pixels)
    
    img.save(output_path, "PNG")
    print("Saved pixelated crisp texture.")

if __name__ == "__main__":
    orig_path = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\ox_kings_ax_pixel_art_1786595878083.png"
    out_path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\ox_kings_ax.png"
    process_pixel_art(orig_path, out_path)
