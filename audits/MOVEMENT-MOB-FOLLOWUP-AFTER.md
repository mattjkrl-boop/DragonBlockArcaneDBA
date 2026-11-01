# Movement and Mob Follow-up After Audit (Nov 1, 2026)

Built at Carter's direction ("The movement and mob stuff do ya"), closing the deferred slices recorded in PHASE5-AFTER.md and PHASE6-AFTER.md.

## Movement

- MovementPhysics now exposes the shared execution API the core slice promised: setVelocity, addVelocity, dampenHorizontal, rootInPlace, applyFlightVelocity with the pure flightVelocity curve, rotation-preserving teleport, and fallDamageScale. The generic verbs (dash, Ki step, fast fall) are unchanged.
- Every player rush now executes through that API, distances, damage, Ki costs, and cooldowns untouched:
  - Teleports: Brave Sword finisher and charged dash, Evil Spear Hell Hunt chain, Hollows Edge blink and Void Rift entry, Katana Flashdraw chain, freeform dash, and Heaven Splitter, Saber Blitz arrivals, arc steps, freeze hold, and Flash Step, Dimensional Sword warp (guarded to ServerPlayer at its Player-typed use site).
  - Velocities: Azure Dragon Rush (flight curve), Evil Spear no-target lunge, Saber Blitz release push, Z Sword recoil, Grand Sword spin drift.
  - Stance braking: Dabura (0.4), Evil Spear (0.5), and Hollows Edge (0.3) charge dampening via dampenHorizontal; Ox Kings Axe brace and Z Sword Ancient Weight root via rootInPlace; Katana focus stance via setVelocity.
  - A repo-wide grep confirms zero player setDeltaMovement or teleportTo calls remain in the item and ki packages. KiTechniqueHandler has no player movement verbs to migrate (it fires projectiles; their velocities stay on their entities).
- Flight unified: Azure Dragon Rush and Flying Nimbus both fly on MovementPhysics.flightVelocity (half target from rest, then blend; Nimbus blend 0.45, Azure 0.40 as before). Banking is one formula in the shared player render state (turn rate plus lateral drift, capped at 40 degrees), applied in the single fly branch of BedrockAnimationApplier with a one-third head counter-roll, so every race and form banks identically. DbaPlayerState gained dba$getFlightBank; AvatarRenderStateMixin is its only implementer.
- Fall damage tuned in the existing LivingEntityMixin fall branch: MovementPhysics.fallDamageScale returns 0.35 while fast falling (a deliberate slam trades damage for its landing feedback) and 0.65 after a Ki step, 1.0 otherwise. Azure rush immunity still returns 0 first.

## Mobs

- fx/SlamDebris generalizes slam debris: on a slam entity's first server tick it throws block debris sampled from the actual ground blocks under the impact area (surface scan per sample point), plus a dust cloud and a per-family accent (Ember for Ox, Spark for Z Gravity, both for King's Slam), all density-scaled through DbaEffectsConfig. Wired into OxFissureEntity (radius 2.5), ZGravitySlamEntity (its radius), and KingsSlamEntity (its radius). No blocks are destroyed; OxShockwave keeps its own weak-block destruction.
- Boss stagger: MobPhysics.maybeStagger fires on BOSS-class non-player targets taking a DBA hit of 18 or more: brief Slowness II (10 ticks) server side, and a BossStaggerPayload (strength 0.35 plus damage over 90, capped 1.2; 16 ticks) to tracking players, with a 30 tick per-boss cooldown cleaned up on death. Client StaggerFx stores strength and progress; DbaStaggerState carries them through LivingEntityRenderState; LivingEntityRendererMixin rocks the boss (Z roll oscillation up to 13 degrees times strength under a sine envelope, plus a forward dip) in setupRotations while alive.

## Audit

- Full-classpath javac on all touched common and client files: clean. The only errors in the sweep are the documented pre-existing raw-jar artifacts in untouched DbaItems.java (CreativeModeTab Output) and DbaMenus.java (MenuType), which Loom handles in a real build.
- One real error was found and fixed during the audit: DimensionalSwordItem's warp site is Player-typed, so the teleport is now guarded by an instanceof ServerPlayer check (the branch is already server-only).
- Mixin files show only the known annotation-parity errors (Mixin, Inject, At, CallbackInfo are absent from the raw classpath); the new stagger handler at LivingEntityRendererMixin line 180 errors on exactly the same symbols as the pre-existing tumble handler at line 171, and its body compiles clean. BedrockAnimationApplier, DbaPlayerState, StaggerFx, and the client init registration compile with zero errors.
- Split source sets stay clean: SlamDebris, the payload, and both physics classes are common; StaggerFx and DbaStaggerState are client only.
- This is an audit result, not a Gradle build; the build remains waived per BUILD-VERIFY.md.
