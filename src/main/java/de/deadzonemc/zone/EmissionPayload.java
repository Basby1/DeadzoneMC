package de.deadzonemc.zone;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Teilt den Spielern mit, ob gerade eine Emission droht oder tobt (färbt den Himmel rot). */
public record EmissionPayload(int stufe) implements CustomPacketPayload {

    public static final Type<EmissionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "emission"));

    public static final StreamCodec<ByteBuf, EmissionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, EmissionPayload::stufe, EmissionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
