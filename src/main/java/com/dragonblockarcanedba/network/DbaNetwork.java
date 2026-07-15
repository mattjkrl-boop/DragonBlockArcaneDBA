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
                    if (ap > 0) {
                        boolean upgraded = false;
                        switch (stat) {
                            case "strength" -> {
                                if (accessor.dba$getStrength() < 5000) { accessor.dba$setStrength(accessor.dba$getStrength() + 1); upgraded = true; }
                            }
                            case "dexterity" -> {
                                if (accessor.dba$getDexterity() < 5000) { accessor.dba$setDexterity(accessor.dba$getDexterity() + 1); upgraded = true; }
                            }
                            case "defense" -> {
                                if (accessor.dba$getDefense() < 5000) { accessor.dba$setDefense(accessor.dba$getDefense() + 1); upgraded = true; }
                            }
                            case "willpower" -> {
                                if (accessor.dba$getWillpower() < 5000) { accessor.dba$setWillpower(accessor.dba$getWillpower() + 1); upgraded = true; }
                            }
                            case "spirit" -> {
                                if (accessor.dba$getSpirit() < 5000) { accessor.dba$setSpirit(accessor.dba$getSpirit() + 1); upgraded = true; }
                            }
                            case "vitality" -> {
                                if (accessor.dba$getVitality() < 5000) { accessor.dba$setVitality(accessor.dba$getVitality() + 1); upgraded = true; }
                            }
                        }
                        if (upgraded) {
                            accessor.dba$setStatPoints(ap - 1);
                        }
                        accessor.dba$syncStats();
                    }
                } else if ("transform".equals(action)) {
                    String formStr = nbt.getStringOr("form", "");
                    if ("none".equals(formStr)) {
                        accessor.dba$setActiveFormId(null);
                    } else {
                        Identifier formId = Identifier.parse(formStr);
                        Form form = DbaRegistries.getForm(formId);
                        if (form != null && form.getCompatibleRaces().contains(accessor.dba$getRaceId())) {
                            accessor.dba$setActiveFormId(formId);
                        }
                    }
                    accessor.dba$syncStats();
                } else if ("untransform".equals(action)) {
                    accessor.dba$setActiveFormId(null);
                    accessor.dba$syncStats();
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
}
