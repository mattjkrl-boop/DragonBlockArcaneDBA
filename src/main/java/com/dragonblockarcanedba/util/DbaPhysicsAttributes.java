package com.dragonblockarcanedba.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility for Minecraft 26.2 Physics & Environmental Attributes:
 * - minecraft:air_drag_modifier (Mid-air resistance)
 * - minecraft:bounciness (Elastic collision bounce)
 * - minecraft:friction_modifier (Ground sliding friction)
 * - minecraft:name_plate_distance (Stealth nameplate rendering distance)
 * - minecraft:mini_name_plate_distance (Stealth mini nameplate distance)
 *
 * All transient modifiers applied via this utility have strict automatic timed lifespans
 * to guarantee they are cleanly stripped and never accumulate or cause infinite bouncing.
 */
public class DbaPhysicsAttributes {
    public static final Identifier AIR_DRAG_ID = Identifier.withDefaultNamespace("air_drag_modifier");
    public static final Identifier BOUNCINESS_ID = Identifier.withDefaultNamespace("bounciness");
    public static final Identifier FRICTION_ID = Identifier.withDefaultNamespace("friction_modifier");
    public static final Identifier NAME_PLATE_DIST_ID = Identifier.withDefaultNamespace("name_plate_distance");
    public static final Identifier MINI_NAME_PLATE_DIST_ID = Identifier.withDefaultNamespace("mini_name_plate_distance");

    public record TimedKey(UUID entityUuid, Identifier attributeId, Identifier modifierId) {}

    public static class TimedModifierEntry {
        public final WeakReference<LivingEntity> entityRef;
        public final Identifier attributeId;
        public final Identifier modifierId;
        public int remainingTicks;

        public TimedModifierEntry(LivingEntity entity, Identifier attributeId, Identifier modifierId, int ticks) {
            this.entityRef = new WeakReference<>(entity);
            this.attributeId = attributeId;
            this.modifierId = modifierId;
            this.remainingTicks = ticks;
        }
    }

    private static final Map<TimedKey, TimedModifierEntry> ACTIVE_TIMED_MODIFIERS = new ConcurrentHashMap<>();

    public static Optional<Holder.Reference<Attribute>> getAttributeHolder(Identifier id) {
        return BuiltInRegistries.ATTRIBUTE.get(ResourceKey.create(Registries.ATTRIBUTE, id));
    }

    /**
     * Applies a transient physics attribute modifier with a strict tick lifespan.
     * Automatically caps bounciness to safe non-exponential values (max 0.30) to prevent infinite launch loops.
     */
    public static void applyTimedModifier(LivingEntity entity, Identifier attributeId, Identifier modifierId, double amount, AttributeModifier.Operation operation, int durationTicks) {
        if (entity == null || !entity.isAlive() || durationTicks <= 0) {
            return;
        }

        // Safety clamp on bounciness: values >= 1.0 or high stacked values create infinite bouncing trampoline effects
        double finalAmount = amount;
        if (BOUNCINESS_ID.equals(attributeId)) {
            finalAmount = Math.max(0.0, Math.min(0.30, amount));
        } else if (AIR_DRAG_ID.equals(attributeId)) {
            finalAmount = Math.max(-0.85, Math.min(0.85, amount));
        }

        final double appliedAmount = finalAmount;

        getAttributeHolder(attributeId).ifPresent(holder -> {
            AttributeInstance instance = entity.getAttribute(holder);
            if (instance != null) {
                instance.removeModifier(modifierId);
                instance.addTransientModifier(new AttributeModifier(modifierId, appliedAmount, operation));

                TimedKey key = new TimedKey(entity.getUUID(), attributeId, modifierId);
                ACTIVE_TIMED_MODIFIERS.put(key, new TimedModifierEntry(entity, attributeId, modifierId, durationTicks));
            }
        });
    }

    /**
     * Fallback applying a default 20-tick (1.0s) timed modifier so no modifier can ever persist indefinitely.
     */
    public static void applyModifier(LivingEntity entity, Identifier attributeId, Identifier modifierId, double amount, AttributeModifier.Operation operation) {
        applyTimedModifier(entity, attributeId, modifierId, amount, operation, 20);
    }

    public static void removeModifier(LivingEntity entity, Identifier attributeId, Identifier modifierId) {
        if (entity != null) {
            getAttributeHolder(attributeId).ifPresent(holder -> {
                AttributeInstance instance = entity.getAttribute(holder);
                if (instance != null) {
                    instance.removeModifier(modifierId);
                }
            });
            ACTIVE_TIMED_MODIFIERS.remove(new TimedKey(entity.getUUID(), attributeId, modifierId));
        }
    }

    /**
     * Purges all DBA-applied physics and stealth modifiers from an entity.
     * Useful on login, respawn, or dimension changes to ensure no orphan modifiers remain.
     */
    public static void purgeAllDbaModifiers(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        Identifier[] physicsAttrs = {BOUNCINESS_ID, AIR_DRAG_ID, FRICTION_ID, NAME_PLATE_DIST_ID, MINI_NAME_PLATE_DIST_ID};
        for (Identifier attrId : physicsAttrs) {
            getAttributeHolder(attrId).ifPresent(holder -> {
                AttributeInstance instance = entity.getAttribute(holder);
                if (instance != null) {
                    for (AttributeModifier mod : instance.getModifiers()) {
                        if (mod.id().getNamespace().equals("dragonblockarcanedba")
                                || mod.id().getPath().contains("bounce")
                                || mod.id().getPath().contains("spin")
                                || mod.id().getPath().contains("shockwave")
                                || mod.id().getPath().contains("drag")
                                || mod.id().getPath().contains("friction")) {
                            instance.removeModifier(mod.id());
                        }
                    }
                }
            });
        }

        UUID uuid = entity.getUUID();
        ACTIVE_TIMED_MODIFIERS.keySet().removeIf(k -> k.entityUuid().equals(uuid));
    }

    public static void onPlayerDisconnect(UUID playerUuid) {
        ACTIVE_TIMED_MODIFIERS.keySet().removeIf(k -> k.entityUuid().equals(playerUuid));
    }

    /**
     * Ticks active timed modifiers, decrementing durations and removing expired modifiers.
     */
    public static void tick() {
        if (ACTIVE_TIMED_MODIFIERS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<TimedKey, TimedModifierEntry>> iter = ACTIVE_TIMED_MODIFIERS.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<TimedKey, TimedModifierEntry> entry = iter.next();
            TimedModifierEntry val = entry.getValue();
            val.remainingTicks--;

            LivingEntity entity = val.entityRef.get();
            if (val.remainingTicks <= 0 || entity == null || !entity.isAlive()) {
                if (entity != null && entity.isAlive()) {
                    getAttributeHolder(val.attributeId).ifPresent(holder -> {
                        AttributeInstance instance = entity.getAttribute(holder);
                        if (instance != null) {
                            instance.removeModifier(val.modifierId);
                        }
                    });
                }
                iter.remove();
            }
        }
    }
}

