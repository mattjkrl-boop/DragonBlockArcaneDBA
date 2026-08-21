package com.dragonblockarcanedba.network;

import com.dragonblockarcanedba.dimension.DimensionTravel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.ki.KiTechnique;
import com.dragonblockarcanedba.ki.KiTechniqueType;
import com.dragonblockarcanedba.ki.KiTechniqueHandler;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.dragonblockarcanedba.registry.Form;

public class DbaNetwork {
    public static void registerCommon() {
        // Stats sync (S2C)
        PayloadTypeRegistry.clientboundPlay().register(StatsSyncPayload.TYPE, StatsSyncPayload.CODEC);
        // Space Pod open screen (S2C)
        PayloadTypeRegistry.clientboundPlay().register(SpacePodOpenPayload.TYPE, SpacePodOpenPayload.CODEC);
        // Race Selection open screen (S2C)
        PayloadTypeRegistry.clientboundPlay().register(RaceSelectOpenPayload.TYPE, RaceSelectOpenPayload.CODEC);
        // Revive UI open screen (S2C)
        PayloadTypeRegistry.clientboundPlay().register(ReviveUiOpenPayload.TYPE, ReviveUiOpenPayload.CODEC);
        // Wish Menu open screen (S2C)
        PayloadTypeRegistry.clientboundPlay().register(WishMenuOpenPayload.TYPE, WishMenuOpenPayload.CODEC);
        // Make Wish (C2S)
        PayloadTypeRegistry.serverboundPlay().register(C2SMakeWishPayload.TYPE, C2SMakeWishPayload.CODEC);
        // Transform broadcast to nearby players (S2C)
        PayloadTypeRegistry.clientboundPlay().register(TransformBroadcastPayload.TYPE, TransformBroadcastPayload.CODEC);

        // Player actions (C2S)
        PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC);
        // Space Pod launch (C2S)
        PayloadTypeRegistry.serverboundPlay().register(SpacePodLaunchPayload.TYPE, SpacePodLaunchPayload.CODEC);
        
        // Techniques (C2S)
        PayloadTypeRegistry.serverboundPlay().register(C2SUnlockTechniquePayload.ID, C2SUnlockTechniquePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2SEquipTechniquePayload.ID, C2SEquipTechniquePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2SToggleTechniquePayload.ID, C2SToggleTechniquePayload.CODEC);

        // Ki Technique payloads (C2S)
        PayloadTypeRegistry.serverboundPlay().register(C2SKiTechniqueSavePayload.ID, C2SKiTechniqueSavePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2SKiTechniqueFirePayload.ID, C2SKiTechniqueFirePayload.CODEC);

        // Gravity Block (C2S)
        PayloadTypeRegistry.serverboundPlay().register(C2SSetGravityPayload.TYPE, C2SSetGravityPayload.CODEC);

