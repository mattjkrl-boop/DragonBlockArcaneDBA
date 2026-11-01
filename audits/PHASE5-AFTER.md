# Phase 5 After Audit (Nov 1, 2026): shared movement layer core

Built the shared layer the scope doc calls for, as the core slice:

- network/C2SMovementPayload (serverbound, action field) registered in DbaNetwork next to the other C2S types.
- movement/MovementPhysics (common, server authoritative): per-player state with air-dash and Ki-step uses consumed until ground contact, fast-fall flag, and tracked fall speed. Dash (speed 1.35 along the look direction, vertical momentum partly preserved), Ki step (0.85 lift plus a small horizontal carry), fast fall (-2.2 slam). Landing past 0.95 vertical speed fires POOF dust, PLAYER_BIG_FALL, and a ScreenShakePayload scaled by impact (fast-fall landings hit 1.35x harder). Validation: dead, spectator, and creative-flying players are rejected; the applied speeds are fixed constants, which are the caps. State is dropped on disconnect.
- Client MovementInput: Dash key (G), mid-air jump edge press sends Ki step, mid-air sneak edge press sends fast fall; intent only, registered in the client init alongside the other tick handlers.

Audit: the three new files compile clean against the real 26.2, loader, API, and GeckoLib jars with zero errors (PLAYER_BIG_FALL, hurtMarked velocity sync, look-angle math, and the disconnect event all verified by the compiler, not assumed). This is an audit result, not a Gradle build; the build remains waived per BUILD-VERIFY.md.

Deliberately left for later slices, per the scope doc's one-at-a-time rule: migrating weapon rushes and Ki techniques onto this layer, unifying the flight acceleration and banking model across races and forms, and fall-damage interplay tuning for Ki step and fast fall landings.

Follow-up completed the same day at Carter's direction: see audits/MOVEMENT-MOB-FOLLOWUP-AFTER.md. All three deferred slices are done.
