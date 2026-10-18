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
        modBus.addListener(de.deadzonemc.client.ClientWaffen::tasten);
        modBus.addListener(de.deadzonemc.client.ClientWaffen::ebenen);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientWaffen::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientWaffen::klick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientWaffen::fov);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientKlasse::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::tick);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::kamera);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientSchreck::bild);
        NeoForge.EVENT_BUS.addListener(DeadzoneClient::titelbildschirm);
        NeoForge.EVENT_BUS.addListener(de.deadzonemc.client.ClientMusik::waehlen);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) -> {
            if (de.deadzonemc.waffen.Waffenpflege.istSchusswaffe(e.getItemStack())) {
                e.getToolTip().add(1, de.deadzonemc.waffen.Waffenpflege.zustand(e.getItemStack()));
                if (Boolean.TRUE.equals(e.getItemStack().get(de.deadzonemc.waffen.Waffenpflege.LADEHEMMUNG.get()))) {
                    e.getToolTip().add(2, net.minecraft.network.chat.Component.translatable("waffe.deadzonemc.ladehemmung")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        });
    }

    /** Titelbildschirm aufräumen: Realms-Knopf weg, die Knöpfe darunter rücken nach. */
    private static void titelbildschirm(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post e) {
        if (!(e.getScreen() instanceof net.minecraft.client.gui.screens.TitleScreen)) {
            return;
        }
        net.minecraft.client.gui.components.AbstractWidget realms = null;
        for (var l : e.getListenersList()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractWidget w
                    && w.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc
                    && tc.getKey().equals("menu.online")) {
                realms = w;
            }
        }
        if (realms == null) {
            return;
        }
        int y = realms.getY();
        e.removeListener(realms);
        for (var l : e.getListenersList()) {
            if (l instanceof net.minecraft.client.gui.components.AbstractWidget w && w.getY() > y + 4
                    && w.getY() < y + 120) {
                w.setY(w.getY() - 24);
            }
        }
    }

    private static void renderer(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(de.deadzonemc.waffen.wurf.Wurfwaffen.WURFKOERPER.get(),
                net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
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
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.WACHE.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "wache", false));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.BEWOHNER.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "bewohner", false));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.AUFTRAGGEBER.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "auftraggeber", false));
        e.registerEntityRenderer(de.deadzonemc.mensch.MenschTypen.BEGLEITER.get(), c -> new de.deadzonemc.client.MenschRenderer<>(c, "begleiter", false));
        e.registerEntityRenderer(de.deadzonemc.zone.Zone.BOLZEN_TYP.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
        e.registerEntityRenderer(de.deadzonemc.zone.Zone.ABWURFKISTE.get(), c -> {
            var r = new software.bernie.geckolib.renderer.GeoEntityRenderer<de.deadzonemc.zone.Abwurfkiste>(c,
                    new software.bernie.geckolib.model.DefaultedEntityGeoModel<>(
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "abwurfkiste")));
            return r;
        });
        e.registerEntityRenderer(MonsterTypen.GEFALLENER_SOLDAT.get(), c -> new MonsterRenderer<>(c, "gefallener_soldat", false, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.GLUTLEIB.get(), c -> new MonsterRenderer<>(c, "glutleib", true, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.SEUCHENHUND.get(), c -> new MonsterRenderer<>(c, "seuchenhund", true, false, 0.4F));
        e.registerEntityRenderer(MonsterTypen.WUEHLER.get(), c -> new MonsterRenderer<>(c, "wuehler", false, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.SAUGER.get(), c -> new MonsterRenderer<>(c, "sauger", true, false, 0.5F));
        e.registerEntityRenderer(MonsterTypen.SEUCHENTRAEGER.get(), c -> new MonsterRenderer<>(c, "seuchentraeger", true, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.ERTRUNKENER.get(), c -> new MonsterRenderer<>(c, "ertrunkener", false, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.SPAEHERDROHNE.get(), c -> new MonsterRenderer<>(c, "spaeherdrohne", true, true, 0.4F));
        e.registerEntityRenderer(MonsterTypen.LICHTMOTTE.get(), c -> new MonsterRenderer<>(c, "lichtmotte", true, false, 0.3F));
        e.registerEntityRenderer(MonsterTypen.SCHEINTOTER.get(), c -> new MonsterRenderer<>(c, "scheintoter", false, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.VERSCHMOLZENER.get(), c -> new MonsterRenderer<>(c, "verschmolzener", true, false, 1.6F));
        e.registerEntityRenderer(MonsterTypen.NEBELMUTTER.get(), c -> new MonsterRenderer<>(c, "nebelmutter", true, true, 0.8F));
        e.registerEntityRenderer(MonsterTypen.STRAHLENDE.get(), c -> new MonsterRenderer<>(c, "strahlende", true, false, 1.3F));
        e.registerEntityRenderer(MonsterTypen.BRUTKOENIGIN.get(), c -> new MonsterRenderer<>(c, "brutkoenigin", true, false, 1.6F));
        e.registerEntityRenderer(MonsterTypen.SCHLAECHTER.get(), c -> new MonsterRenderer<>(c, "schlaechter", true, false, 1.0F));
        e.registerEntityRenderer(MonsterTypen.KOMMANDANT.get(), c -> new MonsterRenderer<>(c, "kommandant", true, false, 0.9F));
        e.registerEntityRenderer(MonsterTypen.HIRTE.get(), c -> new MonsterRenderer<>(c, "hirte", true, false, 0.6F));
        e.registerEntityRenderer(MonsterTypen.SCHLUND.get(), c -> new MonsterRenderer<>(c, "schlund", false, false, 1.6F));
        e.registerEntityRenderer(MonsterTypen.PILZVATER.get(), c -> new MonsterRenderer<>(c, "pilzvater", true, false, 1.3F));
        e.registerEntityRenderer(MonsterTypen.ERFRORENE.get(), c -> new MonsterRenderer<>(c, "erfrorene", true, true, 1.1F));
        e.registerEntityRenderer(MonsterTypen.JUGGERNAUT.get(), c -> new MonsterRenderer<>(c, "juggernaut", true, false, 1.2F));
    }
}
