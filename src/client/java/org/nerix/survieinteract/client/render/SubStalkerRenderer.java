package org.nerix.survieinteract.client.render;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.nerix.survieinteract.entity.SubStalkerEntity;

public class SubStalkerRenderer extends MobEntityRenderer<SubStalkerEntity, PlayerEntityRenderState, PlayerEntityModel> {

    public SubStalkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    @Override
    public void updateRenderState(SubStalkerEntity entity, PlayerEntityRenderState state, float tickDelta) {
        // On laisse la logique vanilla faire le gros du boulot
        super.updateRenderState(entity, state, tickDelta);
        // plus tard: tu pourras foncer la couleur, ajouter des effets, etc.
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        // Pas de skin custom pour l'instant → skin par défaut
        return DefaultSkinHelper.getTexture();
    }
}
