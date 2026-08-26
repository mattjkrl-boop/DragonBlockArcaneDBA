package com.dragonblockarcanedba.command;

import com.dragonblockarcanedba.attribute.PlayerStatsAccessor;
import com.dragonblockarcanedba.registry.DbaRegistries;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import java.math.BigInteger;

public class DbaCommand {
    private enum XpAction {
        ADD,
        SET,
        REMOVE
    }

    private enum XpUnit {
        POINTS,
        LEVELS
    }

    private static class ParsedXp {
        final int amount;
        final XpUnit unit;

        ParsedXp(int amount, XpUnit unit) {
            this.amount = amount;
            this.unit = unit;
        }
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dba")
            .requires(source -> source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("help")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§6--- Dragon Block Arcane DBA Help ---"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba race set <player> <race_id>§f - Sets a player's race"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba ap <add|set|remove> <player> <amount>§f - Modify AP"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba xp <add|set|remove> <player> <amount> [levels|points]§f - Modify XP or Levels"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba level <add|set|remove> <player> <amount>§f - Modify Level"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba technique <unlock|toggle> <player> <technique>§f - Modify Techniques"), false);
                    context.getSource().sendSuccess(() -> Component.literal("§e/dba tail <get|sever|regrow> <player>§f - Check, sever, or regrow a player's tail"), false);
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
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int raw;
                    try {
                        raw = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    final int amount = Math.max(1, raw);
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    long newAp = (long) accessor.dba$getStatPoints() + (long) amount;
                    int clampedAp = (int) Math.min((long) Integer.MAX_VALUE, newAp);
                    accessor.dba$setStatPoints(clampedAp);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " AP to " + target.getName().getString() + " (Now " + clampedAp + " AP)"), true);
                    return 1;
                }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int amount;
                    try {
                        amount = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    if (amount < 0) {
                        context.getSource().sendFailure(Component.literal("AP cannot be negative."));
                        return 0;
                    }
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setStatPoints(amount);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s AP to " + amount), true);
                    return 1;
                }))))
                .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int amount;
                    try {
                        amount = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    final int toRemove = Math.abs(amount);
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    int newAp = Math.max(0, accessor.dba$getStatPoints() - toRemove);
                    accessor.dba$setStatPoints(newAp);
                    accessor.dba$syncStats();
                    context.getSource().sendSuccess(() -> Component.literal("Removed " + toRemove + " AP from " + target.getName().getString() + " (Now " + newAp + " AP)"), true);
                    return 1;
                }))))
            )
            .then(Commands.literal("xp")
                .then(buildXpSubcommand("add", XpAction.ADD))
                .then(buildXpSubcommand("set", XpAction.SET))
                .then(buildXpSubcommand("remove", XpAction.REMOVE))
            )
            .then(Commands.literal("level")
                .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int raw;
                    try {
                        raw = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    final int amount = Math.max(1, raw);
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$addLevel(amount);
                    context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " Level(s) to " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ")"), true);
                    return 1;
                }))))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int amount;
                    try {
                        amount = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    if (amount < 1) {
                        context.getSource().sendFailure(Component.literal("Level cannot be set below 1."));
                        return 0;
                    }
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$setLevel(amount);
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s Level to " + accessor.dba$getLevel()), true);
                    return 1;
                }))))
                .then(Commands.literal("remove").then(Commands.argument("player", EntityArgument.player()).then(Commands.argument("amount", StringArgumentType.word()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    String amountStr = StringArgumentType.getString(context, "amount");
                    int amount;
                    try {
                        amount = parseClampedInt(amountStr, amountStr);
                    } catch (IllegalArgumentException e) {
                        context.getSource().sendFailure(Component.literal(e.getMessage()));
                        return 0;
                    }
                    int toRemove = Math.abs(amount);
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    accessor.dba$addLevel(-toRemove);
                    context.getSource().sendSuccess(() -> Component.literal("Removed " + toRemove + " Level(s) from " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ")"), true);
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
            .then(Commands.literal("tail")
                .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    boolean isTailed = com.dragonblockarcanedba.tail.TailHelper.isTailedRace(accessor.dba$getRaceId());
                    boolean hasTail = accessor.dba$hasTail();
                    boolean severed = accessor.dba$isTailSevered();
                    context.getSource().sendSuccess(() -> Component.literal(
                        "§6" + target.getName().getString() + "§f Tail Status: " +
                        (isTailed ? (hasTail ? "§aIntact" : "§cSevered") : "§7None (Non-tailed race)") +
                        " §8[Tailed Race: " + isTailed + ", Severed: " + severed + "]"
                    ), false);
                    return 1;
                })))
                .then(Commands.literal("sever").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    if (!com.dragonblockarcanedba.tail.TailHelper.isTailedRace(accessor.dba$getRaceId())) {
                        context.getSource().sendFailure(Component.literal(target.getName().getString() + " belongs to a non-tailed race."));
                        return 0;
                    }
                    if (accessor.dba$isTailSevered()) {
                        context.getSource().sendFailure(Component.literal(target.getName().getString() + "'s tail is already severed."));
                        return 0;
                    }
                    com.dragonblockarcanedba.tail.TailHelper.severTail(target, target.damageSources().generic());
                    context.getSource().sendSuccess(() -> Component.literal("Severed tail of " + target.getName().getString()), true);
                    return 1;
                })))
                .then(Commands.literal("regrow").then(Commands.argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer target = EntityArgument.getPlayer(context, "player");
                    PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
                    if (!com.dragonblockarcanedba.tail.TailHelper.isTailedRace(accessor.dba$getRaceId())) {
                        context.getSource().sendFailure(Component.literal(target.getName().getString() + " belongs to a non-tailed race."));
                        return 0;
                    }
                    if (!accessor.dba$isTailSevered()) {
                        context.getSource().sendFailure(Component.literal(target.getName().getString() + "'s tail is already intact."));
                        return 0;
                    }
                    com.dragonblockarcanedba.tail.TailHelper.regrowTail(target);
                    context.getSource().sendSuccess(() -> Component.literal("Regrew tail of " + target.getName().getString()), true);
                    return 1;
                })))
            )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildXpSubcommand(String name, XpAction action) {
        return Commands.literal(name)
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(context -> executeXpChange(context, action, null))
                    .then(Commands.argument("unit", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemainingLowerCase();
                            for (String u : new String[]{"points", "levels", "level", "l", "p", "xp"}) {
                                if (u.startsWith(remaining)) {
                                    builder.suggest(u);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> executeXpChange(context, action, StringArgumentType.getString(context, "unit")))
                    )
                )
            );
    }

    private static int executeXpChange(CommandContext<CommandSourceStack> context, XpAction action, String unitStr) {
        ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(context, "player");
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Invalid target player."));
            return 0;
        }

        String amountStr = StringArgumentType.getString(context, "amount");
        ParsedXp parsed;
        try {
            parsed = parseXp(amountStr, unitStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        PlayerStatsAccessor accessor = (PlayerStatsAccessor) target;
        int rawAmount = parsed.amount;

        switch (action) {
            case ADD -> {
                if (parsed.unit == XpUnit.LEVELS) {
                    accessor.dba$addLevel(rawAmount);
                    context.getSource().sendSuccess(() -> Component.literal("Added " + rawAmount + " Level(s) to " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ")"), true);
                } else {
                    accessor.dba$addXp(rawAmount);
                    context.getSource().sendSuccess(() -> Component.literal("Added " + rawAmount + " XP to " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ", " + accessor.dba$getXp() + " XP)"), true);
                }
            }
            case SET -> {
                if (parsed.unit == XpUnit.LEVELS) {
                    if (rawAmount < 1) {
                        context.getSource().sendFailure(Component.literal("Level cannot be set below 1."));
                        return 0;
                    }
                    accessor.dba$setLevel(rawAmount);
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s Level to " + accessor.dba$getLevel()), true);
                } else {
                    if (rawAmount < 0) {
                        context.getSource().sendFailure(Component.literal("XP cannot be set below 0."));
                        return 0;
                    }
                    accessor.dba$setXp(rawAmount);
                    context.getSource().sendSuccess(() -> Component.literal("Set " + target.getName().getString() + "'s XP to " + rawAmount + " (Level " + accessor.dba$getLevel() + ")"), true);
                }
            }
            case REMOVE -> {
                int toRemove = Math.abs(rawAmount);
                if (parsed.unit == XpUnit.LEVELS) {
                    accessor.dba$addLevel(-toRemove);
                    context.getSource().sendSuccess(() -> Component.literal("Removed " + toRemove + " Level(s) from " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ")"), true);
                } else {
                    accessor.dba$addXp(-toRemove);
                    context.getSource().sendSuccess(() -> Component.literal("Removed " + toRemove + " XP from " + target.getName().getString() + " (Now level " + accessor.dba$getLevel() + ", " + accessor.dba$getXp() + " XP)"), true);
                }
            }
        }

        return 1;
    }

    private static ParsedXp parseXp(String amountStr, String unitStr) throws IllegalArgumentException {
        if (unitStr != null && !unitStr.isEmpty()) {
            int amount = parseClampedInt(amountStr, amountStr);
            String u = unitStr.toLowerCase().trim();
            if (u.equals("levels") || u.equals("level") || u.equals("l")) {
                return new ParsedXp(amount, XpUnit.LEVELS);
            } else if (u.equals("points") || u.equals("point") || u.equals("p") || u.equals("xp")) {
                return new ParsedXp(amount, XpUnit.POINTS);
            } else {
                throw new IllegalArgumentException("Invalid unit: '" + unitStr + "'. Expected 'levels' ('l') or 'points' ('p', 'xp').");
            }
        } else {
            String lower = amountStr.toLowerCase().trim();
            if (lower.endsWith("levels")) {
                String num = lower.substring(0, lower.length() - 6);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.LEVELS);
            } else if (lower.endsWith("level")) {
                String num = lower.substring(0, lower.length() - 5);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.LEVELS);
            } else if (lower.endsWith("points")) {
                String num = lower.substring(0, lower.length() - 6);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.POINTS);
            } else if (lower.endsWith("point")) {
                String num = lower.substring(0, lower.length() - 5);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.POINTS);
            } else if (lower.endsWith("xp")) {
                String num = lower.substring(0, lower.length() - 2);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.POINTS);
            } else if (lower.endsWith("l")) {
                String num = lower.substring(0, lower.length() - 1);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.LEVELS);
            } else if (lower.endsWith("p")) {
                String num = lower.substring(0, lower.length() - 1);
                return new ParsedXp(parseClampedInt(num, amountStr), XpUnit.POINTS);
            } else {
                return new ParsedXp(parseClampedInt(lower, amountStr), XpUnit.POINTS);
            }
        }
    }

    private static int parseClampedInt(String str, String original) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid amount: '" + original + "'. Must be a whole number.");
        }
        try {
            BigInteger big = new BigInteger(str.trim());
            if (big.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                return Integer.MAX_VALUE;
            } else if (big.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                return Integer.MIN_VALUE;
            }
            return big.intValue();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: '" + original + "'. Must be a whole number.");
        }
    }
}
