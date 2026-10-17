package de.deadzonemc.netz;

import de.deadzonemc.client.ClientWetter;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Nachrichten zwischen Server und Spielern. */
public final class Netz {

    private Netz() {
    }

    public static void registrieren(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(WetterPayload.TYPE, WetterPayload.STREAM_CODEC, Netz::wetterEmpfangen);
        registrar.playToClient(SchreckPayload.TYPE, SchreckPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientSchreck.start(n.staerke())));
        registrar.playBidirectional(de.deadzonemc.klasse.KlassePayload.TYPE, de.deadzonemc.klasse.KlassePayload.STREAM_CODEC,
                new net.neoforged.neoforge.network.handling.DirectionalPayloadHandler<>(
                        (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientKlasse.empfangen(n)),
                        de.deadzonemc.klasse.Klassen::empfangen));
        registrar.playToClient(EntdecktPayload.TYPE, EntdecktPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientDaten.entdecktSetzen(n.monster())));
    }

    private static void wetterEmpfangen(WetterPayload nachricht, IPayloadContext context) {
        context.enqueueWork(() -> ClientWetter.setzen(nachricht.art(), nachricht.nebel()));
    }
}
