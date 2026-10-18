package de.deadzonemc.musik;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Wo der Spieler gerade ist (für die Musik): 0 = Land, 1 = Stadt, 2 = Zuflucht. */
public record OrtPayload(int ort) implements CustomPacketPayload {

    public static final Type<OrtPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "ort"));
    public static final StreamCodec<ByteBuf, OrtPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, OrtPayload::ort, OrtPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
