package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FreezeStartS2CPayload(int durationMs, int skullIndex, float pitch, String soundId) implements CustomPayload {
    public static final Id<FreezeStartS2CPayload> ID =
            new Id<>(Identifier.of("so-tuff", "freeze_start"));

    public static final PacketCodec<RegistryByteBuf, FreezeStartS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, FreezeStartS2CPayload::durationMs,
                    PacketCodecs.VAR_INT, FreezeStartS2CPayload::skullIndex,
                    PacketCodecs.FLOAT, FreezeStartS2CPayload::pitch,
                    PacketCodecs.STRING, FreezeStartS2CPayload::soundId,
                    FreezeStartS2CPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
