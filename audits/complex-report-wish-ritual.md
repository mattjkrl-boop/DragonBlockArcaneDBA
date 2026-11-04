# Complex Effects Redo, Phase 2 Report: Wish Ritual

Owner slot: Wish Ritual. File edited, and the only file edited:
`src/client/java/com/dragonblockarcanedba/client/wish/WishRitualVisuals.java`.
No commits. Themes, colors, and every damage number, cooldown, cost,
timing, mechanic, and synced data field are identical to HEAD 9178eac.
The ritual phases and peak timing still come from the same server driven
phase values the visuals already read (FORMATION_TICKS, SUMMON_TICKS,
DRAGON_AT, REVEAL_TICKS, and the column, gate, and burst envelopes). The
dragon entities were not touched. The d74f13a destruction wiring was not
touched. The file stays in the client source set and references only
client side classes.

## WishRitualVisuals (class M)

Pieces layer (added, both builders through ComplexGeometry.submitEnergy
with a DbaShaders canvas texture, peak alpha 0.95 on both, envelopes
carrying the fades):

- tendrilFan, five strands, width 0.34, spread 26 degrees, length
  RING_LIFT + 2.5 (8.5 blocks), rooted at the column base (center minus
  0.5 on Y) and aimed straight up, so the strand tips reach the dragon
  ball ring radius at ring height: golden strands rising around the
  dragon balls. Root color is the palette ring color and tip color is
  the palette soul color, submitted with ComplexTextures.TENDRIL.
  Envelope: the existing column envelope, so the strands fade in,
  hold, and fade out exactly with the summoning column filler.
  Seed: the ritual seed plus a fixed salt, derived from the same synced
  payload state as the existing emission seed, so every client draws
  the same strands.
- flameTongues, six tongues (inside the cap of nine tongues of six
  segments), radius 1.15, height 2.3, hot color 0xFFF5D1 (the pale gold
  the burst front already uses) to the palette ring color at the tips,
  submitted with ComplexTextures.FLAME_TONGUE. Base is the existing
  burst core center (center plus RING_LIFT times 0.45 on Y), sway phase
  is the existing column phase. Envelope: the existing burst envelope,
  so the tongues burn only at the ritual peak, the 18 tick arrival
  window at DRAGON_AT, at small size as the audit specifies.

Vertex accounting at distScale 1.0: one tendril at 24 spine samples
emits 384 vertices, five strands are 1920. One flame tongue emits 48
vertices, six tongues are 288. Combined peak total is 2208, inside the
class M per submit budget of 2500, including the peak frames where the
fading tendrils and the peak flames overlap.

Complex particle layer (added, WISP_CURL throughout, per the audit):

- Continuous emitter on the ball cluster: MaterialEmitters.Preset.fixed
  with DbaParticles.WISP_CURL, source is the existing ball source,
  22 per second with a per second budget of 30, ticked on the existing
  ball envelope.
- Continuous emitter on the column soul stream: Preset.fixed with
  DbaParticles.WISP_CURL, source is the existing column source kind 1
  (the spiraling soul path), 30 per second with a per second budget of
  40, ticked on the existing column envelope. Both continuous rates sit
  under the cap of 40 per second per emitter.
- One shot burst at the ritual peak: MaterialEmitters.burst with
  Preset.fixed WISP_CURL from the existing burst core source, count 24,
  inside the class M burst budget of 30. It fires once per ritual,
  guarded by a per ritual flag, on the first tick where the burst
  envelope is positive.

Filler layer (kept, unchanged): all fourteen pre existing material
emitters (ball plasma and soul, formation mist and plasma, column
plasma, soul, and vapor, ground mist and plasma, gate mist and plasma,
burst front plasma and mist, burst core soul) keep their presets,
colors, rates, budgets, sources, and envelope wiring exactly as at
HEAD. PLASMA emission is now filler around the pieces, not the body of
the effect, and no effect body is a round glow sprite layer alone.

Palette note: Earth reads gold as specified (ring color 0xFFCC40 to
soul color 0xFFE9A8, flame tips in the palette ring color). Namek keeps
its HEAD palette (ring color 0x66FF9E to soul color 0xB8FFD0), because
the colors identical rule governs; the golden wording in the audit is
the Earth theme, and recoloring Namek gold would change a theme color.

Forbidden primitives check: zero calls to drawColoredBox, drawCylinder,
drawDisc, drawSphere, or drawHelix in the file. No private geometry,
particle system, or texture was built; both new layers use only the
shared foundation (ComplexGeometry, ComplexTextures, MaterialEmitters,
DbaParticles).

## Verification

Targeted javac with JDK at ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling only the owned
file, output in private /tmp dirs tagged wish-ritual. Both full logs
were read line by line.

- 26.2 classpath (cp262.txt): 4 errors, all baseline kinds in other
  closure files (DbaItems CreativeModeTab Output access, DbaMenus
  MenuType access). Zero errors in WishRitualVisuals.java.
- 26.3 classpath (cp263.txt): 10 errors, all baseline kinds in other
  closure files (the same DbaItems and DbaMenus access errors, plus
  DbaHostileEntity, PatrolRobotEntity, FriezaSoldierEntity, and
  NamekianWarriorEntity symbol errors from the 26.3 API changes).
  Zero errors in WishRitualVisuals.java.

git diff --check on the owned file is clean, and an em dash scan of the
owned file and this report is zero.

## Foundation change requests

None required. One observation, not a blocker: ComplexGeometry.tendril
and tendrilFan take no caller phase, so the strands are seeded static
shapes whose only motion is their envelope, while the flame tongues do
sway through their swayPhase parameter. If the merge wants the tendril
layer across all owners to move, an optional phase parameter on the
tendril builders would be a shared toolkit extension. This slot ships
without it and does not build a private workaround.

## Honest worse check

Could this read worse than the particles only version? The layers are
purely additive: no filler emitter was removed, retuned, or recolored,
and both piece envelopes are the exact envelopes the filler already
uses, so at worst a piece layer could read as decoration on top of the
previous effect, never as a replacement for it. The real risks, stated
plainly:

- The five tendrils share one root point at the column base, so they
  read as a rising fountain of strands around the ring rather than one
  strand per dragon ball. The spread and length were chosen so the
  tips land at the ball ring radius and height, and the ball cluster
  itself carries the new WISP_CURL layer, so the balls stay marked.
  This is a composition compromise forced by the vertex budget: seven
  per ball tendrils at full spine samples would be 2688 vertices and
  break the class M budget on their own.
- The tendrils do not sway (see the foundation observation above).
  They grow and dissolve with the column envelope, but a viewer
  watching closely will see the strands hold still while the column
  particles swirl. This reads as standing ritual filaments, which fits
  a summoning circle, but it is less alive than the filler motion.
- The flame tongues are small, mid air at the arrival core, and live
  only 18 ticks. That matches the audit wording (small, at the ritual
  peak), and they sit inside the burst core soul volume that already
  glows at that point, so they read as flames crowning the arrival
  flash rather than floating in empty space. If anything in this slot
  risks reading weak in game, it is this one, because of its short
  window; it cannot read worse than its absence, since the burst
  filler is unchanged underneath it.

Against the pre redo version: that version drew bespoke ball shells,
ring bands, a standing ring, motes, and a dome burst. This pass does
not restore those shapes, and the standard forbids the plain ring and
dome reads they carried. What it restores is structured geometry the
particles only version lacked, scoped to exactly what the audit
assigns this slot. On balance this ships: it is strictly more layered
than the current state, inside every budget, and no part of it replaces
or dims what was already there.
