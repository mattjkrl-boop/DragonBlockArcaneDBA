# No-Shapes Report: Forms and Player Auras

Owner surface from audits/no-shapes-audit.md, Forms and Player Auras section.
Measured and converted at dba ad22b32 (Phase 1 foundation). One file edited:
src/client/java/com/dragonblockarcanedba/client/render/AuraRenderer.java.
Nothing committed.

## Scope disposition

- AuraRenderer.java is the only file the audit assigns to this owner, and it
  is converted below.
- The transformation burst presentation is DbaPostFx's form grade burst
  (12 ticks on a family change, steady grade while transformed). Per the
  brief, the Phase 7 form signature grades and post shader chains are screen
  effects and stay as they are: DbaPostFx.java is untouched, and its burst
  timing is byte-identical.
- DbaNetwork is untouched; its Scope 2 sound conversions stand. No form
  mechanics, stats, mastery, Ki drain, unlock, or progression code was
  touched anywhere: they do not live in the assigned file and no shared or
  common file was edited.

## Before and after counts (AuraRenderer.java)

| Measure | Before | After |
|---|---:|---:|
| BespokeGeometry builder calls | 0 | 0 |
| LayeredRender geometry calls | 0 | 0 |
| Direct addVertex / KiRenderHelper.vertex calls | 0 | 0 |
| KI_MOTE emission call sites | 1 | 0 |
| Direct level().addParticle call sites | 1 | 0 |
| MaterialEmitters.burst call sites | 0 | 3 |
| Material layers emitted | 1 (KI_MOTE for every form) | 3 (PLASMA, SOUL, VAPOR) |

The audit counted this surface as a material conversion with zero geometry,
and that held in the code: the whole change is the particle material, the
colors, and the emission path.

## What the aura emits now

The pulse logic is unchanged: same UUID phase, same sine pulse at 0.32 per
tick, same thresholds (idle adds motes above pulse 0.62 and 0.9, charging
adds above 0.5), same per-tick totals (idle 1, 3, or 4; charging 3 or 5),
same spread (1.2 idle, 0.8 charging), same rise (0.04 + pulse x 0.05 idle,
0.10 + pulse x 0.08 charging), same once-per-client-tick cadence from the
existing client tick caller, which is untouched. The total is split across
three layers by a rotating pattern (plasma, plasma, soul, plasma, vapor,
rotated by tick so even a one-mote tick cycles materials over time):

- PLASMA: sampled through the full body column (feet to +2.0, the old
  spawn box), base velocity exactly the old rise vector, preset spread
  0.006, scale 1.0, seed from the player UUID. The plasma profile carries
  the rising turbulence and density fade.
- SOUL: sampled through the lower body (feet to +1.3), base velocity
  rise x 1.3, preset spread 0.008, scale 0.9, offset seed. The soul profile
  is buoyant, so these motes keep climbing and linger as the aura's tail.
- VAPOR: sampled at the rim (0.8 to 1.15 of the old half-spread, feet to
  +0.75) with a small outward sample velocity of 0.035, so the rim sheds
  along the ground instead of stopping at the body; base velocity
  rise x 0.4, preset spread 0.02, scale 1.5 in the paler soul color.

No shell, band, or silhouette is drawn or implied; the sources are data
only, per the standard.

## Materials per form family

Family mapping matches DbaPostFx.formFamily exactly (same contains order),
so the aura family and the screen grade family can never disagree. The
plasma color is the form's own declared color where the form data carries
one (hair or skin override in data/dragonblockarcanedba/forms), otherwise
the family's grade tint; the soul color is a paler variant of the same hue.
The rim VAPOR uses the soul color.

