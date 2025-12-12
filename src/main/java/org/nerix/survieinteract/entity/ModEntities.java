package org.nerix.survieinteract.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<SubStalkerEntity> SUB_STALKER;

    static {
        Identifier id = Identifier.of("survieinteract", "sub_stalker");
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, id);

        SUB_STALKER = Registry.register(
                Registries.ENTITY_TYPE,
                id,
                FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SubStalkerEntity::new)
                        .dimensions(EntityDimensions.fixed(0.6f, 1.95f)) // taille joueur
                        .trackRangeChunks(8)
                        .build(key)
        );
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(
                SUB_STALKER,
                SubStalkerEntity.createAttributes()
        );
    }
}
