package org.nerix.survieinteract.events.bits;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Msg;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.network.BitsEffectPacket;
import org.nerix.survieinteract.events.EventHandler;

public class BitsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Bits event reçu: " + json);

        JsonObject value = json.getAsJsonObject("value");
        if (value == null) return;

        String viewer = value.get("user").getAsString();
        int bits = value.get("bits").getAsInt();
        bits = Math.min(bits, 10000);

        float ratio = bits / 10000f;
        float radius = ratio;   // nouveau : radius augmente avec bits

        int duration = 600; // 30 sec

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        final int f_bits = bits;

        server.execute(() -> {

            System.out.println("[SurvieInteract] " + viewer + " a donné " + f_bits + " bits.");

            Msg.global(server,viewer + " a envoyé " + f_bits + " bits !\nBonne chance avec le côté sombre...");

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!ConfigManager.getConsent(p.getUuid())) continue;
                if (ConfigManager.getLives(p.getUuid()) <= 0) continue;

                ServerPlayNetworking.send(p, new BitsEffectPacket(radius, duration));
            }
        });
    }
}
