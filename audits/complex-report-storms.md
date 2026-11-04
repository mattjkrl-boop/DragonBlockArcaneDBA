# Complex Effects Redo, Phase 2 Report: Storms and AtmosphereClient

Owner slot: Storms and AtmosphereClient. Base HEAD 9178eac. File
edited, and only this one:
src/client/java/com/dragonblockarcanedba/client/fx/AtmosphereClient.java.
No commits made. The diff on the owned file is additive only: two
comment passages extended and two new burst calls added. Nothing
was removed or reordered.

## Preservation check

Everything the prior passes locked in this file is behaviorally
byte-identical after the edit, confirmed by reading the full git
diff:

- Thunder scheduling untouched: the first strike at source accept
  plus 40 ticks (s.nextThunderTick = clientTick + 40), then every
  90 to 260 ticks (clientTick + 90 + rand.nextInt(170)), with the
  same sound choice, volume, and pitch code.
- The gloom curve untouched: full inside 70 percent of the radius,
  feathered at the rim, lerp in at 0.012 and out at 0.008 per tick,
  gloomActive threshold 0.05.
- AtmospherePayload shape untouched (the file only reads it, as
  before) and all AtmosphereServer behavior untouched (no server
  file edited).
- Storms still run off the mod own weather system, never vanilla
  weather. No damage number, cooldown, cost, timing, mechanic, or
  synced data field changed. No geometry was added anywhere in
  this slot. BoltRenderer was not touched by this owner (its
  working tree changes belong to the Shared Systems slot).
- Split source sets stay clean: the only edit is client side, and
  it references DbaParticles (common) exactly as the existing RAIN
  emission already did.

## Storm rain and gloom (class M system): complex sprite layer added

Where: inside emitMaterialLayers, in the existing every 4 ticks
gated block, immediately after the existing DARKNESS burst. Both
new bursts sample the existing gloomVolume source, the source the
darkness layer already uses: points around the player at eye
height (player y plus 1.5, plus or minus 1.7), within
min(7.0, radius) horizontally, clipped to the storm radius by the
source own attempt loop, with the source own fallback to the
player position. No new emission point, radius, or clipping logic
was invented.

- WISP_CURL wind wisps: MaterialEmitters.burst with
  MaterialEmitters.Preset.fixed(DbaParticles.WISP_CURL,
  new Vec3(0.20, 0.012, 0.03), 0.045f), count 2 at gloomVolume.
  The velocity is the wind vector the existing DUST layer already
  uses in this same method, and the spread 0.045f is the dust
  layer spread, so the wisps travel with the storm air the
  material layers established. The sprite style (shared Phase 1)
  is a pale blue gray spiral curl, additive, life 26 to 50 ticks,
  size 0.24 growing, slight negative gravity, so a wisp drifts,
  rises a little, and curls out inside the volume.
- ASH_FLAKE airborne grit: MaterialEmitters.burst with
  MaterialEmitters.Preset.fixed(DbaParticles.ASH_FLAKE,
  new Vec3(0.20, 0.005, 0.03), 0.045f), count 2 at gloomVolume.
  Same wind vector with a lower vertical component; the shared
  style is a small gray multi-lobe flake, non-additive, life 30
  to 60 ticks, size 0.11, gravity 0.02, so the grit hangs in the
  air, rides the wind, and settles slowly instead of falling like
  the rain streaks.
- Rates, in keeping with the existing material rates at the same
  point: the gated block already emits MIST 3, DUST 2, and
  DARKNESS 1 to 2 (2 when gloom is at least 0.7) per gated burst.
  Each new sprite type emits 2 per gated burst, which at the
  20 tick client rate is 10 per second per type, inside the
  standard complex sprite continuous cap of 40 per second per
  emitter. MaterialEmitters.burst multiplies the count by
  DbaEffectsConfig.effectDensity(), the same density scaling
  every other layer in this file uses, and renderStorm already
  gates the whole method on density above 0.
