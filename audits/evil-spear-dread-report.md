# Evil Spear Dread Effect Report

Date: Nov 3, 2026. Tree: bespoke merge commit dc2cd1f plus concurrent
work from the damage-tags and Lava Sword agents (untouched). No commits
made by this pass. Verification is the javac audit only; no Gradle claim.

## What Carter asked for

A horror effect built into the Evil Spear: when it takes hold, the
victim's world goes dark, they can barely see anything, and detailed
red eyes blink at them from the darkness, all around them, staggered,
at different distances and heights, turning to face the victim.

## Host choice and why

The dread rides the spear's signature mark path, but as a NEW effect,
`evil_dread`, applied at exactly the spear's own mark sites with the
mark's own durations. It is deliberately NOT folded into
MarkedByEvilEffect itself, for three code reasons:

1. The mark is shared. DevilTridentItem (line 107) and
   TridentProngEntity (line 174) also apply MARKED_BY_EVIL at 60
   ticks. Folding the horror into the mark would give the trident a
   dread effect Carter never asked for.
2. The mark is load-bearing. EvilSpearProjectileEntity reads it for
   the 1.3x marked-target damage multiplier, and
   mixin/DelayedDamageMixin (line 79) reads it for the spear's
   deferred hits. A separate effect leaves every one of those reads
   untouched.
3. A separate effect gets its own client rendering hooks (its own HUD
   vignette branch and its own renderer) without touching the mark's
   particle orbit.

No existing mod effect honestly fit as the host: DarkFadedEffect is a
generic darkness debuff with no spear connection, and Storm of
Darkness is weather flavored. A new effect in effect/, registered in
DbaEffects in the house style, is the honest shape.

The dread effect deals no damage and modifies no numbers. The spear's
melee, charge, pierce multiplier, Hell Hunt chain, and cooldowns are
byte-identical to before; the only changes to spear files are the
three added effect applications.

## Trigger and duration

Applied alongside the mark, same flags (amplifier 0, ambient false,
show particles true), same source entity:

- Melee hit (EvilSpearItem.hurtEnemy): 120 ticks, with the mark.
- Lunge landing (EvilSpearItem.tickServer): 120 ticks, with the mark.
- Projectile pierce (EvilSpearProjectileEntity): 160 ticks, with the
  mark.

Hell Hunt steps do not re-apply the mark in the current code, so they
do not re-apply the dread either; a hunted victim keeps whatever dread
remains from the hit that marked them. Refreshing works like the
mark: re-application extends both together.

## The darkness

Three layers, all the mod's own machinery, no vanilla Blindness or
Darkness effect anywhere:

1. Victim's screen (client, private): a new branch in
   client/mixin/MeltingOverlayMixin, the mod's existing per-effect
   screen treatment. Strength is tracked statically and advanced by
   whole game ticks: in at +0.05 per tick (full in about a second),
   out at -0.07 per tick when the effect ends or is cured. The draw
   is a near-black murk base (0x030005 at up to 0.84 alpha, gently
   pulsing) under seven stepped edge bands whose alpha falls
   quadratically to zero inward, so the rim of the screen is fully
   black and sight closes to a dim tunnel. One honest trade: the
   center is capped at the murk alpha rather than full black, because
   the eyes are emissive geometry drawn in the world and the victim
   has to be able to see them; at the edges and mid-field the world
   is effectively gone. Gated on the effects config density like the
   renderers, so density zero turns the dread visuals off entirely.
2. The shroud (world, everyone): the gaze renderer wraps the victim
   in two counter-turning BespokeGeometry.brokenVortexBand layers of
   near-black crimson smoke, so the darkness is visible on the victim
   from outside too.
3. Ambient particles: the effect's constructor passes
   DbaParticles.VOID_SMOKE as its particle type (the Dark Faded
   pattern), so void smoke wisps drift around the afflicted entity
   through the normal effect particle sync.

There is no fog hook anywhere in the mod's client code, and none was
invented for this; the vignette plus shroud carries the darkness.

## The eyes

