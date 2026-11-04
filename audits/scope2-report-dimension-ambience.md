# Scope 2 Report: A6 Dimension Ambience

Measured and converted at HEAD ef5ad02 (Scope 2 foundation). Ownership per
audits/bespoke-scope2-audit.md area 5: dimension/DimensionTravel.java,
dimension/PlanetDimension.java, dimension/OtherworldStationGenerator.java,
client/particle/OtherworldCloudParticleManager.java,
block/YellowCloudBlock.java, plus the Otherworld biome JSON cloud_color
review the foundation flagged as A6 territory. Nothing committed. No other
agent's files were touched (the Lava Sword and Evil Spear dread work in
this tree is untouched). Verification is the javac audit only; no Gradle
build ran or is claimed.

## Before and after counts

- Vanilla ParticleTypes.CLOUD call sites: 3 to 0
  (OtherworldCloudParticleManager 2, YellowCloudBlock 1).
- Vanilla ParticleTypes lines in my Java files, imports included: 5 to 0
  (manager 1 import + 2 calls, YellowCloudBlock 1 import + 1 call; both
  imports removed, DbaParticles imported in their place).
- Vanilla SoundEvents call sites in DimensionTravel: 4 to 0.
- DbaSounds call sites in DimensionTravel: 1 to 5 (the pre-existing
  ABILITY_CHARGE arrival layer plus the four swaps below).
- Otherworld biome cloud_color: "#E6FFE070" to "#e6f2a7c3" (one value;
  every other line in the file byte-identical, verified by diff).
- PlanetDimension damageSources().drown(): 1 to 1, deliberately untouched
  (D11, see below).
- OtherworldStationGenerator: zero vanilla references before and after,
  untouched, as the audit recorded.

## Particles: CLOUD_PUFF wiring (D5)

The foundation built DbaParticles.CLOUD_PUFF end to end for exactly these
three call sites. Only the particle type token changed at each site;
every count, spread, velocity, and spawn condition is byte-identical:

- OtherworldCloudParticleManager volumetric pass: still 3 puffs per cloud
  formation per tick, same radius and y-offset spreads, same drift
  velocities.
- OtherworldCloudParticleManager ambient wisps: still 3 wisps every 2
  ticks in the same 50 block scatter and 8 to 30 block height band.
- YellowCloudBlock.animateTick: still a 1 in 4 chance per tick, same
  position scatter and breeze velocity math.

## Sounds: DimensionTravel swaps

Volumes, pitches, sources, and call order are unchanged at all four
sites; the file's existing fully qualified call style was kept.

- Launch, FIREWORK_ROCKET_LAUNCH (2.0, 0.6) to DbaSounds.SPACE_POD_LAUNCH.
  The bank event was synthesized for this exact call and had zero call
  sites until now.
- Launch, GENERIC_EXPLODE (1.5, 0.8) to DbaSounds.BOOM_BIG, per the swap
  guide's explosion mapping. The original launch was two layered sounds;
  the conversion keeps two layers (launch plus boom), as the foundation
  report anticipated.
- Arrival, PORTAL_TRAVEL (1.5, 1.2) to DbaSounds.RIFT_OPEN, per the swap
  guide's portal mapping (RIFT_AMBIENT / RIFT_OPEN; the open event fits
  an arrival beat).
- Arrival, BEACON_ACTIVATE (1.2, 1.4) to DbaSounds.CHARGE_RISE, per the
  swap guide's charge mapping and matching the foundation's own central
  fix, which mapped the same vanilla event to CHARGE_RISE in the main
  mod class. It still layers with the pre-existing ABILITY_CHARGE play
  beside it at the same volume and pitch.

## Biome decision: cloud_color aligned to pink

Conflict confirmed. The dimension_type JSON for Otherworld declares the
dimension's canon look: sky #f7cf3f (yellow), fog #f7e29a (pale yellow),
cloud_color #e6f2a7c3 (pink). That is the canon Otherworld pairing from
the show: a yellow sky over pink clouds. The biome JSON, however,
carried its own attributes entry, "minecraft:visual/cloud_color":
"#E6FFE070" (RGB FF E0 70, a warm yellow at E6 alpha), and a biome level
visual attribute wins inside that biome. So inside the Otherworld biome
the cloud layer rendered yellow against a yellow sky, erasing the
two-tone canon look the dimension definition establishes, while outside
the biome the same dimension showed pink clouds.

Change made: the biome value is now "#e6f2a7c3", the dimension's pink
verbatim. One value on one line. The biome's cloud_height (210.0) is
untouched, as are all gameplay fields: has_precipitation, temperature,
downfall, the effects colors, the ambient particle entry, spawners,
spawn_costs, carvers, and features are all byte-identical (the scoped
diff for the file is a single changed line).

## D11: PlanetDimension drown() left alone, confirmed

dimension/PlanetDimension.java is not modified (its scoped git diff is
empty). The low-oxygen hazard tick at line 44 still reads
serverLevel.damageSources().drown() for 4.0f, exactly as at HEAD. Per
decision D11 this is a game-system environmental hazard, not ability
presentation: no bespoke family fits drowning, and re-typing it would
change only its death message. It stays on the still-vanilla list with
that reason, as the foundation recorded.

## Verification

- Targeted javac (JDK 25.0.5+8, sourcepath over both source sets) of the
  three edited Java files on both audit classpaths: zero error lines in
  DimensionTravel.java, YellowCloudBlock.java, and
  OtherworldCloudParticleManager.java on cp262 and on cp263.
- Residual errors in those runs sit only in dependency-closure files and
  are all pre-existing baseline kinds: cp262, 4 lines (DbaItems 2,
  DbaMenus 2); cp263, 10 lines (DbaItems 2, DbaMenus 2, DbaHostileEntity
  2, PatrolRobotEntity 2, FriezaSoldierEntity 1, NamekianWarriorEntity
  1). None references CLOUD_PUFF, SPACE_POD_LAUNCH, or any symbol this
  task touched.
- JSON: the edited biome JSON and the Otherworld dimension_type JSON
  both parse.
- Scoped git diff for the whole task: 4 files, 10 insertions, 10
  deletions. Grep for ParticleTypes and SoundEvents across the three
  edited Java files returns zero matches.
- Em dashes: none in any changed line or in this report.

## Honest worse check

Nothing reverted. Three flagged trades, all inherent to the conversion
as specified:

1. CLOUD_PUFF is a much larger, longer-lived puff than vanilla CLOUD
   (foundation style: size 2.4, life 90 to 160 ticks, near white). The
   cloud manager spawns at identical counts and spreads, so with each
   puff bigger and lingering longer, the Otherworld formations will
   read fuller and softer-edged than the vanilla version, and
   YellowCloudBlock's occasional ambient puff will read as a bigger,
   slower wisp. That is the purpose-built type behaving as designed,
   but the manager's formation density was tuned against the vanilla
   puff; if it reads crowded in game, the fix is a spawn-count trim in
   the manager, not a type change.
2. The four travel sounds are now synthesized events. The structure is
   preserved exactly (two launch layers, three arrival layers), but
   synthesized timbre versus the firework, explosion, portal, and
   beacon originals is a listen-only judgment for Carter's PC test.
3. The biome cloud alignment is a deliberate visible change inside the
   Otherworld biome: cloud layer tint goes from yellow to pink. It
   matches the dimension definition and the canon look, and the old
   value made clouds invisible against the yellow sky, but it is a
   look change Carter did not have before, stated plainly.
