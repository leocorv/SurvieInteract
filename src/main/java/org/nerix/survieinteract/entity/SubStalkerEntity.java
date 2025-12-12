package org.nerix.survieinteract.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.nerix.survieinteract.ConfigManager;

import java.util.Optional;
import java.util.UUID;

public class SubStalkerEntity extends HostileEntity {

    private static final double AGGRO_RANGE = 50.0;

    // On stocke l’UUID en String
    private static final TrackedData<String> OWNER_UUID =
            DataTracker.registerData(SubStalkerEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> OWNER_NAME =
            DataTracker.registerData(SubStalkerEntity.class, TrackedDataHandlerRegistry.STRING);

    public SubStalkerEntity(EntityType<? extends SubStalkerEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    // Attributs de base
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, 40.0)          // 20 coeurs
                .add(EntityAttributes.MOVEMENT_SPEED, 0.35)      // proche sprint
                .add(EntityAttributes.ATTACK_DAMAGE, 7.0)
                .add(EntityAttributes.FOLLOW_RANGE, AGGRO_RANGE);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, "");
        builder.add(OWNER_NAME, "");
    }

    // ====== liaison au joueur “copié” ======

    public void setOwner(ServerPlayerEntity player) {
        this.dataTracker.set(OWNER_UUID, player.getUuidAsString());
        this.dataTracker.set(OWNER_NAME, player.getGameProfile().name());
    }

    public Optional<UUID> getOwnerUuid() {
        String raw = this.dataTracker.get(OWNER_UUID);
        if (raw == null || raw.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String getOwnerName() {
        return this.dataTracker.get(OWNER_NAME);
    }

    // ====== IA / objectifs ======

    @Override
    protected void initGoals() {
        super.initGoals();
        // On laisse les goals vanilla, on gère juste la target dans tickMovement
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        if (!this.getEntityWorld().isClient()) {
            updateTargetLogic();
        }
    }

    private void updateTargetLogic() {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        LivingEntity current = this.getTarget();
        PlayerEntity currentTarget = current instanceof PlayerEntity p ? p : null;

        // 1) target prioritaire : le joueur “copié”
        PlayerEntity locked = null;
        Optional<UUID> ownerOpt = getOwnerUuid();
        if (ownerOpt.isPresent()) {
            locked = serverWorld.getPlayerByUuid(ownerOpt.get());
        }

        if (locked != null
                && isValidTarget(locked)
                && this.squaredDistanceTo(locked) <= AGGRO_RANGE * AGGRO_RANGE) {

            if (currentTarget != locked) {
                this.setTarget(locked);
            }
            return;
        }

        // 2) sinon, le joueur valide le plus proche dans le rayon
        PlayerEntity closest = findClosestValidPlayer(serverWorld);
        if (closest != null) {
            if (currentTarget != closest) {
                this.setTarget(closest);
            }
        } else {
            this.setTarget(null);
        }
    }

    private PlayerEntity findClosestValidPlayer(ServerWorld world) {
        Box box = this.getBoundingBox().expand(AGGRO_RANGE);
        double best = Double.MAX_VALUE;
        PlayerEntity candidate = null;

        for (PlayerEntity p : world.getEntitiesByClass(PlayerEntity.class, box, this::isValidTarget)) {
            double d = this.squaredDistanceTo(p);
            if (d < best) {
                best = d;
                candidate = p;
            }
        }
        return candidate;
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity player)) return false;
        if (player.isSpectator()) return false;

        if (!ConfigManager.getConsent(player.getUuid())) return false;
        if (ConfigManager.isDead(player.getUuid())) return false;

        return true;
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return isValidTarget(target) && super.canTarget(target);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