- Pieces layer: none in the client system itself, exactly as the
  audit assigns for this slot. Thunder is the shared Bolt owner
  geometry (an M system), and this file only schedules the
  thunder sound at its locked cadence; it draws no bolt and no
  other geometry. Zero drawColoredBox, drawCylinder, drawDisc,
  drawSphere, or drawHelix calls in the owned file (grep
  verified), and no private emitter, geometry, or texture code
  was built: both sprites go through the shared MaterialEmitters
  burst path with Preset.fixed.

## Otherworld cloud field: deliberate exemption, file untouched

Restated exactly as audits/complex-audit.md records it for
OtherworldCloudParticleManager.java: pieces to add are "none: a
background sky field, not an effect body; recorded as a
deliberate exemption", and complex particles are likewise none.
No changes were made to that file in this slot. Verified with
git status: src/client/java/com/dragonblockarcanedba/client/
particle/OtherworldCloudParticleManager.java shows no
modification (it does not appear in git status output at all),
while AtmosphereClient.java shows as the slot only modified
file.

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/
javac, -sourcepath src/main/java:src/client/java, compiling
AtmosphereClient.java, private output tags /tmp/storms-262 and
/tmp/storms-263, logs at /tmp/storms-262.log and
/tmp/storms-263.log. Both full logs were read before making
these claims:

- 26.2 classpath (cp262.txt): zero errors in the owned file.
  The only errors in the closure are the known baseline kinds:
  DbaItems.java (CreativeModeTab Output protected access, 2) and
  DbaMenus.java (MenuType private access, 2), 4 errors total.
- 26.3 classpath (cp263.txt): zero errors in the owned file.
  Baseline kinds only, 10 errors total per the log summary: the
  DbaMenus private access kinds plus the known entity kinds in
  DbaHostileEntity, FriezaSoldierEntity, NamekianWarriorEntity,
  and PatrolRobotEntity (cannot find symbol and the swing
  signature mismatch).
- AtmosphereClient.class (9768 bytes) was produced in both
  output trees. git diff --check on the owned file is clean,
  and an em dash scan of the owned file and this report is zero.

## Foundation change requests

None blocking. One note for the merge gate checker, not a code
request: merge gate 1 in COMPLEX-STANDARD.md asks every class M
and class L effect for at least one ComplexGeometry builder call
in its file. This slot is the audit recorded exception to that
shape: the audit assigns this system no pieces in the client
system itself, because its one geometric element, thunder,
belongs to the shared Bolt owner. The gate should whitelist
AtmosphereClient.java on the audit authority rather than flag
the missing builder call as a defect.

## Honest worse check

Could the storm volume read worse with grit and wisps added? I
considered three ways it could, and none holds up against what
is already in the volume:

- Grit in a rain storm could read as out of place debris. It
  would, in a clean rain effect. This is not one: the storm
  volume already carries wind driven dust (DUST at 0x8E9BA6) on
  the ground and darkness material in the air, because these are
  weapon storms with gloom, not weather rain. The gray ash flake
  extends the existing dust read upward into the air at eye
  height, at the same wind vector and a smaller sprite size
  (0.11 against the dust material), so it reads as the fine
  fraction of material the storm is already throwing around,
  not as a new theme.
- Additive wisps could brighten the gloom and fight the post
  grade. The rate keeps this bounded: 10 per second with a 26
  to 50 tick life gives roughly 13 to 25 wisps alive at once,
  spread through a 7 block radius volume around the player,
  in a desaturated pale blue gray that sits inside the existing
  vapor (0xA9C6DE) and mist (0x93A9BC) palette. They read as
  wind structure inside the mist that is already there. The
  gloom grade itself, its target curve, and its lerp are
  untouched, so the screen darkening is exactly as strong as
  at HEAD; the wisps are lit detail inside it, not a competing
  light source at any count this rate can reach.
- Stacking and settling artifacts. Overlapping storm sources
  each emit their own gated bursts, but that is true of every
  existing layer in this file (mist, dust, and darkness stack
  the same way per source), so the new layers add no new
  stacking behavior. The flakes spawn at eye height inside the
  volume and drift sideways as they settle, which can carry a
  flake across a roofline or doorway; the rain streaks and the
  vapor veil already ignore terrain in the same volume, so
  this is the same artifact class the effect already has, at
  a much lower count.

Nothing in this slot reads worse than the current state in a
way I could not fix inside the standard, so nothing was held
back.
