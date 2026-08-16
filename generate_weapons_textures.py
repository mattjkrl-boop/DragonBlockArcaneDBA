import cv2
import numpy as np
from PIL import Image

def remove_bg_floodfill(input_path, output_path, is_checkerboard=False):
    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    if img.shape[2] == 3:
        img = cv2.cvtColor(img, cv2.COLOR_BGR2BGRA)
        
    gray = cv2.cvtColor(img, cv2.COLOR_BGRA2GRAY)
    
    if is_checkerboard:
        # Checkered background is very noisy, so let's use a standard threshold to extract the sword
        # The background is light gray/white checkered.
        _, dark_mask = cv2.threshold(gray, 180, 255, cv2.THRESH_BINARY_INV)
        # Morphological Close
        kernel = np.ones((3,3), np.uint8)
        dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_CLOSE, kernel)
        dark_mask = cv2.dilate(dark_mask, kernel, iterations=1)
    else:
        # Solid green background, we can just use color thresholding!
        # Background is roughly [B=40-60, G=190-230, R=140-170]
        hsv = cv2.cvtColor(img[:,:,:3], cv2.COLOR_BGR2HSV)
        # Green hue is roughly 35-85
        lower_green = np.array([35, 50, 50])
        upper_green = np.array([85, 255, 255])
        green_mask = cv2.inRange(hsv, lower_green, upper_green)
        
        # Invert to get the object mask
        dark_mask = cv2.bitwise_not(green_mask)

    h, w = img.shape[:2]
    
    # If using floodfill from corners
    ff_mask = np.zeros((h+2, w+2), np.uint8)
    ff_mask[1:h+1, 1:w+1] = dark_mask
    
    ff_img = gray.copy()
    corners = [(0,0), (w-1,0), (0,h-1), (w-1,h-1)]
    for pt in corners:
        cv2.floodFill(ff_img, ff_mask, pt, 0, loDiff=255, upDiff=255, flags=4 | (255 << 8) | cv2.FLOODFILL_MASK_ONLY)
        
    bg_mask = ff_mask[1:h+1, 1:w+1]
    
    if is_checkerboard:
        img[(bg_mask == 255) & (gray > 150)] = (0, 0, 0, 0)
    else:
        img[bg_mask == 255] = (0, 0, 0, 0)
        
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGRA2RGBA))
    
    bbox = pil_img.getbbox()
    if bbox:
        pil_img = pil_img.crop(bbox)
        
    width, height = pil_img.size
    max_dim = max(width, height)
    square_img = Image.new("RGBA", (max_dim, max_dim), (255, 255, 255, 0))
    x_offset = (max_dim - width) // 2
    y_offset = (max_dim - height) // 2
    square_img.paste(pil_img, (x_offset, y_offset))
    pil_img = square_img
    
    pil_img = pil_img.resize((16, 16), Image.Resampling.NEAREST)
    pil_img.save(output_path, "PNG")

if __name__ == "__main__":
    tasks = [
        ("temp_weapons/media__1786858341118.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\evil_spear.png", False),
        ("temp_weapons/media__1786858341146.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\brave_sword.png", False),
        ("temp_weapons/media__1786858341163.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\katana.png", True),
        ("temp_weapons/media__1786858359223.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\blaster_ammo.png", False),
    ]
    
    for in_p, out_p, is_check in tasks:
        try:
            remove_bg_floodfill(in_p, out_p, is_check)
            print(f"Processed {out_p}")
        except Exception as e:
            print(f"Failed {in_p}: {e}")
