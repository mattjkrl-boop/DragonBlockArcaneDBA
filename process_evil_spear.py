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
    
    # Create mask of background (tolerance of 30 for JPG artifacts)
    mask = dist < 45
    
    # Flood fill from corners to only remove connected background
    # This prevents removing green parts inside the spear if any
    h, w = img.shape[:2]
    ff_mask = np.zeros((h+2, w+2), np.uint8)
    
    # Create a binary image where background is 0 and object is 255
    binary = np.where(mask, 0, 255).astype(np.uint8)
    
    ff_img = binary.copy()
    corners = [(0,0), (w-1,0), (0,h-1), (w-1,h-1)]
    for pt in corners:
        cv2.floodFill(ff_img, ff_mask, pt, 255, loDiff=0, upDiff=0, flags=4 | (255 << 8) | cv2.FLOODFILL_MASK_ONLY)
        
    bg_mask = ff_mask[1:h+1, 1:w+1]
    
    # Set flooded background to transparent
    img[bg_mask == 255] = [0, 0, 0, 0]
    
    # Convert to PIL to crop and resize
    pil_img = Image.fromarray(cv2.cvtColor(img, cv2.COLOR_BGRA2RGBA))
    bbox = pil_img.getbbox()
    if bbox:
        pil_img = pil_img.crop(bbox)
        
    width, height = pil_img.size
    max_dim = max(width, height)
    square_img = Image.new("RGBA", (max_dim, max_dim), (0, 0, 0, 0))
    x_offset = (max_dim - width) // 2
    y_offset = (max_dim - height) // 2
    square_img.paste(pil_img, (x_offset, y_offset))
    
    final_img = square_img.resize((32, 32), Image.Resampling.NEAREST)
    final_img.save(output_path, "PNG")
    print(f"Saved {output_path}")

process_spear("temp_weapons/media__1786858341118.jpg", r"src\main\resources\assets\dragonblockarcanedba\textures\item\evil_spear.png")
