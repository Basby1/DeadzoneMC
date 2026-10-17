package de.deadzonemc;

import de.deadzonemc.client.ClientWetter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import de.deadzonemc.client.MonsterRenderer;
import de.deadzonemc.monster.MonsterTypen;

/** Nur auf dem Client: Nebel, Nebelfarbe und Ascheregen. */
@Mod(value = DeadzoneMC.MODID, dist = Dist.CLIENT)
public class DeadzoneClient {

    public DeadzoneClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ClientWetter::tick);
        NeoForge.EVENT_BUS.addListener(ClientWetter::nebel);
        NeoForge.EVENT_BUS.addListener(ClientWetter::nebelFarbe);
        modBus.addListener(DeadzoneClient::renderer);
        modBus.addListener(de.deadzonemc.client.ClientKlasse::tasten);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientKlasse::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::kamera);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::bild);
    }

    private static void renderer(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(MonsterTypen.WANDERER.get(), c -> new MonsterRenderer<>(c, "wanderer", false, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.SCHREIER.get(), c -> new MonsterRenderer<>(c, "schreier", false, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.LAUERER.get(), c -> new MonsterRenderer<>(c, "lauerer", false, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.KRIECHER.get(), c -> new MonsterRenderer<>(c, "kriecher", false, false, 0.8F));
        e.registerEntityRenderer(MonsterTypen.DER_STILLE.get(), c -> new MonsterRenderer<>(c, "der_stille", false, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.IMITATOR.get(), c -> new MonsterRenderer<>(c, "imitator", true, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.NEBELWESEN.get(), c -> new MonsterRenderer<>(c, "nebelwesen", true, true, 0.0F));
        e.registerEntityRenderer(MonsterTypen.SCHWARM.get(), c -> new MonsterRenderer<>(c, "schwarm", true, false, 0.3F));
        e.registerEntityRenderer(MonsterTypen.BRUT.get(), c -> new MonsterRenderer<>(c, "brut", true, false, 0.8F));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.SOLDAT.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "soldat", true));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.KULTIST.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "kultist", true));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.UEBERLEBENDER.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "ueberlebender", false));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.HAENDLER.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "haendler", false));
        e.registerEntityRenderer(MonsterTypen.JUGGERNAUT.get(), c -> new MonsterRenderer<>(c, "juggernaut", true, false, 1.2F));
    }
}
