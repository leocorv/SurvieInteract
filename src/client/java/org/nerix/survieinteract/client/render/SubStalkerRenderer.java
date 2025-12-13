package org.nerix.survieinteract.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.feature.PlayerHeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.*;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.nerix.survieinteract.entity.SubStalkerEntity;

import java.util.UUID;

public class SubStalkerRenderer extends MobEntityRenderer<SubStalkerEntity, PlayerEntityRenderState, PlayerEntityModel> {

    public SubStalkerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
// ===== ARMURE =====
        // En 1.21.9 ArmorFeatureRenderer prend EquipmentModelData + EquipmentRenderer (plus de ArmorEntityModel)
        var playerEquipmentModels = bakeBipedEquipmentModels(ctx, EntityModelLayers.PLAYER_EQUIPMENT);

        this.addFeature(new ArmorFeatureRenderer<>(
                this,
                playerEquipmentModels,
                ctx.getEquipmentRenderer()
        ));

        // ===== ITEM EN MAIN =====
        // En 1.21.9 HeldItemFeatureRenderer ne prend plus HeldItemRenderer via le Context
        this.addFeature(new HeldItemFeatureRenderer<>(this));
    }

    private static EquipmentModelData<BipedEntityModel<PlayerEntityRenderState>> bakeBipedEquipmentModels(
            EntityRendererFactory.Context ctx,
            EquipmentModelData<EntityModelLayer> layers
    ) {
        // EquipmentModelData est un "record" (helmet/chestplate/leggings/boots)
        return new EquipmentModelData<>(
                new BipedEntityModel<>(ctx.getPart(layers.head())),
                new BipedEntityModel<>(ctx.getPart(layers.chest())),
                new BipedEntityModel<>(ctx.getPart(layers.legs())),
                new BipedEntityModel<>(ctx.getPart(layers.feet()))
        );
    }

    @Override
    public PlayerEntityRenderState createRenderState() {
        return new PlayerEntityRenderState();
    }

    @Override
    public void updateRenderState(SubStalkerEntity entity, PlayerEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);

        var ownerUuid = entity.getOwnerUuid().orElse(entity.getUuid());
        var client = net.minecraft.client.MinecraftClient.getInstance();

        net.minecraft.entity.player.SkinTextures textures = null; // <-- adapte le package selon TES mappings

        if (client.getNetworkHandler() != null) {
            var entry = client.getNetworkHandler().getPlayerListEntry(ownerUuid);
            if (entry != null) {
                textures = entry.getSkinTextures();
            }
        }

        if (textures == null) {
            textures = DefaultSkinHelper.getSkinTextures(ownerUuid); // fallback
        }

        state.skinTextures = textures;

        // Sinon tu risques de ne voir que la “base” sans couches (veste, manches, chapeau…)
        state.hatVisible = true;
        state.jacketVisible = true;
        state.leftSleeveVisible = true;
        state.rightSleeveVisible = true;
        state.leftPantsLegVisible = true;
        state.rightPantsLegVisible = true;

        state.equippedHeadStack  = entity.getEquippedStack(EquipmentSlot.HEAD);
        state.equippedChestStack = entity.getEquippedStack(EquipmentSlot.CHEST);
        state.equippedLegsStack  = entity.getEquippedStack(EquipmentSlot.LEGS);
        state.equippedFeetStack  = entity.getEquippedStack(EquipmentSlot.FEET);

        ArmedEntityRenderState.updateRenderState(
                entity,
                state,
                MinecraftClient.getInstance().getItemModelManager()
        );
    }

    @Override
    public Identifier getTexture(PlayerEntityRenderState state) {
        if (state.skinTextures != null) {
            return state.skinTextures.body().texturePath();
        }
        return DefaultSkinHelper.getTexture(); // fallback
    }
}
