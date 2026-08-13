import cv2
import numpy as np
from PIL import Image

def remove_bg_opencv(input_path, output_path, is_saber=False):
    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    if img.shape[2] == 3:
        img = cv2.cvtColor(img, cv2.COLOR_BGR2BGRA)
        
    gray = cv2.cvtColor(img, cv2.COLOR_BGRA2GRAY)
    
    # Threshold to find the dark outline (anything darker than 120 is outline)
    _, dark_mask = cv2.threshold(gray, 120, 255, cv2.THRESH_BINARY_INV)
    
    # Morphological Close to seal any gaps in the outline
    kernel = np.ones((5,5), np.uint8)
    dark_mask = cv2.morphologyEx(dark_mask, cv2.MORPH_CLOSE, kernel)
    
    # Also dilate slightly to make the barrier thicker, ensuring no leaks
    dark_mask = cv2.dilate(dark_mask, kernel, iterations=1)
    
    h, w = img.shape[:2]
    ff_mask = np.zeros((h+2, w+2), np.uint8)
    ff_mask[1:h+1, 1:w+1] = dark_mask
    
    ff_img = gray.copy()
    
    corners = [(0,0), (w-1,0), (0,h-1), (w-1,h-1)]
    for pt in corners:
        cv2.floodFill(ff_img, ff_mask, pt, 0, loDiff=255, upDiff=255, flags=4 | (255 << 8) | cv2.FLOODFILL_MASK_ONLY)
        
    bg_mask = ff_mask[1:h+1, 1:w+1]
    
    # Any pixel that was flooded (255 in bg_mask) AND is light enough to actually be the background (gray > 150)
    # This prevents the mask from accidentally erasing dark parts if it leaked
    img[(bg_mask == 255) & (gray > 130)] = (0, 0, 0, 0)
    
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
    
    pil_img = pil_img.resize((64, 64), Image.Resampling.NEAREST)
    
    if is_saber:
        pixels = pil_img.load()
        for x in range(pil_img.size[0]):
            for y in range(pil_img.size[1]):
                r, g, b, a = pixels[x, y]
                if a > 0:
                    if r > b * 1.1 and g > b * 1.1 and (r + g) > 80:
                        gray_val = int((r + g + b) / 3)
                        dark_gray = int(gray_val * 0.3)
                        pixels[x, y] = (dark_gray, dark_gray, dark_gray, a)
                        
    pil_img.save(output_path, "PNG")

if __name__ == "__main__":
    saber_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\jagged_edge_raw_1786602405303.png"
    saber_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\saber.png"
    
    grand_sword_orig = r"C:\Users\mattj\.gemini\antigravity-ide\brain\72097189-7f90-4b70-9de8-99d2296b52a0\media__1786634815225.jpg"
    grand_sword_out = r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\grand_sword.png"
    
    remove_bg_opencv(saber_orig, saber_out, is_saber=True)
    remove_bg_opencv(grand_sword_orig, grand_sword_out, is_saber=False)
