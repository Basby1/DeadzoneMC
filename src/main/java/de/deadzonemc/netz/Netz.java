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
        registrar.playToClient(de.deadzonemc.zone.EmissionPayload.TYPE, de.deadzonemc.zone.EmissionPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> ClientWetter.emissionSetzen(n.stufe())));
        registrar.playToClient(de.deadzonemc.musik.OrtPayload.TYPE, de.deadzonemc.musik.OrtPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientMusik.ortSetzen(n.ort())));
        registrar.playToServer(de.deadzonemc.waffen.feuer.WaffenPayloads.Schuss.TYPE,
                de.deadzonemc.waffen.feuer.WaffenPayloads.Schuss.STREAM_CODEC, de.deadzonemc.waffen.feuer.Schiessen::empfangen);
        registrar.playToServer(de.deadzonemc.waffen.feuer.WaffenPayloads.Laden.TYPE,
                de.deadzonemc.waffen.feuer.WaffenPayloads.Laden.STREAM_CODEC, de.deadzonemc.waffen.feuer.Nachladen::empfangen);
        registrar.playToClient(de.deadzonemc.waffen.feuer.WaffenPayloads.Rueckmeldung.TYPE,
                de.deadzonemc.waffen.feuer.WaffenPayloads.Rueckmeldung.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientWaffen.rueckmeldung(n)));
        registrar.playToClient(de.deadzonemc.geschichte.GeschichtePayload.TYPE, de.deadzonemc.geschichte.GeschichtePayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientGeschichte.empfangen(n)));
        registrar.playToClient(de.deadzonemc.geschichte.IntroPayload.TYPE, de.deadzonemc.geschichte.IntroPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.IntroScreen.starten(n.namen())));
        registrar.playToClient(EntdecktPayload.TYPE, EntdecktPayload.STREAM_CODEC,
                (n, ctx) -> ctx.enqueueWork(() -> de.deadzonemc.client.ClientDaten.entdecktSetzen(n.monster())));
    }

    private static void wetterEmpfangen(WetterPayload nachricht, IPayloadContext context) {
        context.enqueueWork(() -> ClientWetter.setzen(nachricht.art(), nachricht.nebel()));
    }
}
