# Foundation After Audit: per-holder weapon animation (Nov 1, 2026)

Change: DbaWeaponTickets (new), DbaGeoWeapon (predicate rewrite, perspective aware), WeaponAnimatableIds (new, wired in onInitialize), DbaWeaponRenderer (new client renderer), DbaGeoWeaponRenderers (uses the new renderer, MIGRATED set with katana), GECKOLIB-API-NOTES.md (jar-verified API research).

Before state (from PHASE1-BEFORE addendum): animation state came from Hooks.STATE, a per-item global fed by a client function reading only the local player; animation cache keyed per item with hashCode fallback stack ids; no synced animatable registration; one shared animation file and timeline for all contexts.

After state:
- The predicate reads only DbaWeaponTickets stamped by DbaWeaponRenderer from RenderData.itemOwner (ItemOwner.asLivingEntity, verified in the Minecraft 26.2 jar) for the specific stack being rendered. Swing requires the stack's item in either of the holder's hands; charge ratio comes from the holder's own use ticks and the stack's use duration. Neutral tickets for GUI, ground, and frame renders.
- isPerspectiveAware returns true on DbaGeoWeapon, so GeckoLib builds a ContextAwareAnimatableManager per stack id: first person and third person timelines are separate.
- WeaponAnimatableIds registers all 19 weapon items as synced animatables and assigns durable GeckoLib stack ids on the server (inventory scan once a second), so ids match on every client.
- Per-context presentation hooks live in DbaWeaponRenderer: first person scale 0.9 and a small downward offset, third person scale 1.1, everything else 1.0. First-pass values in one place, to be tuned from in-game screenshots.
- Hooks.STATE and the local-player state() function are deleted; grep confirms no remaining references.

Verification (audit, not a compile):
- javac pass against the real jars (Minecraft 26.2 client jar from the Mega Mod audit libs, geckolib-5.5.3.jar from the Modrinth maven in build.gradle, Fabric loader 0.19.3 and API 0.154.2+26.2 from maven.fabricmc.net, plus the MC dependency jars): zero errors in every new or changed file, common and client.
- The only errors in the pass are 5 pre-existing ones in untouched files (DbaItems creative tab output, DbaMenus MenuType, and a cascade at DragonBlockArcaneDBA line 311), all raw-client-jar access artifacts that Fabric access wideners under Loom handle in a real build. They exist independent of this change and are recorded here, not fixed here.
- Split source sets checked: DbaWeaponTickets and WeaponAnimatableIds are common code using only GeckoLib, Fabric, and Minecraft common types; all Minecraft client types live in the client renderer and renderers classes.
- API use is jar-verified per GECKOLIB-API-NOTES.md. Residual flags: ItemStack.get(DataComponentType) resolves through DataComponentGetter; LivingEntity.getUseItemRemainingTicks and ItemStack.getUseDuration(LivingEntity) verified by javap in the 26.2 jar and by the clean javac pass.
