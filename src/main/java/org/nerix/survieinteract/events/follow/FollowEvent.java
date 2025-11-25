package org.nerix.survieinteract.events.follow;

import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nerix.survieinteract.ConfigManager;
import org.nerix.survieinteract.Survieinteract;
import org.nerix.survieinteract.events.EventHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FollowEvent implements EventHandler {

    @Override
    public void handle(JsonObject json) {

        Survieinteract.getServer().execute(() -> {
            ServerPlayerEntity target = pickTarget();

            if (target != null) {
                spawnTNT(target);
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

    private void spawnTNT(ServerPlayerEntity p) {

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


        p.sendMessage(Text.literal("[SurvieInteract] FOLLOW → TNT surprise."), false);
    }
}
