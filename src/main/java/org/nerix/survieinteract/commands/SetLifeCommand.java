package org.nerix.survieinteract.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class SetLifeCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
                literal("setlife")
                        .requires(source -> source.hasPermissionLevel(3)) // OP uniquement
                        .then(argument("player", StringArgumentType.word())
                                .then(argument("lives", IntegerArgumentType.integer(0, 99))
                                        .executes(ctx -> {

                                            String name = StringArgumentType.getString(ctx, "player");
                                            int lives = IntegerArgumentType.getInteger(ctx, "lives");

                                            ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);

                                            if (target == null) {
                                                ctx.getSource().sendMessage(Text.literal("Joueur introuvable."));
                                                return 0;
                                            }

                                            ConfigManager.setLives(target.getUuid(), lives);

                                            if (lives <= 0) {
                                                target.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                                            } else {
                                                target.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);
                                            }

                                            ctx.getSource().sendMessage(Text.literal("Vies de " + name + " définies à " + lives));
                                            target.sendMessage(Text.literal("Ton total de vies a été mis à " + lives), false);

                                            return 1;
                                        })
                                )
                        )
        );
    }
}
