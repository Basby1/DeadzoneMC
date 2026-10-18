package de.deadzonemc.waffen.feuer;

import de.deadzonemc.DeadzoneMC;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Netzwerk der Schusswaffen. */
public final class WaffenPayloads {

    private WaffenPayloads() {
    }

    /** Spieler drückt ab (zielen = Rechtsklick gehalten). */
    public record Schuss(boolean zielen) implements CustomPacketPayload {
        public static final Type<Schuss> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "schuss"));
        public static final StreamCodec<ByteBuf, Schuss> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Schuss::zielen, Schuss::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Spieler will nachladen (Taste R). */
    public record Laden(int dummy) implements CustomPacketPayload {
        public static final Type<Laden> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "laden"));
        public static final StreamCodec<ByteBuf, Laden> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Laden::dummy, Laden::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Rückmeldung an den Schützen: 1 = Treffer, 2 = Kopfschuss, 3 = tödlich. ladenTicks > 0: Nachladen läuft. */
    public record Rueckmeldung(int treffer, int ladenTicks) implements CustomPacketPayload {
        public static final Type<Rueckmeldung> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "treffer"));
        public static final StreamCodec<ByteBuf, Rueckmeldung> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Rueckmeldung::treffer, ByteBufCodecs.VAR_INT, Rueckmeldung::ladenTicks, Rueckmeldung::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
