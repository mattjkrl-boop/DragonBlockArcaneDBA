from PIL import Image

def analyze_image(path):
    img = Image.open(path).convert("RGB")
    data = img.getdata()
    # sample top left corner
    pixels = [data[0], data[1], data[2], data[img.width], data[img.width+1]]
    print("Corner pixels:", pixels)

analyze_image(r"C:\Users\mattj\.gemini\antigravity-ide\brain\20dab950-84fe-40cb-a9f1-270640865bce\media__1786344340764.jpg")
