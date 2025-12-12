package org.nerix.survieinteract.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.nerix.survieinteract.client.effects.BitsEffectManager;
import org.nerix.survieinteract.client.effects.BitsEffectRenderer;
import org.nerix.survieinteract.entity.ModEntities;
import org.nerix.survieinteract.network.BitsEffectPacket;
import org.nerix.survieinteract.client.render.SubStalkerRenderer;

public class SurvieinteractClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        PayloadTypeRegistry.playS2C().register(BitsEffectPacket.ID, BitsEffectPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(
                BitsEffectPacket.ID,
                (payload, context) -> {
                    BitsEffectPacket p = (BitsEffectPacket) payload;
                    context.client().execute(() -> {
                        BitsEffectManager.activate(p.radius, p.duration);
                    });
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> BitsEffectManager.tick());

        BitsEffectRenderer.init();

        EntityRendererRegistry.register(ModEntities.SUB_STALKER, SubStalkerRenderer::new);
    }
}
