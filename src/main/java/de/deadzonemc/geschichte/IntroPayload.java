package de.deadzonemc.geschichte;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Startet die Intro-Sequenz beim ersten Betreten; enthält die Namen der Überlebenden. */
public record IntroPayload(String namen) implements CustomPacketPayload {

    public static final Type<IntroPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "intro"));
    public static final StreamCodec<ByteBuf, IntroPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, IntroPayload::namen, IntroPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
