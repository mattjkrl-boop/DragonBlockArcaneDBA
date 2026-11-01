# GeckoLib 5.5.3 API Notes (from the real jar)

Researched Nov 1, 2026 for the DBA Phase 1 weapon work. Source: geckolib-5.5.3.jar downloaded from the Modrinth maven named in build.gradle (https://api.modrinth.com/maven/maven/modrinth/geckolib/5.5.3/geckolib-5.5.3.jar, HTTP 200, 694,957 bytes, 423 classes). Every signature below was read with javap (JDK 25.0.5) from that jar, and behavior claims were read from javap -c bytecode. Nothing here comes from web docs or memory. Items that could not be settled from the jar are marked NOT VERIFIED at the end.

## 1. GeoItem and the AnimatableInstanceCache: per-stack ids exist

```java
// com.geckolib.animatable.GeoItem (interface, extends SingletonGeoAnimatable)
public static void registerSyncedAnimatable(com.geckolib.animatable.SingletonGeoAnimatable);
public static long getId(net.minecraft.world.item.ItemStack);
public static long getOrAssignId(net.minecraft.world.item.ItemStack, net.minecraft.server.level.ServerLevel);
public default boolean isPerspectiveAware();
public default com.geckolib.animatable.instance.AnimatableInstanceCache animatableCacheOverride();

// com.geckolib.animatable.GeoAnimatable (base interface)
public abstract void registerControllers(com.geckolib.animatable.manager.AnimatableManager$ControllerRegistrar);
public abstract com.geckolib.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache();
public default com.geckolib.animatable.instance.AnimatableInstanceCache animatableCacheOverride();

// com.geckolib.animatable.SingletonGeoAnimatable
public default void triggerAnim(net.minecraft.world.entity.Entity, long, java.lang.String, java.lang.String);
public default void stopTriggeredAnim(net.minecraft.world.entity.Entity, long, java.lang.String, java.lang.String);
public default void triggerArmorAnim(net.minecraft.world.entity.Entity, long, java.lang.String, java.lang.String);
public default void stopTriggeredArmorAnim(net.minecraft.world.entity.Entity, long, java.lang.String, java.lang.String);
```

How the id works, from the bytecode of GeoItem:
- `getId(ItemStack)` reads a Long from the stack's data component held in `com.geckolib.GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT` (a `Supplier<DataComponentType<Long>>`). If the component is absent, it falls back to `(long) stack.hashCode()`. So an unregistered stack gets an unstable, identity-style id, not a durable per-stack id.
- `getOrAssignId(ItemStack, ServerLevel)` is the durable path: on a stack with a PatchedDataComponentMap, if the component is absent it takes a fresh id from `com.geckolib.cache.AnimatableIdCache.getFreeId(ServerLevel)` and writes it into the stack's component map. This is server-side (it needs a ServerLevel), which is the intended flow: the server assigns the id once, the component syncs to clients as part of the stack, and every client then keys animation state to the same id.
- `registerSyncedAnimatable` (on both GeoItem and SingletonGeoAnimatable) registers the animatable with GeckoLib's synced singleton cache (`com.geckolib.cache.SyncedSingletonAnimatableCache`), which is what makes triggerAnim/stopTriggeredAnim work over GeckoLib's own networking packets (`com.geckolib.network.packet.singleton.SingletonAnimTriggerPacket` and siblings exist in the jar). DBA currently never calls registerSyncedAnimatable and never calls getOrAssignId (verified by grep over DBA src).

Cache classes, from javap:

```java
// com.geckolib.animatable.instance.AnimatableInstanceCache (abstract)
public abstract <T extends com.geckolib.animatable.GeoAnimatable> com.geckolib.animatable.manager.AnimatableManager<T> getManagerForId(long);
public <D> void addDataPoint(long, com.geckolib.constant.dataticket.DataTicket<D>, D);
public <D> D getDataPoint(long, com.geckolib.constant.dataticket.DataTicket<D>);

// com.geckolib.animatable.instance.SingletonAnimatableInstanceCache
protected final it.unimi.dsi.fastutil.longs.Long2ObjectMap<com.geckolib.animatable.manager.AnimatableManager<?>> managers;
public <T extends com.geckolib.animatable.GeoAnimatable> com.geckolib.animatable.manager.AnimatableManager<T> getManagerForId(long);

// com.geckolib.animatable.instance.InstancedAnimatableInstanceCache
public com.geckolib.animatable.manager.AnimatableManager<?> getManagerForId(long); // one shared manager for all ids

// com.geckolib.util.GeckoLibUtil
public static com.geckolib.animatable.instance.AnimatableInstanceCache createInstanceCache(com.geckolib.animatable.GeoAnimatable);
public static com.geckolib.animatable.instance.AnimatableInstanceCache createInstanceCache(com.geckolib.animatable.GeoAnimatable, boolean);
```

Key facts:
- The cache is keyed by the long instance id: SingletonAnimatableInstanceCache.getManagerForId does `managers.computeIfAbsent(id, ...)`, so different ids get different AnimatableManager instances, each with its own controllers and timeline. Per-stack animation separation is built into the cache, provided the ids are real (assigned), not hashCode fallbacks shared or shifting between stacks.
- `GeckoLibUtil.createInstanceCache(animatable)` first honors `animatableCacheOverride()`. For a GeoItem, GeoItem.animatableCacheOverride returns a `GeoItem$ContextBasedAnimatableInstanceCache` when `isPerspectiveAware()` is true, and otherwise delegates to SingletonGeoAnimatable.animatableCacheOverride, which returns a plain SingletonAnimatableInstanceCache (verified in bytecode). For non-entity, non-block-entity animatables the one-arg createInstanceCache passes the singleton flag as true.
- DBA today: DbaGeoWeapon.getAnimatableInstanceCache returns one cache per Item object from its own static map, built with GeckoLibUtil.createInstanceCache. Since DbaGeoWeapon does not override isPerspectiveAware (default false), each weapon item gets one SingletonAnimatableInstanceCache. Inside that cache, managers are per id, but DBA never assigns ids, so the id is the stack hashCode fallback, and DBA's animation predicate never looks at the id at all (see section 4). The per-item DBA cache map is therefore not the main problem; the missing id assignment and the global predicate are.

### Perspective-aware cache (the first/third person mechanism)

```java
// com.geckolib.animatable.GeoItem$ContextBasedAnimatableInstanceCache
//   extends SingletonAnimatableInstanceCache
public com.geckolib.animatable.manager.AnimatableManager<com.geckolib.animatable.GeoItem> getManagerForId(long);

// its manager: com.geckolib.animatable.manager.ContextAwareAnimatableManager<T, C>
//   extends AnimatableManager<T>
protected abstract java.util.Map<C, com.geckolib.animatable.manager.AnimatableManager<T>> buildContextOptions(com.geckolib.animatable.GeoAnimatable);
public abstract C getCurrentContext();
public com.geckolib.animatable.manager.AnimatableManager<T> getManagerForContext(C);
```

The context-based cache builds one ContextAwareAnimatableManager per id, and that manager builds an EnumMap with one plain AnimatableManager per ItemDisplayContext value (verified in the bytecode of the inner class: it iterates ItemDisplayContext.values()). Its getCurrentContext() reads the manager's own animatable data ticket DataTickets.ITEM_RENDER_PERSPECTIVE, defaulting to ItemDisplayContext.NONE. So with isPerspectiveAware() returning true, each display context gets an independent animation manager for the same stack id: the first-person copy and the third-person copy of a weapon animate independently instead of fighting over one timeline. This is the designed answer to viewing the same weapon in first person (owner) and third person at the same time, and to GUI versus in-hand differences.

## 2. What the animation predicate can see: AnimationTest, GeoRenderState, DataTickets

```java
// com.geckolib.animation.state.AnimationTest (record)
public com.geckolib.animation.state.AnimationTest(T, com.geckolib.renderer.base.GeoRenderState, com.geckolib.animatable.manager.AnimatableManager<T>, com.geckolib.animation.AnimationController<T>);
public T animatable();
public com.geckolib.renderer.base.GeoRenderState renderState();
public com.geckolib.animatable.manager.AnimatableManager<T> manager();
public com.geckolib.animation.AnimationController<T> controller();
public boolean isMoving();
public boolean hasData(com.geckolib.constant.dataticket.DataTicket<?>);
public <D> D getData(com.geckolib.constant.dataticket.DataTicket<D>);
public <D> D getDataOrDefault(com.geckolib.constant.dataticket.DataTicket<D>, D);
public com.geckolib.animation.object.PlayState setAndContinue(com.geckolib.animation.RawAnimation);

// com.geckolib.renderer.base.GeoRenderState (interface)
public default <D> void addGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>, D);
public default boolean hasGeckolibData(com.geckolib.constant.dataticket.DataTicket<?>);
public default <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>);
public default int getPackedLight();
public default float getPartialTick();
public default double getAnimatableAge();
public abstract java.util.Map<com.geckolib.constant.dataticket.DataTicket<?>, java.lang.Object> getDataMap();
```

The predicate (the AnimationController state handler, which DBA implements as dbaAnimState taking an AnimationTest) therefore has typed access to whatever tickets the renderer put into the GeoRenderState for this render, plus the manager's own animatable data. A predicate is not limited to global state; it is supposed to read tickets.

Tickets that GeoItemRenderer.captureDefaultRenderState actually writes for every item render (verified in bytecode, in order):
- `GeoItemRenderer.CURRENT_ITEM`: `public static final DataTicket<net.minecraft.world.item.Item> CURRENT_ITEM` (declared on GeoItemRenderer itself).
- `DataTickets.ITEM_RENDER_PERSPECTIVE`: the ItemDisplayContext of this render.
- `DataTickets.IS_ENCHANTED` (stack.isEnchanted), `DataTickets.IS_STACKABLE` (stack.isStackable).
- `DataTickets.MAX_USE_DURATION` (stack.getUseDuration with the client player as the entity argument, note the caveat below).
- `DataTickets.MAX_DURABILITY` and `DataTickets.REMAINING_DURABILITY`.
- `DataTickets.ANIMATABLE_INSTANCE_ID` (from the GeoRenderer base capture; the manager also carries it).

Full DataTickets constant list (com.geckolib.constant.DataTickets, all `public static final DataTicket<...>` unless noted): ANIMATABLE_CLASS, ANIMATABLE_INSTANCE_ID (Long), PARTIAL_TICK (Float), RENDER_COLOR, PACKED_OVERLAY, PACKED_LIGHT, VELOCITY (Vec3), IS_MOVING (Boolean), POSITION (Vec3), BLOCKPOS, ITEM_RENDER_PERSPECTIVE (ItemDisplayContext), HAS_GLINT, ANIMATABLE_MANAGER, SWINGING_ARM (Boolean), SPRINTING, IS_DEAD_OR_DYING, IS_ENCHANTED, IS_STACKABLE, MAX_USE_DURATION (Integer), MAX_DURABILITY, REMAINING_DURABILITY, EQUIPMENT_BY_SLOT (EnumMap of EquipmentSlot to ItemStack), IS_LEFT_HANDED, IS_CROUCHING, ELYTRA_ROTATION, VANILLA_MODEL_MODIFIERS, PER_SLOT_RENDER_DATA, ANIMATION_CONTROLLER_STATES, plus OverridingDataTicket entries driven by vanilla render states: TICK, OUTLINE_COLOR, INVISIBLE_TO_PLAYER, IS_SHAKING, ENTITY_POSE, ENTITY_PITCH, ENTITY_YAW, ENTITY_BODY_YAW.

Important limits, verified from the jar:
- There is no ticket carrying the ItemStack itself on GeoRenderState, and no ticket carrying the holding entity. The stack reaches the renderer inside RenderData (next section), and the predicate can only see it indirectly through the tickets above, through custom tickets a subclass adds (addRenderData / fireCompileRenderStateEvent, section 3), or through manager data points keyed by instance id (AnimatableInstanceCache.addDataPoint(long, ticket, value)).
- SWINGING_ARM exists as a ticket, but in this jar it is only written by GeoEntityRenderer and GeoReplacedEntityRenderer (entity rendering path), not by GeoItemRenderer (verified by scanning every class file for the field reference). An item predicate cannot rely on SWINGING_ARM being set during item rendering.
- MAX_USE_DURATION is computed with ClientUtil.getClientPlayer() as the LivingEntity argument. For a remote player's stack it still uses the local player for that one value. It is a duration, not a "currently using" flag: GeckoLib writes no IS_USING or charge-progress ticket for items.
- Custom tickets are supported by design: `public static <D> DataTicket<D> create(java.lang.String, java.lang.Class<? extends D>)` on both DataTickets and DataTicket, so DBA can define its own tickets (for example holder uuid, using flag, charge ratio) and populate them from a renderer subclass or from cache data points.

The holder at render time: GeoItemRenderer receives the holder only through its render data object:

```java
// com.geckolib.renderer.GeoItemRenderer$RenderData (record)
public com.geckolib.renderer.GeoItemRenderer$RenderData(net.minecraft.world.item.ItemStack, net.minecraft.client.renderer.item.ItemStackRenderState, net.minecraft.world.item.ItemDisplayContext, net.minecraft.client.multiplayer.ClientLevel, net.minecraft.world.entity.ItemOwner);
public net.minecraft.world.item.ItemStack itemStack();
public net.minecraft.world.item.ItemDisplayContext renderPerspective();
public net.minecraft.world.entity.ItemOwner itemOwner();
```

ItemOwner is a Minecraft type (net.minecraft.world.entity.ItemOwner), not a GeckoLib type; its exact members were not inspected here because it lives in the Minecraft jar, not this jar (see NOT VERIFIED). What is verified: the renderer is handed the stack, the display context, and an ItemOwner for the render, so a GeoItemRenderer subclass can read the holder there and stamp custom tickets into the render state before the predicate runs.

## 3. GeoItemRenderer: display contexts and override hooks

```java
// com.geckolib.renderer.GeoItemRenderer<T extends Item & GeoAnimatable>
//   implements GeoRenderer<T, GeoItemRenderer$RenderData, GeoRenderState>
public com.geckolib.renderer.GeoItemRenderer<T> withScale(float);
public com.geckolib.renderer.GeoItemRenderer<T> withScale(float, float);
public com.geckolib.renderer.GeoItemRenderer<T> withRenderLayer(...);
public com.geckolib.renderer.GeoItemRenderer<T> useAlternateGuiLighting();
public long getInstanceId(T, com.geckolib.renderer.GeoItemRenderer$RenderData);
public void captureDefaultRenderState(T, com.geckolib.renderer.GeoItemRenderer$RenderData, com.geckolib.renderer.base.GeoRenderState, float);
public void preRenderPass(com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState>, net.minecraft.client.renderer.SubmitNodeCollector);
public void scaleModelForRender(com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState>, float, float);
public void adjustRenderPose(com.geckolib.renderer.base.RenderPassInfo<com.geckolib.renderer.base.GeoRenderState>);
public void submit(com.geckolib.renderer.base.GeoRenderState, com.mojang.blaze3d.vertex.PoseStack, net.minecraft.client.renderer.SubmitNodeCollector, int);
public void fireCompileRenderStateEvent(T, com.geckolib.renderer.GeoItemRenderer$RenderData, com.geckolib.renderer.base.GeoRenderState, float);
public void addRenderData(T, O, R, float); // default, from GeoRenderer

// com.geckolib.renderer.base.RenderPassInfo (what the hooks receive)
public com.mojang.blaze3d.vertex.PoseStack poseStack();
public R renderState();
public <D> D getGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>);
public <D> D getOrDefaultGeckolibData(com.geckolib.constant.dataticket.DataTicket<D>, D);
```

Verified behavior:
- getInstanceId(T, RenderData) returns `GeoItem.getId(renderData.itemStack())`. Instance separation in rendering is by stack id, full stop.
- Vanilla model display transforms are NOT applied by GeoItemRenderer. No reference to ItemTransform or any display-transform application exists in GeoItemRenderer (checked its bytecode and constant pool). The vanilla item model JSON display blocks therefore do not move a GeckoLib-rendered weapon. The pose the weapon renders in comes from the vanilla special-model rendering pipeline around the renderer (the geo base model's parent, minecraft:item/handheld, supplies the base hand pose at the model level, outside this jar's code), plus whatever the renderer itself does to the PoseStack.
- What GeoItemRenderer itself does: adjustRenderPose in the base class does exactly one thing, `poseStack.translate(0.5f, 0.51f, 0.5f)` (verified constants in bytecode). scaleModelForRender delegates to the GeoRenderer default with the renderer's scaleWidth and scaleHeight fields (set by withScale). There is no built-in per-context branch in either method.
- preRenderPass copies DataTickets.ITEM_RENDER_PERSPECTIVE from the render state into the AnimatableManager's animatable data (verified in bytecode). That copy is what feeds ContextAwareAnimatableManager.getCurrentContext() for perspective-aware items, and it also means any predicate or hook can read the current context from manager data or from the render state ticket.
- The override hooks for per-context work are therefore: `adjustRenderPose(RenderPassInfo<GeoRenderState>)` and `scaleModelForRender(RenderPassInfo<GeoRenderState>, float, float)` for transforms (read ITEM_RENDER_PERSPECTIVE from info.getGeckolibData or info.renderState() and translate/rotate/scale the PoseStack per context), `captureDefaultRenderState` and `fireCompileRenderStateEvent` and `addRenderData` for stamping custom tickets (holder, using state) before predicates run, and `adjustModelBonesForRender(RenderPassInfo, BoneSnapshots)` (GeoRenderer default method) for per-bone adjustments. All are public or default interface methods designed for subclassing; GeoItemRenderer is a concrete class DBA already instantiates directly in DbaGeoWeaponRenderers, so a DBA subclass is a drop-in change on the client side.

## 4. How per-holder swing and use state is meant to be driven

There is no example item, demo mod, or helper class for swing/use driven item animation inside this jar (the jar is the library only; com.geckolib.animatable.client contains only GeoRenderProvider). The mechanisms the jar provides, in order of how directly they fit DBA:

1. Read synced holder state in the predicate via tickets. A player's swinging and using state is already synced by vanilla (swing is a broadcast animation, use state rides the living entity data and the stack's use fields on every client). Nothing stops a predicate from checking the actual holder: the missing piece in stock GeckoLib is that the item predicate is not handed the holder. The bridge is a GeoItemRenderer subclass that, in captureDefaultRenderState or fireCompileRenderStateEvent, reads RenderData.itemOwner() and RenderData.itemStack(), resolves the holder entity on the client, and stamps custom DataTickets (holder swinging, holder using, charge progress computed from the holder's use item and use ticks) into the GeoRenderState. The predicate then reads only tickets and works identically for the local player and for remote players, because it never touches Minecraft.getInstance().player.
2. Triggered animations over GeckoLib networking: SingletonGeoAnimatable.triggerAnim(Entity, long instanceId, String controller, String animation) with registerSyncedAnimatable registered items sends SingletonAnimTriggerPacket so a server or an owning player can fire a one-shot animation on all clients for a specific stack id. This fits discrete events (a special slash, a transformation flourish). It does not fit continuous states (holding a charge), which should stay predicate driven.
3. Vanilla entity animation state needs no GeckoLib help: DBA's own SwingTrailRenderer already proves the pattern by iterating level.players() and reading each player's attack animation. The same per-player fields (swinging, use item, use ticks remaining) are available on remote player entities on the client.

What DBA does today, and why it fails remote players (from the DBA sources, read during the Phase 1 before audit): Hooks.STATE is a ToIntFunction<Item> fed by DbaGeoWeaponRenderers.state, which reads only Minecraft.getInstance().player (isUsingItem, swinging, main-hand check). The predicate receives no holder, no stack, and no context, so every rendered copy of an item shares the local player's state. None of the three mechanisms above are in use: no assigned stack ids, no registerSyncedAnimatable call, no renderer subclass, no custom tickets, isPerspectiveAware not overridden.

## Recommended design

### (1) Per-holder idle, swing, and charge state

1. Assign real ids: call GeoItem.getOrAssignId(stack, serverLevel) once for each weapon stack where stacks are created or first ticked on the server (loot, crafting, commands, login), and call GeoItem.registerSyncedAnimatable(item) for each weapon item at mod init. This makes getInstanceId in the renderer return a stable id that is the same on every client, so managers, data points, and triggered animations all key to the same stack everywhere.
2. Subclass GeoItemRenderer for DBA weapons (client only). Override fireCompileRenderStateEvent (or captureDefaultRenderState) to stamp three custom tickets created with DataTicket.create: HOLDER (the resolved holder entity or its uuid, from RenderData.itemOwner), HOLDER_SWINGING (Boolean), and USE_STATE (an int or float: 0 idle, 1 swinging handled by vanilla swing fields, charge ratio while the holder is using this stack, computed from the holder, not the local player). If itemOwner cannot be resolved to a living holder (GUI, ground, item frame), stamp neutral values.
3. Rewrite the DbaGeoWeapon predicate to read only AnimationTest data: ITEM_RENDER_PERSPECTIVE for context, the custom USE_STATE ticket for charge, and the custom swinging ticket for swing. Delete the Hooks.STATE global and the local-player read. The predicate then behaves per holder and per stack on every client with no networking of its own, because vanilla already syncs swing and use state for remote players.
4. Keep continuous states (idle, charge loop) predicate driven. Reserve triggerAnim for discrete one-shot animations, and only after registerSyncedAnimatable is in place.
5. Turn on isPerspectiveAware() (return true) on DbaGeoWeapon so each stack id gets a ContextAwareAnimatableManager with a separate manager per ItemDisplayContext. Without this, the owner's first-person render and another client's third-person render of the same stack share one manager timeline on any client that renders both perspectives of that stack (notably the owner's own client in third person versus first person switches, and spectator or replay style views).

### (2) Per-display-context transforms

1. Do not rely on vanilla display JSON for the geo model; GeoItemRenderer ignores it (verified). Keep display blocks only for the flat GUI model path DBA already uses.
2. In the same DBA GeoItemRenderer subclass, override adjustRenderPose(RenderPassInfo) and scaleModelForRender(RenderPassInfo, float, float): call the super implementation first (it applies the base 0.5, 0.51, 0.5 translate and the withScale values), then read DataTickets.ITEM_RENDER_PERSPECTIVE from the RenderPassInfo and apply the per-context correction on info.poseStack() for FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND, and THIRD_PERSON_LEFT_HAND, with GUI, GROUND, FIXED, HEAD, and ON_SHELF left at tuned defaults. Left-hand contexts need the mirrored correction, which is why the context ticket (not a boolean) is the right input.
3. Tune first person for the visibility rule: in first-person contexts, apply the offset and scale that keep the weapon in frame while using it, and pair it with the first-person effect scaling already used by some DBA renderers, so charge visuals anchored on the weapon do not fill the screen. Third person gets the fuller, larger framing since the whole weapon is on show.
4. Because the predicate also receives ITEM_RENDER_PERSPECTIVE, per-context animation variants (a first-person charge pose versus a third-person charge pose) can be selected in the predicate once the multi-bone models exist, with the perspective-aware cache keeping the timelines separate.

## NOT VERIFIED from this jar

- The exact members and semantics of net.minecraft.world.entity.ItemOwner (it is a Minecraft class; only its presence as RenderData.itemOwner() in this jar is verified). Resolving an ItemOwner to a LivingEntity holder needs a check against the Minecraft 26.2 jar or the existing DBA code that renders items.
- Whether ItemStack.hashCode in Minecraft 26.2 is identity-based (the fallback path in getId depends on it). The bytecode shows the fallback is `(long) stack.hashCode()`; what that returns per stack is a Minecraft question, not a GeckoLib one.
- The component registration details behind GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT (how and when the DataComponentType is registered on Fabric) are behind a supplier and the GeckoLib init path; the jar shows the field and its use, and DBA's existing GeckoLib items already work in game per the Phase 1 audit, but the registration call itself was not traced.
- No statement is made about Iris or shaderpack interaction; nothing in this jar speaks to it.
