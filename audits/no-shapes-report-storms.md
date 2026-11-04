# No-Shapes Realism Pass Report: Storms and Atmosphere

Owner: Storms and AtmosphereClient (Phase 2). Base: dba ad22b32 (Phase 1 foundation).
Status: converted, verified by targeted javac on both classpaths, NOT committed.

Files edited (2, exactly the audit's Storms and AtmosphereClient rows):
`src/client/java/com/dragonblockarcanedba/client/fx/AtmosphereClient.java` and
`src/client/java/com/dragonblockarcanedba/client/particle/OtherworldCloudParticleManager.java`.
AzureStormRenderer and StormOfDarknessEffect were not edited by this owner;
their working tree changes belong to other owners.

## Partial diff audit and fixes

The previous Storms agent left partial work in both owned files.

AtmosphereClient already had the useful core of the conversion: a clipped
vapor precipitation source, terrain-height ground sampling, and darkness
material for the gloom volume. I kept those pieces and fixed four issues:

1. The partial added a plasma and vapor burst at the thunder sound point.
   I removed it. The audit assigns thunder light to the shared Bolt owner,
   and AtmosphereClient thunder is a scheduled sound at a random point in
   the volume, not an actual bolt strike. A floating glow burst there would
   have read as a strike that never happened. The sound call is restored
   to the exact HEAD form: the same random boolean selects THUNDER_CRACK
   or THUNDER_DARK, and the pitch is again computed inline as
   0.92f plus rand.nextFloat() times 0.16f.
2. The partial had no DUST material, although the Storms brief names dust
   as part of the storm weather. I added a wind driven DUST burst on the
   same clipped terrain source as the ground mist.
3. The partial sampled darkness from MaterialEmitters.volume, an axis
   aligned box. Its corners could place darkness particles outside the
   storm radius. I replaced it with rejection sampling against the storm
   circle, with the player position as the in-volume fallback, matching
   the clipping the rain already used.
4. The ground mist source is now shared by the mist and dust bursts, so
   both ground materials use the same storm clipping and terrain height.

OtherworldCloudParticleManager already had a per-formation seed field and
a cloudMaterial helper returning MIST or VAPOR options, but both particle
call sites still named DbaParticles.CLOUD_PUFF after the DbaParticles
import had been replaced by the DbaMaterialOptions import. The helper was
unused and the file carried an unresolved DbaParticles reference. I wired
cloudMaterial into both call sites, preserving every spawn count,
position spread, drift velocity, tier height, wrap rule, and proximity
rule in the manager.

## Before and after geometry counts

Measured at HEAD ad22b32 and in the finished tree. Both owned surfaces
were already free of drawn effect geometry; this was a material
conversion, not a geometry removal.

| File | BespokeGeometry calls | LayeredRender geometry calls | Direct addVertex calls | KiRenderHelper.vertex calls |
|---|---|---|---|---|
| AtmosphereClient.java | 0 to 0 | 0 to 0 | 0 to 0 | 0 to 0 |
| OtherworldCloudParticleManager.java | 0 to 0 | 0 to 0 | 0 to 0 | 0 to 0 |

Related presentation counts:

- AtmosphereClient MaterialEmitters.burst call sites: 0 to 4
  (VAPOR, MIST, DUST, DARKNESS).
- AtmosphereClient direct RAIN call sites: 1 to 1. The bespoke RAIN type
  and its streak motion are unchanged.
- OtherworldCloudParticleManager CLOUD_PUFF call sites: 2 to 0.
- OtherworldCloudParticleManager cloudMaterial call sites: 0 to 2.

## Materials per effect

### Storm rain and gloom (AtmosphereClient)

- Rain body: the existing DbaParticles.RAIN streaks, unchanged, falling
  at velocity minus 2.4 inside the cylinder around the player and clipped
  to the storm volume.
- Precipitation depth: VAPOR material, color 0xA9C6DE, scale 0.72,
  downward base velocity (0.0, minus 1.05, 0.0), spread 0.10, 3 particles
  per client tick through MaterialEmitters.burst. Its source samples the
  same clipped cylinder as the rain.
- Ground weather, every fourth client tick on one clipped terrain
  source: MIST, color 0x93A9BC, scale 1.55, 3 particles, slow drift; and
  DUST, color 0x8E9BA6, scale 0.85, 2 particles, wind velocity
  (0.20, 0.012, 0.03) with spread 0.045, so dust is driven sideways along
  the ground instead of falling as more rain.
- Gloom body: DARKNESS material, color 0x0A0D13, scale 1.15, sampled
  inside the storm circle around the player at body height, 1 particle
  per ground cycle, or 2 when the source gloom is at least 0.7. The post
  grade keeps its original strength; the darkness particles give that
  grade a visible body in the world.
- Thunder: no local material burst. The DbaSounds thunder scheduling and
  playback are unchanged, and visible lightning light remains the shared
  Bolt owner's job, per the audit row.

### Otherworld cloud field (OtherworldCloudParticleManager)

- Volumetric formations: MIST, color 0xFFF3D6, scale 2.4, carries two of
  each formation's three puffs per tick; VAPOR, color 0xFFFFFF, scale
  6.0, carries every third puff, so each bank keeps a lighter and faster
  dissolving component inside the slower mist. Per-formation seeds come
  from the partial diff's seed field.
- Ambient low wisps: the same MIST and VAPOR mix at the two call sites'
  original counts, scatter, heights, and drift velocities.

## Behavior preservation

Presentation only. The scoped diff and a line review confirm:

- AtmospherePayload handling in accept() is unchanged: sources merge
  when centers are within 2 blocks, expiry takes the max, gloom and
  radius take the max, thunder is OR-ed, and a new source's first strike
  is scheduled at clientTick plus 40.
- Thunder repeats with the unchanged formula clientTick plus 90 plus
  rand.nextInt(170), at the unchanged random point, volume 1.1f, and
  pitch range.
- The gloom curve is unchanged: the rim feather is still full strength
  inside 70 percent of the radius, and gloomCurrent still lerps in at
  0.012 per tick and out at 0.008 per tick.
- No server file, payload file, damage number, cooldown, cost, timing,
  duration, or mechanic was touched by this owner.

## Verification (javac audit only, no Gradle claim)

Targeted javac with JDK 25.0.5+8, -proc:none, sourcepath over
src/main/java and src/client/java, compiling the two owned files as
roots on each audit classpath:

- 26.2 (cp262.txt): zero errors naming AtmosphereClient.java or
  OtherworldCloudParticleManager.java. Four owned class files were
  emitted (AtmosphereClient, AtmosphereClient$Source,
  OtherworldCloudParticleManager, and
  OtherworldCloudParticleManager$CloudFormation). The run's only errors
  are the 4 known baseline lines in item/DbaItems.java (2) and
  inventory/DbaMenus.java (2), in files this owner did not touch.
- 26.3 (cp263.txt): zero errors naming either owned file; the same four
  owned class files were emitted. The run's only errors are the 10 known
  baseline lines: DbaItems (2), DbaMenus (2), DbaHostileEntity (2),
  FriezaSoldierEntity (1), NamekianWarriorEntity (1), and
  PatrolRobotEntity (2), all in files this owner did not touch.
- git diff --check on the two owned files: clean.
- Em dash scan of the two owned files and this report: zero.
- The VAPOR, MIST, DUST, and DARKNESS particle JSONs parse, and all 32
  texture references across their eight-frame sets resolve to files on
  disk.

No Gradle build was run, and no in-game look is claimed from this
environment.

## Shared change requests

None. The Phase 1 foundation covered both surfaces. The clipped sources
in AtmosphereClient are MaterialEmitters.Source data lambdas only; no
private particle system, texture style, or geometry helper was created.

## Honest worse check

Nothing was reverted, and neither surface had geometry to lose. Three
real trades need Carter's in-game judgment:

1. Material lifetime tails. Storm material particles live on their own
   profile clocks after a storm volume expires or the player leaves it:
   MIST runs 55 to 105 ticks and DARKNESS 30 to 62 ticks in the Phase 1
   profiles. Spawning still stops exactly at expiry, so the tail reads
   as weather dissolving rather than a hard cut, but visible material
   can outlast the gloom grade's own fade. That is a read change from
   the rain-only version, stated plainly.
2. Otherworld steady state density. Spawn counts are unchanged, but
   MIST (55 to 105 ticks) and VAPOR (24 to 54 ticks) live shorter than
   the CLOUD_PUFF they replaced (90 to 160 ticks), so a formation's
   steady state bank may read thinner even though the material sprites
   grow larger over their lives. If it reads thin in game, the fix is a
   spawn count adjustment in the manager, not a return to CLOUD_PUFF.
3. Darkness readability. DARKNESS is a normal blended near-black
   material. In a bright storm it can read as discrete drifting dark
   puffs rather than uniform shadow. That is the audit's assigned
   material for the gloom body, and the density is kept low (1 to 2
   particles per ground cycle), but the look is unverified until a PC
   test.
