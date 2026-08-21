import math
from PIL import Image, ImageDraw, ImageFilter
import os

EFFECT_DIR = r"src\main\resources\assets\dragonblockarcanedba\textures\mob_effect"
os.makedirs(EFFECT_DIR, exist_ok=True)

def create_ancient_weight():
    # 64x64 Sacred gold heavy anvil/weight with glowing runes
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Outer glow (golden energy)
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    
    # Grounding heavy weight base / anvil silhouette
    # Draw anvil base
    gdraw.polygon([(14, 52), (50, 52), (46, 44), (18, 44)], fill=(255, 215, 0, 100))
    # Anvil pillar
    gdraw.polygon([(24, 44), (40, 44), (38, 30), (26, 30)], fill=(255, 215, 0, 100))
    # Anvil top / horns
    gdraw.polygon([(10, 20), (54, 20), (52, 30), (14, 30)], fill=(255, 215, 0, 100))
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    img.paste(glow, (0, 0), glow)
    
    # Shadow / Outline
    draw.polygon([(12, 54), (52, 54), (48, 43), (16, 43)], fill=(40, 25, 5, 255))
    draw.polygon([(22, 45), (42, 45), (40, 28), (24, 28)], fill=(40, 25, 5, 255))
    draw.polygon([(8, 18), (56, 18), (54, 32), (12, 32)], fill=(40, 25, 5, 255))
    
    # Main Bronze / Golden Anvil Body
    # Top horn & face
    draw.polygon([(10, 20), (54, 20), (52, 30), (14, 30)], fill=(184, 134, 11, 255)) # Dark Goldenrod
    # Top highlight strip
    draw.line([(12, 21), (52, 21)], fill=(255, 235, 140, 255), width=2)
    draw.line([(12, 23), (50, 23)], fill=(218, 165, 32, 255), width=2) # Goldenrod
    
    # Mid body
    draw.polygon([(24, 30), (40, 30), (38, 44), (26, 44)], fill=(150, 105, 10, 255))
    draw.line([(26, 31), (28, 43)], fill=(240, 200, 80, 255), width=2) # Left pillar highlight
    draw.line([(38, 31), (36, 43)], fill=(100, 70, 5, 255), width=2)  # Right pillar shadow
    
    # Base
    draw.polygon([(14, 44), (50, 44), (48, 52), (16, 52)], fill=(184, 134, 11, 255))
    draw.line([(16, 45), (48, 45)], fill=(255, 220, 100, 255), width=1)
    draw.line([(18, 51), (46, 51)], fill=(90, 60, 5, 255), width=1)
    
    # Heavy Weight glyph / Kaioshin seal rune in center top
    # Glowing Kanji '重' (Weight / Gravity) or Sacred Diamond Rune
    draw.polygon([(32, 23), (36, 26), (32, 29), (28, 26)], fill=(255, 255, 200, 255))
    draw.point([(32, 24), (32, 28), (30, 26), (34, 26)], fill=(255, 255, 255, 255))
    
    # Crackling downward gravity pressure lines & floating golden ki motes
    draw.line([(32, 10), (32, 16)], fill=(255, 215, 0, 220), width=2) # Top central energy pulse
    draw.polygon([(29, 14), (35, 14), (32, 18)], fill=(255, 240, 150, 255))
    
    # Golden particles / spark runes
    draw.point([(10, 14), (54, 14), (16, 38), (48, 38), (8, 48), (56, 48)], fill=(255, 235, 100, 255))
    draw.point([(11, 14), (53, 14), (17, 38), (47, 38)], fill=(255, 255, 200, 255))
    
    # Downward force arrows/chevrons at sides
    draw.line([(18, 8), (18, 14)], fill=(218, 165, 32, 200), width=1)
    draw.line([(46, 8), (46, 14)], fill=(218, 165, 32, 200), width=1)
    draw.polygon([(16, 13), (20, 13), (18, 16)], fill=(255, 215, 0, 230))
    draw.polygon([(44, 13), (48, 13), (46, 16)], fill=(255, 215, 0, 230))
    
    return img

