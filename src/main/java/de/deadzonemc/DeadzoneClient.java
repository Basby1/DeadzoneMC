package de.deadzonemc;

import de.deadzonemc.client.ClientWetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/** Nur auf dem Client: Nebel, Nebelfarbe und Ascheregen. */
@Mod(value = DeadzoneMC.MODID, dist = Dist.CLIENT)
public class DeadzoneClient {

    public DeadzoneClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ClientWetter::tick);
        NeoForge.EVENT_BUS.addListener(ClientWetter::nebel);
        NeoForge.EVENT_BUS.addListener(ClientWetter::nebelFarbe);
    }
}
