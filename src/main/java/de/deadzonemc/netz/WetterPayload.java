package de.deadzonemc.netz;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Teilt den Spielern mit, welches Wetter gerade herrscht (für Nebel und Partikel). */
public record WetterPayload(int art, boolean nebel) implements CustomPacketPayload {

    public static final Type<WetterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "wetter"));

    public static final StreamCodec<ByteBuf, WetterPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, WetterPayload::art,
                    ByteBufCodecs.BOOL, WetterPayload::nebel, WetterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
