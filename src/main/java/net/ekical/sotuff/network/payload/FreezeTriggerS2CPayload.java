package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FreezeTriggerS2CPayload(int durationMs, int skullIndex) implements CustomPayload {
    public static final Id<FreezeTriggerS2CPayload> ID =
            new Id<>(Identifier.of("so-tuff", "freeze_trigger"));
    public static final PacketCodec<RegistryByteBuf, FreezeTriggerS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.INTEGER, FreezeTriggerS2CPayload::durationMs,
                    PacketCodecs.INTEGER, FreezeTriggerS2CPayload::skullIndex,
                    FreezeTriggerS2CPayload::new
            );

    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
