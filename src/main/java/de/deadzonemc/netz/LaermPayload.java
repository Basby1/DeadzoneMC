package de.deadzonemc.netz;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Der Spieler hat gerade Lärm gemacht (Hörweite in Blöcken) – für die Lärmanzeige im HUD. */
public record LaermPayload(int radius) implements CustomPacketPayload {

    public static final Type<LaermPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "laerm"));

    public static final StreamCodec<ByteBuf, LaermPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(LaermPayload::new, LaermPayload::radius);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
