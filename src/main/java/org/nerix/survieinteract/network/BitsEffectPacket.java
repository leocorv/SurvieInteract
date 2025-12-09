package org.nerix.survieinteract.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class BitsEffectPacket implements CustomPayload {

    public static final CustomPayload.Id<BitsEffectPacket> ID =
            new CustomPayload.Id<>(Identifier.of("survieinteract", "bits_effect"));

    // Obligatoire depuis MC 1.21.4+
    public static final PacketCodec<RegistryByteBuf, BitsEffectPacket> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, p -> p.radius,
                    PacketCodecs.VAR_INT, p -> p.duration,
                    BitsEffectPacket::new
            );

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
}
