package org.nerix.survieinteract.events.sub;

import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Msg;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.entity.ModEntities;
import org.nerix.survieinteract.entity.SubStalkerEntity;
import org.nerix.survieinteract.events.EventHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SubEvent implements EventHandler {

    // Types possibles pour le BOSS
    private static final List<EntityType<? extends MobEntity>> BOSS_TYPES = List.of(
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.PILLAGER,
            EntityType.VINDICATOR,
            EntityType.SPIDER
    );

    // Types possibles pour les mobs “minions”
    private static final List<EntityType<? extends MobEntity>> MINION_TYPES = List.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CAVE_SPIDER,
            EntityType.WITCH,
            EntityType.PILLAGER
    );

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] SUB event reçu: " + json);

        String eventName = json.get("event_name").getAsString();
        JsonObject value = json.getAsJsonObject("value");
        if (value == null) {
            System.out.println("[SurvieInteract] SUB event sans champ 'value', ignoré.");
            return;
        }

        switch (eventName) {
            case "gift" -> handleGift(value);
            case "new_sub", "resub" -> handleSubOrResub(eventName, value);
            default -> System.out.println("[SurvieInteract] Type de sub inconnu: " + eventName);
        }
    }

    // =========== SUB / RESUB → STALKER ===========

    private void handleSubOrResub(String kind, JsonObject value) {
        String viewer = value.get("user").getAsString();
        String tier = value.has("tier") ? value.get("tier").getAsString() : "?";

        int months = 0;
        if (kind.equals("resub") && value.has("months")) {
            months = value.get("months").getAsInt();
        }

        final int mois = months;

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        server.execute(() -> {
            ServerPlayerEntity target = pickTarget(server);
            if (target == null) {
                System.out.println("[SurvieInteract] sub/resub ignoré : aucun joueur consentant / vivant.");
                return;
            }

            ServerWorld world = target.getEntityWorld();
            BlockPos basePos = target.getBlockPos().add(2, 0, 0); // à côté

            SubStalkerEntity stalker = ModEntities.SUB_STALKER.create(world, SpawnReason.TRIGGERED);
            if (stalker == null) {
                System.out.println("[SurvieInteract] Impossible de créer SubStalkerEntity.");
                return;
            }

            stalker.refreshPositionAndAngles(
                    basePos.getX() + 0.5,
                    basePos.getY(),
                    basePos.getZ() + 0.5,
                    world.random.nextFloat() * 360.0f,
                    0.0f
            );

            stalker.setOwner(target);

            world.spawnEntity(stalker);

            String msgGlobal;
            if (kind.equals("new_sub")) {
                msgGlobal = viewer + " vient de sub (tier " + tier + ") → un clone sombre est apparu.";
            } else {
                msgGlobal = viewer + " s'est resub (" + mois + " mois, tier " + tier + ") → un clone sombre te traque.";
            }

            Msg.global(server, msgGlobal);
            Msg.player(target, "Un double tordu portant ton visage rôde dans les environs…");
        });
    }

    private ServerPlayerEntity pickTarget(MinecraftServer server) {
        List<ServerPlayerEntity> candidates = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!ConfigManager.getConsent(p.getUuid())) continue;
            if (ConfigManager.isDead(p.getUuid())) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            candidates.add(p);
        }
        if (candidates.isEmpty()) return null;

        Collections.shuffle(candidates, new Random());
        return candidates.getFirst();
    }

    // ============================
    //         GIFT HANDLER
    // ============================

    private void handleGift(JsonObject value) {
        String gifter = value.get("gifter").getAsString();
        final int total = value.has("total") ? value.get("total").getAsInt() : 1;
        String tier = value.has("tier") ? value.get("tier").getAsString() : "?";

        // Hard cap pour éviter de transformer ton serveur en PowerPoint
        int minionsTotal = Math.max(1, Math.min(total, 50));

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        server.execute(() -> {

            var playerList = server.getPlayerManager().getPlayerList();
            if (playerList.isEmpty()) {
                System.out.println("[SurvieInteract] Gift ignoré : aucun joueur en ligne.");
                return;
            }

            List<ServerPlayerEntity> candidates = new ArrayList<>();
            for (ServerPlayerEntity p : playerList) {
                if (!ConfigManager.getConsent(p.getUuid())) continue;
                if (ConfigManager.isDead(p.getUuid())) continue;
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                candidates.add(p);
            }

            if (candidates.isEmpty()) {
                System.out.println("[SurvieInteract] Gift ignoré : aucun joueur consentant / vivant.");
                return;
            }

            // On prend la moitié des joueurs (au moins 1)
            Collections.shuffle(candidates, new Random());
            int groupCount = Math.max(1, candidates.size() / 2);
            List<ServerPlayerEntity> targets = candidates.subList(0, groupCount);

            Msg.global(server, gifter + " a offert " + total + " sub(s) (tier " + tier + ") → des hordes apparaissent !");

            for (ServerPlayerEntity target : targets) {
                ServerWorld world = target.getEntityWorld();
                BlockPos basePos = target.getBlockPos();

                spawnBossAndMinions(world, basePos, gifter, minionsTotal, target);

                Msg.player(target,
                        "Les subs de " + gifter + " ont invoqué une horde près de toi… bonne chance.");
            }
        });
    }

    private void spawnBossAndMinions(ServerWorld world,
                                     BlockPos center,
                                     String gifter,
                                     int minionCount, ServerPlayerEntity target) {

        Random random = new Random();

        // ---------- BOSS ----------
        EntityType<? extends MobEntity> bossType =
                BOSS_TYPES.get(random.nextInt(BOSS_TYPES.size()));

        BlockPos bossPos = findSpawnPos(world, center, random);
        MobEntity boss = bossType.spawn(
                world,
                bossPos,
                SpawnReason.TRIGGERED
        );

        if (boss != null) {
            boss.setTarget(target);

            // Buffs, HP, nom, etc.
            if (boss instanceof LivingEntity living) {

                // 20 coeurs = 40 HP
                EntityAttributeInstance maxHealth = living.getAttributeInstance(EntityAttributes.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(40.0);
                    living.setHealth(40.0F);
                }

                // Buffs (durée : 60s)
                int dur = 20 * 6300;
                int radm_status_effect = random.nextInt(4);
                List<RegistryEntry<StatusEffect>> list_status_effects = new ArrayList<>();
                list_status_effects.add(StatusEffects.SPEED);
                list_status_effects.add(StatusEffects.STRENGTH);
                list_status_effects.add(StatusEffects.RESISTANCE);
                list_status_effects.add(StatusEffects.JUMP_BOOST);

                living.addStatusEffect(new StatusEffectInstance(list_status_effects.get(radm_status_effect), dur, 2));

                living.setCustomName(Text.literal("Boss de " + gifter));
                living.setCustomNameVisible(true);
            }

            boss.setPersistent();
            System.out.println("[SurvieInteract] Boss spawné pour gifter=" + gifter +" sur " + target.getName().getString() +" en " + bossPos.getX() + " " + bossPos.getY() + " " + bossPos.getZ());
        }

        // ---------- MINIONS ----------
        minionCount = Math.max(0, Math.min(minionCount, 40)); // petite sécurité

        for (int i = 0; i < minionCount; i++) {
            EntityType<? extends MobEntity> type =
                    MINION_TYPES.get(random.nextInt(MINION_TYPES.size()));

            BlockPos pos = findSpawnPos(world, center, random);
            MobEntity mob = type.spawn(
                    world,
                    pos,
                    SpawnReason.TRIGGERED
            );

            if (mob != null) {
                mob.setTarget(target);
                System.out.println("[SurvieInteract] Minion " + type +" spawné pour " + target.getName().getString() +" en " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
            }
        }
    }

    /**
     * Cherche une position de spawn "potable" autour du joueur.
     * Rien de magique, juste quelques essais raisonnables.
     */
    private BlockPos findSpawnPos(ServerWorld world, BlockPos center, Random random) {

        for (int i = 0; i < 12; i++) {
            int dx = random.nextInt(16) - 8;   // [-8 ; +8]
            int dz = random.nextInt(16) - 8;

            BlockPos tentative = center.add(dx, 0, dz);

            // On remonte au dessus du sol
            BlockPos top = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    tentative
            );

            // Juste un check basique : bloc d'air au niveau de spawn
            if (world.isAir(top)) {
                return top;
            }
        }

        // Fallback bourrin : on spawn sur le joueur si on a rien trouvé
        return center;
    }
}