        // Weapon Left Click (C2S)
        PayloadTypeRegistry.serverboundPlay().register(C2SWeaponLeftClickPayload.TYPE, C2SWeaponLeftClickPayload.CODEC);
    }

    public static void registerServer() {
        // Handle player action packets (stat upgrades, transformations)
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            CompoundTag nbt = payload.nbtData();
            String action = nbt.getStringOr("action", "");

            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                if ("upgrade".equals(action)) {
                    String stat = nbt.getStringOr("stat", "");
                    int ap = accessor.dba$getStatPoints();
                    
                    int curLvl = 0;
                    switch (stat) {
                        case "strength" -> curLvl = accessor.dba$getStrength();
                        case "dexterity" -> curLvl = accessor.dba$getDexterity();
                        case "defense" -> curLvl = accessor.dba$getDefense();
                        case "willpower" -> curLvl = accessor.dba$getWillpower();
                        case "spirit" -> curLvl = accessor.dba$getSpirit();
                        case "vitality" -> curLvl = accessor.dba$getVitality();
                    }
                    
                    if (curLvl < 500000000) { // arbitrary high limit
                        String raceId = accessor.dba$getRaceId().getPath();
                        int currentUpgrades = accessor.dba$getStatUpgradeCount(stat);
                        int cost = com.dragonblockarcanedba.attribute.PlayerStats.getUpgradeCost(raceId, stat, currentUpgrades);
                        int gain = com.dragonblockarcanedba.attribute.PlayerStats.getStatGain(raceId, stat);
                        
                        // Milestone checking can be based on upgrades now. 
                        // e.g. you need Level = upgradeCount * 2
                        int milestone = (currentUpgrades / 5) * 5;
                        int reqLvl = milestone * 2;
                        
                        if (ap >= cost && accessor.dba$getLevel() >= reqLvl) {
                            switch (stat) {
                                case "strength" -> accessor.dba$setStrength(curLvl + gain);
                                case "dexterity" -> accessor.dba$setDexterity(curLvl + gain);
                                case "defense" -> accessor.dba$setDefense(curLvl + gain);
                                case "willpower" -> accessor.dba$setWillpower(curLvl + gain);
                                case "spirit" -> accessor.dba$setSpirit(curLvl + gain);
                                case "vitality" -> accessor.dba$setVitality(curLvl + gain);
                            }
                            accessor.dba$setStatUpgradeCount(stat, currentUpgrades + 1);
                            accessor.dba$setStatPoints(ap - cost);
                            accessor.dba$syncStats();
                        }
                    }
                } else if ("transform".equals(action)) {
                    String formStr = nbt.getStringOr("form", "");
                    if ("none".equals(formStr)) {
                        accessor.dba$setActiveFormId(null);
                    } else {
                        Identifier formId = Identifier.parse(formStr);
                        Form form = DbaRegistries.getForm(formId);
                        if (form != null && form.getCompatibleRaces().contains(accessor.dba$getRaceId())) {
                            // Form Unlock Validation
                            Form.UnlockRequirements reqs = form.getUnlockRequirements();
                            boolean meetsRequirements = true;
                            
                            // Check minimum level
                            if (accessor.dba$getLevel() < reqs.minLevel()) {
                                meetsRequirements = false;
                            }
                            
                            // Check minimum stats
                            if (meetsRequirements) {
                                com.dragonblockarcanedba.attribute.Attributes minStats = reqs.minStats();
                                if (accessor.dba$getStrength() < minStats.strength()
                                    || accessor.dba$getDefense() < minStats.defense()
                                    || accessor.dba$getSpirit() < minStats.kiCapacity()
                                    || accessor.dba$getWillpower() < minStats.kiControl()
                                    || accessor.dba$getDexterity() < minStats.agility()) {
                                    meetsRequirements = false;
                                }
                            }
                            
                            if (meetsRequirements) {
                                accessor.dba$setActiveFormId(formId);
                            }
                        }
                    }
                    accessor.dba$syncStats();
                    broadcastTransformState(player);
                } else if ("untransform".equals(action)) {
                    accessor.dba$setActiveFormId(null);
                    accessor.dba$syncStats();
                    broadcastTransformState(player);
                } else if ("select_race".equals(action)) {
                    String raceStr = nbt.getStringOr("race", "");
                    if (!raceStr.isEmpty()) {
                        boolean wasAlreadySelected = accessor.dba$hasSelectedRace();
                        accessor.dba$setRaceId(Identifier.parse(raceStr));
                        accessor.dba$setHasSelectedRace(true);
                        accessor.dba$setSkinColor(nbt.getStringOr("skin_color", ""));
                        accessor.dba$setHairColor(nbt.getStringOr("hair_color", ""));
                        
                        if (!wasAlreadySelected) {
                            // Initial race selection: fresh start
                            accessor.dba$setLevel(1);
                            accessor.dba$setXp(0);
                            accessor.dba$setStatPoints(0);
                            accessor.dba$setStrength(0);
                            accessor.dba$setDexterity(0);
                            accessor.dba$setDefense(0);
                            accessor.dba$setWillpower(0);
                            accessor.dba$setSpirit(0);
                            accessor.dba$setVitality(0);
                            for (String stat : new String[]{"strength", "dexterity", "defense", "willpower", "spirit", "vitality"}) {
                                accessor.dba$setStatUpgradeCount(stat, 0);
                            }
                        } else {
                            // Changing race later: 75% stats gone (retain 25%)
                            accessor.dba$setLevel(Math.max(1, (int) Math.round(accessor.dba$getLevel() * 0.25)));
                            accessor.dba$setXp((int) Math.round(accessor.dba$getXp() * 0.25));
                            accessor.dba$setStatPoints((int) Math.round(accessor.dba$getStatPoints() * 0.25));
                            accessor.dba$setStrength((int) Math.round(accessor.dba$getStrength() * 0.25));
                            accessor.dba$setDexterity((int) Math.round(accessor.dba$getDexterity() * 0.25));
                            accessor.dba$setDefense((int) Math.round(accessor.dba$getDefense() * 0.25));
                            accessor.dba$setWillpower((int) Math.round(accessor.dba$getWillpower() * 0.25));
                            accessor.dba$setSpirit((int) Math.round(accessor.dba$getSpirit() * 0.25));
                            accessor.dba$setVitality((int) Math.round(accessor.dba$getVitality() * 0.25));
                            for (String stat : new String[]{"strength", "dexterity", "defense", "willpower", "spirit", "vitality"}) {
                                accessor.dba$setStatUpgradeCount(stat, (int) Math.round(accessor.dba$getStatUpgradeCount(stat) * 0.25));
                            }
                        }
                        
                        accessor.dba$setActiveFormId(null);
                        
                        // Sync stats first to update the player's Max Health attribute based on their new race
                        accessor.dba$syncStats();
                        
                        // Heal to max stats on creation / change
                        accessor.dba$setCurrentKi(com.dragonblockarcanedba.attribute.PlayerStats.getMaxKi(player));
                        accessor.dba$setCurrentStamina(com.dragonblockarcanedba.attribute.PlayerStats.getMaxStamina(player));
                        player.setHealth(player.getMaxHealth());
                        
                        // Sync stats again so the client receives the filled Ki and Stamina bars
                        accessor.dba$syncStats();
                    }
                } else if ("revive".equals(action)) {
                    net.minecraft.server.level.ServerLevel overworld = context.server().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    if (overworld != null) {
                        player.teleportTo(overworld, 0.5, 100, 0.5, java.util.Collections.emptySet(), 0, 0, false);
                    }
                } else if ("set_speed_percent".equals(action)) {
                    int percent = nbt.getIntOr("percent", 100);
                    accessor.dba$setSpeedPercent(percent);
                    accessor.dba$syncStats();
                }
            });
        });

        // Handle Space Pod launch packets
        ServerPlayNetworking.registerGlobalReceiver(SpacePodLaunchPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            String destination = payload.destination();

            context.server().execute(() -> {
                DimensionTravel.travelTo(player, destination);
            });
        });

        // Handle Technique Unlocking
        ServerPlayNetworking.registerGlobalReceiver(C2SUnlockTechniquePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            String techniqueId = payload.techniqueId();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                com.dragonblockarcanedba.registry.Technique tech = com.dragonblockarcanedba.registry.TechniqueRegistry.getTechnique(Identifier.tryParse(techniqueId));
                if (tech != null && !accessor.dba$hasTechnique(techniqueId)) {
                    if (accessor.dba$getStatPoints() >= tech.apCost() && accessor.dba$getLevel() >= tech.unlockLevel()) {
                        accessor.dba$setStatPoints(accessor.dba$getStatPoints() - tech.apCost());
                        accessor.dba$setTechniqueUnlocked(techniqueId, true);
                        accessor.dba$syncStats();
                    }
                }
            });
        });

        // Handle Technique Equipping
        ServerPlayNetworking.registerGlobalReceiver(C2SEquipTechniquePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            int slot = payload.slot();
            String techniqueId = payload.techniqueId();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                if (accessor.dba$hasTechnique(techniqueId) || techniqueId.isEmpty()) {
                    accessor.dba$setEquippedTechnique(slot, techniqueId);
                    accessor.dba$syncStats();
                }
            });
        });

        // Handle Technique Toggling (from Keybinds)
        ServerPlayNetworking.registerGlobalReceiver(C2SToggleTechniquePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            int slot = payload.slot();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                String tech = accessor.dba$getEquippedTechnique(slot);
                if (tech != null && !tech.isEmpty() && accessor.dba$hasTechnique(tech)) {
                    boolean isActive = accessor.dba$isTechniqueActive(tech);
                    accessor.dba$setTechniqueActive(tech, !isActive);
                    accessor.dba$syncStats();
                }
            });
        });

        // Handle Ki Technique Save (from Ki Customizer Tab)
        ServerPlayNetworking.registerGlobalReceiver(C2SKiTechniqueSavePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                int slot = payload.slot();
                if (slot >= 0 && slot < 3) {
                    KiTechniqueType type = KiTechniqueType.fromString(payload.techType());
                    int pct = Math.max(1, Math.min(100, payload.usedPercent()));
                    KiTechnique kiTech = new KiTechnique(type, pct, payload.color(), payload.isBarrage());
                    accessor.dba$setKiTechniqueSlot(slot, kiTech);
                    accessor.dba$syncStats();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "\u00a7aSaved \u00a7b" + kiTech.displayName() + "\u00a7a to slot " + (slot + 1)
                    ), true);
                }
            });
        });

        // Handle Ki Technique Fire (from Keybinds F7/F8/F9)
        ServerPlayNetworking.registerGlobalReceiver(C2SKiTechniqueFirePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                KiTechniqueHandler.fire(player, payload.slot());
            });
        });

        // Handle Make Wish C2S
        ServerPlayNetworking.registerGlobalReceiver(C2SMakeWishPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            int shenronId = payload.shenronId();
            String wishType = payload.wishType();
            context.server().execute(() -> {
                net.minecraft.world.entity.Entity entity = player.level().getEntity(shenronId);
                if (entity instanceof com.dragonblockarcanedba.entity.ShenronEntity shenron && shenron.isAlive()) {
                    if (player.distanceToSqr(shenron) < 256.0) {
                        shenron.grantWish(player, wishType);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SSetGravityPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().containerMenu instanceof com.dragonblockarcanedba.inventory.GravityTrainingMenu menu) {
                    menu.updateGravity(payload.gravity());
                }
            });
        });

        // Handle Weapon Left Clicks on Air / Charge / Stream
        ServerPlayNetworking.registerGlobalReceiver(C2SWeaponLeftClickPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                net.minecraft.world.item.ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof com.dragonblockarcanedba.item.ZSwordItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.ZSwordItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.ZSwordItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    } else {
                        com.dragonblockarcanedba.item.ZSwordItem.onLeftClickRelease(player, stack, 10);
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.CurseBladeItem) {
                    com.dragonblockarcanedba.item.CurseBladeItem.streamCurseChain(player, stack);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.HollowsEdgeItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.HollowsEdgeItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.HollowsEdgeItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    } else {
                        com.dragonblockarcanedba.item.HollowsEdgeItem.onLeftClickDash(player, stack, payload.extraFlag());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.AzureDragonSwordItem) {
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.AzureDragonSwordItem.stopDragonRush(player);
                    } else {
                        if (player.getCooldowns().isOnCooldown(stack)) return;
                        com.dragonblockarcanedba.item.AzureDragonSwordItem.onDragonRushTick(player, stack, payload.extraFlag());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.DimensionalSwordItem) {
                    com.dragonblockarcanedba.item.DimensionalSwordItem.fireSlash(player, stack);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.PowerPoleItem) {
                    com.dragonblockarcanedba.item.PowerPoleItem.performWindSpin(player, stack);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.DevilTridentItem) {
                    com.dragonblockarcanedba.item.DevilTridentItem.performLeftClickTargeting(player, stack, null);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.SaberItem) {
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.SaberItem.onBlitzRelease(player, stack, payload.chargeTicks());
                    } else {
                        com.dragonblockarcanedba.item.SaberItem.onBlitzTick(player, stack, payload.chargeTicks());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.OxKingsAxeItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.OxKingsAxeItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.OxKingsAxeItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    } else {
                        com.dragonblockarcanedba.item.OxKingsAxeItem.onLeftClickRelease(player, stack, 10);
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.GrandSwordItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.GrandSwordItem.onLeftClickSpinTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.GrandSwordItem.onLeftClickSpinRelease(player, stack, payload.chargeTicks());
                    } else {
                        com.dragonblockarcanedba.item.GrandSwordItem.onLeftClickSpinRelease(player, stack, 10);
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.DaburaSwordItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.DaburaSwordItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.DaburaSwordItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.EvilSpearItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.EvilSpearItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.EvilSpearItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.KatanaItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_CHARGE_TICK) {
                        com.dragonblockarcanedba.item.KatanaItem.onLeftClickChargeTick(player, stack, payload.chargeTicks());
                    } else if (payload.actionType() == C2SWeaponLeftClickPayload.ACTION_RELEASE) {
                        com.dragonblockarcanedba.item.KatanaItem.onLeftClickRelease(player, stack, payload.chargeTicks());
                    }
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.BlasterGunItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    com.dragonblockarcanedba.item.BlasterGunItem.onLeftClickBarrageTick(player, stack, payload.chargeTicks());
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.BraveSwordItem) {
                    if (player.getCooldowns().isOnCooldown(stack)) return;
                    com.dragonblockarcanedba.item.BraveSwordItem.onLeftClickAssaultTick(player, stack, payload.chargeTicks());
                }
            });
        });
    }

    public static void sendStatsSync(ServerPlayer player, CompoundTag nbtData) {
        ServerPlayNetworking.send(player, new StatsSyncPayload(nbtData));
    }

    /**
     * Broadcasts a player's transformation state to all other players in the same dimension.
     * Called whenever a player transforms or untransforms.
     */
    public static void broadcastTransformState(ServerPlayer player) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
        Identifier raceId = accessor.dba$getRaceId();
        Identifier formId = accessor.dba$getActiveFormId();

        TransformBroadcastPayload payload = new TransformBroadcastPayload(
            player.getId(),
            raceId != null ? raceId.toString() : "",
            formId != null ? formId.toString() : ""
        );

        // Send to all players in the same level
        for (ServerPlayer other : ((net.minecraft.server.level.ServerLevel) player.level()).players()) {
            if (other != player) {
                ServerPlayNetworking.send(other, payload);
            }
        }
    }

    /**
     * Sends a specific player's transformation and race state to a target player.
     */
    public static void sendTransformStateTo(ServerPlayer sourcePlayer, ServerPlayer targetPlayer) {
        PlayerStatsAccessor accessor = (PlayerStatsAccessor) sourcePlayer;
        Identifier raceId = accessor.dba$getRaceId();
        Identifier formId = accessor.dba$getActiveFormId();

        TransformBroadcastPayload payload = new TransformBroadcastPayload(
            sourcePlayer.getId(),
            raceId != null ? raceId.toString() : "",
            formId != null ? formId.toString() : ""
        );

        ServerPlayNetworking.send(targetPlayer, payload);
    }
}
