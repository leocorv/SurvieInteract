package org.nerix.survieinteract.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ReviveCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {

        dispatcher.register(
                literal("revive")
                        .requires(source -> source.hasPermissionLevel(3)) // OP uniquement
                        .then(argument("player", StringArgumentType.word())
                                .executes(ctx -> {

                                    String name = StringArgumentType.getString(ctx, "player");
                                    ServerPlayerEntity target = ctx.getSource().getServer().getPlayerManager().getPlayer(name);

                                    if (target == null) {
                                        ctx.getSource().sendMessage(Text.literal("Joueur introuvable."));
                                        return 0;
                                    }

                                    ConfigManager.addLives(target.getUuid(), 1);
                                    target.changeGameMode(net.minecraft.world.GameMode.SURVIVAL);

                                    target.sendMessage(Text.literal("Tu as récupéré la vie que tu as perdu."), false);
                                    ctx.getSource().sendMessage(Text.literal("Le joueur a récupéré la vie qu'il a perdu."));

                                    return 1;
                                })
                        )
        );
    }
}
