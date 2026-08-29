package com.dragonblockarcanedba.network;

import com.dragonblockarcanedba.dimension.DimensionTravel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
        PayloadTypeRegistry.serverboundPlay().register(C2SUpgradeTechniquePayload.ID, C2SUpgradeTechniquePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2SEquipTechniquePayload.ID, C2SEquipTechniquePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(C2SToggleTechniquePayload.ID, C2SToggleTechniquePayload.CODEC);

        // Player Ki broadcast to nearby players (S2C)
        PayloadTypeRegistry.clientboundPlay().register(PlayerKiBroadcastPayload.TYPE, PlayerKiBroadcastPayload.CODEC);

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
                            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8f, 1.3f);
                        }
                    }
                } else if ("transform".equals(action)) {
                    String formStr = nbt.getStringOr("form", "");
                    if ("none".equals(formStr)) {
                        accessor.dba$setActiveFormId(null);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 1.2f);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
                    } else {
                        Identifier formId = Identifier.parse(formStr);
                        Form form = DbaRegistries.getForm(formId);
                        if (form != null && form.getCompatibleRaces().contains(accessor.dba$getRaceId())) {
                            Form.UnlockRequirements reqs = form.getUnlockRequirements();
                            boolean meetsRequirements = true;
                            
                            if (accessor.dba$getLevel() < reqs.minLevel()) {
                                meetsRequirements = false;
                            }
                            
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
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.8f, 1.0f);
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 1.2f);
                                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.0f, 1.5f);
                            }
                        }
                    }
                    accessor.dba$syncStats();
                    broadcastTransformState(player);
                } else if ("untransform".equals(action)) {
                    accessor.dba$setActiveFormId(null);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2f, 1.2f);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.8f);
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
                        
                        accessor.dba$syncStats();
                        
                        accessor.dba$setCurrentKi(com.dragonblockarcanedba.attribute.PlayerStats.getMaxKi(player));
                        accessor.dba$setCurrentStamina(com.dragonblockarcanedba.attribute.PlayerStats.getMaxStamina(player));
                        player.setHealth(player.getMaxHealth());
                        
                        accessor.dba$syncStats();
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                    }
                } else if ("revive".equals(action)) {
                    player.resetFallDistance();
                    player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    player.clearFire();
                    boolean traveled = com.dragonblockarcanedba.dimension.DimensionTravel.travelTo(player, "overworld");
                    if (traveled) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e✨ You have been restored to life and returned to the physical realm!"));
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
                    if (tech.hasPrerequisites()) {
                        for (String prereqId : tech.prerequisiteTechniqueIds()) {
                            if (!accessor.dba$hasTechnique(prereqId)) {
                                com.dragonblockarcanedba.registry.Technique prereq = com.dragonblockarcanedba.registry.TechniqueRegistry.getTechnique(Identifier.tryParse(prereqId));
                                String prereqName = (prereq != null) ? prereq.name() : prereqId;
                                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cRequires " + prereqName + " to be unlocked first!"), true);
                                return;
                            }
                        }
                    }
                    int apCost = com.dragonblockarcanedba.attribute.PlayerStats.getTechniqueUpgradeCost(techniqueId, 1);
                    if (accessor.dba$getStatPoints() >= apCost && accessor.dba$getLevel() >= tech.unlockLevel()) {
                        accessor.dba$setStatPoints(accessor.dba$getStatPoints() - apCost);
                        accessor.dba$setTechniqueUnlocked(techniqueId, true);
                        accessor.dba$setTechniqueLevel(techniqueId, 1);
                        accessor.dba$syncStats();
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aUnlocked §b" + tech.name() + " §a(Level 1)!"), true);
                    }
                }
            });
        });

        // Handle Technique Upgrading
        ServerPlayNetworking.registerGlobalReceiver(C2SUpgradeTechniquePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            String techniqueId = payload.techniqueId();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                if (!accessor.dba$hasTechnique(techniqueId)) return;
                int currentLvl = accessor.dba$getTechniqueLevel(techniqueId);
                int targetLvl = currentLvl + 1;
                if (targetLvl <= 10) {
                    int apCost = com.dragonblockarcanedba.attribute.PlayerStats.getTechniqueUpgradeCost(techniqueId, targetLvl);
                    if (accessor.dba$getStatPoints() >= apCost) {
                        accessor.dba$setStatPoints(accessor.dba$getStatPoints() - apCost);
                        accessor.dba$setTechniqueLevel(techniqueId, targetLvl);
                        accessor.dba$syncStats();
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.2f);
                        com.dragonblockarcanedba.registry.Technique t = com.dragonblockarcanedba.registry.TechniqueRegistry.getTechnique(Identifier.tryParse(techniqueId));
                        String disp = (t != null) ? t.name() : techniqueId;
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aUpgraded §b" + disp + " §ato Level " + targetLvl + "!"), true);
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNeed " + apCost + " AP to upgrade!"), true);
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
                    if (!techniqueId.isEmpty()) {
                        accessor.dba$setKiTechniqueSlot(slot, com.dragonblockarcanedba.ki.KiTechnique.EMPTY);
                    }
                    accessor.dba$syncStats();
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
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
                    toggleTechnique(player, accessor, tech);
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
                    int apCost = com.dragonblockarcanedba.attribute.PlayerStats.getKiAttackSaveCost(type, pct, payload.isBarrage());
                    if (accessor.dba$getStatPoints() < apCost) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNot enough AP to save! Need " + apCost + " AP."), true);
                        return;
                    }
                    accessor.dba$setStatPoints(accessor.dba$getStatPoints() - apCost);
                    accessor.dba$setEquippedTechnique(slot, ""); // Clear arcane tech in this slot
                    KiTechnique kiTech = new KiTechnique(type, pct, payload.color(), payload.isBarrage());
                    accessor.dba$setKiTechniqueSlot(slot, kiTech);
                    accessor.dba$syncStats();
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.4f);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§aSaved §b" + kiTech.displayName() + "§a to slot " + (slot + 1) + " §7(Cost: " + apCost + " AP)"
                    ), true);
                }
            });
        });

        // Handle Ki Technique Fire (from Keybinds F7/F8/F9)
        ServerPlayNetworking.registerGlobalReceiver(C2SKiTechniqueFirePayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                String tech = accessor.dba$getEquippedTechnique(payload.slot());
                if (tech != null && !tech.isEmpty() && accessor.dba$hasTechnique(tech)) {
                    toggleTechnique(player, accessor, tech);
                } else {
                    KiTechniqueHandler.fire(player, payload.slot());
                }
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
                PlayerStatsAccessor accessor = (PlayerStatsAccessor) player;
                if (accessor.dba$isSickleActive()) {
                    com.dragonblockarcanedba.item.SickleOfSorrowItem.performTechniqueAirSwing(player);
                    return;
                }
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
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.SickleOfSorrowItem) {
                    com.dragonblockarcanedba.item.SickleOfSorrowItem.performSorrowSlash(player, stack);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.SpiritSwordItem) {
                    com.dragonblockarcanedba.util.WeaponDrainHelper.drainKiDiscrete(player, 55.0, 7);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.BanshoFanItem) {
                    com.dragonblockarcanedba.util.WeaponDrainHelper.drainStaminaDiscrete(player, 45.0, 10);
                } else if (stack.getItem() instanceof com.dragonblockarcanedba.item.WhisStaffItem) {
                    com.dragonblockarcanedba.util.WeaponDrainHelper.drainKiDiscrete(player, 100.0, 13);
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
            formId != null ? formId.toString() : "",
            accessor.dba$isTailSevered(),
            accessor.dba$getSkinColor() != null ? accessor.dba$getSkinColor() : "",
            accessor.dba$getHairColor() != null ? accessor.dba$getHairColor() : ""
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
            formId != null ? formId.toString() : "",
            accessor.dba$isTailSevered(),
            accessor.dba$getSkinColor() != null ? accessor.dba$getSkinColor() : "",
            accessor.dba$getHairColor() != null ? accessor.dba$getHairColor() : ""
        );

        ServerPlayNetworking.send(targetPlayer, payload);
    }

    public static void toggleTechnique(ServerPlayer player, PlayerStatsAccessor accessor, String tech) {
        if ("sickle_of_sorrow".equals(tech)) {
            boolean active = accessor.dba$isTechniqueActive("sickle_of_sorrow");
            if (!active) {
                double currentKi = accessor.dba$getCurrentKi();
                if (currentKi <= 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNot enough Ki to summon Sickle of Sorrow!"), true);
                    return;
                }
                int sickleLvl = accessor.dba$getTechniqueLevel("sickle_of_sorrow");
                int summonPct = com.dragonblockarcanedba.attribute.PlayerStats.getSickleSummonPercent(sickleLvl);
                double cost = currentKi * (summonPct / 100.0);
                accessor.dba$addKi(-cost);
                accessor.dba$pauseKiRecovery(25);
                accessor.dba$setTechniqueActive("sickle_of_sorrow", true);
                accessor.dba$syncStats();
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 1.8f);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§5✦ Summoned Sickle of Sorrow! §7(-" + summonPct + "% Ki)"), true);
            } else {
                accessor.dba$setTechniqueActive("sickle_of_sorrow", false);
                accessor.dba$syncStats();
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.PLAYERS, 0.7f, 1.5f);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§7Dismissed Sickle of Sorrow."), true);
            }
        } else if ("ki_sense".equals(tech)) {
            boolean isActive = accessor.dba$isTechniqueActive("ki_sense");
            accessor.dba$setTechniqueActive("ki_sense", !isActive);
            accessor.dba$syncStats();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, !isActive ? 1.4f : 0.8f);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(!isActive ? "§a✦ Ki Sense Activated" : "§7Ki Sense Deactivated"), true);
        } else {
            boolean isActive = accessor.dba$isTechniqueActive(tech);
            accessor.dba$setTechniqueActive(tech, !isActive);
            accessor.dba$syncStats();
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8f, !isActive ? 1.2f : 0.8f);
        }
    }

    public static void broadcastPlayerKi(ServerPlayer player, double currentKi, double maxKi) {
        PlayerKiBroadcastPayload payload = new PlayerKiBroadcastPayload(player.getId(), (float) currentKi, (float) maxKi);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (ServerPlayer other : serverLevel.players()) {
                ServerPlayNetworking.send(other, payload);
            }
        }
    }
}
