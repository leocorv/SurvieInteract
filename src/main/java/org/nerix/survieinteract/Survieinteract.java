package org.nerix.survieinteract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import org.nerix.survieinteract.commands.ConsentCommand;
import org.nerix.survieinteract.commands.DesactivateTypeCommand;
import org.nerix.survieinteract.events.EventRouter;
import org.nerix.survieinteract.events.bits.BitsEvent;
import org.nerix.survieinteract.events.follow.FollowEvent;
import org.nerix.survieinteract.events.points.PointsEvent;
import org.nerix.survieinteract.events.raid.RaidEvent;
import org.nerix.survieinteract.events.sub.SubEvent;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Survieinteract implements ModInitializer {

    private static final int BROKER_PORT = 25570;
    private static MinecraftServer serverInstance;

    public static MinecraftServer getServer() {
        return serverInstance;
    }

    @Override
    public void onInitialize() {

        System.out.println("[SurvieInteract] Init…");

        // config
        ConfigManager.init();

        // commandes
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            ConsentCommand.register(dispatcher);
            DesactivateTypeCommand.register(dispatcher);
        });

        // message / gestion vie à la connexion
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;

            int lives = ConfigManager.getLives(player.getUuid());

            // Message d'accueil
            player.sendMessage(Text.literal("[SurvieInteract] Utilise /consent [on/off] pour activer les interactions Twitch."), false);

            // Gestion du mode spectateur
            if (lives <= 0) {
                player.changeGameMode(GameMode.SPECTATOR);
                player.sendMessage(Text.literal("Tu es à 0 vie. Tu restes en spectateur permanent."), false);
            } else {
                player.changeGameMode(GameMode.SURVIVAL);
                player.sendMessage(Text.literal("Vies restantes : " + lives), false);
            }
        });


        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {

            // Mort réelle = alive == false
            if (!alive) {
                ConfigManager.decrementLife(newPlayer.getUuid());

                int lives = ConfigManager.getLives(newPlayer.getUuid());

                newPlayer.sendMessage(Text.literal("Vous avez perdu une vie. Il vous reste " + lives + " vies."), false);

                if (lives <= 0) {
                    newPlayer.changeGameMode(GameMode.SPECTATOR);
                    newPlayer.sendMessage(Text.literal("Vous êtes mort définitivement."), false);
                }
            }
        });

        // routing events
        EventRouter.register("follow", new FollowEvent());
        EventRouter.register("sub", new SubEvent());
        EventRouter.register("bits", new BitsEvent());
        EventRouter.register("raid", new RaidEvent());
        EventRouter.register("points", new PointsEvent());

        // serveur prêt → lancer listener broker
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverInstance = server;
            startBrokerListener();
        });
    }

    private void startBrokerListener() {
        Thread t = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(BROKER_PORT)) {
                System.out.println("[SurvieInteract] En écoute sur port " + BROKER_PORT);

                while (true) {
                    Socket socket = ss.accept();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        handleIncomingJson(line);
                    }

                    socket.close();
                }
            } catch (Exception e) {
                System.out.println("[SurvieInteract] Erreur socket: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handleIncomingJson(String jsonLine) {
        try {
            JsonObject json = JsonParser.parseString(jsonLine).getAsJsonObject();
            EventRouter.dispatch(json);
        } catch (Exception e) {
            System.out.println("[SurvieInteract] JSON invalide : " + jsonLine);
        }
    }
}
