# Phase 2 After Audit (Nov 1, 2026): ki surge and danger shaders

Added two fragment shaders in the existing DbaPostFx pipeline, same stepped-chain pattern as eclipse and time_stop, same fail-safe and effects-density gate:

- dba_ki_surge.fsh (chains ki_surge_1..4, Amount 0.3 to 1.0): while the local player is using an item (charging a weapon or technique), the screen edges gain a warm energy burn, saturation and contrast lift, and a radial chromatic fringe that grows with the level. DbaPostFx counts consecutive use ticks; the surge steps in after 8 ticks and drains on release.
- dba_danger.fsh (chains danger_1..3, Amount 0.45 to 1.0): at 35, 20, and 10 percent health the edges bleed to deep red, the center desaturates, and the rim darkens as the level rises. Level eases one step per tick in both directions so it never pops.

The shader count goes from 3 to 5, all driven by real client state with no new networking. Known remaining gap, carried to Phase 3 by design: the Phase 1 weapon glowmask textures still have no emissive render pass consuming them; pulse and glow work belongs with the effects system.
