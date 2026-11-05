package com.dragonblockarcanedba.ability;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Phase 6 swing hook: the passive half of the on-hit system. When a
 * player lands a vanilla main-hand melee hit with a weapon whose
 * left ability carries a swing block, the block's effect is applied
 * to the target. Registered as a second AttackEntityCallback in the
 * main class, additive with the existing stamina hook (both return
 * PASS). An unresolvable effect id is a silent no-op here; the
 * loader already logged it at load time.
 */
public final class AbilitySwingHook {
    private AbilitySwingHook() {}

    public static void onAttack(Player player, Level level, InteractionHand hand,
                                Entity entity) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(entity instanceof LivingEntity target)) {
            return;
        }
        ItemStack stack = serverPlayer.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        AbilityDef def = AbilityRegistry.forItem(itemId, AbilityDef.Slot.LEFT);
        if (def == null || def.swing() == null) {
            return;
        }
        MobEffect effect = BuiltInRegistries.MOB_EFFECT.getValue(def.swing().effectId());
        if (effect == null) {
            return;
        }
        target.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect),
                def.swing().durationTicks(), def.swing().amplifier()));
    }
}
