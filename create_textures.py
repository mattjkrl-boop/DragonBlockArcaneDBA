import os
import struct
import zlib

def create_skin_png(width, height, draw_func):
    png = bytearray(b'\x89PNG\r\n\x1a\n')
    
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    ihdr_crc = zlib.crc32(b'IHDR' + ihdr_data)
    png.extend(struct.pack('>I', len(ihdr_data)) + b'IHDR' + ihdr_data + struct.pack('>I', ihdr_crc))
    
    pixels = [[(0, 0, 0, 0) for _ in range(width)] for _ in range(height)]
    
    draw_func(pixels, width, height)
    
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter 0
        for x in range(width):
            r, g, b, a = pixels[y][x]
            raw_data.extend([r, g, b, a])
            
    compressed = zlib.compress(raw_data)
    idat_crc = zlib.crc32(b'IDAT' + compressed)
    png.extend(struct.pack('>I', len(compressed)) + b'IDAT' + compressed + struct.pack('>I', idat_crc))
    
    iend_crc = zlib.crc32(b'IEND')
    png.extend(struct.pack('>I', 0) + b'IEND' + struct.pack('>I', iend_crc))
    
    return bytes(png)

def fill_rect(p, x1, y1, x2, y2, color):
    for y in range(y1, y2 + 1):
        for x in range(x1, x2 + 1):
            if 0 <= x < 64 and 0 <= y < 64:
                p[y][x] = color

def draw_base_humanoid_skin(p, skin_color, eye_color=(30, 30, 30, 255), shirt_color=None, pants_color=None):
    # Head Base (0..31, 0..15)
    fill_rect(p, 8, 0, 15, 7, skin_color)
    fill_rect(p, 16, 0, 23, 7, skin_color)
    fill_rect(p, 0, 8, 31, 15, skin_color)
    
    # Face details (Front head 8..15, 8..15)
    p[12][10] = eye_color
    p[12][13] = eye_color
    p[12][9] = (255, 255, 255, 255)
    p[12][14] = (255, 255, 255, 255)

    # Right Leg (0..15, 16..31)
    leg_col = pants_color if pants_color else skin_color
    fill_rect(p, 0, 16, 15, 31, leg_col)
    
    # Torso (16..39, 16..31)
    torso_col = shirt_color if shirt_color else skin_color
    fill_rect(p, 16, 16, 39, 31, torso_col)

    # Right Arm (40..55, 16..31)
    arm_col = shirt_color if shirt_color else skin_color
    fill_rect(p, 40, 16, 55, 31, arm_col)

    # Left Leg (16..31, 48..63)
    fill_rect(p, 16, 48, 31, 63, leg_col)

    # Left Arm (32..47, 48..63)
    fill_rect(p, 32, 48, 47, 63, arm_col)

def draw_arcosian(p, w, h):
    white = (240, 240, 250, 255)
    purple = (120, 40, 180, 255)
    red_eyes = (220, 20, 20, 255)
    
    fill_rect(p, 0, 0, 31, 15, white)
    fill_rect(p, 11, 9, 12, 10, purple)
    p[12][10] = red_eyes
    p[12][13] = red_eyes
    
    fill_rect(p, 16, 16, 39, 31, white)
    fill_rect(p, 22, 20, 25, 24, purple)
    
    fill_rect(p, 0, 16, 15, 31, white)
    fill_rect(p, 40, 16, 55, 31, white)
    fill_rect(p, 16, 48, 31, 63, white)
    fill_rect(p, 32, 48, 47, 63, white)
    
    fill_rect(p, 44, 16, 47, 19, purple)
    fill_rect(p, 36, 48, 39, 51, purple)
    fill_rect(p, 4, 26, 7, 29, purple)
    fill_rect(p, 20, 58, 23, 61, purple)

def draw_namekian(p, w, h):
    green = (80, 200, 100, 255)
    pink_patches = (230, 140, 160, 255)
    dark_eyes = (10, 10, 10, 255)
    gi_purple = (70, 40, 120, 255)
    
    draw_base_humanoid_skin(p, green, dark_eyes, gi_purple, gi_purple)
    fill_rect(p, 44, 22, 47, 26, pink_patches)
    fill_rect(p, 36, 54, 39, 58, pink_patches)

def draw_majin(p, w, h):
    pink = (255, 150, 200, 255)
    black_eyes = (20, 20, 20, 255)
    white_pants = (245, 245, 245, 255)
    gold_belt = (240, 200, 30, 255)
    
    draw_base_humanoid_skin(p, pink, black_eyes, pink, white_pants)
    fill_rect(p, 20, 28, 27, 29, gold_belt)

def draw_bio_android(p, w, h):
    green = (60, 170, 80, 255)
    dark_spots = (20, 60, 30, 255)
    orange_eyes = (255, 140, 0, 255)
    black_armor = (30, 30, 35, 255)
    
    draw_base_humanoid_skin(p, green, orange_eyes, black_armor, green)
    p[22][22] = dark_spots; p[24][25] = dark_spots
    p[52][22] = dark_spots; p[54][40] = dark_spots

def draw_saiyan(p, w, h):
    skin = (255, 204, 153, 255)
    armor_blue = (20, 40, 100, 255)
    armor_white = (230, 230, 230, 255)
    draw_base_humanoid_skin(p, skin, (15, 15, 15, 255), armor_blue, armor_blue)
    fill_rect(p, 21, 20, 26, 27, armor_white)

def draw_human(p, w, h):
    skin = (255, 204, 153, 255)
    gi_orange = (230, 90, 20, 255)
    gi_blue = (20, 40, 110, 255)
    draw_base_humanoid_skin(p, skin, (15, 15, 15, 255), gi_orange, gi_blue)

def draw_yardrat(p, w, h):
    pink_skin = (240, 180, 200, 255)
    yardrat_blue = (40, 120, 200, 255)
    yardrat_pink = (230, 100, 160, 255)
    draw_base_humanoid_skin(p, pink_skin, (15, 15, 15, 255), yardrat_pink, yardrat_blue)

def draw_tail(p, w, h):
    brown = (140, 90, 40, 255)
    dark_brown = (100, 60, 25, 255)
    fill_rect(p, 0, 0, 63, 63, brown)
    for y in range(0, 64, 8):
        fill_rect(p, 0, y, 63, y + 1, dark_brown)

races_dir = r"src/main/resources/assets/dragonblockarcanedba/textures/entity/races"
entity_dir = r"src/main/resources/assets/dragonblockarcanedba/textures/entity"
os.makedirs(races_dir, exist_ok=True)
os.makedirs(entity_dir, exist_ok=True)

skins = {
    os.path.join(races_dir, "arcosian.png"): draw_arcosian,
    os.path.join(races_dir, "saiyan.png"): draw_saiyan,
    os.path.join(races_dir, "half_saiyan.png"): draw_saiyan,
    os.path.join(races_dir, "human.png"): draw_human,
    os.path.join(races_dir, "majin.png"): draw_majin,
    os.path.join(races_dir, "namekian.png"): draw_namekian,
    os.path.join(races_dir, "tuffle.png"): draw_human,
    os.path.join(races_dir, "neo_tuffle.png"): draw_human,
    os.path.join(races_dir, "bio_android.png"): draw_bio_android,
    os.path.join(races_dir, "android.png"): draw_human,
    os.path.join(races_dir, "yardrat.png"): draw_yardrat,
    os.path.join(entity_dir, "tail.png"): draw_tail,
}

for path, func in skins.items():
    with open(path, "wb") as f:
        f.write(create_skin_png(64, 64, func))
    print(f"Generated skin texture {path}")
