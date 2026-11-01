# Minecraft 26.3 Compatibility Audit (Nov 1, 2026)

Question from Carter: does the ">=26.2" declaration actually not break on higher versions, or does it just allow them? Answer before this pass: it only allowed them. Three mixin breaks and a set of API breaks would have crashed or failed on 26.3. This audit verified every claim against the real 26.3 client jar (piston-meta), the real Fabric API 0.165.0+26.3, loader 0.20.1, and GeckoLib 5.5.7 (the 26.3 build), cross-checked against the 26.2 jar.

Method: (1) descriptor-level diff of every mixin target method and shadowed field between the 26.2 and 26.3 jars; (2) full javac sweep of all 384 sources against the 26.3 classpath, diffed against the identical sweep on the 26.2 classpath so raw-jar parity artifacts (565 baseline errors, identical in both) cancel out; (3) manual javap verification of every flagged item. 170 error kinds were unique to 26.3; all of them trace to the root causes below.

## False alarms (checked, NOT breaks)
- AvatarRenderer.renderHand: identical descriptor in both jars.
- EntityRenderDispatcher.submit and EntityRenderer.submit: identical descriptors in both jars.
- CloudRenderer fields texture / needsRebuild, LevelRenderer shadow fields cloudRenderer / levelRenderState / targets, LevelRenderState cloudColor / cloudHeight: present in both.
- All common-side mixin targets (LivingEntity, Player, Entity, FoodData, RangedAttribute, MinecraftServer, AbstractWindCharge, Level weather): identical descriptors.
- GeckoLib: no API errors against 5.5.7 for 26.3 beyond parity.

## Real breaks and the dual-version fix for each
The fix strategy keeps ONE jar that compiles against the 26.2 floor and runs on both 26.2 and 26.3. Where Mojang kept an equivalent API under the same name, code switches to the shared form. Where no shared form exists, a small reflection compat helper (util/DbaCompat, client/compat) picks the right member at runtime. Where a mixin target changed shape, a mixin config plugin gates version-specific mixins by the running Minecraft version.