A new invisible anchor entity, DreadGazeEntity (entity type
`dread_gaze`), carries the ring. EvilDreadEffect's server tick
(every 4 ticks) keeps exactly one anchor per victim, using the
spear's own impale dedup pattern (a getEntitiesOfClass scan matched
on the synced victim id). The anchor follows the victim's feet,
never collides, deals no damage, and discards itself after ten ticks
without a live, still-afflicted victim in its level, which covers
effect expiry, milk cures, death, and dimension changes. If an anchor
is ever lost while the effect runs, the effect tick respawns it.

client/render/DreadGazeRenderer draws, per anchor:

- Up to 7 eye PAIRS (scaled by effect density, minimum 3, none at
  density zero). Each pair's parameters come from one
  BespokeGeometry.Rng stream seeded by the victim's UUID, consumed in
  a fixed order per pair, so every client draws the same ring and the
  ring survives anchor respawns: angle (even spread plus jitter),
  radius 2.3 to 4.6 blocks, height 0.35 to 2.6 blocks, pair width
  0.55 to 0.9, appear delay 6 + 5 per index + up to 8 (staggered
  fade-in over 12 ticks each), blink period 53 to 120 ticks with a
  seeded phase, slow angular drift, and a gentle bob.
- Each eye is a billboard quad pair (halo pass at 1.7x scale, alpha
  about 0.16, deep red; core pass at full texture alpha) on
  RenderTypes.entityTranslucentEmissive with the new generated
  texture textures/entity/dread_eye.png, drawn with both windings.
  The texture (tools/gen_dread_textures.py, fixed seed, the
  gen_bespoke_textures.py pattern) is a real eye: an almond of dark
  bloodshot sclera with branching veins, a striated crimson iris with
  a collarette ring and radial fibers, a vertical slit pupil, two
  specular glints, and a baked red glow in the alpha channel.
- Billboard math uses the house camera-conjugate idiom (TimeShatter
  and SkyCracks pattern), so every pair turns to face whoever looks
  at it: from the victim's view, every pair stares straight at them.
- Blink: openness runs 1 to 0.06 to 1 over a 6-tick cycle (close 2,
  shut 1, open 3, smoothstepped) as a vertical squash of the quad,
  eyelid style. Periods and phases are per pair, so the ring never
  blinks in unison; some eyes are always open.
- Envelope: fade in over the anchor's first 16 ticks, out over the
  effect's last 25 ticks (read from the victim's client-side effect
  instance), plus a slow 0.8 to 1.0 glow pulse per pair.

Who sees what: everyone nearby sees the eyes and the shroud around
any afflicted entity, mob or player, because the gaze is a world
entity rendered through the normal entity pass; that is what makes
the effect read for the attacker and bystanders around a mob victim.
The victim additionally gets the private screen darkness. For a
player victim in first person the eyes ring them at 2.3 blocks or
more, outside their own body, and nothing about the effect touches
input or attack paths, so their own attacks are unobstructed. The
eyes are visual only: no collision, no line-of-sight blocking.

## Sound

Nothing synthesized. The bank already had the two right voices:

- Onset (gaze spawn): DbaSounds.VOID_AMBIENT at 0.7 volume, 0.65
  pitch, plus a ten-particle VOID_SMOKE closing puff.
- Heartbeat: DbaSounds.HEARTBEAT_DARK at 0.65 volume, 0.8 pitch,
  once every 28 ticks of remaining duration (multiples of 28 are
  multiples of 4, so beats land exactly on the effect's tick
  cadence). Both volumes sit inside the bank's existing ranges.

## Files changed and registrations

New files:

- src/main/java/com/dragonblockarcanedba/effect/EvilDreadEffect.java
- src/main/java/com/dragonblockarcanedba/entity/DreadGazeEntity.java
- src/client/java/com/dragonblockarcanedba/client/render/DreadGazeRenderer.java
- tools/gen_dread_textures.py (generator, fixed seed 0xD3AD)
- src/main/resources/assets/dragonblockarcanedba/textures/entity/dread_eye.png (128px)
- src/main/resources/assets/dragonblockarcanedba/textures/mob_effect/evil_dread.png (64px effect icon)

Edited files (pure insertions, 88 added lines, 0 deletions):

