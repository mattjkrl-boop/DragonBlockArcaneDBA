# Weapon Abilities Reference (after the spectacle pass)

Every weapon in Dragon Block Arcane, what each click does, and the real numbers from the current code. Damage formulas use your stats: Strength, Spirit, and Dexterity are your character stats. Ticks are game ticks (20 ticks = 1 second). All ability damage also passes through the global damage cap in the mod config.

How costs work: a cost written as "percent of max Ki per minute" is a drain rate. A flat percent (like 2.5 percent of max Ki) is what one use actually takes.

---

## Katana

Theme: supreme speed iaido blade. Pale crescents and a heaven splitting finisher.

**Melee (left click swing):** a plain swing deals the weapon damage (750) and costs nothing. No extra on-hit effect.

**Left click, Flashdraw (hold, then release):**
1. Hold left click to charge (max charge 100 ticks, 5 seconds). Charging drains Ki (40 percent of max Ki per minute, so a full charge costs about 3.33 percent of max Ki).
2. If your Ki runs out mid charge, the charge cancels: no dash, no damage, no cooldown. If you are immobilized when you release, the dash does not fire and the Ki you spent is refunded.
3. On release you dash through targets. The game finds the first target in front of you (search box about 16 blocks, scaled by your movement multiplier), then chains to up to 4 more targets near the previous one (12 block search), for a maximum chain of 5 targets.
4. Damage per target: (500 + Strength x 2.5) x streak bonus x charge multiplier. The streak bonus grows per chain step (1.0x, 1.2x, 1.4x, 1.6x, 1.8x). The charge multiplier is (0.55 + 0.75 x charge), so 0.55x on a tap and 1.30x at full charge.
5. Each target gets Silent Mark (20 ticks) and Cinematic Tracking (25 ticks). You teleport to just past each target as the chain runs.
6. If no target is found, you dash in a straight line instead: (8 + 6 x charge) blocks, so 8 blocks on a tap and 14 at full charge. This free dash deals no damage.
7. Cooldown: 60 ticks (3 seconds). Firing also gives you Blade Guard (20 ticks, 1 second).
8. Every dash leaves an afterimage behind: it lasts 100 ticks (5 seconds) and applies Dark Faded (40 ticks) to nearby enemies every 5 ticks (about a 3 block radius).

**Right click, Iaijutsu: Heaven Splitter (hold, then release):**
1. Hold right click to charge while standing still (max charge 100 ticks, 5 seconds). Charging drains Ki (60 percent of max Ki per minute, a full hold costs 5 percent of max Ki). If Ki empties, it cancels with no dash, no damage, and no cooldown. You need some Ki to start.
2. On release you teleport straight along your aim: (16 + 8 x charge) blocks, so 16.8 blocks at minimum charge and 24 blocks at full charge. You gain Blade Guard (20 ticks).
3. A wall of force is left along the dash path (lasts 40 ticks, 2 seconds). At tick 10 it strikes everything in the corridor. Corridor half width is (2 + 2 x charge) blocks, so 2.2 at minimum and 4.0 at full charge.
4. Wall damage: (600 + 1400 x charge) + (Strength x 4.0 x charge). At minimum charge that is 740 + Strength x 0.4; at full charge it is 2000 + Strength x 4.0. Damage falls off along the path, from 100 percent at the start end to 70 percent at the far end.
5. Hit targets get Cinematic Tracking (20 ticks). If the wall strike kills a target, the katana cooldown is removed.
6. Cooldown: 120 ticks (6 seconds).

---

## Z Sword

Theme: divine heavy sword. Ancient weight, gravity, and Katchin mass.

**Melee (left click swing):** a plain swing deals the weapon damage (1000) and costs nothing. On a hit it throws up debris and sparks only, no extra damage.

**Left click, Z Shockwave (hold, then release):**
1. Hold left click to charge (max charge 300 ticks, 15 seconds). At full charge the wave fires on its own while you are still holding.
2. Charging drains both resources per tick: Ki (75 percent of max Ki per minute) and Stamina (50 percent of max Stamina per minute). A full charge costs about 18.75 percent of max Ki and 12.5 percent of max Stamina. If either runs out, the wave fires with the charge you have.
3. While charging you have Ancient Weight (refreshed while charging), which grounds and slows you.
4. Wave damage: (150 + 1050 x charge) + (Strength x 3.5 x charge). At minimum charge (0.1) that is 255 + Strength x 0.35; at full charge it is 1200 + Strength x 3.5.
5. The wave travels along your aim at (1.4 + 1.4 x charge) blocks per tick and lives about 80 ticks (4 seconds) unless it hits a block. Its crescent shape grows with charge: half width (4.8 + 6.2 x charge) blocks, height (1.5 + 1.8 x charge) blocks. Each target is hit once, for the full wave damage (no falloff).
6. Knockback on hit: (1.8 + 2.2 x charge) along the wave direction, plus (0.4 + 0.3 x charge) up. Firing also recoils you backward.
7. At high charge (0.90 or more) the wave gains three extras:
   - It pierces through terrain instead of stopping at blocks.
   - At tick 3 it spawns 2 trailing sub waves behind it, each with 0.7x its charge, 0.45x its damage, and 0.65x its speed.
   - A direct hit applies Movement Curse at maximum strength (amplifier 9, 80 ticks, 4 seconds). If the ground 1 and 2 blocks below the target is solid, the target is also slammed 2 blocks down and takes 250 crush damage.
8. Cooldown: (40 + 80 x charge) ticks, so 48 ticks (2.4 seconds) at minimum charge and 120 ticks (6 seconds) at full charge.

**Right click, Katchin Weight (hold stance, then release for the Gravity Slam):**
1. Hold right click to take the stance. You are rooted in place and gain Ancient Weight at a stronger level (amplifier 1). The stance maxes out at 200 ticks (10 seconds) and then releases on its own.
2. The stance drains Stamina (75 percent of max Stamina per minute). No Ki cost. You need some Stamina to start.
3. While you hold, a gravity aura drags nearby mobs toward you with no lift. Its radius grows with hold time, from 3 blocks to (3 + 5 x power) = 8 blocks at full power, where power is held ticks / 200.
4. Release too early (under 10 ticks) and nothing fires: no slam, no cooldown, and the Stamina spent is not refunded.
5. On a real release, the slam radius is (6 + 12 x power) blocks: 6.6 blocks at the minimum hold and 18 blocks at full power. Slam damage is (400 + 800 x power) + (Strength x 3.0 x power): 440 + Strength x 0.15 at minimum, 1200 + Strength x 3.0 at full power.
6. The slam visual lives (80 + 40 x power) ticks and the damage lands at 30 percent of its life (24 ticks at minimum hold, 36 ticks at full power). Damage is full inside 45 percent of the radius, easing down to 30 percent at the rim. While the slam builds, a light vortex pins targets in the area.
7. Targets caught at release get Earth Shatter (60 ticks) and Movement Curse at maximum strength (amplifier 9) for the launch delay. The launch delay is (40 + 40 x power) ticks: 42 ticks at minimum hold, 80 ticks (4 seconds) at full power. When the delay ends, each target is flung outward (horizontal 1.2 + 1.8 x power, up 1.0 + 0.8 x power, scaled by the target weight class) and gains Bounciness and reduced air drag for 200 ticks (10 seconds).
8. The slam scars the ground with magma trench lines (14 + 6 x power trenches, restored after the slam life + 60 ticks) and throws a debris field (about 27 pieces at minimum hold, 62 at full power).
9. Cooldown: (100 + 100 x power) ticks, so 105 ticks (5.25 seconds) at minimum hold and 200 ticks (10 seconds) at full power.

---

## Saber

Theme: high speed phasing blitz weapon. Dimensional precision and void tears. Note: unlike most weapons here, the saber attack speed modifier is 0.0, so it swings at full vanilla speed before other modifiers.

**Melee (left click swing):** a plain swing deals the weapon damage (820) and costs nothing. No extra on-hit effect; the blitz and lunge below pay their own stamina.

**Left click, Blitz Flurry (hold, release, re-hold to chain):**
1. Hold left click. If a target is in front of you (search radius 64 blocks, line of sight required, and on a fresh click your aim must be tight, about a 16 degree cone), you fly to it on an arc and strike on arrival. There is no Ki cost; the blitz drains Stamina while it runs (100 percent of max Stamina per minute, about 1.67 percent per second).
2. Arrival damage per chained target: 280 + Strength x 1.8, split as 60 percent on arrival and 40 percent landing 5 ticks later. The target is locked down with Movement Curse and Cinematic Tracking until the sequence ends.
3. Release the click, then re-hold within 20 ticks (1 second) to chain to the next target. Each target can only be chained once per sequence.
4. Each arrival also raises your attack speed for 40 ticks (2 seconds): the bonus grows with your hit count (6.0 + 0.6 per hit, capped at +24.0).
5. If there is no target to fly to, holding instead does a fencing lunge every 6 ticks: instant damage (200 + Strength x 1.4) to the best target within 5.5 blocks in front of you. Each lunge attempt costs Stamina (0.5 percent of max Stamina).
6. The sequence ends when you release and let the window lapse, swap off the saber, run out of Stamina, or get immobilized. Ending by timeout, swap, or empty Stamina fires the finisher (below). Getting immobilized ends it with no finisher, and disconnecting cleans it up with no finisher.

**Finisher, Best Fit Line Snap (automatic when a blitz ends):**
1. All still living chained targets are pulled onto a single straight line through their group over an 18 tick (0.9 second) animation. With 1 target the line is your aim direction; with 2 it is the line between them; with 3 or more it is the dominant direction of the group.
2. At the end of the animation, every target takes finisher damage: 1000 + Strength x 4.5, split as 50 percent at the snap, 30 percent 8 ticks later, and 20 percent 16 ticks later. Each target also gains Bounciness (+0.30, about 20 ticks).
3. A dimensional line slash, a lightning bolt visual, and a debris field (about 28 pieces) play at the group center.
4. Cooldown: 100 ticks (5 seconds), only if at least one chained target was still alive.

