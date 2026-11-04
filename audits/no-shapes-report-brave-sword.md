# No-Shapes Realism Pass Phase 2 Report: Brave Sword

Weapon owner report for the Brave Sword, against HEAD ad22b32 (No-Shapes
Phase 1 foundation). Binding rules: audits/NO-SHAPES-STANDARD.md and the
Brave Sword section of audits/no-shapes-audit.md. Files touched: only the
five Brave renderers listed below. The item, all five Brave entities, and
every other Brave file are byte-identical to HEAD: no damage number,
cooldown, Ki cost, timing, mechanic, or synced data field changed anywhere
in this conversion. Nothing committed; the coordinating pass owns commits.

Files converted:

- client/render/BraveChargeRenderer.java
- client/render/BraveSlashRenderer.java
- client/render/BraveCrossSlashRenderer.java
- client/render/BraveRushTrailRenderer.java
- client/render/BraveShockwaveRenderer.java

ValorFieldEntity note: it is a Grand Sword file, not a Brave file, and its
documented suspended-projectile damage attribution exception is mechanics,
not presentation. Nothing in this conversion touches it, and no Brave file
needed the exception: every Brave damage construction still goes through
DbaDamage.valor exactly as the bespoke pass left it.

## Before and after geometry counts

Counts are BespokeGeometry builder calls (B), LayeredRender geometry
calls (L: drawVortexBands, drawAnchorGlow), and direct vertex draws (V:
addVertex and KiRenderHelper.vertex), per the audit rows.

| Renderer | B before | B after | L before | L after | V before | V after |
| --- | --- | --- | --- | --- | --- | --- |
| BraveChargeRenderer | 12 | 0 | 1 | 0 | 0 | 0 |
| BraveSlashRenderer | 5 | 0 | 0 | 0 | 0 | 0 |
| BraveCrossSlashRenderer | 6 | 0 | 0 | 0 | 0 | 0 |
| BraveRushTrailRenderer | 9 | 0 | 0 | 0 | 0 | 0 |
| BraveShockwaveRenderer | 9 | 0 | 1 | 0 | 0 | 0 |
| Owner total | 41 | 0 | 2 | 0 | 0 | 0 |

All submitCustomGeometry calls are gone with the builders (13 in the
charge renderer alone across its shard, core, and halo loops). No
KiRenderHelper reference of any kind remains in the five files, and no
LayeredRender reference remains either: distance scaling now comes from
MaterialEmitters.distanceScale. The two pose and composition helpers that
survived the bespoke pass (BraveCrossSlashRenderer.drawCross,
BraveRushTrailRenderer.sheet) are deleted; the small helpers that remain
(basis, span, frameDelta, frontSample, strike) compute emission data only
and draw nothing.

## Conversion method

Every renderer now stops drawing entirely and emits through the shared
Phase 1 toolkit:

- Emitters are MaterialEmitters.Emitter instances stored on the render
  state, created lazily per entity seed and level, and ticked with a
  delta derived from the render state's own age, so emission is frame
  rate independent like the toolkit intends.
- The tick envelope is the exact lifecycle envelope the geometry version
  used (FxEnvelope.envelope or the charge fade math), multiplied by
  MaterialEmitters.distanceScale, so distance behavior and the effects
  density config still govern how much material an effect sheds.
- Sources are data only: gathering streams, swing width samples, dash
  path samples, sphere volumes, ground areas, and front ring samples.
  Nothing is rendered from them; they only place particles.
- Every emitter carries a hard per-second budget, recorded per effect
  below. Brave gold 0xFFD700 stays the locked theme color everywhere it
  was before; the heroic cyan 0x00F2FF and white core accents carry over
  unchanged.

## Materials per effect

BraveCharge (was glow fans, shard fans, displaced domes, torn ribbons,
one drawAnchorGlow): valor plasma (gold PLASMA, rate 175/s, budget 280)
and soul light (cyan SOUL, 65/s, budget 120) on a gathering source that
samples the old construct radius and streams inward to the core point at
the caster's chest; white-gold VAPOR (40/s, budget 80) drifting in the
core volume; gold DUST (30/s, budget 60) across the ground area. Envelope
is the old base alpha math without the alpha: charge-scaled brightness,
4 tick spawn fade-in, and the synced FADE factor on the way out.

BraveSlash (was three bladeArc layers and two shardFan strike flashes):
valor plasma shed across the swing width at the cut's leading belly
(gold PLASMA 135/s, budget 240; white PLASMA core stream 75/s, budget
140) sampled across the same opening span the old cut drew, plus cyan
VAPOR (48/s, budget 90) trailing behind the real flight path. The strike
flash is now a one-shot burst in the same early strike window (white
plasma 14, gold plasma 10, scaled by the strike curve).

BraveCrossSlash (was six bladeArc layers, four tip shard fans, two
starburst shard fans, plus an additive replay): valor plasma shed along
the two diagonal swing paths (gold PLASMA 160/s, budget 280; white PLASMA
85/s, budget 150) sampled across the same growing 4.6 times scale span,
cyan SOUL light (55/s, budget 110) and white-gold VAPOR (32/s, budget 70)
drifting through the effect volume. The strike starburst is now a
one-shot burst in the same 0.28 to 0.62 window (gold plasma 26, white
plasma 14, cyan soul 10, scaled by impact and slash scale).

