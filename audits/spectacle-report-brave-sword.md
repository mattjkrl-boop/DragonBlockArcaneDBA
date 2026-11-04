# Spectacle Report: Brave Sword

Phase 2 weapon report, written against audits/SPECTACLE-STANDARD.md and the
Brave Sword section of audits/fresh-audit-blades.md (before picture at
94708b6). Theme unchanged: brave phoenix fire, gold and cyan valor.

## State found on arrival

A previous agent on this weapon died mid-work when the runtime restarted.
Its partial edits were in the working tree across all eleven Brave files
(item, five entities, five renderers). I audited every diff hunk against the
fresh audit. Verdict: the work was complete and coherent, not half done.
The charge fade out, the cross slash pulse restructure, the dash resolution
moving into the trail entity, the shockwave owning its own damage, the
renderer lifetime fixes, and the dead code removals were all finished and
mutually consistent, and every shared block call matched the real shared
signatures. Two things were missing, and I added them: GroundScar at the
dive impact (the standard item the previous agent had not reached), and an
em dash cleanup in the six Brave class headers. Everything below describes
the combined final state of the weapon.

## Before and after numbers

| Value | Before (94708b6) | After |
| --- | --- | --- |
| Base melee damage / speed | 800 (1 + 799), speed -1.8 | unchanged |
| Assault hit damage | 400 + Str x 2.0, heavy 5th x 1.5 | unchanged |
| Slash projectile life | 40 ticks, about 72 blocks of travel | 24 ticks, about 43 blocks, dies on the first solid block |
| Finisher direct damage | 900 + Str x 4.0, instant | unchanged amount, still instant on arrival |
| Cross slash life | 18 ticks | 40 ticks |
| Cross slash splash damage | 450 + Str x 2.0, all of it on ticks 1 to 2 | same total, four pulses of 25 percent at ticks 8, 14, 20, 26 |
| Charge Ki drain | 15 percent per minute, every held tick, forever | same rate, stops once full charge is reached (holding past tick 100 is free) |
| Charge entity ending | discarded instantly at full alpha | 10 tick synced fade out |
| Dash charge floor | 0.1 free on an instant release | removed, ratio is heldTicks / 100 from zero |
| Dash distance | 14 + ratio x 10 (15.4 tap, 24 full) | 8 + ratio x 16 (8 tap, 24 full) |
| Dash damage | 600 + ratio x 1400 + Str x 4 x ratio (740 + Str x 0.4 on a tap), instant in the release tick | same formula (600 flat on a tap, 2000 + Str x 4.0 at full), resolves at trail tick 6 against positions measured then |
| Rush trail life | 14 ticks | 30 ticks |
| Shockwave radius | 6 + ratio x 2.5 (8.25 to 8.5) | 5 + ratio x 3.5 (8.15 to 8.5) |
| Shockwave damage | dash total x 0.4, instant in the release tick, dash targets excluded | dash total x 0.4 per target, dealt when the expanding ring front reaches the target, no exclusion list |
| Shockwave life | 18 ticks, entity dealt no damage | 44 ticks, entity owns its damage |
| Right click cooldown | 100 ticks | 100 ticks, unchanged (now exceeds the longest visual chain: trail 30, then shockwave 44) |
| Debris pieces | 0 anywhere in the kit | 24 at the finisher, 28 at the shockwave |
| Ground scarring | none | magma disk, radius 2, chance 0.6, restore 300 ticks, at the dive impact |

Full charge versus tap now pays honestly: 3.3x the base dash damage and 3x
the dash distance, against 37 percent of the damage and 64 percent of the
distance being free before.

## What was added, per point

1. Layered depth. The slash flies with a layered wake (KI mote core on the
   flight line, gold dust mid band, cyan wisps at the rim). The cross slash
   runs LayeredParticles.emitVortex for its whole life and its renderer adds
   an outer pass at LayeredRender.ALPHA_OUTER around both blades. The rush
   trail emits layered particles along the whole dash path. The shockwave is
   three ring layers (faint wisp ring ahead of the front at ALPHA_OUTER,
   beveled gold-to-cyan crest, bright white core ring) over a shared anchor
   glow, and the charge construct gained the same anchor glow disc. All five
   renderers scale segment counts with camera distance through
   LayeredRender.distScale and FxEnvelope.segments.
2. Debris. The cross slash throws 24 ground-sampled DebrisEntity pieces at
   its first damage pulse; the shockwave throws 28 at ground contact. Both
   meet the finisher-class minimum of 24, in the spawner's three sizes,
   orbiting out and raining back down.
3. World interaction. Ground bursts at the finisher landing, the dash
   launch, the dash landing, and the shockwave impact. Real forces: the
   cross slash flings what it catches once per target, the dash throws
   pierced targets off the path line, and the shockwave throws targets
   outward as the front passes (VortexPhysics.fling throughout). The dive
   impact scorches the ground through GroundScar: a magma disk that restores
   after 300 ticks and is crash safe through the shared SavedData system; a
   shockwave fired high in the air scars nothing. The slash projectile now
   stops at the first solid block with a spark burst instead of cutting
   through walls and hitting targets behind cover.
4. Atmosphere. Brave has no weather or darkness theme and gained none, per
   the standard. Its atmosphere is light: anchor glows grounding the charge
   and the shockwave, and layered translucent glow copies per the known
   kiAdditiveType platform limit technique.