| Family | Forms | PLASMA | SOUL | Color source |
|---|---|---|---|---|
| ssj | super_saiyan_1, super_saiyan_2, super_saiyan_3 | 0xFFD700 | 0xFFEC9E | form hair override FFD700 |
| blue | super_saiyan_blue | 0x1E90FF | 0x9FD8FF | form hair override 1E90FF |
| god | super_saiyan_god | 0xDC143C | 0xFF8A7A | form hair override DC143C |
| ui | ultra_instinct | 0xC8C8DC | 0xEDF1FF | form hair override C8C8DC |
| golden | golden_form | 0xFFD700 | 0xFFF0B3 | form skin override FFD700 |
| kaioken | kaioken | 0xFF3030 | 0xFF9A80 | form skin override FF3030 |
| namekian | giant_namekian, orange_namekian | 0xFF8C00 | 0xFFBE78 | orange form skin override FF8C00 |
| arcosian | arcosian_form_2, arcosian_form_3, arcosian_final_form | 0xB373FF | 0xD8B6FF | grade tint (0.7, 0.45, 1.0); the arcosian forms declare no override |
| default | any form outside the eight families | 0xFFE9A8 | 0xFFF6D8 | the old KI_MOTE gold-white identity, kept as the fallback |

## One deliberate nuance: density scaling

The old renderer applied the effects density itself, as
max(1, round(count x density)). The converted renderer passes the raw pulse
count to MaterialEmitters.burst, which applies the same config density once
per layer. At the default density of 1.0 the per-tick totals are identical.
At fractional densities each layer rounds separately, and the old private
one-mote-per-tick floor is gone, so a very low density setting now thins
the aura toward nothing instead of always showing one mote per tick. That
is the shared material-system rule replacing a private one, recorded here
rather than hidden. The density 0 early return still applies.

## Verification

Targeted javac with sourcepath closure (src/main/java, src/client/java),
JDK 25, audit only, no Gradle claim:

- cp262: zero errors naming AuraRenderer.java; AuraRenderer.class and
  AuraRenderer$AuraColors.class emitted. The 4 closure errors are the
  documented baseline kinds in DbaItems.java (CreativeModeTab Output
  protected access, 2) and DbaMenus.java (MenuType private access, 2).
- cp263: zero errors naming AuraRenderer.java; both classes emitted. The
  10 closure errors are the same DbaItems and DbaMenus kinds plus the
  documented 26.3 hostile mob kinds in DbaHostileEntity.java,
  FriezaSoldierEntity.java, NamekianWarriorEntity.java, and
  PatrolRobotEntity.java, all in files this owner did not touch.
- Em dash scan of the edited file and this report: 0.
- The in-game look is unverified from this environment, as always; Carter's
  PC test is the first real look.

## Honest worse check

Nothing was reverted. The trades, stated plainly:

- The material profiles have their own lifetimes (plasma 10 to 22 ticks,
  soul 34 to 70, vapor 24 to 54), longer than the old mote's tail, so at
  identical emission counts the aura will read denser and longer-tailed
  than before, especially the rising soul layer. That is the standard's
  material behavior, but it is the change most likely to read as too much
  in game; if it does, the fix is emission-side (lower soul share in
  LAYER_PATTERN), not a geometry return.
- The vapor rim is at most one sprite per tick at the old counts, so the
  rim reads as sparse drifting vapor, not a wrap. That is intended (a wrap
  would be a shell), but it is the thinnest layer.
- Per-family aura colors are new information: before this conversion every
  form shared the same gold-white mote. The brief asked for signature
  colors per family and the colors come from the forms' own data, but it
  is a visible identity change. If Carter prefers the uniform gold-white,
  colorsFor returning DEFAULT for every family is a one-method revert.
- The density floor nuance above is a small behavior change at very low
  density settings only.

## Change requests

None. The Phase 1 foundation covered this surface completely. One
observation for the coordinating pass, not a request: the form data's
auraVisualId is read by no code today (Form.getAuraVisualId has no
callers), which is why the aura colors had to be sourced from the form
overrides and grade tints. If a future pass wants one canonical aura color
field in the form data, that is a data decision for Carter, not something
this conversion needed.
