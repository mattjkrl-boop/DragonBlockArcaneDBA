"""
Generates the authentic 64x64 Dragon Ball Martial Arts base player skin and pixel color mask.
Ensures 100% full coverage for both Left Leg and Right Leg, Torso, Arms, and Head.
"""
from PIL import Image

def generate():
    width = 64
    height = 64
    
    # 1. Base skin image (RGBA)
    skin_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    # 2. Mask image (RGB)
    # Red (255,0,0) = Skin
    # Green (0,255,0) = Hair
    # Black (0,0,0) = Clothing/Unmodified
    mask_img = Image.new("RGB", (width, height), (0, 0, 0))
    
    pixels = skin_img.load()
    mask = mask_img.load()
    
    # Palette
    SKIN_BASE = (240, 200, 180, 255) # Neutral peach/skin
    SKIN_SHADE = (215, 175, 155, 255)
    HAIR_BASE = (60, 60, 60, 255)     # Neutral dark/tintable
    HAIR_SHADE = (40, 40, 40, 255)
    
    # DBZ Gi Clothes Colors (Goku/Martial Arts Gi)
    GI_ORANGE = (228, 88, 38, 255)
    GI_ORANGE_SHADE = (195, 70, 28, 255)
    NAVY_BLUE = (35, 52, 98, 255)
    NAVY_SHADE = (25, 38, 72, 255)
    BOOT_DARK = (30, 32, 45, 255)
    BOOT_TRIM = (220, 45, 45, 255)
    BOOT_CORD = (230, 190, 40, 255)
    WHITE = (255, 255, 255, 255)
    BLACK = (20, 20, 20, 255)

    def fill_rect(x1, y1, x2, y2, color, mask_type=None):
        for y in range(y1, y2):
            for x in range(x1, x2):
                pixels[x, y] = color
                if mask_type == "skin":
                    mask[x, y] = (255, 0, 0)
                elif mask_type == "hair":
                    mask[x, y] = (0, 255, 0)
                else:
                    mask[x, y] = (0, 0, 0)

    # ==================== 1. HEAD (0..32, 0..16) ====================
    # Top (Hair): 8..16, 0..8
    fill_rect(8, 0, 16, 8, HAIR_BASE, "hair")
    # Bottom (Neck/Chin): 16..24, 0..8
    fill_rect(16, 0, 24, 8, SKIN_BASE, "skin")
    # Right (0..8, 8..16) - Hair with ear
    fill_rect(0, 8, 8, 16, HAIR_BASE, "hair")
    fill_rect(4, 11, 7, 14, SKIN_BASE, "skin") # Ear
    
    # Front (Face): 8..16, 8..16
    fill_rect(8, 8, 16, 16, SKIN_BASE, "skin")
    # Hairline on forehead
    fill_rect(8, 8, 16, 10, HAIR_BASE, "hair")
    pixels[8, 10] = HAIR_BASE; mask[8, 10] = (0, 255, 0)
    pixels[15, 10] = HAIR_BASE; mask[15, 10] = (0, 255, 0)
    pixels[11, 10] = HAIR_BASE; mask[11, 10] = (0, 255, 0) # Center hair peak
    # Eyes & Eyebrows
    pixels[9, 11] = BLACK; pixels[10, 11] = BLACK; mask[9, 11] = (0, 0, 0); mask[10, 11] = (0, 0, 0) # Brow L
    pixels[13, 11] = BLACK; pixels[14, 11] = BLACK; mask[13, 11] = (0, 0, 0); mask[14, 11] = (0, 0, 0) # Brow R
    pixels[9, 12] = WHITE; pixels[10, 12] = BLACK; mask[9, 12] = (0, 0, 0); mask[10, 12] = (0, 0, 0) # Eye L
    pixels[13, 12] = BLACK; pixels[14, 12] = WHITE; mask[13, 12] = (0, 0, 0); mask[14, 12] = (0, 0, 0) # Eye R
    pixels[11, 14] = SKIN_SHADE; pixels[12, 14] = SKIN_SHADE # Nose/mouth
    
    # Left (16..24, 8..16) - Hair with ear
    fill_rect(16, 8, 24, 16, HAIR_BASE, "hair")
    fill_rect(17, 11, 20, 14, SKIN_BASE, "skin") # Ear
    
    # Back (24..32, 8..16) - Hair
    fill_rect(24, 8, 32, 16, HAIR_BASE, "hair")

    # ==================== 2. HEAD OVERLAY (32..64, 0..16) ====================
    # Outer hair volume and spikes
    fill_rect(32, 0, 64, 16, (0, 0, 0, 0)) # default transparent
    # Top hair spikes
    fill_rect(40, 0, 48, 8, HAIR_BASE, "hair")
    # Front fringe bangs
    pixels[41, 9] = HAIR_BASE; mask[41, 9] = (0, 255, 0)
    pixels[42, 9] = HAIR_BASE; mask[42, 9] = (0, 255, 0)
    pixels[42, 10] = HAIR_BASE; mask[42, 10] = (0, 255, 0)
    pixels[44, 9] = HAIR_BASE; mask[44, 9] = (0, 255, 0)
    pixels[45, 9] = HAIR_BASE; mask[45, 9] = (0, 255, 0)
    pixels[45, 10] = HAIR_BASE; mask[45, 10] = (0, 255, 0)
    # Sideburns
    fill_rect(32, 8, 36, 12, HAIR_BASE, "hair")
    fill_rect(48, 8, 52, 12, HAIR_BASE, "hair")
    # Back hair volume
    fill_rect(56, 8, 64, 16, HAIR_BASE, "hair")

    # ==================== 3. TORSO / BODY (16..40, 16..32) ====================
    # Top (Neck & Shoulders): 20..28, 16..20
    fill_rect(20, 16, 28, 20, GI_ORANGE)
    fill_rect(22, 17, 26, 20, SKIN_BASE, "skin") # Neck opening
    
    # Bottom (Pelvis): 28..36, 16..20
    fill_rect(28, 16, 36, 20, NAVY_BLUE) # Undershirt / pants top
    
    # Sides: 16..20, 20..32 and 28..32, 20..32
    fill_rect(16, 20, 20, 32, GI_ORANGE)
    fill_rect(28, 20, 32, 32, GI_ORANGE)
    fill_rect(16, 28, 20, 30, NAVY_BLUE) # Belt side
    fill_rect(28, 28, 32, 30, NAVY_BLUE) # Belt side
    
    # Front (20..28, 20..32): DBZ Gi Chest
    fill_rect(20, 20, 28, 28, GI_ORANGE)
    # V-neck opening showing skin and blue undershirt
    fill_rect(23, 20, 25, 23, SKIN_BASE, "skin")
    pixels[22, 21] = NAVY_BLUE; pixels[25, 21] = NAVY_BLUE
    pixels[22, 22] = NAVY_BLUE; pixels[25, 22] = NAVY_BLUE
    pixels[23, 23] = NAVY_BLUE; pixels[24, 23] = NAVY_BLUE
    # Navy Belt Sash
    fill_rect(20, 28, 28, 30, NAVY_BLUE)
    pixels[22, 30] = NAVY_SHADE; pixels[22, 31] = NAVY_SHADE # Sash knot tail
    fill_rect(20, 30, 28, 32, GI_ORANGE) # Gi skirt/bottom
    
    # Back (32..40, 20..32): DBZ Gi Back
    fill_rect(32, 20, 40, 28, GI_ORANGE)
    fill_rect(32, 28, 40, 30, NAVY_BLUE) # Belt
    fill_rect(32, 30, 40, 32, GI_ORANGE) # Gi bottom

    # ==================== 4. RIGHT ARM (40..56, 16..32) ====================
    # Top (Shoulder): 44..48, 16..20
    fill_rect(44, 16, 48, 20, GI_ORANGE)
    # Bottom (Hand palm): 48..52, 16..20
    fill_rect(48, 16, 52, 20, SKIN_BASE, "skin")
    
    # Arm length: 40..56, 20..32
    # Gi shoulder cap: 20..22
    fill_rect(40, 20, 56, 22, GI_ORANGE)
    # Bare muscular arm: 22..28 (Skin!)
    fill_rect(40, 22, 56, 28, SKIN_BASE, "skin")
    # Navy Wristband: 28..30
    fill_rect(40, 28, 56, 30, NAVY_BLUE)
    # Hand: 30..32 (Skin!)
    fill_rect(40, 30, 56, 32, SKIN_BASE, "skin")

    # ==================== 5. LEFT ARM (32..48, 48..64) ====================
    # Top (Shoulder): 36..40, 48..52
    fill_rect(36, 48, 40, 52, GI_ORANGE)
    # Bottom (Hand palm): 40..44, 48..52
    fill_rect(40, 48, 44, 52, SKIN_BASE, "skin")
    
    # Arm length: 32..48, 52..64
    # Gi shoulder cap: 52..54
    fill_rect(32, 52, 48, 54, GI_ORANGE)
    # Bare muscular arm: 54..60 (Skin!)
    fill_rect(32, 54, 48, 60, SKIN_BASE, "skin")
    # Navy Wristband: 60..62
    fill_rect(32, 60, 48, 62, NAVY_BLUE)
    # Hand: 62..64 (Skin!)
    fill_rect(32, 62, 48, 64, SKIN_BASE, "skin")

    # ==================== 6. RIGHT LEG (0..16, 16..32) ====================
    # Top (Pants upper): 4..8, 16..20
    fill_rect(4, 16, 8, 20, GI_ORANGE)
    # Bottom (Boot sole): 8..12, 16..20
    fill_rect(8, 16, 12, 20, BOOT_DARK)
    
    # Leg length: 0..16, 20..32
    # Orange Gi Pants: 20..27
    fill_rect(0, 20, 16, 27, GI_ORANGE)
    # DBZ Combat Boots: 27..32
    fill_rect(0, 27, 16, 32, BOOT_DARK)
    # Red trim & Yellow rope cords on boots
    for x in range(0, 16):
        pixels[x, 27] = BOOT_TRIM
        if x % 2 == 0:
            pixels[x, 29] = BOOT_CORD

    # ==================== 7. LEFT LEG (16..32, 48..64) [COMPLETELY FIXED!] ====================
    # Top (Pants upper): 20..24, 48..52
    fill_rect(20, 48, 24, 52, GI_ORANGE)
    # Bottom (Boot sole): 24..28, 48..52
    fill_rect(24, 48, 28, 52, BOOT_DARK)
    
    # Leg length: 16..32, 52..64
    # Orange Gi Pants: 52..59
    fill_rect(16, 52, 32, 59, GI_ORANGE)
    # DBZ Combat Boots: 59..64
    fill_rect(16, 59, 32, 64, BOOT_DARK)
    # Red trim & Yellow rope cords on boots
    for x in range(16, 32):
        pixels[x, 59] = BOOT_TRIM
        if x % 2 == 0:
            pixels[x, 61] = BOOT_CORD

    out_skin = r"src\main\resources\assets\dragonblockarcanedba\textures\entity\player\base.png"
    out_mask = r"src\main\resources\assets\dragonblockarcanedba\textures\entity\player\base_mask.png"
    skin_img.save(out_skin)
    mask_img.save(out_mask)
    print(f"[+] Successfully generated 64x64 DBZ Base Skin to: {out_skin}")
    print(f"[+] Successfully generated 64x64 DBZ Skin Mask to: {out_mask}")

if __name__ == "__main__":
    generate()
