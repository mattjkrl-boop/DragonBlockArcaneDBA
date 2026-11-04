# Spectacle Report: Spirit Sword

Phase 2 weapon pass, built to audits/SPECTACLE-STANDARD.md from the Spirit Sword
section of audits/fresh-audit-heavy.md. Theme unchanged: cyan and gold spirit
energy. Files owned and edited: item/SpiritSwordItem.java,
entity/SpiritCannonBeamEntity.java, entity/SpiritCleaveEntity.java,
entity/SpiritImpaleEntity.java, and the three matching renderers. No shared
files were edited.

## State found on arrival (partial work audit)

A previous agent on this weapon died when the runtime restarted, mid-work.
Its edits were already in the tree across all seven files (about 595 inserted
lines). I read every diff hunk against the fresh audit before picture and
kept the work: it was substantive and correct (the occlusion fix, the beam
fade system, the cleave flight rebuild, the impale rebuild, and the renderer
envelope work all check out, and every shared-block call it makes matches the
real signatures in util/ and client/render/ki/). My own additions on top are
listed separately at the end of the bug section.

One brief inaccuracy to flag: the handoff said a SpiritCannonEntity.java
exists and that the War Mace agent had touched its quake damage staging. No
such file exists anywhere in the repo (grep for "class SpiritCannon" and for
"SpiritCannonEntity" returns only SpiritCannonBeamEntity and its renderer).
The cannon has always been item logic plus the beam visual entity, so there
was nothing to reconcile. The War Mace agent's actual quake work lives in its
own files and does not touch this weapon.

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Cannon pulse interval | 10 ticks | 5 ticks |
| Cannon pulse damage | (200 + Spirit x 1.5) x ramp | (100 + Spirit x 0.75) x ramp per pulse (same total rate, paid in twice as many half sized pulses) |
| Cannon percent HP bonus | 2% of max HP per pulse | 1% per pulse (same total rate) |
| Cannon pulse occlusion | none: full 32 block line, hits through walls | block raycast truncates damage at the first block, the same point the visual stops |
| Cannon SPARK per hit target | 6 per pulse | 4 per pulse (same total at the new cadence) |
| Cannon beam end on release | discarded in 1 tick | 6 tick synced fade out: alpha and drawn length ride the fade, motes rain off the dying beam |
| Cannon ground mark | none | at full ramp (40+ ticks) on an upward ground strike: light blue stained glass GroundScar, 120 tick restore, plus 8 crystal DebrisEntity shards once per second |
| Cleave flight life | discarded when tickCount > 8, about 15.3 blocks at 1.7 blocks per tick | 14 flight ticks (about 23.8 blocks) + 5 tick dissipation, total 19 ticks, lifetime synced to the client |
| Cleave damage | 500 + Strength x 2 + 2% max HP | unchanged |
| Cleave particles | 0 emitted, ever | three layer trail every tick, hit bursts, block impact burst |
| Cleave block hit | instant silent discard | ground burst, 8 SPARK, 6 KI_MOTE, 10 debris pieces sampled from the struck block, glass break sound, then dissipates in place |
| Impale lifetime | 28 ticks | 40 ticks |
| Impale particles | KI_MOTE every 2 ticks at about 3 x density | LayeredParticles.emitVortex every tick (core, mid, outer), opening ground burst, closing burst, mote rain over the last 10 ticks |
| Impale world mark | none | on open: 12 ground palette debris pieces, plus a crystal GroundScar under the target (center and cardinals at 65% each, own column heights, 60 tick restore) |
| Impale dead constructor | (Level, owner, Vec3, scale) with zero callers | removed |

Unchanged on purpose: melee 500 total damage at 3.0 attacks per second, swing
Ki drain 55.0 per 7 ticks, cannon Ki drain 55 percent per minute, cannon
release cooldown 20 ticks, impale effect durations (120 ticks melee and
cleave, 100 ticks cannon), BOUNCINESS 0.85 on melee and 0.80 on cannon hits.

## What was added, per point

1. Layered depth. The cannon beam now sheds particles along its whole length
   (mote core on the axis, cyan dust sheath at beam radius, gold wisps off the
   terminus third) instead of trickles at the two ends, and its renderer
   gained the orbital ring nodes (see bugs), a distance scaled flow segment
   count, and a low alpha glow pass over the core and sheath. The cleave
   flies a three layer particle trail (bright mote core, cyan mid a half step
   behind, gold wisps a full step behind) and its renderer draws three wake
   layers plus a distance faded halo pass. The impale runs the shared
   LayeredParticles.emitVortex every tick, and its renderer wraps the pillar
   in LayeredRender.drawVortexBands (three counter rotating bands) with a
   drawAnchorGlow ground disc; ring and pillar segment counts step down with
   camera distance through FxEnvelope.segments and LayeredRender.distScale.
