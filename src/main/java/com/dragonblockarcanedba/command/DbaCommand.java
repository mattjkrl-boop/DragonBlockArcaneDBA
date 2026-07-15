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
                                
                                // Preserve Stats, XP, AP (they are not wiped)
                                // Wipe technique tree (Form Mastery map)
                                for (Identifier formId : DbaRegistries.getAllFormIds()) {
                                    accessor.dba$setFormMastery(formId, 0.0);
                                }
                                
                                // Clear active form just in case they aren't compatible
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
        );
    }
}
