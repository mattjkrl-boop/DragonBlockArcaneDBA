import os
from PIL import Image

def modify_texture(input_path):
    print(f"Opening {input_path}")
    img = Image.open(input_path).convert("RGBA")
    
    # Flip the image left-to-right
    img = img.transpose(Image.FLIP_LEFT_RIGHT)
    
    # Tilt it slightly (rotate by -30 degrees clockwise)
    # Using BICUBIC interpolation for smoothness
    img = img.rotate(-30, resample=Image.Resampling.BICUBIC, expand=False)
    
    img.save(input_path, "PNG")
    print("Saved modified texture.")

if __name__ == "__main__":
    path = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\ox_kings_ax.png"
    modify_texture(path)
