package net.ekical.sotuff.network.payload;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record EffectivePrefsS2CPayload(boolean perAction, double freq01, boolean vary, boolean bars,
                                       float pitchMin, float pitchMax, float pitchDefault) implements CustomPayload {
    public static final Id<EffectivePrefsS2CPayload> ID = new Id<>(Identifier.of("so-tuff","effective_prefs"));
    public static final PacketCodec<RegistryByteBuf, EffectivePrefsS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN,  EffectivePrefsS2CPayload::perAction,
                    PacketCodecs.DOUBLE,   EffectivePrefsS2CPayload::freq01,
                    PacketCodecs.BOOLEAN,  EffectivePrefsS2CPayload::vary,
                    PacketCodecs.BOOLEAN,  EffectivePrefsS2CPayload::bars,
                    PacketCodecs.FLOAT,    EffectivePrefsS2CPayload::pitchMin,
                    PacketCodecs.FLOAT,    EffectivePrefsS2CPayload::pitchMax,
                    PacketCodecs.FLOAT,    EffectivePrefsS2CPayload::pitchDefault,
                    EffectivePrefsS2CPayload::new
            );
    @Override public Id<? extends CustomPayload> getId() { return ID; }
}
