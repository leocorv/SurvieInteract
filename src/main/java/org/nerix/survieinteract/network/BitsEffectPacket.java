package org.nerix.survieinteract.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import io.netty.buffer.Unpooled;
import net.minecraft.server.network.ServerPlayerEntity;

public class BitsEffectPacket implements CustomPayload {

    public static final Id<BitsEffectPacket> ID =
            new Id<>(Identifier.of("survieinteract", "bits_effect"));

    public final float radius;
    public final int duration;

    public BitsEffectPacket(float radius, int duration) {
        this.radius = radius;
        this.duration = duration;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(RegistryByteBuf buf) {
        buf.writeFloat(radius);
        buf.writeInt(duration);
    }

    public static BitsEffectPacket read(RegistryByteBuf buf) {
        return new BitsEffectPacket(buf.readFloat(), buf.readInt());
    }

    public static void send(ServerPlayerEntity player, float radius, int duration) {
        ServerPlayNetworking.send(player, new BitsEffectPacket(radius, duration));
    }
}