2. Debris. Cleave block impacts throw 10 DebrisEntity pieces sampled from the
   block actually struck. The impale opening throws 12 pieces from the ground
   palette under the target. The cannon at full ramp throws 8 crystal shards
   per second off its ground strike point. All pieces use the shared
   DebrisEntity lifecycle: helical rise, release, rain down, fade on landing.
   Counts are below the standard's 24 piece finisher minimum because none of
   these three is a finisher class move: the impale is a deduped on-hit proc
   that can fire three times a second, the cleave is a spammable swing, and
   the cannon shards repeat every second for as long as the channel holds.
3. World interaction. The cannon pulse is occluded by blocks (bug fix below),
   burns a temporary crystal scar into open ground at full ramp, and sheds
   ground dust where it meets a block every 10 ticks. The impale crystallizes
   the ground under its target temporarily. The cleave bursts against the
   block it strikes instead of dying silently. No vortex forces were added:
   this weapon's theme is a cutting blade and a beam, not pull or lift, and
   its pre-existing BOUNCINESS identity is untouched.
4. Atmosphere. The Spirit Sword has no weather or darkness theme, so per the
   standard it gains no weather. Its atmosphere is light: the beam glow pass,
   the impale anchor glow, and the ring nodes riding the beam.
5. Lifecycle. Everything runs the shared envelope now. The beam fades in over
   5 ticks and out over 6 synced ticks with retraction, instead of vanishing
   in one tick; damage pulses land every 5 ticks inside the beam's active
   window rather than in half second steps. The cleave's renderer and its
   server trail both run FxEnvelope over the synced lifetime, and the blade
   dissipates in place over 5 ticks rather than discarding mid-flight. The
   impale builds, holds, and dies across 40 ticks with a closing burst and a
   mote rain finish.

## Bugs fixed

1. HEADLINE (from the audit): cannon damage had no block occlusion check
   while the beam visual truncated at walls, so the cannon killed through
   walls. The item pulse now raycasts blocks first and ends the damage line
   at the same distance the visual stops.
2. Beam discarded in a single tick on release (audit lifecycle fail). Fixed
   with beginFadeOut, a synced FADE value, and renderer alpha and length
   riding it. releaseUsing and the beam's own stop conditions all route
   through the fade now.
3. Cleave discarded mid-flight at tick 8 and instantly on any block, with no
   dissipation (audit lifecycle fail). Fixed with the flight and dying phase
   split and the synced LIFE and DYING values.
4. Dead code: SpiritImpaleEntity's third constructor had no callers (audit,
   re-verified by grep across src/ before removal). Removed.
5. Doc drift: SpiritCannonBeamRenderer's javadoc promised orbital Ki ring
   nodes and pulsing energy reticles along the beam that submit never drew.
   They are now actually implemented (drawBeamRingNodes: per node a rotating
   ring, a counter rotating pulsing reticle, and three orbiting beads,
   distance scaled), and the section comments inside submit run 1 to 5 in
   order.
6. Small inherited-state fixes of my own: the impale scar now skips columns
   whose surface sits more than 3 blocks below the target, so an airborne
   target no longer crystallizes distant ground; the cleave's startDying
   dropped an unused parameter; a stale "28-tick entity" comment in the item
   now says 40.

Noted, not changed: the cleave's damage field is still a plain server side
field, as the audit observed. The renderer does not scale with damage, and
the synced LIFE now gives the client the value it actually needs, so there
was nothing honest to sync it for.

## Shared change requests

None. Every shared block this weapon needed already existed with the right
signature. One verification note for the coordinating pass:
Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE) is used in two of my files for
the crystal scar state; it compiles clean against both the 26.2 and 26.3
jars (see verification), so no palette change is needed.

## Honest worse check

Nothing was reverted. The trade-offs I can name: cannon damage per pulse is
halved but the cadence doubled, so total damage rate is identical and only
the granularity changed, which the standard explicitly asks for. The fading
beam deals no damage during its 6 tick fade out, because cannon damage lives
in onUseTick and stops when the channel stops; a damaging ghost beam would
be the dishonest version. The cleave's longer flight is a real range increase
(about 15.3 to 23.8 blocks) that comes with the readable flight time the
standard demands; its per hit damage is unchanged. The impale's opening
debris and scar fire on a melee proc, which could read busy in a crowd, but
spawns are deduped per target, the scar restores in 60 ticks, and particle
counts ride the effects density config.

## Verification

Targeted javac of the seven owned files against both audit classpaths
(cp262 and cp263, -Xmaxerrs 10000, sourcepath over both source sets): zero
errors in any Spirit file on both versions, before and after my final edits.
The only errors in those runs are pre-existing baseline kinds in unrelated
files (DbaItems CreativeModeTab.Output access, DbaMenus MenuType access, mob
entity fields) plus other agents' in-flight work. No Gradle build was run;
it cannot run in this VM, per the standing rule. Final diff is exactly the
seven owned files, 606 insertions and 157 deletions, no em dashes in any
added line.
