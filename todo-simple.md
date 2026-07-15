# Dragon Block Arcane (DBA) — Simple Project Guide

Welcome to the simple guide for the Dragon Block Arcane DBA mod! This document explains what the mod does, how the systems connect, what is already working, and what needs to be added before it is ready for a professional release—written in plain English.

---

## 🌟 How the Mod Works (The Big Picture)

The mod is designed to bring Dragon Ball mechanics into Minecraft. Instead of coding everything permanently into the game, we made the mod **future-proof** by using a "data-driven" design. This means things like races and transformations are loaded from simple text files (JSONs). If you want to add a new form or race later, you can just add a text file without writing any code!

Here is how the core systems work:
1. **Races & Custom Stats:** You have stats like Strength, Defense, Agility, and Ki Capacity. Different races (like Saiyans, Namekians, or Humans) scale these stats differently.
2. **Energy (Ki) & Forms:** Tapping into your power drains Ki. If you transform into a Super Saiyan, your stats multiply, but your Ki starts draining. If your Ki hits zero, you automatically power down.
3. **Custom Planets & Space Pods:** Right-clicking a Space Pod item opens a menu where you can fly to Namek, Vegeta, or Yardrat. Each planet has custom rules: Vegeta has heavy gravity (making you fall faster), and Yardrat has low oxygen (requiring a space helmet or causing damage).
3. **Interactive Menus:** Pressing `V` opens a sleek, customized character screen where you can upgrade your stats, view your forms, and check your masteries. 

---

## ✅ What is Already Built & Working

Here is a list of features that are fully completed, tested, and working in the game:
* **The Stat Engine:** Calculations for leveling up, stat scaling, and Ki regeneration/drain.
* **Saving & Loading:** A reliable system that saves your race, level, stats, and masteries to your player file so you never lose progress.
* **11 Playable Races:** Saiyan, Half-Saiyan, Namekian, Arcosian, Human, Yardrat, Majin, Bio-Android, Tuffle (+ hidden: Android, Neo-Tuffle via admin command).
* **Super Saiyan 1 Transformation:** Fully functional, including stat multipliers and Ki drain.
* **Space Pod & Planetary Travel:** The item, the destination screen, and the teleportation code to go between Earth and the custom planets.
* **Planet Physics:** High gravity on Vegeta, low gravity on Yardrat, and oxygen depletion codes.
* **Sleek Character Screen:** The customized `V` key menu with tabs for Stats, Forms, and Techniques, now fully textured with high-quality UI assets.
* **Transformed Auras:** Glowing, animated particles that float around you when you transform.
* **Automatic Backups:** The build tool (`build.py`) automatically compiles the mod, puts it in the `mod/` directory, and keeps a backup of the previous version.
* **Advanced Stat Engine Features:** Defense stat calculation (caps at 55% mitigation), Vitality max health scaling, Strength-based stamina drain on punches, Sprinting stamina drain, and a 5000-point hardcap on all attributes.
* **Arcosian Universal Survival:** Passive underwater breathing and survival logic implemented.
* **ModMenu Configuration:** In-game configuration screen works natively on 26.2 using ModMenu 20.0.1.

---

## ❌ What is Left to Do (For a Professional Release)

To make the mod a polished, public-ready release, the following systems need to be completed:

1. **Race Selection Screen on First Join:**
   - [x] A popup screen when a player joins the world for the first time so they can pick their starting race (Human, Saiyan, Half-Saiyan, Namekian, Arcosian, Yardrat, Majin, Bio-Android, or Tuffle).
2. **In-Game HUD Overlay (Ki & Stamina Bars):**
   - [x] Custom top-left HUD overlay with numerical bars for Health, Ki, and Stamina. Vanilla hearts are hidden.
   - [x] Integrate Dexterity to increase speed and stamina limit.
   - [x] Integrate Strength to increase physical damage and appropriately increase stamina drain on attacks.
   - [x] Every punch should take away stamina, scaling with Strength.
   - [x] Sprinting takes away stamina., you won't be able to sprint.

3. **Admin Commands & Hidden Races:**
   - [x] Operator command `/dba race set <player> <race_id>` to change a player's race (Includes auto-complete and /dba help).
   - [x] Add two locked races that are only obtainable via this command: **Android** and **Neo-Truffle**. Changing a player's race preserves their allocated stats, unspent points (AP), and total level, but resets their unlocked technique tree.
4. **Specialized Race Technique Trees:**
   - [x] Every race gets a unique set of abilities.
   - **Yardrat:** Instant Transmission, Cloning Technique, Size Alteration, Healing (Instant Health I), Spirit Fission, Chroma Stasis, Fly, Meditation.
   - **Human:** Dodon Ray, Sky Dance, Multi-Form (shadow clones with 50% stats), Solar Flare, Overdrive (temporary stat double), Fly, Meditation.
   - **Namekian:** Gigantification (increases reach and damage, but makes you a bigger target), Clothes Beam, Regeneration, Magic Materialization, Namekian Fusion (absorbing NPCs on planet Namek for stats), Terrestrial Whip, Fly, Meditation.
   - **Saiyan:** Zenkai Boost (chance to get permanent stats when near death), Oozaru Form, Dragon Fist, Fly, Meditation.
   - **Half-Saiyan:** Zenkai Boost (temporary stat surge), Oozaru Form, Dragon Fist, Fly, Meditation.
   - **Majin:** Regeneration, Absorb, Fission, Mirage of the Pink Horizon, Fly, Meditation.
   - **Bio-Android:** Regeneration, Instant Transmission, Zenkai Boost, Multi-Form, Swarm, Fly, Meditation.
   - **Tuffle:** Fly, Meditation.
   - **Arcosian:** Fly, Meditation. (Universal Survival passive ability is fully completed)
5. **Custom Geology Blocks:**
   - [x] Adding custom grass and stone blocks for Namek, Vegeta, and Yardrat dimensions (e.g. crimson grass/iron rock for Vegeta, teal grass/green stone for Namek). Registered using MC 26.2's `setId()` pattern with vanilla placeholder textures.
6. **"Ki Technique" Move Customizer:**
   - [x] A sub-menu inside the character panel where players can craft custom moves by choosing: Projectile vs Beam, Charge Time, Size, and Color. The mod automatically calculates the Ki cost based on these custom properties.
7. **Planet Gravity Tick Loop:**
   - [x] Activating the gravity and oxygen loops on custom planet dimensions.
8. **Form Unlock Checks & Safety:**
   - [x] Checking stats and levels before letting a player transform.
9. **Death & Respawn Configs:**
    - [x] Resetting Ki and clearing active transformations on player death. 10% XP penalty applied on death.
10. **sound.json Mapping:**
    - [x] Sound event configuration linking mod sound calls to actual `.ogg` audio files. Created `sounds.json` manifest for all 7 sound events.
11. **Config File & Client Settings:**
    - [x] Saving config settings to local folders. `DbaConfig.load()` called on client init, persists to `config/dragonblockarcanedba.json`.
12. **Custom Race Models & Animations:**
    - Every race gets its own distinct 3D model. 
    - Transformations must have high-quality custom visual effects and unique textures (no simple vanilla-style textures).
