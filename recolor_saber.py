import os
from PIL import Image

def is_golden(r, g, b, a):
    if a < 10:
        return False
    # Gold/Yellow typically has high R and G, lower B
    # It might also have some shading, so we look for yellow hues
    # R > B and G > B and R+G > 100
    if r > b * 1.1 and g > b * 1.1 and (r + g) > 80:
        return True
    return False

def recolor_saber(input_path):
    img = Image.open(input_path).convert("RGBA")
    pixels = img.load()
    
    for x in range(img.size[0]):
        for y in range(img.size[1]):
            r, g, b, a = pixels[x, y]
            if is_golden(r, g, b, a):
                # Convert to grayscale and darken
                gray = int((r + g + b) / 3)
                # Map gray from [0, 255] to a darker range [0, 60] for black hilt
                dark_gray = int(gray * 0.3)
                pixels[x, y] = (dark_gray, dark_gray, dark_gray, a)
                
    img.save(input_path, "PNG")
    print("Recolored hilt to black.")

if __name__ == "__main__":
    out_path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\saber.png"
    recolor_saber(out_path)
