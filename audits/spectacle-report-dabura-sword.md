# Spectacle Report: Dabura Sword (Darkness Sword)

Phase 2 weapon report. Base commit 5e4b1de, before picture from
audits/fresh-audit-dark.md section 1.

## State found on arrival

A previous agent on this task died mid-work when the runtime restarted. Its
javac verification command was killed before producing output, so nothing it
did was verified. On arrival I audited all eight edited files line by line
against the fresh audit and against the real shared block APIs. The partial
work was substantially complete and correct: every shared call it made
(LayeredParticles.emitVortex, emitRain, emitGroundBurst, dust,
DebrisSpawner.spawnField, VortexPhysics.applyVortex,
AtmosphereServer.begin, GroundScar.scarDisk, BoltEntity, FxEnvelope)
matches the real signature in the shared files. I kept it, with three
corrections of my own:

1. Debris palette. The partial work called DebrisSpawner.spawnField with no
   palette, which samples whatever ground the move stands on (dirt, grass).
   The brief for this weapon asks for a dark stone palette, and the theme
   (obsidian spires, void shards) supports it. All three call sites (domain
   fields, full eclipse, partial eclipse) now pass an explicit palette:
   obsidian, blackstone, polished blackstone, basalt.
2. A comment in DarknessBladeEntity claimed the blade "used to emit nothing
   at all on the way down". The diff shows the falling trail already existed.
   Comment corrected; comments in this pass must tell the truth.
3. Full self audit and dual classpath verification (below), which the dead
   agent never got to run.

## Before and after numbers

Unchanged on purpose: melee 800 base damage, attack speed -2.4, melee Ki
drain (35 over 10 ticks) and petrify 40 ticks amplifier 0; left charge cap
160 ticks, charge Ki drain 35 percent per minute, movement dampen 0.4;
slash damage 400 + chargeRatio x 800 + Strength x 3 x chargeRatio; wave
speed 1.6 + chargeRatio x 1.0; full charge extra waves (2 at 0.5x damage,
0.78x and 0.58x speed); slash cooldown 20 + chargeRatio x 40; channel cap
300 ticks; domain radius 8 to 30; channel Ki drain 35 percent per minute;
domain petrify reapply 40 ticks amplifier 0; hostile projectile velocity
scale 0.05; full eclipse burst 1200 + Strength x 4; eclipse suspension,
petrify 60 ticks amplifier 1, Cinematic Tracking 60 ticks; blade damage
350 each; blade spawn height 18 to 23 blocks, fall speed -1.8 per tick;
under-200-tick release cooldown 40 ticks with no effect.

