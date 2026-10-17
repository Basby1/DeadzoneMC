package de.deadzonemc.klasse;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * In beide Richtungen: Server -> Client „öffne die Klassenwahl“ (klasse = aktuelle oder leer) bzw.
 * „deine Klasse ist jetzt …“; Client -> Server „ich wähle …“ oder „Fähigkeit einsetzen“ (klasse = "!").
 */
public record KlassePayload(String klasse, boolean oeffnen) implements CustomPacketPayload {

    public static final Type<KlassePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse"));

    public static final StreamCodec<ByteBuf, KlassePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, KlassePayload::klasse, ByteBufCodecs.BOOL, KlassePayload::oeffnen, KlassePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
