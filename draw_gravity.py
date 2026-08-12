import random
from PIL import Image, ImageDraw

img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pixels = img.load()

# Define base metal colors
METAL_BASE = 140
METAL_VAR = 20

# Add random noise for a brushed metal look
random.seed(42)  # for consistency
for y in range(16):
    for x in range(16):
        # Base metallic color with slight noise
        val = METAL_BASE + random.randint(-METAL_VAR, METAL_VAR)
        
        # Add a subtle vertical grain
        val += (x % 3) * 5
        
        # Shading (darker at bottom, lighter at top left)
        shade = int((15 - y) * 1.5 - x * 1.0)
        val += shade
        
        val = max(20, min(255, val))
        pixels[x, y] = (val, val, val+5, 255) # slight blue tint for metal

# Borders
BORDER_DARK = (60, 60, 70, 255)
BORDER_LIGHT = (200, 200, 210, 255)

for i in range(16):
    pixels[i, 0] = BORDER_LIGHT
    pixels[0, i] = BORDER_LIGHT
    pixels[i, 15] = BORDER_DARK
    pixels[15, i] = BORDER_DARK

# Bolted corners (dark dots)
BOLT = (40, 40, 40, 255)
pixels[2, 2] = BOLT
pixels[13, 2] = BOLT
pixels[2, 13] = BOLT
pixels[13, 13] = BOLT
pixels[1, 1] = BORDER_LIGHT
pixels[14, 1] = BORDER_LIGHT
pixels[1, 14] = BORDER_DARK
pixels[14, 14] = BORDER_DARK

# Front Grille/Vents
VENT_BG = (30, 30, 35, 255)
VENT_LINE = (10, 10, 15, 255)
for y in range(10, 14):
    for x in range(3, 13):
        pixels[x, y] = VENT_BG
        if y % 2 == 1:
            pixels[x, y] = VENT_LINE

# Top Panel
PANEL_BG = (50, 50, 60, 255)
for y in range(3, 8):
    for x in range(4, 12):
        pixels[x, y] = PANEL_BG

# Glowing screen on panel
for y in range(4, 7):
    for x in range(5, 9):
        # gradient cyan screen
        c = int(150 + (x - 5)*20)
        pixels[x, y] = (30, c, c+50, 255)

# Buttons
pixels[10, 4] = (255, 50, 50, 255) # Red Button
pixels[10, 6] = (255, 200, 50, 255) # Yellow Button
pixels[10, 5] = (40, 40, 40, 255)

img.save('src/main/resources/assets/dragonblockarcanedba/textures/block/gravity_training_block.png')
print("Detailed texture saved!")
