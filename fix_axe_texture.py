import os
from PIL import Image

def fix_texture(input_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    # Resize to 32x32 using LANCZOS to preserve detail
    img = img.resize((32, 32), Image.Resampling.LANCZOS)
    
    data = img.getdata()
    
    # Use top-left pixel as background color reference
    bg_color = data[0]
    
    new_data = []
    for item in data:
        # Check if the pixel is close to the background color or white/black
        # Sometimes AI adds a white or black background
        if item[3] < 10:
            new_data.append((255, 255, 255, 0))
        elif abs(item[0] - bg_color[0]) < 30 and abs(item[1] - bg_color[1]) < 30 and abs(item[2] - bg_color[2]) < 30:
            new_data.append((255, 255, 255, 0)) # Fully transparent
        else:
            new_data.append(item)
            
    img.putdata(new_data)
    img.save(input_path, "PNG")
    print("Saved fixed texture.")

if __name__ == "__main__":
    path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\ox_kings_ax.png"
    fix_texture(path)
