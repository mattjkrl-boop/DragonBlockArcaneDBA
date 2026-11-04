# Spectacle Report: Z Sword (Phase 2 weapon agent)

Base: commit 5e4b1de. Before picture: the Z Sword section of audits/fresh-audit-blades.md
(7 abilities, 21 numbered issues). Theme kept throughout: ancient heavy blade, gravity,
weight. Gold for the wave, charge and slam; violet for the gravity stance and telegraph.
No colors, identities or signatures were changed.

Files changed (11, all inside my scope):
- item/ZSwordItem.java
- entity/ZChargeEntity.java, entity/ZShockwaveEntity.java, entity/ZStanceAuraEntity.java,
  entity/ZGravitySlamEntity.java, entity/DelayedLaunchEntity.java
- client/render/ZChargeRenderer.java, ZShockwaveRenderer.java, ZStanceAuraRenderer.java,
  ZGravitySlamRenderer.java, DelayedLaunchRenderer.java

## Before and after numbers

| Value | Before | After |
|---|---|---|
| Melee damage | 1000 (1 + 999), speed -3.0 | unchanged |
| Melee on-hit | nothing at all (empty hurtEnemy) | SlamDebris burst radius 1.2 + 8 SPARK at the target, particles only, still 0 Ki, 0 stamina, 0 extra damage |
| Left charge drain | Ki 75%/min + stamina 50%/min (doc falsely claimed about 5% Ki/sec; real 1.25%/sec) | rates unchanged, docs corrected to the real per-second numbers |
| Left charge max behaviour | ratio clamps at 300 ticks, drains forever | hard stop: the wave auto-fires at 300 ticks; the rest of that hold and its release packet cannot fire a second wave (MAX_CHARGE_FIRED guard, cleared on release and disconnect) |
| Wave damage base | 300 + 900 x ratio | 150 + 1050 x ratio |
| Wave damage, tap (ratio 0.1) | 330 + 0.35 x Strength | 255 + 0.35 x Strength |
| Wave damage, full charge | 1200 + 3.5 x Strength | 1200 + 3.5 x Strength (unchanged at the cap; full charge is now worth about 4.7x a tap on base, was 3.6x) |
| Wave speed | 1.4 + 1.4 x ratio (1.54 to 2.8 blocks/tick) | unchanged |
| Wave cooldown | 20 + 40 x ratio (24 to 60 ticks) | 40 + 80 x ratio (44 to 120 ticks; a max wave outlives its 80 tick flight) |
| Wave hitbox | square AABB, half width 3 + 5c (max 8), half height 1.6, against a visual 11 wide and 3.3 tall | crescent test in wave space using the renderer's exact span 4.8 + 6.2c (max 11), chord 1.6 + 1.4c, height 1.5 + 1.8c (max 3.3), band tolerance 1.4 blocks; height tapers to the tips like the mesh |
| Wave travel particles | one type (SPARK or EMBER), every 2 ticks, max 24 per emission in a flat box | three layers every tick: 4 KI_MOTE core at the apex, about 13 SPARK/EMBER sampled along the crescent curve at full charge, 3 WIND wisps in the wake every 2 ticks |
| Root crush (Tweak C) | teleport 2 down unchecked (over air, water, ledges too), crush 250, root 80 ticks | teleport and crush only when both destination cells are solid; root 80 ticks, debris, shake and sound apply either way; crush damage 250 unchanged |
| Stance drain | stamina 75%/min | unchanged |
| Stance hold length | unbounded (getUseDuration 72000), ratio clamps at 200 ticks, drains forever | hard stop at 200 ticks: stopUsingItem fires the full power slam |
| Stance world effect | none: the "gravity suction field" pulled nothing for up to 10 seconds | VortexPhysics.applyVortex every tick: radius 3 + 5 x power (3 to 8), strength 0.06 + 0.10 x power, swirl 0.6, no lift, envelope 0.4 + 0.6 x power, weight scaled, players at 0.6 |
| Stance particles | 3 VOID_SMOKE every 4 ticks | layered every 2 ticks: 2 KI_MOTE core, 2 to 4 violet dust on the contracting rim, 3 VOID_SMOKE outer |
| Slam radius | 6 + 12 x power (radius; docs said "6 to 18 wide") | radius unchanged; docs now say 6 to 18 block radius, 12 to 36 across, in both the item and entity headers |
| Slam damage total | 400 + 800 x power + 3.0 x Strength x power | unchanged totals (440 + 0.15 Str at min hold, 1200 + 3.0 Str at full) |
| Slam damage timing | dealt by the item in the release tick, before the visual starts | dealt by ZGravitySlamEntity at the visual peak tick, 30 percent of life (tick 24 to 36) |
| Slam damage falloff | none: edge took the same as centre | full inside 45 percent of radius, easing to 30 percent at the rim |
| Slam effect durations | EARTH_SHATTER 140, CINEMATIC_TRACKING 50, MOVEMENT_CURSE 40 flat | EARTH_SHATTER 60, CINEMATIC_TRACKING = launch delay, MOVEMENT_CURSE = launch delay |
| Launch delay | 40 ticks flat at every power | 40 + 40 x power (40 to 80 ticks) |
| Launch velocity | horizontal 1.2 + 1.8p, up 1.0 + 0.8p, applied raw to everything | same numbers through VortexPhysics.fling, so weight classes apply: boss class now gets 0.12x like everywhere else in the mod |
| Launch modifiers | BOUNCINESS 0.90 and AIR_DRAG -0.50 applied with no removal anywhere (permanent) | applyTimedModifier, 200 ticks, then they expire |
| Slam visual life | 45 + 15 x power (45 to 60 ticks) | 80 + 40 x power (80 to 120 ticks) |
| Slam cooldown | 60 + 80 x power (64 to 140 ticks) | 100 + 100 x power (105 to 200 ticks, always at least the visual life) |
| Slam debris | one SlamDebris burst on tick 1 | same burst, plus DebrisSpawner field at tick 2: 26 + 36 x power pieces (26 to 62), 3 sizes, palette sampled from the real ground, helical orbit, rain back down |
| Slam ground scar | none (renderer decals only) | GroundScar magma along the trench lines, per column surface search, protected blocks refused, restores at life + 60 ticks (140 to 180), crash safe via SavedData |
| Launch debris and sound | none, silent | DebrisSpawner 16 + 16 x power pieces, gold ground burst, impact shake, GENERIC_EXPLODE at the launch tick |
| Telegraph ring | hardcoded 5.0 shrinking to 1.2, delay hardcoded 40 in the renderer, nothing synced | ring starts at the true slam radius (6 to 18) and contracts to 35 percent of it; a faint full footprint ring and anchor glow mark the real area the whole time; radius, power and delay are synced data |
| DelayedLaunch persistence | no save or load: chunk unload silently cancelled the launch; no owner | full save/load: centre, power, delay, elapsed, radius, owner UUID, target UUIDs; targets and owner rebuilt by UUID on the first tick after a reload |
| Wave and slam persistence | plain fields lost on reload (wave came back with default 500 damage) | ZShockwaveEntity saves charge, sub wave flag, damage, pierce, root, sub wave spawn flag; ZGravitySlamEntity saves radius, power, lifetime, damage, pin ticks and a struck flag so a reload can neither skip nor double the strike |
| Ki costs | charge drain per tick only while charging; stance stamina per tick only while holding | unchanged semantics; nothing is charged for a blocked or cancelled move (the sub-10-tick stance release still costs only the stamina already drained, no cooldown, same as before) |

