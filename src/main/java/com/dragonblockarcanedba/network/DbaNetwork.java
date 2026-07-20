package com.dragonblockarcanedba.network;

import com.dragonblockarcanedba.dimension.DimensionTravel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
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
        // Transform broadcast to nearby players (S2C)
        PayloadTypeRegistry.clientboundPlay().register(TransformBroadcastPayload.TYPE, TransformBroadcastPayload.CODEC);

        // Player actions (C2S)
        PayloadTypeRegistry.serverboundPlay().register(ActionPayload.TYPE, ActionPayload.CODEC);
        // Space Pod launch (C2S)
        PayloadTypeRegistry.serverboundPlay().register(SpacePodLaunchPayload.TYPE, SpacePodLaunchPayload.CODEC);
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
                    
                    if (curLvl < 5000) {
                        int cost = com.dragonblockarcanedba.attribute.PlayerStats.getUpgradeCost(curLvl);
                        int milestone = (curLvl / 5) * 5;
                        int reqLvl = milestone * 2;
                        
                        if (ap >= cost && accessor.dba$getLevel() >= reqLvl) {
                            switch (stat) {
                                case "strength" -> accessor.dba$setStrength(curLvl + 1);
                                case "dexterity" -> accessor.dba$setDexterity(curLvl + 1);
                                case "defense" -> accessor.dba$setDefense(curLvl + 1);
                                case "willpower" -> accessor.dba$setWillpower(curLvl + 1);
                                case "spirit" -> accessor.dba$setSpirit(curLvl + 1);
                                case "vitality" -> accessor.dba$setVitality(curLvl + 1);
                            }
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
                        accessor.dba$setRaceId(Identifier.parse(raceStr));
                        accessor.dba$setHasSelectedRace(true);
                        accessor.dba$setSkinColor(nbt.getStringOr("skin_color", ""));
                        accessor.dba$setHairColor(nbt.getStringOr("hair_color", ""));
                        // Reset stats for fresh start
                        accessor.dba$setLevel(1);
                        accessor.dba$setXp(0);
                        accessor.dba$setStatPoints(0);
                        accessor.dba$setStrength(0);
                        accessor.dba$setDexterity(0);
                        accessor.dba$setDefense(0);
                        accessor.dba$setWillpower(0);
                        accessor.dba$setSpirit(0);
                        accessor.dba$setVitality(0);
                        accessor.dba$setActiveFormId(null);
                        accessor.dba$syncStats();
                    }
                } else if ("revive".equals(action)) {
                    net.minecraft.server.level.ServerLevel overworld = context.server().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                    if (overworld != null) {
                        player.teleportTo(overworld, 0.5, 100, 0.5, java.util.Collections.emptySet(), 0, 0, false);
                    }
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
}