**Right click, Flash Step:**
1. Pressing right click performs one step immediately. Holding right click performs another step every 7 ticks while charges remain.
2. You have 3 charges. One charge refills every 60 ticks (3 seconds) while you are below max. A perfect dodge (triggered elsewhere in combat) resets all 3 charges.
3. Each step costs Ki (2.5 percent of max Ki). Charges are checked first, then immobilization, then Ki, so a blocked step costs nothing.
4. If a target is within (16 x movement multiplier) blocks and roughly in front of you, you step to 1.5 blocks behind that target and turn to face its way. Otherwise you dash (7 x movement multiplier) blocks along your aim, phasing through a wall if there is clear space 1.5 blocks past it, otherwise stopping just before the wall.
5. Everything you cross takes path damage: 320 + Strength x 1.6, split 60 percent on the step and 40 percent 5 ticks later.
6. Each step leaves an afterimage (lasts 100 ticks, applies Dark Faded for 40 ticks to nearby enemies) and opens a void tear at both ends (20 ticks). Each step also opens a 15 tick (0.75 second) perfect dodge window.

---

## Brave Sword

Theme: heroic legendary sword of valor, gold and cyan. A combo assault that builds to a finisher, and a charged heroic dash.

**Melee (left click swing):** a plain swing deals the weapon damage (800) and costs nothing. No extra on-hit effect.

**Left click, Brave Sword Assault (hold):**
1. While you hold left click, strikes fire on an interval that speeds up as your combo grows: every 8 ticks at combo 0 to 1, down to every 4 ticks at combo 8 to 9.
2. Each strike launches a slash projectile (speed 1.8 blocks per tick, life 24 ticks, about 43 blocks) that deals (400 + Strength x 2.0). The slash stops if it hits a solid block. Each target can be hit once per slash, and takes knockback along the slash direction (1.4 horizontal, 0.25 up) plus Cinematic Tracking (25 ticks).
3. Strikes that connect raise your combo by 1. Every 5th combo hit is a heavy hit dealing 1.5x damage (600 + Strength x 3.0). Empty swings still fire a normal slash but do not raise the combo. The combo resets if more than 100 ticks (5 seconds) pass without a combo hit.
4. Each strike (hit or empty) drains a small amount per call: Ki (25 percent of max Ki per minute rate) and Stamina (20 percent of max Stamina per minute rate), which works out to about 0.021 percent of max Ki and 0.017 percent of max Stamina per strike. If the drain fails, the strike does not happen.
5. There is no cooldown on the assault itself.

**Assault finisher (automatic at combo 10):**
1. You teleport to 2 blocks past the target (or onto the target if that spot is inside a wall).
2. The primary target takes (900 + Strength x 4.0) instantly and gains Bounciness (+0.85, about 20 ticks).
3. A cross slash bursts at the target (radius 3.5 x 1.25 scale = about 4.4 blocks, life 40 ticks). It pulses at ticks 8, 14, 20, and 26, and each pulse deals 25 percent of the splash total. The splash total is half the finisher damage (450 + Strength x 2.0), so a target that stays for all 4 pulses takes the full splash. The primary target is excluded from the splash. First pulse contact also flings targets outward (0.9 horizontal, 0.45 up).
4. The combo resets to 0.

**Right click, Charged Piercing Heroic Dash (hold, then release):**
1. Hold right click to charge (max charge 100 ticks, 5 seconds). Charging drains Ki (15 percent of max Ki per minute, and only during the first 100 ticks; a full charge costs 1.25 percent of max Ki). You need some Ki to start. While charging you have Heroic Focus. If you are immobilized on release, no dash fires and there is no cooldown.
2. On release you teleport along your aim: (8 + 16 x charge) blocks, scaled by your movement multiplier. That is 8 blocks on an instant release and 24 blocks at full charge. There is no minimum charge floor, so an instant release has charge 0.
3. Dash damage: (600 + 1400 x charge) + (Strength x 4.0 x charge). Instant release: 600 flat. Full charge: 2000 + Strength x 4.0.
4. The damage resolves 6 ticks after the dash, measured fresh at that moment: everything in the corridor (inflated by 2 + 2.5 x charge) takes the full dash damage, Cinematic Tracking (20 ticks), Bounciness (+0.80, 20 ticks), reduced air drag (20 ticks), and a fling off the dash line (0.85 horizontal, 0.5 up).
5. If any target died from the dash, or the charge was 0.90 or higher, a heroic shockwave erupts at the dash end: radius (5 + 3.5 x charge) blocks, damage 40 percent of the dash damage per target, riding the expanding ring front so targets are hit as the ring reaches them. It also scars a small magma disk into the ground (restored after 300 ticks) and throws 28 debris pieces.
6. Cooldown: 100 ticks (5 seconds).

---

## Grand Sword

Theme: balanced legendary greatsword of the knight and crusader. Area damage, defense, ranged energy, and team utility.

**Melee (left click swing):** a plain swing deals the weapon damage (900) and costs nothing. A hit only bursts sparks at the target, no extra damage.

**Left click, Grand Cyclone (hold, release for the finisher):**
1. Hold left click to spin (max spin 140 ticks, 7 seconds). Spinning drains Ki (90 percent of max Ki per minute, about 1.5 percent per second). If the drain fails, the spin releases straight into the finisher.
2. You rotate faster the longer you spin (15 degrees per tick rising to 35) and drift forward slowly (0.06 blocks per tick).
3. Contact damage lands every 3 ticks on enemies within (2.6 to 3.5) blocks of you: (100 + Strength x 0.5) x ramp, where the ramp rises from 0.4 to 1.0 over the first 60 ticks. Hit enemies are pulled toward you (0.45 horizontal, 0.12 up).
4. Every 3 ticks you also throw a blade shard in your facing direction (speed 1.5 to 1.9 blocks per tick, life 60 ticks). Shard damage: 80 + Strength x 0.5. A shard that hits a target while flying deals its damage and applies Valor Stun (60 ticks), stacking one amplifier per hit up to amplifier 5, plus Cinematic Tracking (60 ticks). A shard that hits a block sticks into it for up to 200 ticks (10 seconds) and triggers the same hit on the first enemy that steps into it.
5. While spinning, marked enemies within 14 blocks (enemies carrying Valor Stun) get Cinematic Tracking (40 ticks, refreshed). Hostile projectiles within 4 blocks are reversed back at their sender (velocity x -1.4, ownership transferred to you). Your own shards and crescent waves are exempt.

**Release finisher (slash plus Grand Crescent Wave):**
1. The finisher multiplier is (0.8 + 1.6 x spin ratio), where spin ratio is spin ticks / 140. That is 0.8x on a tap release and 2.4x after a full spin.
2. Finisher slash: (450 + Strength x 2.0) x multiplier to enemies in a frontal arc within 6 blocks. Hit enemies get Cinematic Tracking (20 ticks), Bounciness (+0.85, 100 ticks), and a fling along your aim.
3. Crescent wave: fired with the slash, travels at 2.2 blocks per tick, lives 80 ticks (4 seconds). Wave damage: (600 + Strength x 2.5) x multiplier. It pierces: each target is hit once, takes Cinematic Tracking (30 ticks) and knockback (1.5 along the wave, 0.3 up). If it hits a block it stops, throws debris, scars the ground, and dissipates over 12 ticks.
4. The release also scars a coarse dirt disk at your feet (radius 1, or 2 after a long spin, restored after 240 ticks) and throws a 28 piece debris field.
5. Cooldown after release: 160 ticks (8 seconds).

