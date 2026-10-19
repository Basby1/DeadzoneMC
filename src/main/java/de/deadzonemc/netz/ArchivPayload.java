package de.deadzonemc.netz;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Welche Notizen der Spieler schon gelesen hat (für das Archiv im Tagebuch). */
public record ArchivPayload(List<String> notizen) implements CustomPacketPayload {

    public static final Type<ArchivPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "archiv"));
    public static final StreamCodec<ByteBuf, ArchivPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(ArchivPayload::new, ArchivPayload::notizen);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
