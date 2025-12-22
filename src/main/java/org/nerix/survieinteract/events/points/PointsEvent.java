package org.nerix.survieinteract.events.points;

import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;

import java.util.*;

import net.minecraft.network.packet.s2c.play.PositionFlag;

import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Msg;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.events.EventHandler;

import java.util.EnumSet;

public class PointsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Points event reçu: " + json);

        JsonObject value = json.getAsJsonObject("value");
        if (value == null) return;

        String viewer = value.get("user").getAsString();
        String reward = value.get("reward").getAsString();

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        server.execute(() -> {

            switch (reward.toLowerCase()) {

                case "faites glisser !":
                    handleFaitesGlisser(viewer);
                    break;

                case "switch !":
                    handlePermutationSauvage(viewer);
                    break;

                case "paranoia":
                    handleParanoia(viewer);
                    break;

                case "roulette d'effet":
                    handlePotionRoulette(viewer);
                    break;

                case "inventaire en pagaille":
                    handleInventoryShuffle(viewer);
                    break;

                case "hoquet spatial":
                    handleHoquetSpatial(viewer);
                    break;

                case "aimant inversé":
                    handleRepulseur(viewer);
                    break;

                // futur : d’autres rewards ici
                // case "invisibilité totale":
                //     handleInvisibilite(viewer);
                //     break;

                default:
                    System.out.println("[SurvieInteract] Reward points ignorée : " + reward);
                    break;
            }
        });
    }

    /* =====================================================
                     LOGIQUE "Faites glisser !"
                     DROP D'UN SLOT
       ===================================================== */

    private void handleFaitesGlisser(String viewer) {

        ServerPlayerEntity target = pickTarget();
        if (target == null) return;

        Boolean dropped = dropRandomSlot(target);

        String name = target.getName().getString();
        MinecraftServer server = Survieinteract.getServer();

        Random rand = new Random();

        //Le message est envoyé entre 4 à 10 secondes après le drop
        int nbr_tick = 20 * rand.nextInt(4,11);

        Survieinteract.scheduleInTicks(nbr_tick, () -> {
                // message joueur
                Msg.player(target, viewer + " t’a forcé à drop un slot ! (Il y a 4 à 10 secondes)");

                // message global
                Msg.global(server, viewer + " a fait drop un slot de " + name, target);
            }
        );
    }


    /* =====================================================
                  NOUVELLE LOGIQUE "Permutation sauvage"
                  SWAP / TP LOCAL
       ===================================================== */

    private void handlePermutationSauvage(String viewer) {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        List<ServerPlayerEntity> candidates = getCandidates();
        if (candidates.isEmpty()) {
            System.out.println("[SurvieInteract] 'Permutation sauvage' ignorée : aucun joueur consentant / vivant.");
            return;
        }

        Random rand = new Random();

        if (candidates.size() >= 2) {
            // On prend deux joueurs distincts
            ServerPlayerEntity a = candidates.get(rand.nextInt(candidates.size()));
            ServerPlayerEntity b = a;

            while (b == a && candidates.size() > 1) {
                b = candidates.get(rand.nextInt(candidates.size()));
            }

            if (b == a) {
                // Cas ultra rare : un seul joueur au final
                randomTeleportNear(a, 20);
                Msg.player(a, viewer + " t’a déstabilisé : téléportation chaotique proche de ta position.");
                Msg.global(server, viewer + " a utilisé 'Permutation sauvage' sur " + a.getName().getString(), a);
                return;
            }

            // Swap des positions
            ServerWorld worldA = a.getEntityWorld();
            ServerWorld worldB = b.getEntityWorld();

            Vec3d posA = a.getEntityPos();
            Vec3d posB = b.getEntityPos();

            float yawA = a.getYaw();
            float pitchA = a.getPitch();
            float yawB = b.getYaw();
            float pitchB = b.getPitch();

            Set<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class);

            a.teleport(worldB,
                    posB.getX(), posB.getY(), posB.getZ(),
                    flags,
                    yawB, pitchB,
                    false);

            b.teleport(worldA,
                    posA.getX(), posA.getY(), posA.getZ(),
                    flags,
                    yawA, pitchA,
                    false);

            String nameA = a.getName().getString();
            String nameB = b.getName().getString();

            Msg.player(a, viewer + " t’a échangé de place avec " + nameB + " !");
            Msg.player(b, viewer + " t’a échangé de place avec " + nameA + " !");
            Msg.global(server, viewer + " a utilisé 'Permutation sauvage' : " + nameA + " ↔ " + nameB);

        } else {
            // Un seul joueur possible → TP aléatoire à proximité
            ServerPlayerEntity target = candidates.get(0);
            randomTeleportNear(target, 20);

            Msg.player(target, viewer + " t’a téléporté aléatoirement à proximité…");
            Msg.global(server, viewer + " a utilisé 'Permutation sauvage' sur " + target.getName().getString(), target);
        }
    }

    private void randomTeleportNear(ServerPlayerEntity player, int radius) {
        if (radius < 1) radius = 1;

        ServerWorld world = player.getEntityWorld();
        BlockPos base = player.getBlockPos();
        Random rand = new Random();

        for (int i = 0; i < 16; i++) {
            int dx = rand.nextInt(-radius, radius + 1);
            int dz = rand.nextInt(-radius, radius + 1);

            BlockPos tentative = base.add(dx, 0, dz);

            BlockPos top = world.getTopPosition(
                    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    tentative
            );

            if (!world.isAir(top)) continue;

            Vec3d dest = Vec3d.ofBottomCenter(top);
            Set<PositionFlag> flags = EnumSet.noneOf(PositionFlag.class);

            player.teleport(
                    world,
                    dest.getX(), dest.getY(), dest.getZ(),
                    flags,
                    player.getYaw(),
                    player.getPitch(),
                    false
            );

            System.out.println("[SurvieInteract] TP proche pour " + player.getName().getString() +
                    " → " + dest.getX() + " " + dest.getY() + " " + dest.getZ());
            return;
        }

        System.out.println("[SurvieInteract] TP proche raté : aucune position safe trouvée pour " +
                player.getName().getString());
    }

    /* =====================================================
                    PARANOIA - SONS ALÉATOIRES
       ===================================================== */

    private void handleParanoia(String viewer) {

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

            for (ServerPlayerEntity p : candidates) {
                executeParanoia(viewer, p);
            }
        });
    }

    private void executeParanoia(String viewer, ServerPlayerEntity target) {

        ServerWorld world = target.getEntityWorld();
        Random rand = new Random();

        // direction derrière le joueur
        float yaw = target.getYaw();
        double rad = Math.toRadians(yaw);
        double fx = -Math.sin(rad);
        double fz = Math.cos(rad);
        double bx = -fx;
        double bz = -fz;

        double distance = 6.0;
        double sx = target.getX() + bx * distance;
        double sy = target.getEyeY();
        double sz = target.getZ() + bz * distance;

        int choice = rand.nextInt(4);
        var sound = switch (choice) {
            case 0 -> SoundEvents.ENTITY_ENDERMAN_SCREAM;
            case 1 -> SoundEvents.ENTITY_WARDEN_HEARTBEAT;
            case 2 -> SoundEvents.ENTITY_TNT_PRIMED;
            default -> SoundEvents.ENTITY_CREEPER_PRIMED;
        };

        world.playSound(
                null,
                sx, sy, sz,
                sound,
                SoundCategory.PLAYERS,
                1.0f,
                0.8f + rand.nextFloat() * 0.4f
        );

        System.out.println("[SurvieInteract] Paranoia → " + viewer +
                " a fait jouer un son derrière " + target.getName().getString());
    }

 /* =====================================================
                 ROULETTE DE POTION - 30 SECONDES
       ===================================================== */

    private void handlePotionRoulette(String viewer) {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        ServerPlayerEntity target = pickTarget();
        if (target == null) return;

        Random rand = new Random();
        int dur = 20 * 60; // 30s

        List<StatusEffectInstance> effects = new ArrayList<>();
        effects.add(new StatusEffectInstance(StatusEffects.LEVITATION, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.BLINDNESS, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.NAUSEA, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.SPEED, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.JUMP_BOOST, dur, 2));
        effects.add(new StatusEffectInstance(StatusEffects.NIGHT_VISION, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.DARKNESS, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.WITHER, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.POISON, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.GLOWING, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.NAUSEA, dur, 10));
        effects.add(new StatusEffectInstance(StatusEffects.ABSORPTION, dur, 5));
        effects.add(new StatusEffectInstance(StatusEffects.HASTE, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.INVISIBILITY, dur, 0));
        effects.add(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, dur, 3));
        effects.add(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.STRENGTH, dur, 2));
        effects.add(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.REGENERATION, dur, 2));
        effects.add(new StatusEffectInstance(StatusEffects.SLOWNESS, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, dur, 1));
        effects.add(new StatusEffectInstance(StatusEffects.HUNGER, dur, 0));

        StatusEffectInstance chosen = effects.get(rand.nextInt(effects.size()));
        target.addStatusEffect(chosen);

        Msg.player(target, viewer + " t’a appliqué un effet aléatoire pendant 60 secondes.");
        Msg.global(server, viewer + " a lancé une roulette d'effet sur " + target.getName().getString(), target);
    }

    /* =====================================================
                   INVENTAIRE EN PAGAILLE
       ===================================================== */

    private void handleInventoryShuffle(String viewer) {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        ServerPlayerEntity target = pickTarget();
        if (target == null) return;

        var inv = target.getInventory();

        int start = 0;
        int end = 36; // slots 0..35 = hotbar + main, on garde armure/offhand intacts

        List<ItemStack> stacks = new ArrayList<>(end - start);
        for (int i = start; i < end; i++) {
            stacks.add(inv.getStack(i).copy());
        }

        Collections.shuffle(stacks, new Random());

        for (int i = start; i < end; i++) {
            inv.setStack(i, stacks.get(i - start));
        }

        target.currentScreenHandler.sendContentUpdates();

        Msg.player(target, viewer + " a mis ton inventaire en pagaille.");
        Msg.global(server, viewer + " a mélangé l’inventaire de " + target.getName().getString(), target);
    }

    /* =====================================================
                        HOQUET SPATIAL
       ===================================================== */

    private void handleHoquetSpatial(String viewer) {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        ServerPlayerEntity target = pickTarget();
        if (target == null) return;

        Random rand = new Random();
        int seconds = rand.nextInt(60, 301); // entre 60 et 300s
        int durationTicks = seconds * 20;

        startHoquetEffect(target, durationTicks);

        Msg.player(target, viewer + " t’a donné le hoquet entre 60 et 300 secondes.");
        Msg.global(server, viewer + " a déclenché un hoquet sur " + target.getName().getString(), target);
    }

    private void startHoquetEffect(ServerPlayerEntity player, int ticksLeft) {
        if (ticksLeft <= 0) return;

        Random rand = new Random();
        int delay = 20 * rand.nextInt(2, 5);
        if (delay > ticksLeft) delay = ticksLeft;

        final int fdelay = delay;

        Survieinteract.scheduleInTicks(delay, () -> {
            if (!player.isAlive()) return;
            if (ConfigManager.isDead(player.getUuid())) return;

            // petit bump vertical
            player.addVelocity(0.0, 0.5, 0.0);

            startHoquetEffect(player, ticksLeft - fdelay);
        });
    }

     /* =====================================================
                        AIMANT INVERSÉ
       ===================================================== */

    private void handleRepulseur(String viewer) {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        ServerPlayerEntity target = pickTarget();
        if (target == null) return;

        Random rand = new Random();
        int seconds = rand.nextInt(60, 121); // 60 à 120s
        int durationTicks = seconds * 20;

        startRepulseEffect(target, durationTicks);

        Msg.player(target, viewer + " t’a transformé en aimant inversé pendant " + seconds + " secondes.");
        Msg.global(server, viewer + " a activé un aimant inversé sur " + target.getName().getString(), target);
    }

    private void startRepulseEffect(ServerPlayerEntity player, int ticksLeft) {
        if (ticksLeft <= 0) return;

        int delay = 5; // tick entre chaque “pulse” de répulsion
        if (delay > ticksLeft) delay = ticksLeft;

        final int fdelay = delay;

        Survieinteract.scheduleInTicks(delay, () -> {
            if (!player.isAlive()) return;
            if (ConfigManager.isDead(player.getUuid())) return;

            applyRepulsePulse(player);

            startRepulseEffect(player, ticksLeft - fdelay);
        });
    }

    private void applyRepulsePulse(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        double radius = 3.0;
        Vec3d center = player.getEntityPos();

        List<Entity> entities = world.getOtherEntities(player, player.getBoundingBox().expand(radius));

        for (Entity e : entities) {
            if (!e.isAlive()) continue;
            if (e instanceof ServerPlayerEntity) continue; // on évite de pousser les autres joueurs (ou tu enlèves cette ligne si tu veux le chaos total)

            Vec3d delta = e.getEntityPos().subtract(center);
            if (delta.lengthSquared() < 0.0001) {
                delta = new Vec3d(0.01, 0, 0);
            }

            Vec3d dir = delta.normalize();
            double strength = 0.6;

            Vec3d vel = new Vec3d(dir.x * strength, dir.y * 0.2, dir.z * strength);

            e.addVelocity(vel.x, vel.y, vel.z);
        }
    }

    /* =====================================================
                     FONCTIONS UTILITAIRES
       ===================================================== */

    private ServerPlayerEntity pickTarget() {
        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return null;

        List<ServerPlayerEntity> candidates = new ArrayList<>();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (ConfigManager.getConsent(p.getUuid()) && ConfigManager.getLives(p.getUuid()) > 0) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) return null;

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private boolean dropRandomSlot(ServerPlayerEntity player) {
        var inv = player.getInventory();
        int size = inv.size(); // ~41 slots

        if (size <= 0) return false;

        Random rand = new Random();
        ItemStack removed = ItemStack.EMPTY;

        for (int attempt = 0; attempt < 41 && (removed == ItemStack.EMPTY || removed.isEmpty()); attempt++) {

            int slot = rand.nextInt(size);
            ItemStack stack = inv.getStack(slot);

            if (!stack.isEmpty()) {
                removed = inv.removeStack(slot);

                System.out.println(
                        "[SurvieInteract] DROP LOG → " +
                                "Player=" + player.getName().getString() +
                                " | Slot=" + slot +
                                " | Item=" + removed.getItem().toString() +
                                " | Qty=" + removed.getCount()
                );

                if (!removed.isEmpty()) {
                    player.dropItem(removed, true, true);
                    System.out.println("[SurvieInteract] 'Faites glisser !' → Slot " +
                            slot + " droppé pour " + player.getName().getString());
                    return true;
                }
            } else {
                System.out.println(
                        "[SurvieInteract] DROP LOG → " +
                                "Player=" + player.getName().getString() +
                                " | Slot=" + slot +
                                " | Item=" + removed.getItem().toString() +
                                " | Qty=" + removed.getCount()
                );
            }
        }

        System.out.println("[SurvieInteract] 'Faites glisser !' → Aucun slot non vide (3 tentatives)");
        return false;
    }

    private List<ServerPlayerEntity> getCandidates() {
        MinecraftServer server = Survieinteract.getServer();
        List<ServerPlayerEntity> candidates = new ArrayList<>();
        if (server == null) return candidates;

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (!ConfigManager.getConsent(p.getUuid())) continue;
            if (ConfigManager.getLives(p.getUuid()) <= 0) continue;
            candidates.add(p);
        }

        return candidates;
    }

}
