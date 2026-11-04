# Complex Report: Forms and Player Auras

Owner slot from audits/complex-audit.md, Forms and Player Auras section.
One file edited:
src/client/java/com/dragonblockarcanedba/client/render/AuraRenderer.java.
Nothing committed. No other file was touched by this owner: DbaPostFx and
DbaNetwork are untouched, transformation bursts and form signature post
grades stay with their own systems, and the player and race models are
untouched.

## Surface: Form aura emission (class L)

### Layers after this pass

- Pieces layer (new), drawn from a level render submit pass inside the
  owned file:
  - ComplexGeometry.flameTongues through ComplexGeometry.submitEnergy
    with ComplexTextures.FLAME_TONGUE: nine tongues per player (the
    builder's full count budget, six segments each), base radius 0.75
    idle and 0.55 while charging, height 2.0 idle and 2.3 while charging,
    each scaled 0.9 to 1.1 by the pulse. Colors are the existing form
    colors: the paler soul color hot at the base running to the plasma
    color at the torn tips. Peak alpha 0.95.
  - ComplexGeometry.tendrilFan through ComplexGeometry.submitEnergy with
    ComplexTextures.TENDRIL, high form grades only: four strands from the
    feet aimed up at 30 degrees spread, length 2.6, width 0.16, plasma
    color at the root to the soul color at the tip. Peak alpha 0.90.
    High grade means form data stage 3 and up, read from the synced form
    id through DbaRegistries.getForm(formId).getStage(): super_saiyan_3,
    arcosian_form_3, arcosian_final_form, super_saiyan_god,
    super_saiyan_blue, golden_form, and ultra_instinct with the shipped
    form data. Forms the registry does not know get tongues only.
  - Class L rule: two builder types at high grades; at lower grades the
    single builder runs at its full count budget (nine tongues).
- Complex particle layer (new): DbaParticles.WISP_CURL shed through
  MaterialEmitters.Preset.fixed, one curl every second tick per player
  (10 per second per player, under the continuous cap of 40 per second
  per emitter), sampled through the same body column as the plasma
  filler and rising at 1.2 times the pulse rise rate.
- Material filler layer (unchanged): the existing PLASMA body column,
  SOUL lower body, and VAPOR rim bursts, with identical presets, seeds,
  sources, and counts. The filler is now the atmosphere between the
  tongues, not the aura body, so PLASMA is neither the sole nor the
  dominant layer.

### Wiring note

AuraRenderer is not an entity renderer: the main client class calls
renderAura from the client tick loop, and that caller is not this
owner's file. The pieces therefore draw from
LevelRenderEvents.COLLECT_SUBMITS, the same event SwingTrailRenderer
and WishRitualVisuals use. The hook registers itself lazily, once, from
the first transformed player's aura tick; before that tick no aura
exists to draw, so nothing is missed. The submit pass re-reads the same
synced state the emission uses (active form id, UUID, tick count,
isUsingItem) for every player in the level, anchors each player on its
interpolated position minus the camera, and applies
FxEnvelope.distScale for distance scaling.

### Preservation

- The aura pulse logic in renderAura is byte-identical in behavior:
  same UUID phase, same sine at 0.32 per tick, same thresholds, counts,
  spread (1.2 idle, 0.8 charging), and rise values. The pieces evaluate
  the same pulse formula with the partial tick added, so they breathe
  on the same wave without changing the emission's numbers.
- Colors: the pieces take colorsFor(formId.getPath()) exactly as the
  emission does. No per form color is hardcoded anywhere new; the stage
  threshold is a single grade cut, not a per form table.
- Synced state: pieces are seeded from the player UUID hash, the same
  seed the emission uses, plus fixed salt offsets per builder, so every
  client draws the same tongues and strands for a remote player.
- Alpha discipline: peak alphas are 0.95 (tongues) and 0.90 (tendrils).
  Faintness and breathing come from the pulse envelope (0.55 to 1.0)
  through the builders' life remap, never from a low constant alpha.
- Zero calls to drawColoredBox, drawCylinder, drawDisc, drawSphere, or
  drawHelix in the owned file (grep count 0 for each). No private
  geometry, particle system, or texture was built.

### Budget accounting per player at distScale 1.0

- flameTongues: 9 tongues x 6 segments x 8 vertices = 432.
- tendrilFan (high grades): 4 tendrils x 24 spine samples max x 16
  vertices per segment = 1536.
- Total worst case 1968 vertices per player per submit, against the
  class L budget of 6000, with FxEnvelope.distScale stepping counts
  down beyond 32 blocks. A crowd of transformed players multiplies per
  player, which is why the totals were kept at about a third of the
  per instance budget.

## Verification

Targeted javac, JDK ~/workspace/tools/jdk-25.0.5+8/bin/javac,
sourcepath src/main/java:src/client/java, compiling only the owned
file, private output dirs /tmp/dba-forms-auras-262 and
/tmp/dba-forms-auras-263. Full logs read at /tmp/javac-forms-auras-262.log
and /tmp/javac-forms-auras-263.log.

- cp262 (cp from ~/workspace/dba-audit-libs/cp262.txt): 4 errors, all
  baseline kinds in files this owner did not touch (DbaItems.java
  CreativeModeTab Output protected access x2, DbaMenus.java MenuType
  private access x2). Zero errors naming AuraRenderer.java;
  AuraRenderer.class and AuraRenderer$AuraColors.class emitted.
- cp263 (cp from ~/workspace/dba-audit-libs/cp263.txt): 10 errors, the
  same 4 baseline kinds plus the documented 26.3 hostile mob kinds
  (DbaHostileEntity x2, FriezaSoldierEntity x1, NamekianWarriorEntity
  x1, PatrolRobotEntity x2). Zero errors naming AuraRenderer.java;
  both classes emitted.
- git diff --check on the owned file: clean. Em dash scan of the owned
  file and this report: 0.
- The in-game look is unverified from this environment, as always;
  Carter's PC test is the first real look.

## Foundation change requests

No foundation gaps blocked this slot: ComplexGeometry, ComplexTextures,
DbaShaders, DbaParticles, MaterialEmitters, and FxEnvelope covered
everything. One wiring observation for the merge, not a request to
change shared code: AuraRenderer has no registration entry point in any
file this owner may edit, so the submit hook self-registers lazily from
the aura tick. If the merge prefers explicit registration, one line in
DragonBlockArcaneDBAClient calling a new AuraRenderer register method
would replace the lazy flag; it is a cleanup, not a correctness issue,
because the hook is registered on the client thread before the first
frame that could contain an aura.

## Honest worse check

Judged plainly, because auras are always on screen:

- Busyness at high grades is the real risk. A stage 3 or higher aura now
  stacks nine flame tongues, four tendril strands, three material layers,
  and the wisp shed around one player, all in the same two colors. The
  mitigations are structural: the tongues are narrow strips (about 0.15
  to 0.22 wide) rooted inside the existing emission spread, the tendrils
  are thin (0.16) and only four, and at the pulse trough the envelope
  drops to 0.55 so the shader dissolves the pieces back and the aura
  visibly settles instead of holding a constant wall of flame. Even so,
  in a crowd of several high grade players the combined read could be
  louder than the particles-only version. If Carter finds it too much,
  the first dial is the tendril count (4 to 2) or the tongue height,
  both one-line changes in submitAuraPieces; the composition itself
  should stay.
- Against the pre-redo and particles-only versions, the pieces should
  read better, not worse: the old aura was exactly the failure this pass
  exists to fix (round glow sprites with no silhouette), and the tongues
  give the aura the lashing vertical silhouette of the reference
  picture while the filler keeps the soft glow between them. The aura
  also reads at a distance now, where individual motes used to vanish.
- First person was a genuine worse risk and was handled in code: the
  local player's pieces are skipped while the camera is first person,
  because the tongues stand inside the camera near plane and would
  flash at the screen edges. The unchanged filler still shows in first
  person, and the same player shows full pieces in third person and to
  every remote client.
- One smaller trade: because the pieces draw in the render pass while
  the filler emits on ticks, a one frame registration lag exists on the
  very first transformed tick of a session (the hook registers during
  that tick, the first submit lands the next frame). It is invisible in
  practice and recorded here only for completeness.

Nothing in this slot reads worse in a way that needs a revert; the high
grade busyness trade above is stated so the merge and Carter can judge
it with eyes on the actual game.