| Value | Before | After |
|---|---|---|
| DarknessWaveEntity life | 40 ticks | 60 ticks |
| Wave max range at full charge speed 2.6 | about 104 blocks | about 156 blocks |
| Wave renderer fade window | full to tick 30, gone at 40 | full to tick 45, gone at 60 (matches new life) |
| Wave trail particles | 3 VOID_SMOKE per 2 ticks (about 30 per sec) | layered: 2 KI_MOTE core + 2 violet dust mid + 2 VOID_SMOKE outer per 2 ticks (about 60 per sec at density 1) |
| Wave renderer segments | 28 fixed | distance scaled via LayeredRender.distScale, min 14 |
| Domain particle emission | 5 smoke per 4 ticks + 2 spark per 12 ticks (about 28 per sec over radius up to 30) | LayeredParticles.emitVortex, 3 layers, about 11 per tick at peak (about 220 per sec at density 1), fade gated |
| Domain lifecycle | 10 tick refreshed lifetime, instant discard, constant renderer alpha (pop in, pop out) | build 30 ticks, teardown 25 ticks, synced FADE drives every alpha in the renderer |
| Domain debris | none | opening field 44 + radius x 0.7 pieces (65 at radius 30), plus 28 piece waves every 200 ticks, dark stone palette, 170 tick orbit |
| Domain lightning | 4 short fake arcs drawn in the renderer, no sound, no strike | real BoltEntity strikes inside the dome, about 1 per 14 ticks, branched, ground glow |
| Domain weather and gloom | none | AtmosphereServer THUNDER weather + 0.85 gloom, radius = domain + 14, refreshed every 20 ticks for 45 ticks, prior weather saved and restored by the shared system |
| Domain vortex force on mobs | none (vortex was a picture) | VortexPhysics.applyVortex every tick: pull 0.055, swirl 0.85, lift 0.05, envelope 0.35 + 0.65 x chargeRatio, weight classes apply |
| Full eclipse cooldown | 100 ticks | 160 ticks |
| Eclipse blade count | 10 flat at any radius | 10 + radius x 0.5 (25 at radius 30) |
| Eclipse ground scar | none | GroundScar obsidian disk, radius min(12, radius x 0.5), restore 240 ticks, chance 0.55, plus ground burst |
| Eclipse debris | none | 48 pieces, dark stone palette, radius x 0.7 orbit, 90 tick life |
| Eclipse lightning ring | none | 5 BoltEntity strikes at 0.75 x radius, length 16, life 8 |
| Partial eclipse (release 200 to 299) contents | burst only, at 0.6 x chargeRatio | burst (same formula) plus suspension kit, blade rain scaled by chargeRatio (14 blades at tick 200, 24 at tick 299), shatter, scar (radius min(8, radius x 0.4), restore 160, chance 0.4), 24 debris, ground burst |
| Partial eclipse cooldown | 80 ticks | 100 ticks |
| DarknessBladeEntity damage field default | 400 (dead, disagreed with the only call site) | 350, matching the call site |
| Blade impact detection | onGround() on a noPhysics entity (never true) plus block at position | block at position or directly below, 40 tick failsafe kept |
| Blade impact world effect | shatter decal only | plus GroundScar obsidian disk radius 2, restore 120 ticks, chance 0.6, plus ground burst |
| DarknessShatterEntity particles | ticks 0 to 8 only, silent for the last 32 of 40 | FxEnvelope driven across the full 40 ticks (build 0.08, decay 0.45), smoke and spark counts scale with the envelope |
| DarknessChargeEntity particles | 2 VOID_SMOKE per 3 ticks | plus KI_MOTE core of 1 + 3 x chargeRatio per 3 ticks, thickening with the charge |

## Per point additions

Point 1, layered depth. The domain emits through LayeredParticles.emitVortex
every server tick: KI_MOTE core, violet dust helical mid band, VOID_SMOKE
outer wisps, all scaled by effectDensity and gated by the lifecycle fade.
The wave trail is now three layers (mote core, dust mid, smoke rim). The
charge entity gained a mote core that thickens with charge ratio. The domain
renderer multiplies every alpha by the synced fade and distance-scales its
segment counts (ground bands 48, dome longitudes 36, inner dome 24, vortex
column 16, all with minimums); the wave renderer distance-scales its 28
segments the same way.

Point 2, debris. Domain class counts are met: opening field 65 pieces at
full radius in at least 3 sizes from DebrisSpawner, helical orbits, then
they fall and fade on landing through DebrisEntity. Refresh waves of 28
keep a long channel fed. The eclipses add their own fields (48 full, 24
partial). All pieces use the dark stone palette.

Point 3, world interaction. The domain vortex is a real force now
(VortexPhysics pull, swirl, and core lift, weight classed so bosses barely
move). Base dust bursts fire where the slash leaves the caster, at both
eclipses, and at every blade impact. Ground scarring is temporary and
restored through GroundScar: obsidian disks under both eclipses and a
radius 2 scar at each blade impact. No permanent destruction; darkness is
not a block breaking theme at this scale.

Point 4, atmosphere. The domain drives AtmosphereServer with THUNDER
weather and a 0.85 client gloom grade for the channel's life; the shared
system saves and restores the prior weather. The renderer's fake interior
lightning arcs (and their now dead helper methods drawBoltRibbon, boltHalf,
renderBeam) were removed and replaced with real seeded BoltEntity strikes
from the domain entity, plus a five strike ring at the full eclipse peak.

