package de.deadzonemc.charakter;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Charakter-Editor und Kino-Intro.
 * <ul>
 *     <li>Server → Client: {@link #ALLE} alle Aussehen ("uuid=code;…"), {@link #EDITOR} Editor öffnen,
 *     {@link #WARTEN} auf Mitspieler warten (Namen), {@link #KINO} Intro starten ("uuid,name;…").</li>
 *     <li>Client → Server: {@link #ALLE} eigenes Aussehen setzen (Code), {@link #KINO} Intro fertig.</li>
 * </ul>
 */
public record CharakterPayload(int art, String daten) implements CustomPacketPayload {

    public static final int ALLE = 0;
    public static final int EDITOR = 1;
    public static final int WARTEN = 2;
    public static final int KINO = 3;

    public static final Type<CharakterPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "charakter"));
    public static final StreamCodec<ByteBuf, CharakterPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CharakterPayload::art, ByteBufCodecs.STRING_UTF8, CharakterPayload::daten, CharakterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