## What was added, per point

1. LAYERED DEPTH. Every ability now emits or draws in the standard three bands.
   Charge: KI_MOTE core, gold dust mid band (LayeredParticles.dust), SPARK rim,
   on top of the existing layered mesh funnel; it previously emitted zero
   particles. Stance: KI_MOTE core sinking into the singularity, violet dust on
   the contracting rim, VOID_SMOKE outer. Wave: core motes at the apex, theme
   particles sampled along the crescent curve itself, WIND wake. Slam:
   LayeredParticles.emitVortex across the whole life plus the existing dome,
   rings, trenches and monoliths, and a LayeredRender.drawAnchorGlow pass; the
   telegraph also gained drawAnchorGlow over the full footprint. All five
   renderers now scale segment counts with camera distance through
   LayeredRender.distScale and FxEnvelope.segments (no Z renderer scaled with
   distance before). The wave crescent grows from 80 percent across its fade-in
   instead of appearing at full 22 block span on tick one.
2. DEBRIS. Slam throws a real DebrisSpawner field (26 to 62 pieces, three
   sizes, ground sampled palette, helical orbits, rain down) and the launch
   throws 16 to 32 more; combined a full power slam cycle throws up to 94
   individual tumbling pieces, above the 24 finisher floor. The charge prisms
   now tumble on their own axes at individual speeds instead of sitting axis
   aligned on one ring. Melee and the root crush keep their SlamDebris bursts.
