package org.nerix.survieinteract.client.effects;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

public class BitsEffectRenderer {

    private static final Identifier MASK =
            Identifier.of("survieinteract", "textures/mask/radial_mask.png");

    public static void init() {

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {

            if (!BitsEffectManager.isActive()) return;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null) return;

            int w = client.getWindow().getScaledWidth();
            int h = client.getWindow().getScaledHeight();

            float radius = BitsEffectManager.getRadius();
            float scale = 0.20f + (radius * 0.80f);

            int drawW = (int) (w / scale);
            int drawH = (int) (h / scale);

            // drawTexture de 1.21.9 directement
            drawContext.drawTexture(
                    MASK,
                    (w - drawW) / 2,
                    (h - drawH) / 2,
                    0f,
                    0f,
                    drawW,
                    drawH,
                    drawW,
                    drawH
            );
        });
    }
}
