package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FreezeRequestC2SPayload(int durationMs) implements CustomPayload {
    public static final Id<FreezeRequestC2SPayload> ID =
            new Id<>(Identifier.of("so-tuff", "freeze_request"));

    public static final PacketCodec<RegistryByteBuf, FreezeRequestC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, FreezeRequestC2SPayload::durationMs,
                    FreezeRequestC2SPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
