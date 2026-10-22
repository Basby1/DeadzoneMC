package de.deadzonemc.geschichte;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Erste Schritte: erledigte Schritte als Bitmaske (in beide Richtungen). */
public record TutorialPayload(int erledigt) implements CustomPacketPayload {

    public static final Type<TutorialPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "tutorial"));
    public static final StreamCodec<ByteBuf, TutorialPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TutorialPayload::erledigt, TutorialPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
