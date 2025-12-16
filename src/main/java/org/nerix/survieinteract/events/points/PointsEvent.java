package org.nerix.survieinteract.events.points;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import java.util.EnumSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import java.util.Set;
import net.minecraft.world.Heightmap;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Msg;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.events.EventHandler;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

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