def create_blade_guard():
    # 64x64 Crossed Katana blades forming defensive parry with gleaming barrier
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Defensive shield barrier glow behind
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    
    # Hexagonal / Diamond parry shield aura
    gdraw.polygon([(32, 6), (56, 20), (56, 44), (32, 58), (8, 44), (8, 20)], outline=(180, 230, 255, 180), width=3)
    gdraw.ellipse([(20, 20), (44, 44)], fill=(220, 245, 255, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(2))
    img.paste(glow, (0, 0), glow)
    
    draw = ImageDraw.Draw(img)
    
    # Crisp shield outline
    draw.polygon([(32, 8), (54, 21), (54, 43), (32, 56), (10, 43), (10, 21)], outline=(200, 240, 255, 230), width=2)
    
    # Crossed Katana Blades
    # Blade 1: Top-Left (12, 12) to Bottom-Right (52, 52)
    # Dark outline
    draw.line([(10, 10), (54, 54)], fill=(30, 40, 50, 255), width=5)
    # Steel blade
    draw.line([(12, 12), (52, 52)], fill=(210, 225, 235, 255), width=3)
    # Sharp edge highlight
    draw.line([(11, 13), (49, 51)], fill=(255, 255, 255, 255), width=1)
    
    # Blade 2: Top-Right (52, 12) to Bottom-Left (12, 52)
    # Dark outline
    draw.line([(54, 10), (10, 54)], fill=(30, 40, 50, 255), width=5)
    # Steel blade
    draw.line([(52, 12), (12, 52)], fill=(225, 235, 245, 255), width=3)
    # Sharp edge highlight
    draw.line([(51, 13), (13, 51)], fill=(255, 255, 255, 255), width=1)
    
    # Center Clash Spark / Parry Burst
    draw.polygon([(32, 24), (35, 30), (41, 32), (35, 34), (32, 40), (29, 34), (23, 32), (29, 30)], fill=(255, 255, 255, 255))
    draw.polygon([(32, 28), (34, 31), (37, 32), (34, 33), (32, 36), (30, 33), (27, 32), (30, 31)], fill=(160, 240, 255, 255))
    
    # Katana Tsuba (Guards) & Tsuka (Hilts) at bottom corners
    # Left hilt
    draw.line([(14, 50), (8, 56)], fill=(180, 30, 30, 255), width=4) # Red wrap
    draw.line([(16, 48), (12, 52)], fill=(220, 180, 50, 255), width=3) # Gold tsuba
    # Right hilt
    draw.line([(50, 50), (56, 56)], fill=(180, 30, 30, 255), width=4) # Red wrap
    draw.line([(48, 48), (52, 52)], fill=(220, 180, 50, 255), width=3) # Gold tsuba
    
    # Radiating defensive spark particles
    draw.point([(32, 14), (32, 50), (18, 32), (46, 32)], fill=(255, 255, 255, 255))
    draw.point([(24, 24), (40, 24), (24, 40), (40, 40)], fill=(180, 230, 255, 255))
    
    return img

def create_bleeding():
    # 64x64 Crimson blood drops with wind laceration cuts and dripping splatter
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Blood aura / glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([(22, 22), (42, 48)], fill=(180, 0, 0, 130))
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    img.paste(glow, (0, 0), glow)
    
    draw = ImageDraw.Draw(img)
    
    # Wind cut / Slash Laceration marks (diagonal sharp slashing lines)
    draw.line([(8, 14), (36, 6)], fill=(255, 80, 80, 190), width=2)
    draw.line([(28, 56), (56, 48)], fill=(255, 80, 80, 190), width=2)
    
    # Big central tear-drop blood droplet
    # Droplet top triangle
    drop_outline = [(32, 12), (44, 38), (42, 48), (32, 54), (22, 48), (20, 38)]
    draw.polygon(drop_outline, fill=(60, 0, 0, 255)) # Dark crimson outline
    
    drop_body = [(32, 15), (42, 38), (40, 46), (32, 52), (24, 46), (22, 38)]
    draw.polygon(drop_body, fill=(180, 10, 10, 255)) # Deep red
    
    # Inner blood core highlight & gradient
    draw.ellipse([(25, 34), (39, 48)], fill=(220, 20, 20, 255)) # Bright red
    # White glossy reflection shine
    draw.polygon([(26, 32), (30, 24), (31, 25), (28, 36)], fill=(255, 210, 210, 255))
    draw.ellipse([(26, 36), (30, 42)], fill=(255, 230, 230, 255))
    
    # Secondary smaller blood drop on right
    small_drop = [(48, 26), (54, 38), (52, 44), (48, 48), (44, 44), (42, 38)]
    draw.polygon(small_drop, fill=(50, 0, 0, 255))
    small_inner = [(48, 28), (53, 38), (51, 43), (48, 46), (45, 43), (43, 38)]
    draw.polygon(small_inner, fill=(190, 15, 15, 255))
    draw.point([(46, 36), (46, 37), (47, 34)], fill=(255, 220, 220, 255))
    
    # Small dripping droplet on bottom-left
    mini_drop = [(16, 38), (20, 46), (18, 50), (16, 52), (14, 50), (12, 46)]
    draw.polygon(mini_drop, fill=(50, 0, 0, 255))
    mini_inner = [(16, 40), (19, 46), (17, 49), (16, 50), (15, 49), (13, 46)]
    draw.polygon(mini_inner, fill=(200, 20, 20, 255))
    draw.point([(15, 44), (15, 45)], fill=(255, 200, 200, 255))
    
    # Splatter droplets
    draw.ellipse([(31, 56), (33, 58)], fill=(180, 10, 10, 255))
    draw.ellipse([(38, 52), (40, 54)], fill=(200, 20, 20, 255))
    draw.ellipse([(10, 32), (12, 34)], fill=(180, 10, 10, 255))
    
    return img

def create_celestial_grace():
    # 64x64 Divine cyan/white angelic wings and Whis Angel Ring with starlight
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Celestial aura glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    # Halo glow
    gdraw.ellipse([(18, 8), (46, 36)], outline=(0, 255, 255, 180), width=4)
    # Wings glow
    gdraw.polygon([(32, 30), (10, 16), (4, 32), (16, 50), (32, 42)], fill=(0, 230, 255, 120))
    gdraw.polygon([(32, 30), (54, 16), (60, 32), (48, 50), (32, 42)], fill=(0, 230, 255, 120))
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    img.paste(glow, (0, 0), glow)
    
    draw = ImageDraw.Draw(img)
    
    # Left Angelic Wing (Layered white & cyan feathers)
    # Wing outline
    draw.polygon([(30, 36), (14, 18), (6, 26), (12, 38), (18, 48), (28, 44)], fill=(0, 60, 80, 255))
    # Wing body (Cyan feather base)
    draw.polygon([(30, 35), (15, 20), (8, 27), (13, 37), (19, 46), (28, 43)], fill=(0, 200, 230, 255))
    # Inner white feather highlights
    draw.polygon([(28, 34), (18, 22), (12, 28), (16, 36), (22, 42)], fill=(220, 255, 255, 255))
    draw.line([(18, 22), (28, 34)], fill=(255, 255, 255, 255), width=2)
    draw.line([(12, 28), (24, 38)], fill=(255, 255, 255, 255), width=1)
    
    # Right Angelic Wing (Mirrored)
    draw.polygon([(34, 36), (50, 18), (58, 26), (52, 38), (46, 48), (36, 44)], fill=(0, 60, 80, 255))
    draw.polygon([(34, 35), (49, 20), (56, 27), (51, 37), (45, 46), (36, 43)], fill=(0, 200, 230, 255))
    draw.polygon([(36, 34), (46, 22), (52, 28), (48, 36), (42, 42)], fill=(220, 255, 255, 255))
    draw.line([(46, 22), (36, 34)], fill=(255, 255, 255, 255), width=2)
    draw.line([(52, 28), (40, 38)], fill=(255, 255, 255, 255), width=1)
    
    # Whis Angel Floating Ring / Divine Halo (Tilted 3D Ring)
    draw.ellipse([(20, 10), (44, 34)], outline=(0, 80, 100, 255), width=4)
    draw.ellipse([(21, 11), (43, 33)], outline=(0, 255, 255, 255), width=2)
    draw.ellipse([(22, 12), (42, 32)], outline=(220, 255, 255, 255), width=1)
    
    # Central Angelic Diamond Core
    draw.polygon([(32, 24), (38, 34), (32, 44), (26, 34)], fill=(0, 80, 100, 255))
    draw.polygon([(32, 26), (36, 34), (32, 42), (28, 34)], fill=(200, 255, 255, 255))
    draw.polygon([(32, 28), (35, 34), (32, 40), (29, 34)], fill=(255, 255, 255, 255))
    
    # Divine Starlight cross sparkles
    # Top-right sparkle
    draw.line([(48, 8), (48, 14)], fill=(255, 255, 255, 255), width=1)
    draw.line([(45, 11), (51, 11)], fill=(255, 255, 255, 255), width=1)
    draw.point([(48, 11)], fill=(255, 255, 255, 255))
    
    # Top-left sparkle
    draw.line([(16, 10), (16, 16)], fill=(255, 255, 255, 255), width=1)
    draw.line([(13, 13), (19, 13)], fill=(255, 255, 255, 255), width=1)
    
    # Bottom starlight drops
    draw.point([(32, 50), (32, 54), (28, 48), (36, 48)], fill=(180, 255, 255, 255))
    
    return img

def create_cinematic_tracking():
    # 64x64 Cinematic camera targeting crosshair / action lock reticle
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # Purple & Gold tech glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([(14, 14), (50, 50)], outline=(180, 80, 255, 180), width=3)
    gdraw.ellipse([(24, 24), (40, 40)], fill=(255, 215, 0, 100))
    glow = glow.filter(ImageFilter.GaussianBlur(3))
    img.paste(glow, (0, 0), glow)
    
    draw = ImageDraw.Draw(img)
    
    # Outer Tech Corner Brackets (Cinematic viewfinder frame)
    # Top-Left Bracket
    draw.line([(8, 18), (8, 8), (18, 8)], fill=(255, 215, 0, 255), width=2)
    # Top-Right Bracket
    draw.line([(46, 8), (56, 8), (56, 18)], fill=(255, 215, 0, 255), width=2)
    # Bottom-Left Bracket
    draw.line([(8, 46), (8, 56), (18, 56)], fill=(255, 215, 0, 255), width=2)
    # Bottom-Right Bracket
    draw.line([(46, 56), (56, 56), (56, 46)], fill=(255, 215, 0, 255), width=2)
    
    # Circular Targeting Ring
    draw.ellipse([(14, 14), (50, 50)], outline=(40, 10, 60, 255), width=3)
    draw.ellipse([(15, 15), (49, 49)], outline=(160, 60, 240, 255), width=2)
    
    # Crosshair tick notches (N, S, E, W)
    draw.line([(32, 8), (32, 16)], fill=(255, 215, 0, 255), width=2)
    draw.line([(32, 48), (32, 56)], fill=(255, 215, 0, 255), width=2)
    draw.line([(8, 32), (16, 32)], fill=(255, 215, 0, 255), width=2)
    draw.line([(48, 32), (56, 32)], fill=(255, 215, 0, 255), width=2)
    
    # Inner Precision Reticle / Focus Eye
    draw.ellipse([(22, 22), (42, 42)], outline=(220, 120, 255, 255), width=2)
    # Central Lock-on diamond
    draw.polygon([(32, 27), (37, 32), (32, 37), (27, 32)], fill=(255, 235, 120, 255))
    draw.point([(32, 32)], fill=(255, 255, 255, 255))
    
    # REC dot / Cinematic Indicator in top-left
    draw.ellipse([(14, 14), (18, 18)], fill=(255, 40, 40, 255))
    draw.point([(15, 15)], fill=(255, 200, 200, 255))
    
    # Diagonal lock indicator ticks
    draw.point([(20, 20), (44, 20), (20, 44), (44, 44)], fill=(255, 255, 255, 255))
    
    return img

def main():
    icons = {
        "ancient_weight.png": create_ancient_weight(),
        "blade_guard.png": create_blade_guard(),
        "bleeding.png": create_bleeding(),
        "celestial_grace.png": create_celestial_grace(),
        "cinematic_tracking.png": create_cinematic_tracking(),
    }
    
    for filename, img in icons.items():
        out_path = os.path.join(EFFECT_DIR, filename)
        img.save(out_path, "PNG")
        print(f"Generated {out_path} (size={img.size}, mode={img.mode})")

if __name__ == "__main__":
    main()
