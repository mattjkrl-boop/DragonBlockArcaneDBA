package com.dragonblockarcanedba.util;

import com.dragonblockarcanedba.entity.ITrackedSwarmEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwarmHelper {

    /**
     * Initializes the swarm health array for a newly deployed swarm.
     * @param stack The weapon ItemStack.
     * @param count The total number of swarm entities deployed.
     * @param defaultHealth The starting health for each entity.
     */
    public static void deploySwarm(ItemStack stack, int count, float defaultHealth) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("isDeployed", true);
        
        ListTag healthList = new ListTag();
        for (int i = 0; i < count; i++) {
            healthList.add(FloatTag.valueOf(defaultHealth));
        }
        tag.put("swarmHealths", healthList);
        
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Updates the health of a specific entity in the swarm, saving it to the item.
     * Call this when a swarm entity takes damage.
     * @param player The owner of the swarm.
     * @param weaponClass The class of the weapon (e.g. DevilTridentItem.class).
     * @param index The unique index of the swarm entity (0 to count-1).
     * @param newHealth The new health of the swarm entity (0.0f means destroyed).
     */
    public static void updateSwarmHealth(Player player, Class<? extends Item> weaponClass, int index, float newHealth) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (weaponClass.isInstance(stack.getItem())) {
                CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                if (tag.getBoolean("isDeployed").orElse(false) && tag.contains("swarmHealths")) {
                    ListTag healthList = tag.getListOrEmpty("swarmHealths");
                    if (index >= 0 && index < healthList.size()) {
                        healthList.set(index, FloatTag.valueOf(Math.max(0.0f, newHealth)));
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                        return; // Found and updated
                    }
                }
            }
        }
    }

    /**
     * Checks the item's saved swarm state against the currently active entities.
     * Returns a map of index -> health for any entities that should be alive but are missing from the world.
     * 
     * @param stack The weapon ItemStack.
     * @param activeEntities The list of entities currently active in the world for this player.
     * @return Map of missing indexes and their expected health.
     */
    public static Map<Integer, Float> getMissingEntities(ItemStack stack, List<? extends ITrackedSwarmEntity> activeEntities) {
        Map<Integer, Float> missing = new HashMap<>();
        
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.getBoolean("isDeployed").orElse(false) || !tag.contains("swarmHealths")) {
            return missing;
        }

        ListTag healthList = tag.getListOrEmpty("swarmHealths");
        for (int i = 0; i < healthList.size(); i++) {
            float savedHealth = healthList.getFloatOr(i, 0.0f);
            if (savedHealth > 0.0f) {
                // This entity should be alive. Let's see if it's in the active list.
                boolean isFound = false;
                for (ITrackedSwarmEntity active : activeEntities) {
                    if (active.getSwarmIndex() == i) {
                        isFound = true;
                        break;
                    }
                }
                
                if (!isFound) {
                    missing.put(i, savedHealth);
                }
            }
        }
        
        return missing;
    }
    
    /**
     * Call this when recalling the swarm to clear deployment state.
     */
    public static void recallSwarm(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("isDeployed", false);
        tag.remove("swarmHealths");
        tag.remove("swarmTarget"); // Assuming generic target tag
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
