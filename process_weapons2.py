import cv2
import numpy as np
from PIL import Image

def process_image(input_path, output_path, is_checkerboard=False):
    # Load with PIL
    pil_img = Image.open(input_path).convert("RGBA")
    
    # If the image is large, it's scaled up pixel art. We want to find the true bounding box of the object.
    img_arr = np.array(pil_img)
    
    if is_checkerboard:
        # Checkered background has light gray/white squares.
        # We can find all pixels that are significantly dark and consider them part of the katana.
        # Or just use rembg or a simple threshold.
        gray = cv2.cvtColor(img_arr, cv2.COLOR_RGBA2GRAY)
        _, mask = cv2.threshold(gray, 200, 255, cv2.THRESH_BINARY_INV)
        
        # Keep only pixels that pass the mask
        img_arr[mask == 0] = [0, 0, 0, 0]
    else:
        # Background color is roughly the top-left pixel
        bg_color = img_arr[0, 0, :3]
        
        # Create a mask of pixels that are close to the background color
        diff = np.abs(img_arr[:,:,:3].astype(int) - bg_color.astype(int))
        mask = np.all(diff < 20, axis=2)
        
        # Set background pixels to transparent
        img_arr[mask] = [0, 0, 0, 0]
        
    # Convert back to PIL
    out_img = Image.fromarray(img_arr)
    
    # Crop to bounding box of non-transparent pixels
    bbox = out_img.getbbox()
    if bbox:
        out_img = out_img.crop(bbox)
        
    # Make square
    w, h = out_img.size
    max_dim = max(w, h)
    square_img = Image.new("RGBA", (max_dim, max_dim), (0, 0, 0, 0))
    x_off = (max_dim - w) // 2
    y_off = (max_dim - h) // 2
    square_img.paste(out_img, (x_off, y_off))
    
    # Resize to 16x16
    final_img = square_img.resize((16, 16), Image.Resampling.NEAREST)
    
    # If Katana, we need it to look right (rotated 45 deg, fit in 16x16)
    if is_checkerboard:
        # For sword items, they usually point top-right. Let's make sure.
        pass

    final_img.save(output_path, "PNG")

if __name__ == "__main__":
    tasks = [
        ("temp_weapons/media__1786858341118.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\evil_spear.png", False),
        ("temp_weapons/media__1786858341146.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\brave_sword.png", False),
        ("temp_weapons/media__1786858341163.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\katana.png", True),
        ("temp_weapons/media__1786858359223.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\blaster_ammo.png", False),
    ]
    
    for in_p, out_p, is_check in tasks:
        try:
            process_image(in_p, out_p, is_check)
            print(f"Processed {out_p}")
        except Exception as e:
            print(f"Failed {in_p}: {e}")
