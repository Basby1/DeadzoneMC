package de.deadzonemc.netz;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Welche Monster der Spieler schon gesehen hat (für die Einträge im Handbuch). */
public record EntdecktPayload(List<String> monster) implements CustomPacketPayload {

    public static final Type<EntdecktPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "entdeckt"));

    public static final StreamCodec<ByteBuf, EntdecktPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(EntdecktPayload::new, EntdecktPayload::monster);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
