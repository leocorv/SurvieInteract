package org.nerix.survieinteract.events.points;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.events.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointsEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {

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

        //Le message est envoyé entre 3 à 10 secondes après le drop
        int nbr_tick = 20 * rand.nextInt(3,11);

        Survieinteract.scheduleInTicks(nbr_tick, () -> {
                // message joueur
                target.sendMessage(
                        Text.literal("[SurvieInteract] " + viewer + " t’a forcé à drop un slot ! (Il y a 3 à 10 secondes)"),
                        false
                );

                // message global
                server.getPlayerManager().broadcast(
                        Text.literal("[SurvieInteract] " + viewer +
                                " a fait drop un slot de " + name),
                        false
                );
            }
        );
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

        for (int attempt = 0; attempt < 3 && (removed == ItemStack.EMPTY || removed.isEmpty()); attempt++) {

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
}
