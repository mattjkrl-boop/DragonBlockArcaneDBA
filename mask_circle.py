from PIL import Image, ImageDraw

def mask_circle(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    
    # Create a mask image the same size as the original
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    
    # Draw a white circle in the mask
    # Assuming the circle touches the edges or is slightly padded.
    # Let's add a small padding/margin if needed, or just bounding box.
    width, height = img.size
    # Adjust bounding box to slightly inside the edges if there's a border
    bbox = (0, 0, width, height)
    draw.ellipse(bbox, fill=255)
    
    # Apply the mask to the alpha channel
    img.putalpha(mask)
    
    img.save(output_path, "PNG")

mask_circle(
    r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\media__1786344340764.jpg",
    r"C:\Users\mattj\Downloads\DragonBlockArcaneDBA\src\main\resources\assets\dragonblockarcanedba\textures\item\dba_logo.png"
)
print("Done masking")
