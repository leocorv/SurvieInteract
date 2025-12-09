package org.nerix.survieinteract;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Msg {

    private static final String PREFIX = "[SurvieInteract] ";

    // Message global en vert
    public static void global(MinecraftServer server, String msg) {
        if (server == null) return;
        server.getPlayerManager().broadcast(
                Text.literal(PREFIX + msg).formatted(Formatting.GREEN),
                false
        );
    }
    // Message global excluant un joueur spécifique
    public static void global(MinecraftServer server, String msg, ServerPlayerEntity excluded) {
        if (server == null) return;

        Text t = Text.literal(PREFIX + msg).formatted(Formatting.GREEN);

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (excluded != null && p.getUuid().equals(excluded.getUuid())) continue;
            p.sendMessage(t, false);
        }
    }

    // Message individuel en violet/rose
    public static void player(ServerPlayerEntity player, String msg) {
        if (player == null) return;
        player.sendMessage(
                Text.literal(PREFIX + msg).formatted(Formatting.LIGHT_PURPLE),
                false
        );
    }
}