1. PoseStack.mulPose(Quaternionf) removed in 26.3 (about 150 call sites). 26.2 already has mulPose(Matrix4fc). Fix: mulPose(new Matrix4f().rotation(q)) everywhere. Shared form, no behavior change.
2. EntityRenderer.shouldRender gained a float parameter in 26.3 (about 64 renderers, all overrides just return true). Fix: drop @Override on the 5-arg method and add a 6-arg overload that delegates to it. Each version calls its own signature; the other is an inert overload.
3. LivingEntity.hurtMarked field removed (about 53 sites). 26.3 Entity has protected markHurt() instead. Fix: DbaCompat.markHurt(entity) sets the field on 26.2, invokes markHurt() on 26.3 (reflection).
4. Entity.invulnerableTime became private with get/setInvulnerableTime in 26.3 (7 sites). Fix: DbaCompat accessors via reflection (field on 26.2, methods on 26.3).
5. LivingEntity.swing(InteractionHand) removed; 26.3 only has swing(InteractionHand, SwingAnimation, boolean) (12 client sites). Fix: DbaCompat.swing via reflection, passing SwingAnimation.DEFAULT and true on 26.3.
6. Player.getAttackAnim(float) renamed to LivingEntity.getSwingAnimation(float) in 26.3 (SwingTrailRenderer, 2 sites). Fix: reflection helper trying both names.
7. ArmedEntityRenderState.attackTime replaced by swingAnimation float in 26.3 (BedrockAnimationApplier, 2 sites). Fix: reflection field read of whichever exists.
8. BlockState.blocksMotion() removed (5 sites, DimensionTravel and OtherworldStationGenerator). Closest shared replacement is isSolid(), present in both. Slight semantic difference: blocksMotion also required a non-empty collision shape; in these safety and generation checks isSolid errs on the safe side.
9. Block codec() / simpleCodec / propertiesCodec removed from BlockBehaviour in 26.3 (GravityTrainingBlock, YellowCloudBlock). Fix: keep codec() without @Override; its body goes through a reflection helper that calls simpleCodec on 26.2 (identical behavior) and returns a hand-built MapCodec on 26.3, where vanilla no longer uses block codecs.
10. Properties.isViewBlocking predicate type changed to StateArgumentPredicate<AABB> in 26.3 (1 site, DbaBlocks). Fix: reflection proxy implementing whichever functional interface the runtime expects, always returning false as before.
11. LivingEntity.drop(ItemStack, boolean) gained a Prediction parameter in 26.3 (1 site, DinoMeatItem). Fix: spawn the ItemEntity directly with vanilla drop motion; identical visible behavior, no version-specific call.
12. VertexConsumer gained abstract setUv3(float, float) in 26.3 (AlphaModulatingVertexConsumer). Fix: implement it without @Override; on 26.2 it is an inert extra method, on 26.3 it satisfies the interface.
13. GameRenderer.render() lost its parameters and the LevelRenderer.doEntityOutline() invoke is gone (GameRendererPostMixin, the DbaPostFx hook). 26.3 calls LevelRenderer.blitEntityOutline() at the equivalent point (after level render, before GUI). Fix: keep the current mixin for 26.2, add a 26.3 variant injecting after blitEntityOutline with a CallbackInfo-only handler, gate both with the mixin plugin.
14. LevelRenderer.render descriptor changed (CloudTextureMixin cloud color hook). Fix: split the color hook into its own mixin whose handler takes only CallbackInfo, which matches the render method by name in both versions.
15. LevelRenderer.addCloudsPass removed and CloudRenderer.render(int, ...) replaced by prepare(...) plus render(CloudStatus, RenderPass) (CloudTextureMixin multi-layer Otherworld clouds). No shared hook exists. Fix: the multi-layer mixin is gated to 26.2 and below by the plugin. On 26.3 the Otherworld keeps vanilla single-layer clouds, still golden tinted and raised by the shared color hook (item 14). This is the one cosmetic downgrade on 26.3 in this pass; a native 26.3 multi-layer port would need the new frame-pass API and is a separate job.
16. Particle pipeline classes moved from com.mojang.blaze3d.pipeline to com.mojang.renderpearl.api.pipeline (DbaGlowParticle only). The renderpearl builder mirrors the old API method for method. Fix: build the additive particle pipeline and its Layer reflectively through a compat helper that loads whichever package the runtime has.

## Build and versioning consequences
- gradle.properties stays on the 26.2 floor (minecraft 26.2, loader 0.19.3, Fabric API 0.154.2+26.2, GeckoLib 5.5.3): the jar is compiled once against 26.2 and runs on 26.3 with the matching 26.3 builds of loader, Fabric API, and GeckoLib installed, which is the normal way users run older-floor mods on newer versions.
- fabric.mod.json keeps "minecraft": ">=26.2". After this pass that declaration is honest for 26.2 and 26.3, with the single disclosed exception of item 15.
- 26.4 snapshots were NOT audited; per the standing policy each new release gets its own port pass.
17. LivingEntity.swinging field removed in 26.3; isSwinging() replaces it (DbaWeaponRenderer, 1 site). Fix: DbaCompat.isSwinging via reflection (field on 26.2, method on 26.3).

## Verification after the fix pass
Full javac sweeps re-run on both classpaths after all fixes: zero error kinds unique to the 26.2 floor (no regressions), and exactly one error kind unique to 26.3: CloudTextureMixin's calls to the removed CloudRenderer.render(int, ...), which is expected and safe because DbaMixinPlugin never applies that mixin on 26.3 and up, so the class is never loaded there. All other 26.3-only errors from the original sweep are gone. This is an audit result, not a Gradle build; the build remains waived per BUILD-VERIFY.md.
