from PIL import Image

def remove_background(input_path, output_path, tolerance=30):
    img = Image.open(input_path).convert("RGBA")
    data = img.getdata()
    
    # Use top-left pixel as the background color reference
    bg_color = data[0]
    
    new_data = []
    for item in data:
        # Check if the pixel is close to the background color
        if abs(item[0] - bg_color[0]) < tolerance and \
           abs(item[1] - bg_color[1]) < tolerance and \
           abs(item[2] - bg_color[2]) < tolerance:
            new_data.append((255, 255, 255, 0)) # Fully transparent
        else:
            new_data.append(item)
            
    img.putdata(new_data)
    img.save(output_path, "PNG")

remove_background(
    r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\blood_ruby_1786391421985.png",
    r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\blood_ruby_transparent.png"
)
print("Done removing background from Blood Ruby")
