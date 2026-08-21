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

import java.util.Optional;

/**
 * Utility for Minecraft 26.2 Physics & Environmental Attributes:
 * - minecraft:air_drag_modifier (Mid-air resistance)
 * - minecraft:bounciness (Elastic collision bounce)
 * - minecraft:friction_modifier (Ground sliding friction)
 * - minecraft:name_plate_distance (Stealth nameplate rendering distance)
 * - minecraft:mini_name_plate_distance (Stealth mini nameplate distance)
 */
public class DbaPhysicsAttributes {
    public static final Identifier AIR_DRAG_ID = Identifier.withDefaultNamespace("air_drag_modifier");
    public static final Identifier BOUNCINESS_ID = Identifier.withDefaultNamespace("bounciness");
    public static final Identifier FRICTION_ID = Identifier.withDefaultNamespace("friction_modifier");
    public static final Identifier NAME_PLATE_DIST_ID = Identifier.withDefaultNamespace("name_plate_distance");
    public static final Identifier MINI_NAME_PLATE_DIST_ID = Identifier.withDefaultNamespace("mini_name_plate_distance");

    public static Optional<Holder.Reference<Attribute>> getAttributeHolder(Identifier id) {
        return BuiltInRegistries.ATTRIBUTE.get(ResourceKey.create(Registries.ATTRIBUTE, id));
    }

    public static void applyModifier(LivingEntity entity, Identifier attributeId, Identifier modifierId, double amount, AttributeModifier.Operation operation) {
        getAttributeHolder(attributeId).ifPresent(holder -> {
            AttributeInstance instance = entity.getAttribute(holder);
            if (instance != null) {
                instance.removeModifier(modifierId);
                instance.addTransientModifier(new AttributeModifier(modifierId, amount, operation));
            }
        });
    }

    public static void removeModifier(LivingEntity entity, Identifier attributeId, Identifier modifierId) {
        getAttributeHolder(attributeId).ifPresent(holder -> {
            AttributeInstance instance = entity.getAttribute(holder);
            if (instance != null) {
                instance.removeModifier(modifierId);
            }
        });
    }
}
