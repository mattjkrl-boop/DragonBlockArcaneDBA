import cv2
import numpy as np
from PIL import Image

def process_spear(input_path, output_path):
    print(f"Processing {input_path}")
    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    if img.shape[2] == 3:
        img = cv2.cvtColor(img, cv2.COLOR_BGR2BGRA)
        
    # Get background color from top-left pixel
    bg_color = img[0, 0, :3].astype(np.int32)
    
    # Calculate color distance for all pixels
    diff = np.abs(img[:, :, :3].astype(np.int32) - bg_color)
    dist = np.sum(diff, axis=2)
    
    # Create mask of background
    mask = dist < 45
    
    # Flood fill from corners
    h, w = img.shape[:2]
    ff_mask = np.zeros((h+2, w+2), np.uint8)
    binary = np.where(mask, 0, 255).astype(np.uint8)
    ff_img = binary.copy()
    corners = [(0,0), (w-1,0), (0,h-1), (w-1,h-1)]
    for pt in corners:
        cv2.floodFill(ff_img, ff_mask, pt, 255, loDiff=0, upDiff=0, flags=4 | (255 << 8) | cv2.FLOODFILL_MASK_ONLY)
        
    bg_mask = ff_mask[1:h+1, 1:w+1]
    
    # Set flooded background to transparent
    img[bg_mask == 255] = [0, 0, 0, 0]
    
    # Convert to PIL
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGRA2RGBA))
    
    # DO NOT CROP - just use the full framing of the original image
    # Resize using LANCZOS to perfectly preserve all detail without dropping pixels
    final_img = pil_img.resize((32, 32), Image.Resampling.LANCZOS)
    
    final_img.save(output_path, "PNG")
    print(f"Saved {output_path}")

process_spear(r"C:\Users\mattj\.gemini\antigravity-ide\brain\2827ab8a-c5e9-4272-ab71-275a05b1cc3b\media__1786859371774.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\evil_spear.png")
