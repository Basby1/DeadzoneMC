package de.deadzonemc.geschichte;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Stand der Geschichte für Tagebuch und Zielmarker: Schritt, Ziel (x, z, Welt; welt -1 = kein Ziel). */
public record GeschichtePayload(int schritt, int x, int z, int welt) implements CustomPacketPayload {

    public static final Type<GeschichtePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "geschichte"));
    public static final StreamCodec<ByteBuf, GeschichtePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GeschichtePayload::schritt, ByteBufCodecs.INT, GeschichtePayload::x,
            ByteBufCodecs.INT, GeschichtePayload::z, ByteBufCodecs.INT, GeschichtePayload::welt, GeschichtePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
