from PIL import Image

# 16x16 image with RGBA
img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
pixels = img.load()

# Colors
TRANS = (0, 0, 0, 0)
OUTLINE = (50, 50, 50, 255)
WHITE = (240, 240, 240, 255)
SCREEN_BG = (40, 180, 40, 255)
GRID = (20, 100, 20, 255)
BTN_RED = (200, 30, 30, 255)
BTN_HL = (255, 100, 100, 255)
DOT = (255, 255, 0, 255)

# Circle body
for y in range(16):
    for x in range(16):
        dx = x - 7.5
        dy = y - 8.5
        dist = (dx*dx + dy*dy)**0.5
        
        if dist < 4.2:
            # Screen
            pixels[x, y] = SCREEN_BG
            # Grid
            if x % 3 == 0 or y % 3 == 0:
                pixels[x, y] = GRID
        elif dist < 5.0:
            pixels[x, y] = OUTLINE
        elif dist < 6.8:
            pixels[x, y] = WHITE
        elif dist < 7.5:
            pixels[x, y] = OUTLINE

# Top button
for x in range(6, 10):
    pixels[x, 1] = OUTLINE
    pixels[x, 0] = BTN_RED
pixels[6, 0] = OUTLINE
pixels[9, 0] = OUTLINE
pixels[7, 0] = BTN_HL

# Fix corner pixels of the button
pixels[5, 1] = TRANS
pixels[10, 1] = TRANS

# Dragon ball dots
pixels[7, 8] = DOT # center
pixels[5, 6] = DOT
pixels[9, 7] = DOT

img.save('src/main/resources/assets/dragonblockarcanedba/textures/item/dragon_radar.png')
print("Texture saved!")
