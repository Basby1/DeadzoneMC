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
    }

    private static void wetterEmpfangen(WetterPayload nachricht, IPayloadContext context) {
        context.enqueueWork(() -> ClientWetter.setzen(nachricht.art()));
    }
}