**Right click, Valor Field (hold):**
1. Hold right click to raise the field (you need some Ki to start). The field is a dome of radius 9 blocks that follows you. The field itself drains Ki (90 percent of max Ki per minute) while it is up.
2. Every 10 ticks, everything inside is refreshed (effects last 40 ticks each cycle):
   - You: Valor (amplifier 2), plus vanilla Strength, Resistance (amplifier 2), and Haste (amplifier 1).
   - Allies (by the game's ally check): Valor, Strength, and Resistance (amplifier 1).
   - Enemies: Valor Stun and Slowness (amplifier 1), Cinematic Tracking (40 ticks), and pressure damage (55 + Strength x 0.45) per cycle.
3. The field freezes hostile projectiles inside it (velocity zeroed, gravity off). A projectile that leaves the dome resumes its flight. One still frozen after 200 ticks (10 seconds) breaks. If a living target touches a frozen projectile, it takes collision damage of 16 x the projectile original speed (a Ki blast instead deals its own damage), and the projectile is destroyed. When the field fades, everything frozen resumes at once.
4. Releasing the hold, dying, swapping the sword away, or running out of Ki fades the field out over 12 ticks.
5. Cooldown after release: 60 ticks (3 seconds).

---

## Azure Dragon Sword

Theme: wind, flight, and tempest manipulation.

**Melee (left click swing):** the swing deals the weapon damage (800). A landed hit also drains a tiny amount of Ki (15 percent of max Ki per minute over 7 ticks, about 0.0875 percent of max Ki per hit): if that drain pays, the hit adds a gust that pushes the target (0.3 horizontal, 0.12 up). If it does not pay, the plain hit still lands without the push.

**Left click, Azure Dragon Rush (hold):**
1. Hold left click to fly. You are forced into gliding flight along your aim. Flight speed is 1.85 blocks per tick normally, 0.75 while sneaking, and 3.7 during a double rush (below). You take no fall damage from the rush.
2. The rush drains Ki per tick (20 percent of max Ki per minute, or 40 percent during a double rush). If the drain fails, the rush stops and the sword goes on a 20 tick (1 second) cooldown.
3. Double rush: starting a new rush within 8 ticks of the last rush start makes it a double rush: faster flight (3.7), stronger push (2.5 instead of 1.5), bigger hit box (4.0 instead of 2.5), and higher damage and proc chances below.
4. Passing through an enemy deals damage (350 normally, 600 on a double rush, plus Strength x 2.0) and pushes it away from you (plus 0.4 up).
5. Mini tornado proc: a fresh pass-through hit can spawn a mini tornado on the enemy (40 percent chance, 80 percent on a double rush, at most one per 25 ticks). The tornado lives 60 ticks (3 seconds), has radius 3 blocks and height 6 blocks, deals 25 damage every 10 ticks inside its active window, drags targets with its vortex, and flings everything out near the end of its life (power 1.1, up 0.7).
6. Sonic Quake: hitting the ground or a wall while rushing triggers a quake (at most once per 30 ticks). It costs Ki (60 percent per minute over 20 ticks, about 1 percent of max Ki); if the drain fails, no quake. Quake damage: 450 + Strength x 2.5, in a radius of 8 blocks, falling off to half at the rim. Targets are launched up (vertical velocity +1.4) and gain Bounciness (+0.85), which the quake cleans up after 60 ticks. A double rush flag is spent on impact.

**Right click, Call of the Tempest (hold to charge, release for the storm):**
1. Hold right click to channel (you need some Ki to start). Channeling drains Ki (20 percent of max Ki per minute). Full charge is 300 ticks (15 seconds). Past 30 percent charge, the channel itself tugs nearby targets with a small vortex (radius 2.5 + 3.5 x charge, no damage).
2. Release with under 10 ticks held and nothing happens: no storm, no cooldown.
3. On a real release, a storm is born. Storm radius: (10 + 20 x charge) blocks, so 10 to 30. Storm life: (120 + 220 x charge) ticks, about 6.4 seconds for the shortest storm and 17 seconds (340 ticks) at full charge.
4. If you had a locked target (the sword lock-on map) that is alive within 64 blocks, the storm spawns on that target and follows it; otherwise it follows you. If its target dies, it falls back to following you.
5. The storm is a real weather event: it calls thunder weather and gloom over an area (storm radius + 24 blocks) for as long as it lives. Its vortex pulls every tick (radius equals the storm radius, height 14, pull strength 0.05 + 0.06 x charge, with lift at the core).
6. Lightning: the storm strikes repeatedly, starting 14 ticks in, with the gap between strikes shrinking from 30 ticks toward 16 ticks as the storm peaks. Each strike prefers a random enemy inside the storm, otherwise a random ground point. Strike damage is 450 in a cylinder of radius 3.5 blocks (targets also get a small upward pop, 0.4). A strike has a 20 percent chance to be rare: radius 4.5 blocks and damage x 1.25 (562.5). Every strike is telegraphed 12 ticks before it lands.
7. Debris: once, at 20 percent of its life, the storm throws a debris field (28 + 44 x charge pieces).
8. At maximum charge (0.95 or more), the storm also spawns a giant tornado at its center, once, at 20 percent of storm life: radius 14.4 blocks, height 28.8 blocks, life 200 ticks (10 seconds). It deals 60 damage every 10 ticks inside its active window, drags targets with a strong vortex (lift 0.85), flings everything out near the end of its life (power 1.7, up 1.0), and calls its own lightning every 20 ticks inside its window (350 damage, 10 tick telegraph).
9. Cooldown after release: storm life + 100 ticks. That is 227 ticks (about 11.4 seconds) for the shortest storm and 440 ticks (22 seconds) at full charge, so the cooldown always outlasts the storm.

---

## Dabura Sword (Darkness Sword)

Theme: abyssal darkness. Petrification, darkness waves, and a darkness domain that ends in an eclipse.

**Melee (left click swing):** the swing deals the weapon damage (800). A landed hit drains a small amount of Ki (35 percent of max Ki per minute over 10 ticks, about 0.29 percent of max Ki; the hit works even if the drain cannot pay) and applies Petrification Curse (40 ticks).

**Left click, Abyssal Slash (hold to charge, release):**
1. Hold left click to charge (max charge 160 ticks, 8 seconds). Charging drains Ki (35 percent of max Ki per minute) and dampens your movement. If the drain fails, the slash releases at the charge you have. Releasing costs no extra Ki.
2. Slash damage: (400 + 800 x charge) + (Strength x 3.0 x charge). At minimum charge (0.1) that is 480 + Strength x 0.3; at full charge it is 1200 + Strength x 3.0.
3. The wave travels at (1.6 + 1.0 x charge) blocks per tick, lives about 60 ticks (3 seconds), and passes through terrain. Its hit box is wide: 3.5 blocks across for the main wave. It pierces, hitting each target once.
4. On hit, a target takes the wave damage, Petrification Curse (100 ticks), Dark Faded (100 ticks), heavy friction and air drag penalties, Cinematic Tracking (25 ticks), and knockback (1.2 along the wave, 0.25 up).
5. At full charge (0.95 or more), two extra waves trail behind you at 1.5 and 3 blocks back, each dealing half the main damage at 0.78x and 0.58x of its speed, with a narrower 2.2 block hit box.
6. Cooldown: (20 + 40 x charge) ticks, so 24 ticks (1.2 seconds) at minimum charge and 60 ticks (3 seconds) at full charge.

**Right click, World of Darkness (hold to channel):**
1. Hold right click to raise the domain (you need some Ki to start). Channeling drains Ki (35 percent of max Ki per minute); if the drain fails, you stop channeling.
2. Full charge is 300 ticks (15 seconds). The domain follows you and its radius grows with charge: (8 + 22 x charge) blocks, so 8 to 30. Its dome height is the smaller of 26 or radius x 1.35.
3. While you channel: enemies inside get Petrification Curse (40 ticks, refreshed), hostile projectiles inside are slowed to 5 percent of their velocity each tick, a vortex drags targets (pull 0.055, scaling up with charge), and real thunder weather with heavy gloom (0.85) covers the area (domain radius + 14 blocks), refreshed every 20 ticks.
4. The domain builds over 30 ticks when it appears and tears down over 25 ticks when the channel ends. Visual lightning flickers inside it and debris fields spawn while it lives; those bolts and debris deal no damage on their own.
5. Abyssal Eclipse at exactly 300 ticks (full channel): every enemy inside takes burst damage (1200 + Strength x 4.0), is stopped dead (velocity zeroed with a small hop), and gets Petrification Curse (60 ticks, amplifier 1) and Cinematic Tracking (60 ticks). Then blades rain: 25 blades at full charge radius, each falling from 18 to 23 blocks up and exploding for 350 damage in a splash box (about 4 blocks across) where it lands, leaving a small obsidian scar (radius 2, restored after 120 ticks). Five visual lightning bolts ring you, an obsidian scar disk scars the ground (radius 12, restored after 240 ticks), and 48 debris pieces fly. The channel ends. Cooldown: 160 ticks (8 seconds).
6. Partial eclipse: releasing between 200 and 299 ticks fires a weaker version. Burst damage is (1200 + Strength x 4.0) x 0.6 x charge, Petrification Curse is 40 ticks at amplifier 1, the blade rain count scales with charge (each blade still 350 damage), and the ground scar is smaller (restored after 160 ticks). Cooldown: 100 ticks (5 seconds).
7. Releasing before 200 ticks just ends the domain. Cooldown: 40 ticks (2 seconds).

---

## Cursed Blade

Theme: dark curse and battlefield denial. Stacking chains, a curse storm domain, and a verdict finisher. Note the naming: the item is the Cursed Blade.

**Melee (left click swing):** a plain swing deals the weapon damage (750) and nothing else: no Ki cost, no extra damage, no curse. The chains and the domain are what pay for this weapon.

**Left click, Curse Chain Streaming:**
1. Each left click fires one homing curse chain, as fast as every 5 ticks. Cost per shot: Ki (30 percent of max Ki per minute over 4 ticks, about 0.1 percent of max Ki). The cooldown is set when you pay, so a paid shot with no valid target still costs the Ki and the 5 ticks.
2. Targeting: the chain prefers an enemy in front of you within 24 blocks (aim dot above 0.3, best score wins). If nothing is in front, it takes the closest target in range, even behind you.
3. The chain flies at its target (1.6 blocks per tick, homing). A target can carry at most 10 chains; if it is full, the new chain destroys the weakest attached chain on it and takes its place, or jumps to another nearby enemy if one exists.
4. On attach: the target takes 75 damage and gains Movement Curse for 100 ticks (5 seconds). Each chain raises the curse by one amplifier, up to amplifier 9 (the 10th stack). The chain stays attached for 100 ticks, or until the target loses Movement Curse.
5. Chains are real targets in the world: a chain has 60 health and can be attacked and destroyed. Destroying an attached chain lowers the target curse by one amplifier.
6. Attach sustain: you are healed for (4 + 2.5 x new amplifier), so 4 on the first chain and 26.5 at amplifier 9. If you are already at full health, you gain absorption instead (flat 4, capped at your max health).
7. Heavy curse (5 or more stacks, amplifier 4 or more): attaching also drags the target toward you, and you regain a flat 10 Ki and 5 Stamina (literal values in the code). While attached at that level, the chain keeps reeling the target in toward you whenever it is more than 1.5 blocks away.
8. Attached chains weigh the target down: friction and air drag penalties scale with the curse amplifier (weight 1.0 + 0.8 per amplifier), refreshed while attached.

**Right click, Cursed Tempest (hold to channel, release for the verdict):**
1. Hold right click to channel the storm (you need some Ki to start). Channeling drains Ki (30 percent of max Ki per minute); if the drain fails, you stop. Full charge is 300 ticks (15 seconds).
2. The storm domain follows you with radius (10 + 20 x charge) blocks, so 10 to 30. Real thunder weather and gloom cover the area (gloom 0.45 rising to 0.90 with charge). A vortex winds through the domain every tick (pull 0.055, lift 0.06, ramping up over the first 60 ticks).
3. Enemies inside get Storm of Darkness (60 ticks, amplifier 0 to 3 scaling with charge) and Cinematic Tracking (60 ticks). Every 20 ticks, each enemy inside has a 10 percent chance to drop its held weapon (Curse Blades are exempt). At exactly 300 ticks the storm throws a 60 piece debris field and a ground burst.
4. Telegraphed lightning: every 40 ticks a telegraph ring (radius 1.8) appears for 20 ticks on the most cursed enemy in the storm (highest Movement Curse wins), and 20 ticks later a bolt strikes that target for (450 + Strength x 1.5), sets it on fire for 5 seconds, places a fire block at its feet if the spot is open, scorches the ground (radius 1, restored after 160 ticks), and throws 12 debris pieces.
5. Ambient lightning: every tick there is a 12 percent chance of a stray strike, near a random enemy (45 percent chance) or at a random point in the domain. Anything within about 3.5 blocks of the strike point takes (120 + Strength x 0.35) damage and burns for 3 seconds. The ground at the strike is scorched (restored after 120 ticks).
6. Sky cracks open overhead every 80 ticks while channeling (visual).
7. Slam phase: once you reach 300 ticks, every 20 ticks every enemy inside with 5 or more curse stacks (amplifier 4 or more) is slammed straight down (velocity set to straight down at 2.5), takes (220 + Strength x 1.0) damage, has its curse refreshed to the cap (amplifier 9, 60 ticks), and gets a ground shatter at its feet (radius 3.5) with scorching (radius 2, restored after 200 ticks).
8. Tempest Verdict (release at 40 or more ticks held): every enemy in the domain takes verdict damage: (150 + 650 x charge) + (Strength x (0.8 + 1.7 x charge)). At the minimum 40 ticks that is about 237 + Strength x 1.03; at full charge it is 800 + Strength x 2.5. Targets burn for 4 seconds, gain 2 more curse amplifiers (capped at 9, 60 ticks), and are flung away from you (power 0.6 + 1.2 x charge, up 0.5 + 0.5 x charge). Bolts strike the first 6 targets (visual), debris flies (24 to 60 pieces), and the ground is scorched (restored after 240 ticks). Releasing under 40 ticks just ends the storm.
9. Cooldown on release: (100 + 100 x charge) ticks, so 100 ticks (5 seconds) on a tap and 200 ticks (10 seconds) at full charge. The verdict itself costs no extra Ki beyond the channel drain.

---

## Hollow's Edge

Theme: void assassin blade. Charge a rush of shadow dashes, and channel a void rift that implodes.

**Melee (left click swing):** the swing deals the weapon damage (850). A landed hit drains a tiny amount of Ki (20 percent of max Ki per minute over 7 ticks, about 0.117 percent of max Ki per hit; the effects still apply if the drain cannot pay) and applies Dark Faded (40 ticks) to the target.

**Left click, Hollow Rush (hold to charge, release to open the rush window, click again to dash):**
1. Hold left click to charge (max charge 100 ticks, 5 seconds). Charging drains Ki (30 percent of max Ki per minute) and dampens your movement. If the drain fails, the release fires at the charge you have.
2. Releasing opens the rush window: it lasts (40 to 200) ticks, from 2 seconds at minimum charge to 10 seconds (200 ticks) at full charge.
3. During the window, each left click dashes you 5 blocks along your aim (scaled by your movement multiplier), reversed if you are holding backward. The dash phases through a wall if the space 1.5 blocks past it is clear for your feet and head; otherwise it stops just in front of the wall. Dashes in the window cost no extra Ki and have no cooldown.
4. Each dash leaves an afterimage at the start point (lasts 100 ticks, 5 seconds) that applies Dark Faded (40 ticks) to enemies within about 3 blocks every 5 ticks, and gives you Hollowed (60 ticks, 3 seconds).
5. Path damage: everything you pass through takes (450 + Strength x 2.5) and gets Dark Faded (80 ticks) and Cinematic Tracking (80 ticks).
6. Every 3rd dash hits harder: the path damage is multiplied by 1.8 first, and a void slash fires at your destination dealing that damage x 1.5 again, for a total of (450 + Strength x 2.5) x 2.7 to what it hits. The slash flies at 1.8 blocks per tick, lives up to 40 ticks, hits each target once, and applies Dark Faded (80 ticks, amplifier 1).
7. Outside the window, a left click instead does a short fallback blink: 3 blocks, costing Ki (30 percent per minute over 20 ticks, 0.5 percent of max Ki). If you cannot pay, no blink and you get a "Not enough Ki to blink!" message. Blink cooldown: 10 ticks (0.5 seconds).

**Right click, Void Rift (hold to channel, release to implode):**
1. Hold right click to channel (you need Ki above 0.5 to start). The rift appears at your crosshair point (12 blocks ahead, or the block you are aiming at out to 32 blocks) and follows your aim while you channel, out to 48 blocks.
2. Channeling drains Ki (30 percent of max Ki per minute); if the drain fails, you stop channeling. Full charge is 100 ticks (5 seconds). The rift can be channeled for at most 240 ticks (12 seconds), then it implodes on its own.
3. Rift radius grows with charge: (2.5 + 6.5 x charge), so 2.5 blocks up to 9 blocks. Rift damage stat: (550 + Strength x 2.0) x (0.7 + 0.6 x charge), from (385 + Strength x 1.4) at the start to (715 + Strength x 2.6) at full charge.
4. While it channels, the rift drags everything toward itself with a vortex larger than the rift (radius x 1.35), including loose items and debris (but not your own projectiles). Every 5 ticks it refreshes Dark Faded (30 ticks) on enemies in that area. Every 10 ticks it pulses damage on targets inside the rift radius itself: 8 percent of the rift damage at the core, falling to 2.8 percent at the rim.
5. Release: if you are aiming at the rift (within about 40 blocks), you are teleported into it and gain Rifted (200 ticks, 10 seconds). Either way, the rift implodes.
6. Implosion: over 30 ticks (1.5 seconds) the suction gets stronger (radius x 1.6, final pull strength 0.5), then it bursts. Burst radius is radius x 1.5. Everything inside takes the full rift damage at the core, falling to 60 percent at the edge, is flung outward (2.2 out, 0.8 up, scaled by weight class), and gets Dark Faded (100 ticks, amplifier 1). The burst leaves a temporary obsidian scar disk on the ground (restored after 240 ticks).
7. Cooldown after release: (60 + 60 x charge) ticks, so 60 ticks (3 seconds) on a tap and 120 ticks (6 seconds) at full charge.

---

## Dimensional Sword

Theme: space cutting blade. Slashes that tear space, and a two stage warp.

**Melee (left click swing):** the swing deals the weapon damage (750). A landed hit drains Stamina (100 percent of max Stamina per minute over 13 ticks, about 1.083 percent of max Stamina per hit) and applies Melting (200 ticks, 10 seconds, amplifier 1) to the target.

**Left click, Dimensional Slash:**
1. Each left click fires one space slash. Cost: Stamina (the same drain as a melee hit, about 1.083 percent of max Stamina). If you cannot pay, no slash and no cooldown.
2. Slash damage: 750 + Strength x 3.0. The slash flies at 2.5 blocks per tick and lives up to 100 ticks (5 seconds, about 250 blocks).
3. Successive slashes alternate their tilt (left, right, left).
4. On a hit, the target takes the slash damage, Melting (300 ticks, 15 seconds, amplifier 1), and heavy friction and air drag penalties. The slash then stops and lingers as a tear for 14 ticks before fading (it does not hit anything else). A hit throws 10 debris pieces, and a block hit scars the struck block into crying obsidian (restored after 140 ticks).
5. Cooldown: 16 ticks (0.8 seconds).

**Right click, Dimensional Warp:**
1. Aim and right click. The sword validates the destination first, at no cost: if you are aiming at a block (raycast out to 1024 blocks), the landing is the free space on its far side; if you are aiming at open sky, the destination is 128 blocks along your aim. Either way the landing needs a free cell for your feet and head (it can climb up to 4 cells to find one). If no landing exists, the warp fails with no cost and no cooldown.
2. If the landing is valid, you pay Ki (100 percent of max Ki per minute over 100 ticks, about 8.333 percent of max Ki). If you cannot pay, nothing happens.
3. A rift opens at your position. It builds for 10 ticks while its vortex pulls targets in (radius 3 blocks, pull strength 0.32), then bursts: everything within 3 blocks takes (400 + Strength x 2.0), Melting (200 ticks, amplifier 1), and the friction and air drag penalties. The burst leaves 12 void debris pieces and a small scar disk (radius 1, restored after 200 ticks).
4. At that burst moment you are teleported to the landing and gain Rifted (60 ticks, 3 seconds). A second rift opens at the destination point (radius 4.5). It builds for 12 ticks (pull strength 0.20), then bursts for (900 + Strength x 3.0) with the same Melting and physics penalties, plus a fling on every target (0.95 out, 0.5 up), 24 debris pieces, and a bigger scar disk (radius 2, restored after 240 ticks).
5. Both rifts keep a weak aftershock pull after their bursts for the rest of their short lives (origin rift total life 34 ticks, destination rift 46 ticks).
6. Cooldown: 100 ticks (5 seconds).

---

## Spirit Sword

Theme: the Annihilator Blade, endgame legendary. Every swing throws a cleave, contact hits deal percent health damage, and the right click is a piercing spirit cannon beam.

**Melee and swings:** this weapon has two left click layers, and both can apply.
- Contact hit (when your swing actually connects): the weapon damage (500), plus two bonus hits: magic damage equal to 2 percent of the target max health, and (Strength x 2.0) as a player attack hit. The target gains Spirit Impale (120 ticks, 6 seconds), Cinematic Tracking (120 ticks), and a strong Bounciness modifier (+0.85).
- Disarm proc: each contact hit has a 20 percent chance to disarm a non player, non boss target (boss means knockback resistance 0.9 or more, or max health 400 or more): its held item drops on the ground and its hand is emptied.
- Thrown cleave: every swing also throws a Spirit Cleave projectile (speed 1.7 blocks per tick), even on a miss. Cost per swing: Ki (55 percent of max Ki per minute over 7 ticks, about 0.321 percent of max Ki), paid once per swing; if you cannot pay, no cleave is thrown. Cleave damage value: 500 + Strength x 2.0.
- The cleave flies for 14 ticks (about 24 blocks) and then dissipates over 5 ticks. It skips targets within 3 blocks of you (those are left to the contact hit, so nobody is paid twice). On a hit it deals its damage plus 2 percent of the target max health as one hit, applies Spirit Impale (120 ticks), and marks the target with a following impale visual. On a block hit it bursts with 10 debris pieces from the struck block and dissipates, dealing no damage.
- Impale marker: hits leave a Spirit Impale marker entity on the target for 40 ticks (2 seconds). It deals no damage itself; on arrival it bursts, throws 12 ground debris pieces, and scars the ground under the target (a plus shape of light blue stained glass, restored after 60 ticks). Scale differs by source: melee 1.0, cleave 0.85, cannon 0.75.

**Right click, Spirit Cannon (hold to channel the beam):**
1. Hold right click to fire the beam (you need Ki above 0.5 to start). Channeling drains Ki (55 percent of max Ki per minute); if the drain fails, you stop firing.
2. The beam reaches 32 blocks and stops at the first block it hits. That same point is where the damage stops: the beam cannot damage anything behind a wall.
3. Damage pulses every 5 ticks (0.25 seconds). Per pulse, each target the beam line touches takes (100 + Spirit x 0.75) x ramp, plus magic damage equal to 1 percent of its max health. Note this ability scales with Spirit, not Strength.
4. The ramp: pulses deal 0.6x damage in the first 20 ticks, 0.85x from 20 to 40 ticks, and full damage from 40 ticks (2 seconds) on.
5. The beam pierces: every living target the line crosses takes each pulse. Per pulse, targets also gain Spirit Impale (100 ticks, 5 seconds), Bounciness (+0.80), and an impale marker (scale 0.75).
6. At full ramp (40 or more ticks held), pulses that strike the top face of a block scar that block into light blue stained glass (restored after 120 ticks), and every 20 ticks the impact point throws 8 crystal debris pieces.
7. Releasing stops the beam (it fades over 6 ticks and deals no damage while fading). Cooldown: 20 ticks (1 second).

---

## Evil Spear

Theme: cursed hunter spear. Mark your prey, then hunt it through its marked allies.

**Melee (left click swing):** the swing deals the weapon damage (750). A landed hit drains a small amount of Ki (45 percent of max Ki per minute over 8 ticks, 0.3 percent of max Ki per hit; the mark applies even if the drain cannot pay) and applies Marked by Evil (120 ticks, 6 seconds) to the target.

**Left click, Evil Impale (hold to charge, release to throw):**
1. Hold left click to charge (max charge 120 ticks, 6 seconds). Charging drains Ki (45 percent of max Ki per minute) and dampens your movement. If the drain fails, the throw fires at the charge you have.
2. Throw damage: 450 + (700 x charge) + (Strength x 3.0 x charge). At minimum charge (0.1) that is 520 + Strength x 0.3; at full charge it is 1150 + Strength x 3.0.
3. Projectile speed: (1.8 + 1.8 x charge) blocks per tick, so 1.98 at minimum and 3.6 at full charge. Flight is capped at about 40 ticks (2 seconds).
4. The spear pierces: every target is hit at most once and the spear flies on. A marked target takes 1.3x damage. Every hit refreshes Marked by Evil (160 ticks, 8 seconds), applies heavy friction and air drag penalties and Cinematic Tracking (25 ticks), and spawns a hell impact at the target feet (radius 2.6, visual and ground scarring only, no extra damage).
5. The first target pierced opens a vortex at its position for 10 ticks: radius 6 blocks, height 4, with strong pull, swirl, and lift (strength 0.5, swirl 0.9, lift 0.30). The vortex itself deals no damage. If the spear hits a wall while a vortex is open, it pins into the wall until the vortex closes; otherwise a wall hit ends it.
6. Cooldown after release: (40 + 20 x charge) ticks, so 44 ticks (2.2 seconds) at minimum charge and 60 ticks (3 seconds) at full charge.

**Right click, Hell Hunt:**
1. Right click to hunt. If you are immobilized, it fails before any cost. Cost: Ki (45 percent of max Ki per minute over 40 ticks, 1.5 percent of max Ki). If you cannot pay, nothing happens (no hunt, no lunge, no cooldown).
2. Lock-on: the hunt takes the best target in a 60 degree cone out to 32 blocks with line of sight. From there it builds a chain: up to 7 more targets, each the nearest target carrying Marked by Evil within 16 blocks of the previous one. Maximum chain: 8 targets. Only the first target needs to be unmarked.
3. The hunt strikes one step every 6 ticks (the first step is immediate, so a full 8 target chain spans 42 ticks). Each step teleports you onto the target and deals damage with a growing multiplier: step 1 is 1.0x, and each later step adds 0.15, so step 8 is 2.05x.
4. Step damage: normal steps deal (350 + Strength x 2.0) x the step multiplier. The final step deals (400 + 150 x chain size + Strength x 3.5) x the step multiplier. For a full 8 chain, the final step is (1600 + Strength x 3.5) x 2.05.
5. Each step target gets Movement Curse (100 ticks, 5 seconds), Cinematic Tracking (100 ticks), heavy friction, and Bounciness (+0.80). A hell impact erupts at its feet (radius 3.0, or 4.5 on the final step). If a chain target dies before its step, that step is skipped and the chain moves on.
6. When the chain finishes, you gain Demon Surge (40 ticks, 2 seconds).
7. Cooldown for a targeted hunt: 160 ticks (8 seconds), set when the hunt starts.
8. No target fallback: if the lock-on finds nothing, you lunge forward instead (velocity 2.0 along your aim, 0.4 up, scaled by your movement multiplier). When you land (checked 8 ticks later), enemies within 3 blocks take (200 + Strength x 1.0) and gain Marked by Evil (120 ticks); a hell impact (radius 2.6) erupts if at least one enemy was hit. Lunge cooldown: 40 ticks (2 seconds).
9. Hell impacts (from the throw, the hunt, and the lunge) scar the ground with an obsidian disk (normal: radius 1, restored after 100 ticks; final-step: radius 2, restored after 160 ticks) and throw debris (10 pieces normal, 24 final).

---

## Devil Trident

Theme: demonic overlord trident. A piercing prong shot while undeployed, and a deployed shard swarm with a ground slam.

**Melee (left click swing):** the swing itself is the weapon hit (750). What left click does depends on whether the swarm is deployed:

**Left click while undeployed, Three Pronged Shot:**
1. A melee hit, or an air swing, fires one formation of three prongs as a single projectile.
2. Cost: Ki (60 percent of max Ki per minute over 10 ticks, 0.5 percent of max Ki per shot). If you cannot pay, no shot and a "Not enough Ki!" message. Cooldown: 10 ticks (0.5 seconds).
3. The prongs fly at 2.5 blocks per tick from your eye and begin dissolving after 28 ticks (gone 6 ticks later), or die on a block hit.
4. Piercing: the shot passes through up to 3 targets, dealing 750 to the first, 495 to the second (0.66 share), and 337.5 to the third (0.45 share). No target is hit twice by one shot, and the shot dies on the third hit. Each pierced target gains Marked by Evil (60 ticks, 3 seconds).

**Left click while deployed, Swarm Targeting:**
1. Left click does not fire the shot. A melee hit, or an air swing that raytraces a target (search up to 64 blocks, tight forward cone), marks that target as the swarm target: every shard is sent at it.
2. The target gains Marked by Evil (60 ticks, 3 seconds). The targeting click itself costs nothing and has no cooldown.

**Right click while undeployed, Deployment Slam and Shard Swarm:**
1. A slam erupts at your position (radius 6 blocks) and 10 trident shards deploy in a ring around you. Deploy cooldown: 20 ticks (1 second). There is no Ki cost at cast; the swarm upkeep below applies while deployed.
2. Slam damage lands at tick 8 of the slam 35 tick life, not on the cast tick: 750 damage, full out to 3 blocks, then falling to a 40 percent floor at the 6 block rim. Hit targets gain Devil's Hands (amplifier 2, 300 ticks, 15 seconds), Cinematic Tracking (300 ticks), Bounciness (+0.85), reduced air drag, and are flung up and out (0.85 out, 1.05 up, scaled by weight class).
3. The slam tears a brimstone scar disk (magma near the center, blackstone mid, soul soil at the edge; restored after 240 ticks, 12 seconds) and throws 30 ground sampled debris pieces.

**Right click while deployed, Recall:**
1. All your shards within 64 blocks fly home and the swarm undeploys, clearing the swarm target.
2. Cooldown: 600 ticks (30 seconds). No Ki or Stamina cost.

**Passive, the Shard Swarm:**
- Each shard deals (100 + Strength x 0.5) damage per hit. A shard dives at its target at 2.5 blocks per tick, hits from within 2 blocks at most once per 6 ticks, applies Devil's Hands (60 ticks, 3 seconds) on a hit, then recoils for 4 ticks before diving again.
- Each shard has a health budget of 15000. It spends its own health by the damage it deals, by damage it takes, and by 50 per intercepted projectile. At zero health it flies home and despawns at your hand.
- Interception: each management pass, hostile projectiles within 12 blocks of you are assigned one shard each (first come, first served); the shard destroys the projectile at a cost of 50 shard health.
- Targeting: with no marked target, shards attack monsters on their own within 40 blocks, sending only as many shards per monster as it takes to kill it (monster health divided by shard damage, rounded up). With a marked target, every remaining shard goes at it, even if it is not a monster. If the target dies, the mark clears.
- Idle shards orbit you in shells (radius 2.2, 2.7, and 3.2 blocks, alternating directions). On deploy they emerge outward over 8 to 26 ticks. Lost shards with saved health respawn at your position. A shard despawns if it ends up more than 64 blocks from you.
- Upkeep: while deployed, the swarm drains Ki (20 percent of max Ki per minute). If that drain fails, the swarm is recalled in disgrace: all shards fly home and the trident gets a 3600 tick (180 second, 3 minute) wipe cooldown once the last shard arrives (or after 40 straight ticks with no shards in range).

---

## Sickle of Sorrow

Theme: dimensional reaper sickle. Melting, lifesteal, and a gravitational rift domain.

Costs on this weapon scale with your sickle technique level (level 1 to 10). Every action uses a base drain rate that starts at 25 percent of max Ki per minute at level 1 and drops by 2 per level, to 7 percent at level 10 (level 5 is 17). Actions charge that rate prorated over a set number of ticks.

**Melee (left click hit):**
1. A connected hit deals the weapon damage (750) plus a separate hit of (Strength x 3.0).
2. The target gains Melting at amplifier 2 (Melting III, 300 ticks, 15 seconds) and Sorrow Rift (80 ticks, 4 seconds).
3. Soul Rend: you heal for 3 percent of (750 + Strength x 3.0) per connected hit.
4. Cost: the base drain prorated over 13 ticks (about 0.271 percent of max Ki at level 1, about 0.076 percent at level 10). The hit effects land even if the drain cannot pay.
5. The impact burst visual on the target deals no damage.

**Left click air swing, Sorrow Slash wave:**
1. Swinging at air fires a traveling wave (speed 1.6 blocks per tick, life 30 ticks, up to about 48 blocks, dies on the first block it clips). Successive swings alternate the wave tilt.
2. Cost: the base drain prorated over 8 ticks (about 0.167 percent of max Ki at level 1, about 0.047 percent at level 10). This one is gated: if you cannot pay, no wave.
3. Wave damage per target: 750 + Strength x 3.0. Each target is hit once per wave and gains Melting (amplifier 2, 300 ticks), Sorrow Rift (80 ticks), and a friction penalty. Soul Rend heals you for 3 percent of the wave damage per target hit.
4. No cooldown beyond the Ki gate.

**Right click, Dimensional Rift:**
1. Right click opens a rift at your position. Cost: the base drain prorated over 60 ticks (base rate / 20 percent of max Ki: 1.25 percent at level 1, 0.35 percent at level 10). If you cannot pay, no rift and no cooldown. Cooldown: 120 ticks (6 seconds), checked before the Ki is charged, so a blocked cast costs nothing.
2. Rift burst damage: 500 + Strength x 3.0. Rift life: 60 ticks (3 seconds).
3. The radius starts at 3 blocks, expands to 15 blocks over 20 ticks, holds until tick 50, then contracts to 7 blocks by tick 60.
4. At tick 20, every target inside takes the burst damage once. Soul Rend from the burst heals you for 1 percent of the burst damage per target.
5. Sustained damage: at ticks 15, 25, 35, and 45, targets inside take 60 damage per pulse (up to 240 total for a target that stays in).
6. Every tick, targets inside gain Melting (200 ticks, 10 seconds, refreshed) at a strength that fades with distance: amplifier 3 inside 3.75 blocks, then 2, then 1, then 0 at the rim. They also gain Sorrow Rift (200 ticks), Cinematic Tracking (40 ticks), a friction penalty, and a strong air drag increase.
7. The rift pulls everything with a vortex every tick (height 7 blocks, pull 0.5, swirl 0.85, lift 0.55). At tick 50, everything caught is flung outward (1.5 out, 0.85 up).
8. On its first tick the rift throws a debris field (at least 8 pieces, about 64 at normal effect density, from a soul sand, soul soil, basalt, blackstone, and crying obsidian palette) and scars the ground below it if there is a surface within 8 blocks (soul soil disk radius 4, restored after 300 ticks). A rift opened high in the air scars nothing.

---

## Ox King's Ax

Theme: immovable powerhouse ax. Hits shove by weight class, the held left click charges a ground shockwave, and the held right click is a king's stance that ends in a slam. This is a Stamina weapon: no Ki costs anywhere on it. Both held abilities drain Stamina at 125 percent of max Stamina per minute while held, so a full Stamina bar lasts about 48 seconds of holding.

**Melee (left click hit):**
1. A connected hit deals the weapon damage (950) and nothing else for damage. No cost.
2. The shove: the target horizontal velocity is cut to 30 percent, then a flat push away from you is added (0.85 horizontal, at least 0.42 up), scaled by the target weight class: bosses (knockback resistance 0.9 or more, or max health 400 or more) take 0.12 of the push, heavy targets (knockback resistance 0.45 or more, or max health 90 or more) take 0.45, and everything else takes the full push.
3. The target also gains Fissure Stun (12 ticks), with ember particles and an anvil sound.

**Left click, Groundbreaker (hold to charge, release for the shockwave):**
1. Hold left click to charge (max charge 200 ticks, 10 seconds). Charging drains Stamina (125 percent of max Stamina per minute). If Stamina runs out, the wave force releases at the charge you have.
2. Poise while charging: under 40 ticks you get Ox Brace (amplifier 0). From 40 to 99 ticks, Ox Brace (amplifier 1). At 100 ticks and up you are rooted in place and get Ox Brace (amplifier 2). At 190 ticks a sound and a "Groundbreaker fully charged!" message play.
3. Wave damage on release: (350 + 850 x charge) + (Strength x 3.5 x charge). At minimum charge (0.05) that is about 392.5 + Strength x 0.175; at full charge it is 1200 + Strength x 3.5.
4. The shockwave ring expands from 0.5 blocks out to a max radius of (4 + 20 x charge): 5 blocks at minimum charge, 24 blocks at full charge. Its life is radius x 1.3 ticks (minimum 10), so up to 31 ticks at full charge. A target is hit when the ring passes over it (within 1.8 blocks of the current ring radius): full wave damage, no falloff, once per wave.
5. Wave hits apply Cinematic Tracking (30 ticks), Movement Curse (amplifier 1, 30 ticks), Bounciness (+0.90), reduced air drag, and a big knockback: horizontal (1.4 + 1.8 x charge), up (0.6 + 0.7 x charge).
6. Echo waves at charge 0.50 or more: the main wave spawns a second wave at its tick 6 (0.8x charge, 0.40x damage) and a third at tick 12 (0.6x charge, 0.40x damage). Echo waves reach 75 percent of their computed radius.
7. Ground scarring at charge 0.50 or more: the expanding ring scars the ground it passes (magma 40 percent, otherwise basalt; restored after 240 ticks, or 300 at charge 0.95 or more).
8. At charge 0.95 or more, the wave also breaks terrain: ring blocks with blast resistance 2.0 or less are destroyed with drops (protected blocks, bedrock, and barriers are exempt). It also opens 6 fissures around the center (out to 60 percent of the wave radius): each fissure lives 100 ticks (5 seconds), is about 4.2 by 2 blocks, and every 10 ticks deals 75 damage and ignites targets inside for 2 seconds.
9. Cooldown: (20 + 40 x charge) ticks, so 22 ticks at minimum charge and 60 ticks (3 seconds) at full charge.

**Right click, Colossal Stance (hold) and King's Slam (release):**
1. Hold right click to take the stance (you need Stamina above 0.5 to start). You are rooted in place and gain Ox Brace (amplifier 1, refreshed). The stance drains Stamina (125 percent of max Stamina per minute); if it runs out, you stop using the item.
2. King's Force aura (radius 12 blocks, pets exempt): every tick, enemies inside gain Fissure Stun (20 ticks, refreshed) and are pushed outward (0.35 at the center, easing to 0.105 at the rim, with a small upward push). Every 5 ticks they also take pressure damage: (45 + Strength x 0.4) scaled by distance, full at the center and half at the rim, plus Cinematic Tracking (25 ticks).
3. Self pressure: from 200 ticks (10 seconds) held onward, you take damage every tick equal to 3 percent of your current health per second.
4. The peak window is 280 to 300 ticks held (14 to 15 seconds), heralded by a charge sound every 5 ticks.
5. Holding past 300 ticks fails the stance: you take recoil damage of 25 percent of (2000 + Strength x 5.0), the aura ends, and the ax goes on a 200 tick (10 second) cooldown.
6. King's Slam on release (needs at least 10 ticks held; releasing earlier does nothing):
   - Flawless slam (released in the 280 to 300 peak window): damage 2000 + Strength x 5.0, radius 20 blocks, slam life 55 ticks, cooldown 100 ticks (5 seconds).
   - Normal slam: with hold ratio = held ticks / 200 (capped at 1.0), damage is (400 + 900 x ratio) + (Strength x 3.0 x ratio) and radius is (6 + 8 x ratio). At 10 ticks that is 445 + Strength x 0.15 at radius 6.4; at 200 or more ticks it is 1300 + Strength x 3.0 at radius 14. Cooldown: (40 + 60 x ratio) ticks, 43 ticks at minimum hold and 100 ticks (5 seconds) at full ratio. Slam life 40 ticks.
   - The slam damage lands as the visual ring expands: a target is hit the first tick the growing rim reaches it, once per slam. Full radius is reached at 55 percent of the slam life.
   - Slam hits launch targets and apply: flawless, Fissure Stun (amplifier 1, 140 ticks), Cinematic Tracking (60 ticks), Movement Curse (amplifier 2, 60 ticks), launch 2.5 horizontal and 0.9 up. Normal: Fissure Stun (amplifier 0, 80 ticks), Cinematic Tracking (40 ticks), Movement Curse (amplifier 1, 40 ticks), launch 1.5 horizontal and 0.5 up.
   - At full radius the slam scars a ground disk (radius 7 flawless, 5 normal, magma or basalt, restored after 300 ticks) and throws a debris field (30 pieces normal, 48 flawless).

---

## Power Pole

Theme: the extending crimson and gold staff. A sustained wind whirlwind on the left, and a 30 block extending poke on the right. Stamina only, no Ki.

**Melee (left click hit):** a connected hit deals the weapon damage (750) only. The hurtEnemy code is intentionally empty: no cost, no extra damage, no effect. The whirlwind and the poke are what pay.

**Left click, Wind Spin (hold):**
1. Hold left click to spin. The game calls the spin every 2 ticks while held, and each call costs Stamina (20 percent of max Stamina per minute over 10 ticks, about 0.167 percent of max Stamina per call). If a call cannot pay, it does nothing. There is no cooldown beyond that gate.
2. One whirlwind entity serves the whole spin. It builds to full strength over 10 ticks, sustains while refreshed, starts decaying 6 ticks after the last refresh, and dissipates over 24 ticks. A single tap lasts at least 40 ticks (2 seconds) in total.
3. Damage and deflection only run in the active window: after the 10 tick build and while the funnel strength is above 25 percent. A funnel still building deals nothing.
4. Damage area: a cone of 35 degrees along your aim out to 25 blocks, plus a close barrier: anything within 4.5 blocks of you is included regardless of angle.
5. Damage per hit: 750 / max(1, distance / 3). That is 750 at 3 blocks or closer, 375 at 6 blocks, 300 at 7.5 blocks, and 90 at 25 blocks. A target can be hit at most once per 10 ticks.
6. Hits throw targets outward: a target rushing inward has its inward momentum cancelled and is counter forced out (its incoming speed x 1.12 plus a close bonus: 2.5 within 4 blocks, 1.5 within 8, 0.6 beyond, plus 0.25 up). A target not rushing gets a flat outward force (at least 0.6, stronger up close, plus 0.2 up). Hits also give the target Bounciness (+0.90), so enemies ricochet off terrain.
7. The funnel itself drags targets with two vortex zones: one around you (radius 4.5) and one down the cone (radius 5.0, centered 9 blocks ahead of your eyes).
8. Projectile deflection in the active window: projectiles within 25 blocks that are in the cone (or within 4.5 blocks of you) become yours and are re-aimed away from your eye at 2.2 speed.
9. While spinning (not decaying), every 20 ticks after the build the whirlwind throws a small debris field at your feet (8 pieces). When decay begins, everything alive within about 5 blocks of you is flung out (0.9 out, 0.5 up).

**Right click, Extended Reach poke:**
1. Right click extends the pole up to 30 blocks along your aim. Cost: Stamina (20 percent of max Stamina per minute over 40 ticks, about 0.667 percent of max Stamina); if you cannot pay, the use fails. Cooldown: 40 ticks (2 seconds).
2. The nearer of the first entity hit and the first block hit sets the extension length. On a total miss the pole still extends the full 30 blocks, with no impact effects.
3. Entity hit: 750 damage flat, plus Pole Stun (100 ticks, 5 seconds, guaranteed), Bounciness (+0.95), and a 15 percent chance of a heavy concussion: Pole Stun at amplifier 1 for 100 to 140 ticks (5 to 7 seconds).
4. The extension visual lives 12 ticks (extends over 3, holds, retracts over the last 3), and an impact visual (16 ticks) plays at the hit point.
5. Block hit: a dust burst, a 24 piece debris field, and if the pole struck the top face of a grass block, a 3 by 3 patch around it can be scarred (each grass cell has a 70 percent chance to become dirt, restored after 120 ticks).

---

## Whis Staff

Theme: temporal god rod, endgame legendary time staff. Time reversal, stasis, and a temporal rift.

**Melee (left click):** each swing costs Ki (100 percent of max Ki per minute over 13 ticks, about 1.083 percent of max Ki per swing), charged once per swing.
- Contact hit (normal melee range): the weapon damage (350), plus two bonus hits: (Dexterity x 2.0) as a player attack hit, and magic damage equal to 1.5 percent of the target max health. The target starts reversing time for 100 ticks (5 seconds), gains Temporal Stasis (amplifier 0, 100 ticks) with heavy friction and air drag penalties, and a time shatter visual cracks at its mid-height.
- Extended strike: the staff own strike reaches targets 3.0 to 6.5 blocks from your eye in a tight aim cone (targets inside 3 blocks are left to the contact hit). It deals one combined hit of 350 + (Dexterity x 2.0) + 1.5 percent of the target max health, with the same 100 tick reversal, stasis, and physics penalties. Three shatter cracks appear along your aim (at 1.2, 2.6, and 4.2 blocks) whether or not it connects.

**Right click, Temporal Rift:**
1. Right click opens a rift centered on you (radius 12 blocks). Cost: Ki (100 percent of max Ki per minute over 100 ticks, about 8.333 percent of max Ki), checked before anything fires. Cooldown: 140 ticks (7 seconds).
2. Total damage: 400 + Spirit x 1.5, as magic damage, split in two equal halves. The first half (200 + Spirit x 0.75) lands at cast on everything within 12 blocks. The second half lands at rift tick 15 (the peak) on everything within 12 blocks of the rift at that moment.
3. Targets hit at cast also gain Temporal Stasis (amplifier 0, 60 ticks, 3 seconds).
4. At rift tick 60, as the stasis ends, every affected target still within 12 blocks starts reversing time for 100 ticks (5 seconds).
5. You gain Celestial Grace (amplifier 0, 200 ticks, 10 seconds) at cast.
6. The rift lives 100 ticks (5 seconds). While it lives it drifts targets with a gentle vortex (pull 0.030, strong swirl, slight lift) and throws a 60 piece debris field early in its life. No damage beyond the two halves above.

**Passive, Auto-Dodge:** while the staff is in your main hand, every incoming hit against you has a 50 percent chance to be completely negated: the damage becomes 0, a white dust burst and a gust sound play, and your invulnerable time is set to at least 10 ticks.

---

## Bansho Fan

Theme: the Storm Emperor, endgame legendary wind fan. Gale launches, a cyclone, and a wind barrage.

**Melee (left click):** each swing costs Stamina (45 percent of max Stamina per minute over 10 ticks, 0.375 percent of max Stamina per swing), charged once per swing. Two things happen on a swing:
- Contact hit: the weapon damage (400), plus a bonus hit of (Strength x 1.5). The target is launched hard: 5.0 horizontally away from you and 1.2 up. It gains Bleeding II (amplifier 1, 300 ticks, 15 seconds), Bounciness (+0.85), and reduced air drag. A shockwave visual (radius 3.2, 14 ticks, no damage) erupts at the target, and the hit tries to raise the Great Cyclone (see Passives).
- Gale wave: every swing also throws a gale projectile (speed 1.35 blocks per tick, life about 10 ticks, up to about 13.5 blocks), even on a miss. Gale damage: 400 + Strength x 1.5. It skips targets within 3 blocks of you (left to the contact hit). Each target is hit once: full gale damage, Bleeding II (300 ticks), and a launch along the gale direction (1.6 horizontal, 0.55 up). If the gale hits a block it stops there with a shockwave visual (radius 2.2, no damage).

**Right click, Tempest Barrage:**
1. Cost: Stamina (45 percent of max Stamina per minute over 40 ticks, 1.5 percent of max Stamina). Cooldown: 40 ticks (2 seconds).
2. A cone shockwave visual (radius 3.5, no damage) plays at your eyes, then 5 wind projectiles fire in a fan: yaw offsets of -8, -4, 0, 4, and 8 degrees, with slight pitch offsets, at speed 4.5 blocks per tick with perfect accuracy.
3. Each projectile deals (300 + Spirit x 1.0). Note this one scales with Spirit, not Strength. A projectile lives up to 50 ticks (2.5 seconds) and hits only the first living target or blocking block in its sweep, then is gone. It does not pierce.
4. On a hit: the full projectile damage, Bleeding III (amplifier 2, 400 ticks, 20 seconds), knockback along the projectile direction (1.4 horizontal, 0.45 up), and a shockwave visual at the impact (radius 2.6 on an entity hit, 2.0 on a block hit, no damage).

**Passive proc, Great Cyclone:**
1. Every landed melee contact hit tries to raise a cyclone at the target position. It is not a percent chance; it is gated by a personal storm cooldown of 120 ticks (6 seconds) and by the rule that you cannot have another of your cyclones alive within 24 blocks of the cast point.
2. The cyclone lives 100 ticks (5 seconds), with radius 4 blocks and height 6.5 blocks. It shreds only in its active window (age 20 to 82) and only every 4 ticks: 16 damage ticks in total.
3. Shred damage per tick: 11 percent of the cyclone figure. The cyclone figure is half your melee hit value: (400 + Strength x 1.5) x 0.5 = 200 + Strength x 0.75. So each shred tick deals 22 + Strength x 0.0825, and a target that stays in for the whole window takes 16 ticks worth: 352 + Strength x 1.32 in total. Each shred tick also refreshes Bleeding I (amplifier 0, 100 ticks).
4. While a target is not yet ejected, the cyclone drags it with a vortex (pull 0.30, swirl 1.6, lift 0.55).
5. The cyclone brings its own weather: real rain and gloom (0.45) over a 24 block radius for its 100 tick life, starting at age 1. At age 20 it throws a 64 piece debris field. Inside its window, every 10 ticks it scours the ground in a radius 3 disk (about 60 percent of cells): grass and mycelium become dirt, dirt and podzol become coarse dirt (restored after 240 ticks).
6. Ejection: at age 82, every living target inside is flung out (1.15 out, 0.9 up) and the vortex stops.

---

## Blaster Gun

Theme: energy firearm with a real ammo and heat economy. No Ki and no Stamina costs anywhere: it runs on Blaster Ammo and heat.

**Melee (left click swing):** a swing deals the weapon damage (450) only. No on-hit effect.

**Ammo rules:** ammo is the Blaster Ammo item, counted across your whole inventory and consumed stack by stack. Creative players do not consume ammo and skip the shortage checks. After each barrage shot the action bar shows your ammo count and heat percent.

**Left click, Blaster Barrage (hold):**
1. Holding left click fires one bolt every 3 ticks, about 6.67 shots per second. Heat is stored on the gun itself (0 to 100).
2. Normal shot: costs 1 ammo. Damage: 250 + (Spirit x 1.5) x (1 + heat x 0.5 percent per point), so at heat 0 it is 250 + Spirit x 1.5, and the Spirit part grows by up to 1.5x as heat climbs to 100.
3. Each normal shot adds 8 heat after firing. Starting cold, the 13th shot fires at heat 96 and pushes heat to 100.
4. Overcharged shot: firing at heat 100 costs 3 ammo and deals (700 + Spirit x 3.0). After it fires, heat resets to 0 and the gun vents on a 20 tick (1 second) cooldown.
5. If you lack the ammo for the next shot (1, or 3 when overcharged) and are not in creative, no bolt fires and you get an "Out of Blaster Ammo!" message and a fail click.
6. Bolts fly at 2.4 blocks per tick for up to 30 ticks (about 72 blocks), then collapse over 5 ticks; a solid block stops a bolt and starts the same collapse. A bolt pierces: every target it touches is hit once and it flies on. Its hit radius is 0.6 blocks cold, growing to 1.0 at full heat, and 1.5 for an overcharged bolt.
7. Bolt hits deal the full bolt damage, apply Cinematic Tracking (15 ticks), and knock the target along the bolt direction (0.6 horizontal normal, 1.5 overcharged, plus 0.2 up). An overcharged bolt also gives its target Bounciness (+0.75) and reduced air drag, and splashes everything else within 3 blocks of the target for 60 percent of the bolt damage, with an explosion at the target.

**Right click, Erasure Cannon (hold to charge, release to fire):**
1. You need at least 10 Blaster Ammo in your inventory to start (unless creative), or the use fails with a message. Hold right click to charge (max charge 100 ticks, 5 seconds). While charging you gain Energy Overcharge (refreshed) and a charge orb tracks the charge at your aim.
2. Fizzle: releasing under 0.2 charge (under 20 ticks) fizzles the shot: no beam, no ammo spent, no cooldown.
3. Ammo cost of a real shot: 10 + (10 x charge) ammo, so 12 at the minimum charge and 20 at full charge. The full cost must be available or the shot does not fire and no ammo is spent. Cooldown after a fired shot: 120 ticks (6 seconds).
4. The beam is fired from your eye along your aim, up to 48 blocks, clipped at the first solid block. It stays fixed where you fired it; it does not track you afterwards. Beam life: 24 ticks (1.2 seconds).
5. Total beam damage: 800 + (1500 x charge) + (Spirit x 4.0). At minimum charge that is 1100 + Spirit x 4.0; at full charge it is 2300 + Spirit x 4.0.
6. The damage lands as 6 equal pulses (each one sixth of the total) at beam ages 3, 6, 9, 12, 15, and 18. The beam radius is (1.5 + 1.5 x charge) blocks: 1.8 at minimum charge, 3.0 at full charge (plus half the target width).
7. Each pulse also applies Cinematic Tracking (25 ticks) and sucks targets toward the beam centerline (0.35 horizontal, 0.08 up). A target first pulse also gives it Bounciness (+0.85) and reduced air drag.
8. Endpoint detonation at charge 0.9 or higher: at age 18, everything within 4 blocks of the beam end point takes 50 percent of the total beam damage (on top of any pulses it already took), and a 24 piece debris field erupts there.

---

## Lava Sword

Theme: living lava blade. Burning melee, a traveling lava cleave, and a frontal lava eruption that paints the ground with temporary lava and fire.

**Melee (left click swing):** the swing deals the weapon damage (860). A landed hit sets the target burning for at least 60 ticks (3 seconds) and throws flame particles. No extra damage and no Ki cost on the melee hit itself.

**Left click, Lava Cleave:**
1. Cost: Ki (90 percent of max Ki per minute over 14 ticks, 1.05 percent of max Ki). Cooldown: 8 ticks (0.4 seconds), there to swallow the swing packet of the same press.
2. Damage: 740 + Strength x 2.0.
3. The cleave travels at 1.35 blocks per tick and lives 12 ticks, about 16.2 blocks. Each target in its path is hit once: full cleave damage, burning for at least 60 ticks (3 seconds), and a shove (0.26 horizontal, 0.18 up).
4. As it travels, the cleave scorches the ground segment under its path (disk radius 1.35). If it slams into a block it scorches the exact landing block, throws a 12 piece debris field (netherrack, magma block, basalt, blackstone), and ends. Its lava budget is 4 real lava sources per flight (see ground rules below).

**Right click, Lava Eruption:**
1. Cost: Ki (90 percent of max Ki per minute over 40 ticks, 3.0 percent of max Ki). You need Ki above 0.5 to start. Cooldown: 40 ticks (2 seconds).
2. Damage: 880 + Strength x 2.2.
3. The eruption lives 12 ticks at your position, facing locked to your rotation. Its reach grows from 1.1 blocks to 6.5 blocks over its life. A target is hit only when the expanding ring reaches it and it stands inside the frontal cone (about 132 degrees total, so nothing behind you or far to the side is hit).
4. Each target is hit once: full eruption damage, burning for at least 80 ticks (4 seconds), and knockback along your aim (0.60 horizontal, 0.40 up).
5. At tick 1 it throws a 24 piece debris field (radius 3.2). Each tick it scorches the arc ring it just expanded over. Its lava budget is 8 real lava sources per eruption.

**Ground burn rules (shared by the cleave and the eruption):**
- A burn lasts 80 ticks (4 seconds), then the original block is restored. Burns were always placed into air cells, so the restore puts back air.
- Fire or lava is placed only in an air cell sitting on solid ground (the code scans down up to 6 blocks, and can climb up to 4 to find air first).
- The first eligible cells a swing reaches, up to its budget (cleave 4, eruption 8), become real lava sources. Every other eligible cell becomes fire, if fire can live there. Fire cells do not consume the lava budget.
- The placed lava does not flow. Every tick, any neighbor cell that turned into lava, cobblestone, stone, obsidian, or basalt, or caught stray fire because of the burn, is reverted to what it was.
- No lava is placed if water is in the burn cell or next to it (fire is tried instead). Nothing is placed in a cell a living player is standing in, and nothing is placed on protected ground: bedrock, spawners, trial spawners, chests, trapped chests, ender chests, and barrels.
- Crash safety: every pending burn is saved with the level. After a server restart, overdue burns restore at once and live ones resume their remaining time. When a server stops cleanly, all pending burns are restored immediately. There are no permanent lava scars from this sword.
- Standing in the ground fire or lava hurts through normal vanilla fire and lava damage; the sword adds no custom burn damage number of its own.

---

## Spike Flail

Theme: one handle, one chain, one spiked ball. A wide swing arc, and an orbiting spin channel that cracks the ground on release. Stamina weapon, no Ki.

**Melee (left click hit):** a connected hit deals the weapon damage (820) only. The code intentionally adds nothing: no cost, no extra damage, no effect. The swing below is the real attack.

**Left click, Swing:**
1. Cost: Stamina (100 percent of max Stamina per minute over 20 ticks, about 1.667 percent of max Stamina). You need Stamina above 0.5. Cooldown: 8 ticks (0.4 seconds). It will not fire while you are channeling the spin.
2. Damage: 720 + Strength x 2.0.
3. The ball sweeps an arc in front of you over 11 ticks with its facing locked at swing start: it sweeps side to side out to 2.65 blocks, extends forward up to about 2.8 blocks at mid swing, and rises and falls as it goes.
4. Each target within 1.7 blocks of the ball is hit once per swing: full swing damage, and a push away from you through the ball (0.55 horizontal, 0.22 up).

**Right click, Spin channel and Crack (hold, then release):**
1. Hold right click to spin the ball around you (you need Stamina above 0.5 to start). The channel lasts 32 ticks (1.6 seconds) and then stops on its own; it also stops the moment a Stamina drain tick fails.
2. Channel cost: Stamina drained per tick (150 percent of max Stamina per minute, 0.125 percent per tick), about 4.0 percent of max Stamina for a full channel, paid as you go.
3. While orbiting, the ball circles at radius 2.55 blocks, advancing 32 degrees per tick. Contact damage is checked every 4 ticks, and a target can be re-hit at most every 8 ticks: 200 + Strength x 0.5 per contact hit, for targets within 1.25 blocks of the ball, plus a push away from you (0.35 horizontal, 0.12 up).
4. Charge is spin time / 32. On release the crack radius freezes at (2.7 + 2.7 x charge): 2.7 blocks on a tap, 5.4 blocks at full channel.
5. The crack disc then expands honestly over 7 ticks from 2.55 blocks to the crack radius, and no crack damage lands during the expansion. At release tick 7 the crack lands once: damage (480 + 600 x charge) + (Strength x (1.0 + 1.0 x charge)). That is 480 + Strength x 1.0 on a tap and 1080 + Strength x 2.0 at full channel. Full damage with no distance falloff, to everything inside the crack radius (the exact center point under you is excluded).
6. Crack knockback: radial (0.55 + 0.45 x charge) and up (0.30 + 0.16 x charge), so 0.55 and 0.30 on a tap, 1.0 and 0.46 at full channel.
7. The crack tears the ground surface inside its radius to gravel (certain within 1.5 blocks, 60 percent chance beyond; restored after 100 ticks, 5 seconds) and throws a debris field of (24 + 8 x charge) pieces: 24 on a tap, 32 at full channel. The entity then settles for 4 ticks and is gone.
8. Cooldown when the hold ends: 36 ticks (1.8 seconds).

---

## War Mace

Theme: heavy overhead mace, the heaviest hitter here. Ground smashes, a falling heavy smash that relaunches you, and a rising uppercut. Stamina weapon, no Ki. All three abilities share the same Stamina rate (125 percent of max Stamina per minute) charged over different durations.

**Melee (left click hit):** a connected hit deals the weapon damage (1300) only. The code intentionally adds nothing on the contact hit; the smash below is the real attack.

**Left click, Smash:**
1. Every left click is a smash. Which variant you get depends on your motion: falling (off the ground and moving down) means a heavy smash; otherwise a ground smash.
2. Costs: ground smash, Stamina (125 percent per minute over 28 ticks, about 2.917 percent of max Stamina). Heavy smash, Stamina (125 percent per minute over 48 ticks, 5.0 percent of max Stamina). Casting a heavy smash also resets your fall distance.
3. Locks: the ground smash has its own 10 tick lock, the heavy smash its own 16 tick lock, and they do not block each other. After a paid cast the mace itself gets a 5 tick cooldown.
4. Ground smash damage: 900 + Strength x 2.5 at the center, max radius 4.5 blocks, life 10 ticks. Heavy smash damage: 1600 + Strength x 4.0 at the center, max radius 7.6 blocks, life 14 ticks.
5. Damage fades with distance: full at the center, falling linearly to 35 percent at the rim. At the rim the ground smash deals about (315 + Strength x 0.875) and the heavy smash about (560 + Strength x 1.4).
6. The smash zone grows from 0.7 blocks to its max radius over its life. On ticks 1 and 2 everything inside the current radius is hit; from tick 3 on, only the expanding edge hits. Each target is hit once per smash. Hit knockback is small (0.25 out, 0.08 up); the spectacle is the damage.
7. A heavy smash follows you down as you fall and plants where you land (or on its first connecting hit, or at the ground below it within 24 blocks, or on its final tick). Impacts scar a fracture disk (radius 3 for ground, 5 for heavy; restored after 200 ticks for ground and 300 for heavy) and throw debris (24 pieces ground, 36 heavy). Fracturing turns the dirt family into coarse dirt, brick and deepslate brick blocks into their cracked forms, deepslate into cobbled deepslate, and stone family blocks into gravel. A heavy impact also shakes (intensity 2.4, 14 ticks, with a 3 tick hitstop).
8. Relaunch: the first time any smash connects with a target, your ground and heavy locks are both cleared, your fall distance resets, and you are launched: vertical velocity set to 1.08, plus a small forward push along your aim (0.18). This is the loop: smash down, connect, get relaunched, smash again. The 5 tick item cooldown is not cleared.

**Right click, Rising Blow:**
1. Cost: Stamina (125 percent of max Stamina per minute over 22 ticks, about 2.292 percent of max Stamina). You need Stamina above 0.5. It has its own 18 tick lock, and the mace gets a 6 tick cooldown after a paid cast.
2. You launch yourself: vertical velocity +1.08 and a small forward push along your aim (0.18), fall distance reset.
3. Damage: 480 + Strength x 1.5.
4. The rising hit zone climbs with you: its center rises 0.22 blocks per tick, and damage is live for the first 6 ticks of the entity 8 tick life. Each target within 3.4 blocks of the climbing center is hit once, the first tick the zone reaches it, for the full rise damage.
5. Hit targets are launched: 0.45 away from the rise origin and 0.95 up. The first connecting hit also bursts sparks at that target.

---

*Compiled from the weapon item and entity code in the current tree after the spectacle pass, cross-checked against the per weapon spectacle reports. Where a report and the code disagreed, this reference follows the code.*
