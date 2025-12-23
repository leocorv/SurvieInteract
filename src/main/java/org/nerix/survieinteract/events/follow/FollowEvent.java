package org.nerix.survieinteract.events.follow;

import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Msg;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.db.FollowersDb;
import org.nerix.survieinteract.events.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FollowEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {
        System.out.println("[SurvieInteract] Follow event reçu: " + json);

        MinecraftServer server = Survieinteract.getServer();
        if (server == null) return;

        String viewer = json.get("value").getAsString();
        String event_name = json.get("event_name").getAsString();

        Survieinteract.getServer().execute(() -> {
            boolean isNew = FollowersDb.registerFollowerIfNew(viewer);
            if (isNew) {
                int total = ConfigManager.incrementFollowCounter();
                System.out.println("[SurvieInteract] Nouveau follow enregistré: " + viewer +
                        " | compteur = " + total);

                // Tous les 10 follows → tentative +1 vie
                if (total % 10 == 0) {
                    distributeLifeFromFollow(server, viewer);
                }
            } else {
                System.out.println("[SurvieInteract] Follow déjà connu en DB: " + viewer);
                return;
            }

            ServerPlayerEntity target = pickTarget();

            if (target != null && ConfigManager.getConsent(target.getUuid()) && target.getGameMode() != GameMode.SPECTATOR) {
                spawnTNT(target, viewer);
            }
        });
    }

    private ServerPlayerEntity pickTarget() {
        var server = Survieinteract.getServer();
        if (server == null) return null;

        List<ServerPlayerEntity> candidates = new ArrayList<>();

        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (ConfigManager.getConsent(p.getUuid()) && ConfigManager.getLives(p.getUuid()) > 0) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty())
            return null;

        return candidates.get(new Random().nextInt(candidates.size()));
    }

    private void spawnTNT(ServerPlayerEntity p, String viewerName) {

        ServerWorld world = p.getEntityWorld();

        BlockPos pos = p.getBlockPos();

        EntityType.TNT.spawn(
                world,
                null,
                pos,
                SpawnReason.TRIGGERED,
                true,
                false
        );

        Msg.player(p,viewerName + " -> " + "TNT surprise ! :)");

        Msg.global(world.getServer(),viewerName + " a follow → TNT sur " + p.getName().getString(), p);
    }

    // SECTION POUR LA GESTION DE 10 FOLLOWS GIVE DE VIE

    private void distributeLifeFromFollow(MinecraftServer server, String viewer) {

        int defaultLives = ConfigManager.getDefaultLives();

        // 1) on récupère les joueurs éligibles
        List<ServerPlayerEntity> all = server.getPlayerManager().getPlayerList();
        List<ServerPlayerEntity> candidates = new ArrayList<>();

        for (ServerPlayerEntity p : all) {
            if (!ConfigManager.getConsent(p.getUuid())) continue;
            int lives = ConfigManager.getLives(p.getUuid());
            if (lives <= 0) continue;              // morts exclus
            if (lives >= defaultLives) continue;   // déjà au max via Twitch → exclus
            candidates.add(p);
        }

        if (candidates.isEmpty()) {
            System.out.println("[SurvieInteract] Distribution de vie: aucun joueur éligible.");
            return;
        }

        // 2) on cherche le nombre de vies minimum parmi eux
        int minLives = Integer.MAX_VALUE;
        for (ServerPlayerEntity p : candidates) {
            int l = ConfigManager.getLives(p.getUuid());
            if (l < minLives) {
                minLives = l;
            }
        }

        // 3) on garde uniquement ceux qui ont ce minimum
        List<ServerPlayerEntity> pool = new ArrayList<>();
        for (ServerPlayerEntity p : candidates) {
            if (ConfigManager.getLives(p.getUuid()) == minLives) {
                pool.add(p);
            }
        }

        if (pool.isEmpty()) {
            System.out.println("[SurvieInteract] Distribution de vie: pool vide (logique pétée ?).");
            return;
        }

        // 4) tirage au sort
        Random rand = new Random();
        ServerPlayerEntity chosen = pool.get(rand.nextInt(pool.size()));

        int current = ConfigManager.getLives(chosen.getUuid());
        int newLives = Math.min(current + 1, defaultLives);
        if (newLives == current) {
            // sécurité, normalement impossible vu les filtres
            return;
        }

        ConfigManager.setLives(chosen.getUuid(), newLives);

        String name = chosen.getName().getString();

        Msg.player(chosen, "Grâce aux follows Twitch, tu gagnes +1 vie (" +
                newLives + "/" + defaultLives + ").");

        Msg.global(server,
                 "Grace aux follows, " + name+" a reçus +1 vie :)",
                chosen
        );

        System.out.println("[SurvieInteract] Distribution de vie → " +
                name + " passe de " + current + " à " + newLives + " vies.");
    }

}
