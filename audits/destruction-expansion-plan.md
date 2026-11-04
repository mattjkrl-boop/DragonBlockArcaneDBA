# Destruction Expansion Plan

Directive (Carter, Nov 4, 2026): weapons may destroy MORE blocks, the
destruction does NOT need to restore itself, and terrain results may be
real permanent blocks. This supersedes the restore-everything behavior
the spectacle and lava redesign passes built. Written at HEAD ad22b32
plus the uncommitted no-shapes Phase 2 working tree (read only; nothing
in that tree was touched by this pass).

Lines that still hold:

- Unbreakable blocks are still refused (bedrock class, anything with a
  negative destroy speed).
- Protected blocks are still refused: the GroundScar list (any block
  with a block entity, bedrock, barrier, spawner, end portal frame and
  portals, command blocks, structure blocks, jigsaw) plus the lava
  sword's own ground list (chests, trapped chests, ender chests,
  barrels, trial spawners as ground under a burn).
- Everything stays crash-safe. Under the new rule this gets simpler,
  not harder: permanent edits are ordinary world edits saved by the
  chunk system, so there is nothing to orphan. The SavedData machinery
  below only survives where a timed stage schedule still needs it
  (the lava crust's cooling stages), never to undo a change.

## Measured inventory: what changes terrain today

Three terrain systems exist. Every weapon terrain effect in the mod
runs through one of them.

### 1. GroundScar (util/GroundScar.java)

SavedData-backed temporary replacement: scar() and scarDisk() record
the original block state per cell and a server tick restores it when
due. 26 call sites in 23 files. ALL of them restore today.

Entities (18 files):

- OxShockwaveEntity (Ox King's Ax): the ONLY real destruction in the
  mod today. Its expanding ring destroys weak blocks (explosion
  resistance at most 2.0, so dirt, grass, leaves, glass, snow class)
  2 cells high with drops, via destroyBlock. Ring surface cells are
  also scarred to magma block or basalt, restore 300 ticks on the
  destroying wave, 240 otherwise.
- KingsSlamEntity: heart of the slam scarred to magma block or basalt,
  restore 300.
- DevilSlamShockwaveEntity (Devil Trident): surface scar, restore 240.
- WarMaceSmashEntity: surface cells converted to their fracture
  variant (state-mapped cracked forms), restore per-ability ticks.
- SpikeFlailSpinEntity: surface to gravel, restore 100.
- ZGravitySlamEntity (Z Sword): surface to magma block, restore life
  plus 60.
- HellHuntImpactEntity (Sickle of Sorrow): impact scar, restore 160
  final form, 100 otherwise.
- DimensionalWarpRiftEntity (Whis Staff): surface scar per rift.
- DimensionalRiftEntity (Dimensional Sword): scarDisk radius 4, soul
  soil, 45 percent coverage, restore 300.
- VoidRiftEntity (Whis Staff): scarDisk radius 2 to 6, obsidian, 45
  percent, restore 240.
- DimensionalSlashEntity: one block to crying obsidian, restore 140.
- GrandCrescentWaveEntity (Grand Sword): scarDisk radius 1, coarse
  dirt, 50 percent, restore 200.
- BraveShockwaveEntity (Brave Sword): scarDisk radius 2, magma block,
  60 percent, restore 300.
- DarknessBladeEntity (Dabura's Sword): scarDisk radius 2, obsidian,
  60 percent, restore 120.
- SpiritImpaleEntity (Spirit Sword): surface to glass, restore 60.
- BanshoCycloneEntity (Bansho Fan): radius 3 scour, grass and mycelium
  to dirt, dirt and podzol to coarse dirt, 60 percent, restore 240.
- AzureTornadoEntity (Azure Dragon Sword): grass block to dirt,
  restore 240.
- AzureSonicQuakeEntity (Azure Dragon Sword): grass block to dirt,
  restore 200.

Items (5 files):

- DaburaSwordItem: two scarDisks, radius up to 12 (domain, restore
  240) and up to 8 (wave, restore 160), obsidian, 55 and 40 percent.
- GrandSwordItem: spin scarDisk radius 1 to 2, coarse dirt, restore
  240.
- CurseBladeItem: scorch radius 1, blackstone, restore 160.
- SpiritSwordItem: sustained beam strike crystallizes one surface
  cell to light blue stained glass, restore 120.
- PowerPoleItem: descending strike tears grass to dirt in a small
  patch, restore 120.

### 2. LavaSwordGround (entity/LavaSwordGround.java)

The lava sword's legacy burn: fire on air over sturdy ground plus a
per-swing budget of real lava sources placed with flow suppressed.
Every cell restores after 80 ticks; the lava cells also watch their
26 neighbors and revert any flow or water-cooking conversions.
SavedData mirrored, crash adopted, shutdown restores. Callers:
LavaEruptionEntity (scorchArc) and LavaMeteorEntity (scorchDisk),
both with lava budget 0 since the redesign; the budget path is live
code but currently unused by the field-based abilities.

### 3. LavaField (entity/LavaField.java)

The redesign engine. Magma crust cells cool through stages 0, 1, 2
and restore to the original air cell at the end of their profile:
POOL 60/120/200 ticks, CHANNEL 40/80/140, WAVE 14/30/54. Channels
flow downhill up to 6 cells. Mini volcanoes live 600 ticks and
restore. Caps: 800 cells per level, 240 per owner. SavedData
mirrored, crash adopted, shutdown restores. Callers: LavaCleaveEntity
(waveStep, carveChannel), LavaEruptionEntity (crustArc),
LavaBlobEntity, LavaMeteorEntity, LavaGeyserEntity (crustColumn),
LavaSwordItem (plantVolcano).

### What does NOT change terrain today

- Ki techniques: zero. KiBlastEntity.explode() is sound and particles
  only; Ki Explosion, beams, and disks never touch a block.
- Katana, Saber, Hollow's Edge, Evil Spear, Sickle of Sorrow (beyond
  the HellHunt scar), and Blaster Gun abilities: no terrain calls.
- The one non-weapon destroyBlock, DragonWishRitual consuming its
  ritual blocks, is already permanent and is not weapon scope.

## Restore behaviors this plan retires

1. GroundScar restores at every call site listed above, migrated per
   weapon below. GroundScar itself (class, SavedData type, and its
   register() call in the main class) is deleted only when the last
   caller migrates; until then it stays for the unmigrated callers.
   Its isProtected() check is NOT retired: the new utility delegates
   to it, and when GroundScar finally goes, isProtected moves into
   the new utility verbatim.
2. LavaSwordGround's 80 tick restore of lava source cells, and its
   neighbor watch that reverts flow and water conversions. Fire cells
   keep an 80 tick lifetime (that is a fire duration, not terrain
   restoration); placed lava becomes real and stays.
3. LavaField's end-of-profile restore: crust cells settle into a
   permanent end block instead (see the lava section). The stage
   schedule, SavedData mirroring, and crash adoption stay, because
   the cooling stages are timed behavior, not restoration. Shutdown
   no longer restores field cells; it settles overdue cells to their
   end block. The volcano entry's restore retires the same way: the
   site ends as a permanent dormant volcano (below).

## The shared foundation (built this pass)

util/PermanentDestruction.java, new file, no edits to existing files.
Server-side only by type (every method takes ServerLevel), refusal
rules delegated to GroundScar.isProtected plus an unbreakable check
(negative destroy speed) and a per-call explosion resistance cap.
Every operation is budget-clamped (hard ceiling 1024 blocks per call)
and safe under repeated calls: destroyed cells are air and are
skipped, refused cells stay refused, no state is kept anywhere.

Operations:

- destroy(level, pos, dropItems, maxBlastResistance): one block.
- crater(level, center, radius, depthScale, dropItems,
  maxBlastResistance, maxBlocks, rand): jittered ellipsoid crater.
- fissure(level, start, direction, length, width, depth, dropItems,
  maxBlastResistance, maxBlocks): a cut line along a heading.
- destroyDiskSurface(level, center, radius, chance, dropItems,
  maxBlastResistance, rand): top block of each column in a disk.
- convert(level, pos, newState) and convertDisk(...): the permanent
  analog of scar and scarDisk, for themed end states (obsidian,
  blackstone, soul soil, crystal) that should stay.

Drop policy: craters and fissures default to no drops (a 200 block
crater raining item entities is a lag source, and Dragon Ball
destruction vaporizes more than it harvests). The Ox ring keeps its
current drops, and small precision breaks (convert targets, single
blocks) drop normally.

Resistance tiers for the wiring pass: WEAK 2.0 (vegetation, dirt,
glass class, today's Ox cap), STANDARD 6.0 (stone class joins for
real slams), HEAVY 50.0 (finishers and ultimates only; obsidian class
still mostly survives at 50, and unbreakable is always refused
regardless of tier).

## Per-weapon expansion plan

Sizes are the wiring pass targets. Radii are blocks; caps are blocks
changed per ability use. Every entry keeps the weapon's current
theme block as its permanent conversion where it has one.

- Ox King's Ax (the destroyer, expand the lead): the ring keeps
  destroying as it expands. Resistance cap scales with charge:
  2.0 uncharged to 6.0 at full charge. Depth: 2 high today, 3 high
  inside half radius at full charge. The magma and basalt scar ring
  becomes a permanent convertDisk at 40 percent. Per-wave cap 700.
- War Mace (crater flagship): the smash becomes a real crater,
  radius 3 (4 on the heavy smash), depthScale 0.6, STANDARD tier,
  cap 220. The fracture ring outside the crater converts
  permanently to the same state-mapped cracked variants it uses
  today, one ring wider than the crater.
- Devil Trident (Devil Slam): crater radius 3, depthScale 0.5,
  STANDARD, cap 160, plus the current surface scar converted
  permanently to basalt with magma block at 40 percent.
- Z Sword (Z Gravity Slam): crater radius 2.5, depthScale 0.5,
  STANDARD, cap 120; the magma block surface conversion stays,
  permanent. Falling monolith impacts (shared systems slot) get a
  radius 2, depth 1 crater, WEAK tier, cap 60.
- King's Slam (KingsSlamEntity): heart crater radius 2, depthScale
  0.5, STANDARD, cap 100; magma and basalt conversion permanent.
- Brave Sword (shockwave): crater radius 2 at the wave's ground
  contact, WEAK, cap 60; magma block disk permanent at 60 percent.
- Grand Sword: the crescent wave carves as it travels: fissure
  width 1, depth 1, WEAK, along the wave's ground track, cap 90.
  The spin move: crater radius 2, depth 1, WEAK, cap 60, coarse
  dirt conversion permanent.
- Azure Dragon Sword: the tornado strips the top block per column
  along its path permanently (destroyDiskSurface behavior on its
  existing track, WEAK, drops on, cap 200 per tornado life, grass
  and foliage only at WEAK this is mostly soil and plants). Sonic
  quake: ring fissures, two cracks radius 3 and 5, depth 1, WEAK,
  cap 80; the grass-to-dirt scour becomes permanent conversion.
- Bansho Fan (cyclone): the scour becomes permanent conversion
  (same grass to dirt, dirt to coarse dirt mapping), plus the
  cyclone path destroys WEAK surface blocks (leaves, grass,
  flowers, snow) with drops, cap 150 per cyclone.
- Dabura's Sword: domain disks convert permanently to obsidian at
  reduced radii (domain 8, wave 6) and 45 percent; the domain
  center gets a crater radius 2, depth 1, WEAK, cap 60.
- Cursed Blade: blackstone scorch converts permanently, radius
  grows 1 to 2 on the charged strike; ground shatter impacts get a
  crater radius 2, depth 1, WEAK, cap 60.
- Dimensional Sword: rift ground converts permanently (soul soil
  disk radius 4 as today, obsidian for warp rifts); the rift mouth
  eats a shallow crater radius 2, depth 1, WEAK, cap 50. The slash's
  single crying obsidian conversion becomes permanent.
- Whis Staff (Void Rift): obsidian disk converts permanently as
  sized today. No crater: a void rift consumes, it does not blast.
  Time effects stay terrain-free.
- Spirit Sword: impale glass and the beam's crystal conversion
  become permanent as-is. No crater (precision weapon).
- Power Pole: descending strike crater radius 2, depth 1, WEAK,
  cap 50; torn turf conversion permanent.
- Spike Flail: spin fracture converts permanently to gravel as
  today, and the spin destroys WEAK surface blocks in its radius,
  cap 60 per spin.
- Sickle of Sorrow (HellHunt impact): crater radius 2, depth 1,
  WEAK, cap 60 on the final form only; the scar converts
  permanently.
- Blaster Gun: the Erasure beam's ground contact carves a trench:
  fissure width 1, depth 1, STANDARD, along the contact track
  while the beam holds, cap 120 per beam. Erasure orb detonation:
  crater radius 2.5, depthScale 0.5, STANDARD, cap 120.
- Katana: ground-cut fissure on the charged slash's landing line,
  width 1, depth 1, WEAK, length 6, cap 30. Otherwise terrain-free.
- Evil Spear: lunge landing fissure, width 1, depth 1, WEAK,
  length 4, cap 24.
- Saber, Hollow's Edge: terrain-free by identity (duelist and
  phantom weapons). No changes.
- Ki techniques (systems slot, not a weapon): Ki Explosion gains a
  real crater scaled by charge, radius 2 to 4.5, depthScale 0.55,
  STANDARD (HEAVY at full charge), cap 260. Charged Ki Blasts get a
  radius 1.5, depth 1, WEAK crater on block impact, cap 40. Beams
  and disks stay terrain-free in this pass.

## Lava Sword under the new rule

- Crust end states (replacing restore): when a crust cell finishes
  its profile it settles into a real permanent block instead of
  returning the original air cell. POOL cells settle to basalt.
  CHANNEL cells settle to basalt, with the channel's centerline
  cells at 25 percent magma block (the last heat of the flow).
  WAVE cells still vanish on schedule (a permanent stripe under
  every cleave path would paint the world; wave cells that the
  channel carve upgrades become CHANNEL and settle as above).
  Settle happens through PermanentDestruction.convert so the
  refusal rules apply at settle time too; a cell a player built
  over meanwhile is never stomped (the existing stillOurs check
  already guarantees this).
- Legacy scorch: LavaSwordGround's budgeted lava sources become
  real lava, placed with normal update flags so it flows, and are
  never restored. The neighbor watch and its reverts are deleted.
  Fire keeps its 80 tick lifetime. The meteor and eruption callers
  may spend a small lava budget again (meteor 3, eruption 8) now
  that the result is honest lava.
- Volcano: at the end of its 600 tick life the volcano stops
  erupting and stays as a permanent dormant volcano block. The
  field entry converts from a restore guarantee to a settle
  record. endVolcano (manual removal) still removes it.
- Look of the lava blocks (Carter's note: the crust and volcano
  blocks look crappy): the wiring pass includes a dedicated art
  step for MagmaCrustBlock and MiniVolcanoBlock before any of the
  above ships: regenerate their textures procedurally at 128px in
  the no-shapes material style (fractal noise basalt with glowing
  crack networks for stage 0, cooling to dark cracked rock at
  stage 2, three distinct stage textures instead of tints of one),
  and revise the crust model from a flat slab to a low irregular
  puddle profile. Permanent blocks get looked at forever, so this
  step gates the lava wiring, not the other way around.

## Wiring pass sequence (after the visual merge lands)

1. Lava block art step (above). Gates step 4 only.
2. Migrate the pure conversions first (lowest risk): Azure scour,
   Bansho scour, Spirit crystal and glass, Dimensional soul soil
   and obsidian, Dabura disks, Cursed blackstone, Power Pole turf,
   Spike Flail gravel, War Mace fracture ring, Ox scar ring.
   Each is a call-site swap from GroundScar.scar to
   PermanentDestruction.convert with identical sizes.
3. Add the new destruction: Ox charge scaling, then the slams
   (War Mace, Devil, Z, King, Brave), Grand fissure, Blaster
   trench and orb, Ki Explosion and Ki Blast craters, the small
   fissures (Katana, Evil Spear), tornado strip.
4. Lava migration: crust settle end states, legacy lava made real,
   volcano settle, LavaSwordGround watch deletion.
5. Retire GroundScar when its last caller is gone: delete the
   class, its SavedData type, and the register() call; move
   isProtected into PermanentDestruction verbatim. LavaField and
   LavaSwordGround keep their SavedData only for live stage and
   fire schedules.
6. Dual javac sweeps (26.2 and 26.3) against the pre-wiring
   baseline after each step, per standing practice. No Gradle
   claim; in-game look and crater feel are Carter's PC test.

## Verification for this pass

- Inventory measured from the working tree code (grep for
  GroundScar, destroyBlock, setBlock across items and entities),
  not from prior reports.
- util/PermanentDestruction.java compiles with zero errors in the
  new file under targeted javac on both audit classpaths
  (cp262.txt, cp263.txt).
- Nothing else was edited, committed, or reverted. The Phase 2
  working tree changes are untouched.
