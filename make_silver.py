from PIL import Image

def make_silver(input_path, output_path):
    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()
    
    new_data = []
    for item in data:
        # Check if pixel is fully transparent
        if item[3] == 0:
            new_data.append(item)
        else:
            # Convert gold to silver by taking average or luminosity
            # Luminosity formula: 0.299*R + 0.587*G + 0.114*B
            lum = int(0.299 * item[0] + 0.587 * item[1] + 0.114 * item[2])
            # To make it slightly shiny/bluish silver, we can adjust the channels
            # Just pure grayscale is usually a good silver for pixel art
            r = lum
            g = lum
            b = min(255, int(lum * 1.05)) # Tiny blue tint for silver
            new_data.append((r, g, b, item[3]))
            
    img.putdata(new_data)
    img.save(output_path, "PNG")

make_silver(
    r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\zeni_coin_transparent.png",
    r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\zeni_coin_silver.png"
)
print("Done converting to silver")