BraveRushTrail (was eight tornRibbon sheets and one brokenVortexBand
wake): valor plasma shed along the real dash path (gold PLASMA 175/s,
budget 300; white PLASMA core stream 85/s, budget 150), widest at the tip
and narrowing down the trail where the old cone volume sat; cyan VAPOR
(60/s, budget 110) on the same path; gold DUST (30/s, budget 60) kicked
up along the ground under the dash. First person owner scaling still
shrinks the path width and now also trims the emission envelope to 0.6.
The strike wake is a one-shot burst at the trail tip (white plasma 10,
gold vapor 8) in the same early strike window.

BraveShockwave (was four brokenVortexBand layers, two crackStar ground
sets, two epicenter shard fans, up to twelve riding shard fan spikes,
one drawAnchorGlow): compressed ground DUST (gold, 155/s, budget 300)
sampled on the expanding front at the entity's own sine ease radius,
valor PLASMA (gold, 95/s, budget 180) at crest height on the same front,
cyan SOUL light (52/s, budget 110) just behind the front, and a white
PLASMA core stream (60/s, budget 120) in the epicenter volume. The
epicenter burst is a one-shot at spawn (gold plasma 20, gold dust 14).
The entity's own debris, ground scar, and ring-front dust were already
material and are untouched.

## Numbers and mechanics check

No entity or item file was edited, so every number from the bespoke
report stands byte-identical: melee 800 and -1.8 attack speed; assault
400 plus Strength times 2.0 with the heavy fifth hit at 1.5 times; slash
speed 1.8 and life 24; finisher 900 plus Strength times 4.0; cross slash
life 40 with four 25 percent pulses at ticks 8, 14, 20, 26 inside radius
3.5 times scale 1.25; charge Ki drain 15 percent per minute held to 100
ticks; dash 8 plus ratio times 16 blocks with damage 600 plus ratio times
1400 plus Strength times 4.0 times ratio resolving at rush trail tick 6
of 30; shockwave radius 5 plus ratio times 3.5, damage at ring contact,
life 44, 28 debris, magma scar disk radius 2 at chance 0.6 restoring
after 300 ticks; right click cooldown 100 ticks. Synced data is
unchanged: CHARGE_RATIO, CASTER_ID, and FADE on the charge; SLASH_SCALE
on the cross; TRAIL_SCALE, TRAIL_LENGTH, and CASTER_ID on the rush trail;
MAX_RADIUS on the shockwave; the slash's UUID-derived presentation seed
is computed the same way as before, only spent on emitter seeds instead
of geometry seeds.

## Verification

Targeted javac with JDK 25 (workspace tools jdk-25.0.5+8) over the five
converted renderers, sourcepath covering both source roots:

- Classpath cp262.txt: 4 errors total, all pre-existing baseline kinds in
  untouched shared files (DbaItems creative tab access x2, DbaMenus
  MenuType access x2). Zero errors in any Brave file; 13 Brave renderer
  classes emitted.
- Classpath cp263.txt: 10 errors total, all pre-existing baseline kinds
  in untouched files (DbaItems x2, DbaMenus x2, DbaHostileEntity x2,
  FriezaSoldierEntity swing signature, NamekianWarriorEntity,
  PatrolRobotEntity x2). Zero errors in any Brave file; 13 Brave
  renderer classes emitted.

Grep audit over all five files after conversion: zero BespokeGeometry
references, zero LayeredRender references, zero KiRenderHelper
references, zero submitCustomGeometry, zero addVertex, zero em dashes.
One cleanup landed after the 26.3 run started: an unused FxEnvelope
import was removed from BraveChargeRenderer (grep proved the name
appeared nowhere else in the file, so the removal cannot change its
compile result). Javac audit only; no Gradle build was run or claimed.

## Toolkit gaps and shared change requests

No blocking gap. Two optional foundation notes for the coordinating pass:

- Emitter rate is fixed at construction, so charge-driven intensity is
  applied through the tick envelope multiplier instead of the rate
  itself. A rate scale setter on Emitter would make charge-style
  effects cleaner, but the envelope route works and stays inside the
  budget.
- DbaMaterialOptions carries no lifetime scale, so an owner cannot cap
  how long a spawned soul or vapor mote lives. A lifetime factor in the
  options record would let short effects keep their afterglow inside
  their own lifecycle window (see the worse check).

## Honest worse check

Nothing was reverted, but the trades are real and stated plainly:

- Material lifetimes are profile-fixed (plasma 10 to 22 ticks, soul 34
  to 70, vapor 24 to 54, dust 20 to 44) and are not slaved to the effect
  envelope. Soul and vapor motes shed near the end of the cross slash or
  a charge can linger as a faint afterglow after the entity is gone,
  where the geometry version vanished exactly on its envelope. Plasma,
  the primary material in every Brave effect, dies inside 22 ticks and
  does not have this problem.
- The charge loses its readable central construct. There is no core
  object and no converging shard silhouettes anymore; the charge reads
  as light streaming into the caster, which is what the standard asks
  for, but the strong focal object Carter may have liked is gone by
  design.
- The slash and cross no longer present a continuous cut surface. At
  low effects density settings the cut will read sparser than the old
  crescent did, because emission is density scaled and budgeted.
- The shockwave front is sampled dust and plasma rather than a
  continuous crest. At the largest radius (8.5 blocks) the front can
  read beaded between samples instead of solid, especially at distance
  where distScale thins the emission.
- Emission lives in the renderers' submit path with emitters stored on
  the render state. If a render state is ever recreated mid-effect
  rather than reused, its accumulator restarts and that frame falls
  back to a one-tick delta; emission continues, but the rate is
  approximated for that frame.

Damage amounts, timings, radii, counts, and cooldowns are numerically
identical everywhere, because no file that owns them was touched.
