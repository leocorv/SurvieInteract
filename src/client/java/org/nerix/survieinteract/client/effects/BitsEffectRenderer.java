package org.nerix.survieinteract.client.effects;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
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

            float fade = BitsEffectManager.getFadeFactor();
            float radius = BitsEffectManager.getRadius() * fade;
            radius = Math.max(0f, Math.min(1f, radius));

            int alpha = (int) (fade * 255);
            int color = (alpha << 24) | 0xFFFFFF;

//            float scale = 1f - (radius * 0.90f);
            float scale = radius * 0.95f;

            int drawW = (int) (w / scale);
            int drawH = (int) (h / scale);

            int x = (w - drawW) / 2;
            int y = (h - drawH) / 2;

            drawContext.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    MASK,
                    x, y,
                    0f, 0f,
                    drawW, drawH,
                    drawW, drawH,
                    drawW, drawH,
                    color
            );

        });
    }
}
