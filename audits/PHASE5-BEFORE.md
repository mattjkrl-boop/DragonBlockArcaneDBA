# Phase 5 Before Audit (Nov 1, 2026): movement physics

From FEATURE-WAVE-SCOPE.md and the code: movement exists only inside abilities. PlayerEntityMixin (1,051 lines) carries flight and ability movement case by case; dashes, rushes, afterimages, and launches are per weapon and per technique. There is no shared movement layer: no common dash, no air dash, no double jump or Ki step, no fast fall, no landing impact system, no shared cooldown or air-use framework, and no server-side validation shared across abilities.

Plan for the core slice (ability migration follows one ability at a time later, per the scope doc): a common movement layer that any player can use and any ability can build on.

- network/C2SMovementPayload: one serverbound payload with an action field (DASH, KI_STEP, FAST_FALL), following the C2SKiTechniqueFirePayload codec pattern.
- movement/MovementPhysics (common, server authoritative): per-player state (air dash used, Ki step used, fast-fall flag, last vertical speed), reset on ground contact. Dash is momentum-preserving: it sets horizontal velocity along the look direction while keeping a fraction of current vertical velocity, on the ground or once in the air. Ki step is one mid-air refresh jump. Fast fall slams downward. Landing impact is detected in the server tick from the tracked fall speed: past a threshold it fires dust particles, the vanilla big-fall sound, and a ScreenShakePayload scaled by impact, strongest after a fast fall.
- Client input: a Dash key (G) plus edge detection on the existing jump key (mid-air press is a Ki step) and sneak key (mid-air press is a fast fall), sent as payloads. Verbs disable while creative-flying or spectating.

Validation is server side: dead, spectating, or creative-flying players are rejected, air uses are consumed until ground contact, and applied speeds are fixed constants (the cap is the constant itself). Flight unification and per-ability migration are later slices, recorded here so the boundary is explicit.
