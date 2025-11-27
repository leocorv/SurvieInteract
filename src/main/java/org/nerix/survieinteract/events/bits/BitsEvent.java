package org.nerix.survieinteract.events.bits;

import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.network.BitsEffectPacket;
import org.nerix.survieinteract.events.EventHandler;

public class BitsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {

        JsonObject value = json.getAsJsonObject("value");
        if (value == null) return;

        int bits = value.get("bits").getAsInt();
        bits = Math.min(bits, 10000);

        float ratio = bits / 10000f;
        float radius = 1f - ratio;   // 1 = full vision, 0 = tunnel extrême

        int duration = 1200; // 60 sec

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        server.execute(() -> {

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (!ConfigManager.getConsent(p.getUuid())) continue;
                if (ConfigManager.getLives(p.getUuid()) <= 0) continue;

                BitsEffectPacket.send(p, radius, duration);
            }
        });
    }
}