Point 5, lifecycle. DarknessDomainEntity runs the shared envelope as a
state machine: 30 tick build, hold while refreshed, 25 tick teardown with
particle rain as the suspended darkness falls back down. Every exit path
tears down instead of popping: clean release (releaseUsing calls
endDomain), Ki exhaustion (stopUsingItem routes through releaseUsing),
owner death, and refresh expiry all converge on beginTeardown. The wave's
fade window was realigned to its life. The shatter's particles now run its
whole 40 tick life on the envelope. Damage honesty: the full eclipse still
lands at the peak tick after a 300 tick telegraphed build; the partial
eclipse pays out proportionally instead of the old all or nothing cliff at
exactly tick 300.

## Bugs fixed

1. DarknessBladeEntity damage default 400 disagreed with its only call
   site (350). Default now 350.
2. DarknessBladeEntity impact relied on onGround() with noPhysics set, so
   it could never fire; the block check did all the work. onGround()
   removed, block at or below check kept, 40 tick failsafe kept.
3. Unused imports removed: DustParticleOptions, ParticleTypes, MobEffects
   in DarknessWaveEntity; DustParticleOptions, ParticleTypes in
   DarknessBladeEntity.
4. DarknessDomainEntity's class comment advertised atmospheric dark
   lightning the entity never spawned. The entity now spawns real
   BoltEntity strikes and the comment describes what the code does.
5. The domain had no teardown path: any interruption other than a clean
   release stopped the refresh and the dome vanished mid frame. Fixed by
   the teardown state machine above.
6. Wave lifecycle mismatch: the fade began at tick 30 of a 40 tick life, so
   a full charge wave spent a quarter of its visible life already dying.
   Life raised to 60 and the renderer fade moved to ticks 45 to 60.
7. Eclipse blade count was flat 10 at radius 8 and radius 30 alike, so a
   full size domain looked emptier than a small one. Now scales with
   radius.
8. DarknessShatterEntity went particle silent after tick 8 of 40 while its
   decal was still large. Emission now spans the whole life.

## Shared change requests

None required. One note for the coordinating pass: the dark stone debris
palette is declared as a small private constant in both DaburaSwordItem and
DarknessDomainEntity because weapon agents may not edit shared files. If a
shared theme palette home is ever added to DebrisSpawner or a theme util,
these two constants are the ones to fold into it.

## Honest worse check

Nothing was reverted. Judgment calls, stated plainly:

1. Wave range grew from about 104 to about 156 blocks. The audit flagged
   the fade and life mismatch, not the range; extending the life and
   matching the fade was the fix, and the longer reach is a buff consistent
   with a piercing wave that ignores terrain. If playtesting says 156 is
   too far, the honest knob is the 60 tick life, not the fade.
2. Full eclipse cooldown rose 100 to 160 ticks. Stacking is still
   impossible (the domain exists only inside a channel, and the cooldown
   gates the next channel), so this is pacing for the raised spectacle,
   not a stacking fix. It is a real, if small, power cost and is reported
   as one.
3. A channel that ends because Ki ran dry at 200 plus ticks still pays the
   partial eclipse through releaseUsing. The Ki for those 200 ticks was
   genuinely spent, so this reads as a payoff for Ki paid, not a free move.
   Kept from the partial work; flagged here so it is a decision on record,
   not an accident.
4. Debris uses the explicit dark palette instead of ground sampling. In a
   bright biome, black shards orbiting a darkness dome read as void debris
   rather than torn up terrain, which fits the fiction better. The scar
   work still touches the real ground, so point 3 does not depend on the
   debris palette.

## Verification

Targeted javac of all 11 Dabura files (8 edited, 3 unmodified renderers)
with -sourcepath over both audit classpaths: zero errors in any
Dabura or Darkness file on 26.2 and on 26.3. Remaining errors in those runs
belong to baseline noise and other agents' in flight files (DbaItems,
DbaMenus, mob entities, ZShockwaveEntity, OxShockwaveEntity,
KingsSlamEntity, CurseBladeItem, LavaSwordGround), none of which this
weapon touches. This is a javac audit, not a Gradle build; no build was run
or claimed. The full tree sweep is the coordinating pass's job once the
other weapon agents land.
