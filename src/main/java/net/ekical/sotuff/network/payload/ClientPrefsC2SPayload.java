package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ClientPrefsC2SPayload(boolean perAction, double freq01, boolean vary, boolean bars,
                                    float pitchMin, float pitchMax, float pitchDefault) implements CustomPayload {
    public static final Id<ClientPrefsC2SPayload> ID = new Id<>(Identifier.of("so-tuff","client_prefs"));
    public static final PacketCodec<RegistryByteBuf, ClientPrefsC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN,  ClientPrefsC2SPayload::perAction,
                    PacketCodecs.DOUBLE,   ClientPrefsC2SPayload::freq01,
                    PacketCodecs.BOOLEAN,  ClientPrefsC2SPayload::vary,
                    PacketCodecs.BOOLEAN,  ClientPrefsC2SPayload::bars,
                    PacketCodecs.FLOAT,    ClientPrefsC2SPayload::pitchMin,
                    PacketCodecs.FLOAT,    ClientPrefsC2SPayload::pitchMax,
                    PacketCodecs.FLOAT,    ClientPrefsC2SPayload::pitchDefault,
                    ClientPrefsC2SPayload::new
            );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
