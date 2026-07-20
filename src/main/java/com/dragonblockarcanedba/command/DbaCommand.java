package com.dragonblockarcanedba.command;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class DbaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dba")
            .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("help")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§6--- Dragon Block Arcane DBA Help ---"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba race set <player> <race_id>§f - Sets a player's race"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba ap <add|set> <player> <amount>§f - Modify AP"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba xp <add|set> <player> <amount>§f - Modify XP"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba level <add|set> <player> <amount>§f - Modify Level"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba technique <unlock|toggle> <player> <technique>§f - Modify Techniques"), false);
                    return 1;
                })
            )
            .then(Commands.literal("race")
                .then(Commands.literal("set")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("race_id", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                for (Identifier id : DbaRegistries.getRaces().keySet()) {
                                    if (id.getPath().startsWith(builder.getRemainingLowerCase())) {
                                        builder.suggest(id.getPath());
                                    }
                                }
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "player");
                                String raceIdStr = StringArgumentType.getString(context, "race_id");
                                Identifier raceId = Identifier.fromNamespaceAndPath("dragonblockarcanedba", raceIdStr);

                                if (DbaRegistries.getRace(raceId) == null) {
                                    context.getSource().sendFailure(Component.literal("Unknown race: " + raceId));
                                    return 0;
                                }

                                PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                                accessor.dba$setRaceId(raceId);
                                for (Identifier formId : DbaRegistries.getAllFormIds()) {
                                    accessor.dba$setFormMastery(formId, 0.0);
                                }
                                accessor.dba$setActiveFormId(null);
                                accessor.dba$syncStats();

                                context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s race to " + raceIdStr), true);
                                target.sendSystemMessage(Component.literal("Your race was changed to " + raceIdStr + ". Your stats are preserved, but your technique unlocks have been reset."));
                                return 1;
                            })
                        )
                    )
                )
            )
            .then(Commands.literal("ap")
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setStatPoints(accessor.dba$getStatPoints() + amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " AP to " + target.getName().getString()), true);
                    return 1;
                }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setStatPoints(amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s AP to " + amount), true);
                    return 1;
                }))))
            )
            .then(Commands.literal("xp")
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$addXp(amount); // This auto-levels up via mixin
                    context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " XP to " + target.getName().getString()), true);
                    return 1;
                }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setXp(amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s XP to " + amount), true);
                    return 1;
                }))))
            )
            .then(Commands.literal("level")
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setLevel(accessor.dba$getLevel() + amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " Levels to " + target.getName().getString()), true);
                    return 1;
                }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1)).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setLevel(amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s Level to " + amount), true);
                    return 1;
                }))))
            )
            .then(Commands.literal("technique")
                .then(Commands.literal("unlock").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("technique", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String technique = StringArgumentType.getString(context, "technique");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setTechniqueUnlocked(technique, true);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Unlocked technique " + technique + " for " + target.getName().getString()), true);
                    return 1;
                }))))
                .then(Commands.literal("toggle").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("technique", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String technique = StringArgumentType.getString(context, "technique");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    if (!accessor.dba$hasTechnique(technique)) {
                        context.getSource().sendFailure(Component.literal("Player has not unlocked this technique."));
                        return 0;
                    }
                    boolean currentState = accessor.dba$isTechniqueActive(technique);
                    accessor.dba$setTechniqueActive(technique, !currentState);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Toggled technique " + technique + " for " + target.getName().getString() + " to " + !currentState), true);
                    return 1;
                }))))
            )
        );
    }
}
