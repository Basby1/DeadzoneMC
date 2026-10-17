package de.deadzonemc.netz;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Jumpscare: der Bildschirm des Spielers wackelt und blitzt kurz auf. */
public record SchreckPayload(int staerke) implements CustomPacketPayload {

    public static final Type<SchreckPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "schreck"));

    public static final StreamCodec<ByteBuf, SchreckPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(SchreckPayload::new, SchreckPayload::staerke);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
