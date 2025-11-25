package org.nerix.survieinteract.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;

import static net.minecraft.server.command.CommandManager.literal;


public class ConsentCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("consent")
                        .then(literal("on").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            ConfigManager.setConsent(p.getUuid(), true);
                            p.sendMessage(Text.literal("Vivre les events du stream: ON"), false);
                            return 1;
                        }))
                        .then(literal("off").executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            ConfigManager.setConsent(p.getUuid(), false);
                            p.sendMessage(Text.literal("Vivre les events du stream: OFF"), false);
                            return 1;
                        }))
        );
    }
}