3. WORLD INTERACTION. The stance is a real gravity well now (VortexPhysics
   pull every tick, the audit's single largest gap in the weapon). The slam
   pins with a light vortex for the launch window, scars real ground along its
   trenches through GroundScar (restored, crash safe, protected blocks
   refused), and bursts dust at the base on the cast tick
   (LayeredParticles.emitGroundBurst) and again at the launch. The wave's
   block impact, when it dies on a wall below max charge, bursts sparks and
   dust at the hit point with an anvil thud instead of discarding silently.
4. ATMOSPHERE. The Z Sword has no weather or darkness theme, so per the
   standard it gains no weather. Its atmosphere is light and sound: anchor
   glows ground the slam and the telegraph, and the payoff sounds moved to
   the payoff ticks (ANVIL_LAND and heavy impact at the strike tick, explode
   and impact at the launch) instead of everything being front loaded on
   release. No BoltEntity was added: lightning is not in this weapon's
   identity, and adding strikes would change the theme.
5. LIFECYCLE. Charge and stance constructs fade out over 8 synced ticks
   instead of popping at full alpha (the stance also gained its missing fade
   in). The slam runs the shared envelope end to end: FxEnvelope drives its
   particles and pin strength, the renderer decay window now sinks the
   monoliths to 40 percent height and shortens trenches to 55 percent while
   they fade, and EMBER rain falls through the dissipation window. The
   previously dead drawVerticalGeyser is wired in at trench endpoints on the
   strike envelope, so the class header's geyser promise is finally true.
   Damage sits inside the visual window everywhere: the wave damages as the
   visible crescent passes, the slam damages at its peak tick, the launch
   fires exactly when its telegraph finishes. Telegraph size equals hit size
   (slam radius, both rings).

## Bugs fixed (audit numbering, fresh-audit-blades.md Z Sword)

1. Unused PlayerStats import: removed.
2. Unused java.util.Random import in ZShockwaveRenderer: removed.
3. DelayedLaunchEntity no save/load, unused CompoundTag import: full
   save/load added (see numbers table); the class was rewritten, import gone.
4. Dead locals below2/targetPos and the unchecked teleport: locals gone;
   teleport guarded by a solid check on both destination cells.
5. Dead drawShockRib: removed.
6. Dead drawFrontCompressionRing: removed.
7. Dead drawVerticalGeyser with a false header promise: now called; header
   is accurate.
8. Drain documentation mismatch (5 percent per second claim): header now
   states the real rates, 75 percent per minute equals 1.25 percent per
   second Ki, 0.83 percent per second stamina, stance 1.25 percent per second.
9. Slam size documentation mismatch ("6 to 18 wide"): both headers now say
   radius, 12 to 36 across.
10. Wave visual and hitbox size mismatch: hit test uses the renderer's span,
    chord and height numbers exactly.
11. Square AABB hit shape: replaced by the crescent test in wave space;
    targets beside or behind the wave inside the old box are no longer hit.
12. Telegraph size mismatch: covered above, ring now spans the real radius.
13. Duplicated magic delay (40) in the renderer: delay is synced data now.
14. No fade out on charge and stance: 8 tick synced fade on both.
15. Renderer comment said fade in over 3 ticks, code was 6: comment fixed.
16. hurtEnemy stub: now gives the connect real feedback (debris burst and
    sparks), still free and damage neutral by design.
17. No hard stop at max charge or stance: auto-fire at 300 ticks with a
    same-hold guard; stance stops itself at 200 ticks into the slam.
18. Launch modifiers never removed: timed at 200 ticks.
19. DelayedLaunchEntity never set an owner: owner set from the item and
    persisted by UUID.
20. Deterministic Random reuse in the renderers: partially addressed. The
    per frame restart is what keeps prism, trench and monolith traits stable
    across frames (their identity), so the restart itself stays; the actual
    gap, no tumble, is fixed for the charge prisms, which now rotate on their
    own axes at individual speeds. Slam traits remain fixed per slam by
    design, like a fingerprint of that slam.
21. Silent launch moment: launch now has explode sound, impact feedback,
    debris and a ground burst; the slam strike tick gained its own sounds.

Also fixed from the numbers section of the audit: slam damage timing (peak
tick, not release tick), slam distance falloff (added), EARTH_SHATTER
outliving the launch (140 to 60), launch delay flat at every power (now
scales), wave cooldown short next to its charge cost (44 to 120), wave
particles outrun by the wave (now emitted every tick along the curve), and
the high tap floors on both abilities (wave tap 330 to 255; slam minimum
hold damage unchanged at 440 base but now lands at the peak tick on rooted
targets only inside a real footprint).

## Self audit of the diff

- Re-read all 11 files after editing. The charge auto-fire path was caught
  in self audit calling the public release (which would have swallowed it
  via its own tail guard) and restructured into a private fireShockwave that
  both entry points share; the guard set is cleared on release, on a fresh
  sub-max charge tick, and on disconnect.
- Root crush order changed deliberately: the root effect now applies even
  when the bury is refused, so a ledge target is still rooted by a max
  charge wave; only the teleport and the 250 crush are gated on solid ground.
- ZGravitySlamEntity strike excludes the owner and uses playerAttack when
  the owner is a player, mobAttack for a living owner, generic otherwise, so
  attribution survives the move from item to entity.
- GroundScar scarring searches each column's own surface (dy +3 to -6), so
  trenches follow terrain instead of writing at one fixed height.
- No em dashes in any added line of code, comments or this report (checked
  by grep over my files only).

## Verification (javac audit, the repo's standing method; no Gradle claim)

- Fresh baselines measured this session at HEAD 5e4b1de with the dual sweep
  harness (-Xmaxerrs 10000, error kinds compared with line numbers stripped):
  132 unique error kinds on the 26.2 classpath, 141 on 26.3.
- Full tree sweeps after all Phase 2 agents' edits (tree total, not just my
  weapon): 135 kinds on 26.2, 146 on 26.3. New kinds versus baseline: mine
  was exactly one, ZShockwaveEntity "variable density is already defined",
  from the two density declarations in tick; fixed by reusing the in-scope
  variable. The other new kinds are in other agents' files and are listed
  under shared change requests for the coordinating pass.