- effect/DbaEffects.java: EVIL_DREAD_KEY, EVIL_DREAD instance,
  EVIL_DREAD_HOLDER, one registerForHolder line, all beside the
  MARKED_BY_EVIL entries in the same style.
- entity/DbaEntities.java: DREAD_GAZE entity type (0.5 x 0.5,
  tracking range 32, update interval 1, fully qualified ResourceKey
  build, same as HELL_HUNT_IMPACT).
- item/EvilSpearItem.java: two EVIL_DREAD applications (melee 120,
  lunge 120) beside the mark applications.
- entity/EvilSpearProjectileEntity.java: one EVIL_DREAD application
  (pierce 160) beside the mark application.
- client/DragonBlockArcaneDBAClient.java: one EntityRenderers.register
  line for DREAD_GAZE beside HELL_HUNT_IMPACT.
- client/mixin/MeltingOverlayMixin.java: the dread vignette branch
  plus its static strength tracker, appended after the four existing
  branches; no existing branch changed.
- lang/en_us.json: "Evil Dread" effect name and "Dread Gaze" entity
  name.

Split source sets stay clean: the effect and the anchor entity are
common code with no client references; all rendering is client side
(the renderer behind the client entrypoint registration, the darkness
in the client mixin). No Lava file, no damage tag data, and no other
agent's audit file was touched.

## Verification

- Targeted javac of all nine new and edited Java files with
  -sourcepath src/main/java:src/client/java on BOTH audit classpaths
  (cp262.txt and cp263.txt, JDK 25): zero error lines naming any of
  the nine files. One real bug was caught and fixed by this sweep
  (a heartbeat clock referenced a variable that only exists in the
  gate method; it now reads the live instance duration).
- The audit classpaths carry no mixin library jar, so every mixin
  file in the tree errors on the missing org.spongepowered package
  in these sweeps (baseline behavior, present before this pass). To
  verify this mixin's own code, four minimal stub annotations were
  compiled to ~/workspace/dba-audit-libs/mixinstub-classes and added
  to the classpath for the verification run only; with them, the
  mixin type-checks clean on both classpaths. The stubs are audit
  scaffolding, not mod code, and are not referenced by the build.
- The only files with errors in the targeted runs are pre-existing
  baseline files (DbaItems, DbaMenus, the client entrypoint's known
  audit-jar access kinds) and the Lava agent's in-progress files.
  No new error kind was introduced by this pass.
- Grep proof: no ParticleTypes and no SoundEvents references in any
  new or edited file of this pass; particles are DbaParticles only,
  sounds are DbaSounds only, geometry is BespokeGeometry plus
  textured quads through KiRenderHelper.vertex (the house pattern).
- Textures verified by pixel sampling after generation: black slit
  pupil at center (0,0,0), deep red iris (96,16,4), dark bloodshot
  sclera (49,7,11), fully transparent surround. en_us.json parses as
  valid JSON.

## Honest worse check

- The spear's damage, cooldowns, hunt behavior, and mark mechanics
  are untouched; the diff on both spear files is three added
  addEffect lines and two comments. Nothing about the 22 bespoke
  weapons changed.
- Center-screen darkness is a murk, not full black, by the trade
  documented above; Carter's "can barely see anything" is carried by
  the fully black edges plus the murk plus the world-side shroud. If
  he wants the center darker on his PC test, it is one constant in
  the mixin branch (the 0.82 base alpha).
- The vignette cannot run during pause the way a per-frame lerp
  would smear; strength advances by game ticks against the level
  clock, so pausing freezes the close-in rather than snapping it.
- One anchor entity per afflicted victim is the only new server
  cost, plus a small box scan every 4 ticks per victim, the same
  pattern the spear's impale already uses.
- The eyes render for spectators as well as the victim. That is the
  documented default the task set (everyone nearby sees them around a
  mob); a victim-only variant would need a render-time viewer check
  the entity pass does not offer cleanly, and it would make the
  effect invisible on mob victims to the player who caused it.
- Not verified, by construction: the in-game look (blink cadence,
  eye scale against real mob sizes, vignette weight at real
  resolutions). The first real look is Carter's PC build, same as
  every pass in this mod.