5. Lifecycle. Every Brave effect now runs the shared FxEnvelope (build 15
   to 25 percent, decay 30 to 40 percent) with the entity and the renderer
   reading the same lifetime from the entity. All damage lands inside the
   visual's active window: cross slash pulses at ticks 8 to 26 of 40, dash
   damage at the trail's peak tick 6 of 30, shockwave damage on ring front
   arrival across 44 ticks. The charge construct builds over 4 ticks and now
   dissolves over 10 ticks on release, on idle, and on unequip, instead of
   popping out at full alpha. Telegraphs are honest: the shockwave's damage
   band is the drawn front, plus or minus 0.75 blocks.

## Bugs fixed

- Renderer lifetime desync (the headline defect): BraveSlashRenderer
  hardcoded 40, BraveRushTrailRenderer 14, and BraveShockwaveRenderer 18 as
  second copies of the entity lifetimes. All renderers now read
  entity.getMaxLifetime(); the slash fade also no longer runs linearly
  across the whole flight (it was half faded mid flight).
- Charge construct popped out at full alpha on release, idle discard, and
  unequip: now a synced FADE factor and a 10 tick fade out on every exit
  path. The item replaces a fading charge entity immediately if a new
  charge starts, and onUseTick no longer treats a fading entity as live.
- The dash comment promised a delayed finishing slash but the damage ran
  instantly in the release tick against a target list collected before the
  teleport (stale by construction). The trail entity now resolves the dash
  at tick 6 against fresh positions.
- Sound duplication on heavy assault hits: the heavy branch played its
  louder sweep and whoosh pair and then the normal pair in the same tick.
  A heavy hit now plays only its own pair.
- Finisher teleport had no destination check: the past-the-target spot is
  now tested at feet and head height, and the player lands on the target
  instead of inside a wall.
- Dead renderer code removed: drawCrescentArc (slash), drawVolumetricCrescent,
  drawTipSpurs, and drawTaperedBeam (cross slash), drawBeam (rush trail).
- Promised geometry that was never drawn, now drawn: the shockwave header's
  crystalline valor spikes and epicenter starburst (which also gives the
  previously dead drawTriangle a real caller), and the rush trail header's
  twin helical streamlines (was a single gold strand, now gold and cyan
  strands half a turn apart).
- TrailRenderState.onRight was computed every frame and never read: removed
  with its computation.
- Unused DustParticleOptions import in BraveSlashEntity: removed.
- Ki kept draining at full charge for no extra damage, radius, or visual
  growth: the drain now stops at tick 100 of holding.
- The 0.1 charge floor gave away over a third of the dash for free: removed.
- Shockwave damage was applied by the item before the visual entity existed:
  the entity now owns and times its own damage.
- Misindented DbaImpactFx.heavy calls in the finisher and shockwave
  branches: indentation now matches the control flow.

## Shared change requests

- effect/HeroicFocusEffect.java (shared, read only for this agent): the
  charge buff's colour and dust particles are lime green 0x32CD32, while
  every Brave visual is gold and cyan, so the buff reads as a different
  ability. Recommend the coordinating pass recolor it to gold (0xFFD700).
- No DbaNetwork or registry change was needed: the shockwave is a normal
  entity spawn from the trail entity, and no new payload exists.

## Deliberately left, with reasons

- shouldRender returning true in four Brave renderers: left. The effect
  geometry extends many blocks past the entity's small AABB, so vanilla
  frustum culling would pop whole constructs at screen edges, which is
  worse. Flagged for a possible mod-wide policy decision.
- The finisher's BOUNCINESS request of 0.85: DbaPhysicsAttributes clamps
  internally, so the request is harmless; left at its pre-existing value.
- hurtEnemy's empty body: intentional per the audit; the 800 attribute is
  the melee implementation.
- Zero-length dash edge: if a movement multiplier reduces a dash to
  effectively zero length, no trail spawns and no dash damage resolves.
  Before, damage applied at zero distance. Rare, and a dash that goes
  nowhere dealing no dash damage is arguably honest; flagged here rather
  than silently kept.

## Honest worse check

Nothing was reverted, but four tradeoffs are real and stated plainly:

- Slash reach fell from about 72 to about 43 blocks. The audit showed the
  projectile outranged the combo logic that creates it by a wide margin;
  the shorter life plus wall stops make it a combo tool again, but it is a
  genuine range reduction.
- Cross slash splash damage is slower: a target that leaves the cross
  early takes only the pulses it stood in, where before it took the full
  amount on tick 2. A target that stays takes the full total and a fling.
- Dash damage can now be dodged in the 6 ticks before it resolves, where
  before it was instant. That is the standard's telegraph honesty rule,
  applied knowingly.
- The shockwave no longer excludes dash targets, so a target hit by the
  dash and still standing in the ring takes both hits. Slight overlap
  damage increase in that case.

No change made the weapon look worse: every visual change is additive
layering, debris, or geometry the renderers already promised, and the one
pure removal was dead code that never drew.

## Verification

Dual javac sweeps of the full tree against the 26.2 and 26.3 audit
classpaths (the established audit method for this repo) show zero error
kinds in any Brave-owned file on either version. Tree-wide totals during
the sweep (134 kinds on 26.2, 144 on 26.3) include other weapons'
concurrent Phase 2 work and are not a Brave signal. Gradle was not run:
the build cannot run in this VM and the compile check stands waived per
Carter's standing instruction. No files outside the Brave set were edited;
no commits made.