- After the fix, a targeted javac of my 11 files with the full sourcepath on
  both classpaths reports zero errors in total on both 26.2 and 26.3,
  including every shared block call (FxEnvelope, LayeredParticles,
  DebrisSpawner, GroundScar, VortexPhysics, LayeredRender), the timed
  modifier API, the ValueOutput save/load idiom, and the render state
  position fields used for distance scaling.

## Shared change requests

- None required by the Z Sword work. Everything I needed already existed in
  the Phase 1 shared blocks.
- For the coordinating pass, new error kinds seen in the full tree sweeps
  that are NOT in my files: KatanaChargeRenderer "variable dist is already
  defined" (both versions), CurseBladeItem "cannot find symbol" (both),
  KingsSlamEntity "cannot find symbol" (26.3), OxShockwaveEntity "cannot
  find symbol" (26.3). Those belong to other weapon agents or were still in
  flight when I swept; flagging them here rather than touching their files.
- Observation, not a request: LayeredParticles.emitVortex evaluates
  FxEnvelope with the standard build and decay fractions, which does not fit
  open ended held effects (a charge or stance has no fixed life). I hand
  rolled layered emission for the charge and stance using
  LayeredParticles.dust for the mid band. If other held effects hit the same
  wall, a shared emitVortex variant taking explicit fractions would help.

## Honest worse check

Nothing was reverted; no part of the rebuild reads worse against the
standard. Four deliberate power or behaviour changes to flag plainly:

- Slam damage now lands 24 to 36 ticks after release instead of instantly.
  Targets are rooted for the full launch delay (40 to 80 ticks), which
  covers the strike tick, so they cannot simply walk out; but a root removed
  early by another system can now dodge a slam that used to be instant.
  That is the standard's telegraph-first rule working as intended.
- The crescent hit test no longer catches targets beside or behind the wave
  that the old square box clipped. Fewer cheap hits in clutter; the visual
  and the damage now agree.
- The launch is weight scaled through VortexPhysics.fling: boss class
  targets (knockback resistance at least 0.9 or max health at least 400)
  now get 0.12x launch instead of the full throw. The slam strike damage
  itself is not weight scaled. This matches the mod wide weight classes,
  but it is a real change on bosses.
- Holding a full charge as a threat display is gone: at 300 ticks the wave
  fires. That is the hard stop the audit asked for (bug 17), traded
  knowingly.
- Small known wrinkle: if a DelayedLaunchEntity is reloaded mid countdown,
  the server resumes its saved elapsed count, but the client renderer ages
  from the entity's fresh client tick count, so the telegraph contraction
  can restart visually while the launch still fires on time. Cosmetic only,
  and the launch itself can no longer be cancelled by the unload, which was
  the bug that mattered.
