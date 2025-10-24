package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FreezeUpdatePitchS2CPayload(float pitch) implements CustomPayload {
    public static final Id<FreezeUpdatePitchS2CPayload> ID = new Id<>(Identifier.of("so-tuff","freeze_update_pitch"));
    public static final PacketCodec<RegistryByteBuf, FreezeUpdatePitchS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.FLOAT, FreezeUpdatePitchS2CPayload::pitch,
                    FreezeUpdatePitchS2CPayload::new
            );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
