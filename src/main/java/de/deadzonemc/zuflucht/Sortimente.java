package de.deadzonemc.zuflucht;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.mensch.Haendler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;

import java.util.Optional;

/**
 * Die festen Sortimente der Händler in den Zufluchten. Bezahlt wird mit Militärpatronen – man kann
 * dort kaufen und Beute gegen Patronen verkaufen.
 */
public final class Sortimente {

    public static final String VERSORGER = "versorger";
    public static final String WAFFEN = "waffen";
    public static final String TECHNIK = "technik";
    public static final String ARZT = "arzt";
    public static final String[] ALLE = {VERSORGER, WAFFEN, TECHNIK, ARZT};

    private Sortimente() {
    }

    private static Item patrone() {
        return ModRegistry.MILITAERPATRONE.get();
    }

    private static Item mod(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    /** Spieler kauft: zahlt Patronen, bekommt Ware. */
    private static void kauf(MerchantOffers o, int preis, ItemStack ware, int vorrat) {
        if (!ware.isEmpty() && ware.getItem() != Items.AIR) {
            o.add(new MerchantOffer(new ItemCost(patrone(), preis), Optional.empty(), ware, vorrat, 1, 0.05F));
        }
    }

    private static void kauf(MerchantOffers o, int preis, ItemLike ware, int menge, int vorrat) {
        kauf(o, preis, new ItemStack(ware, menge), vorrat);
    }

    /** Spieler verkauft: gibt Ware, bekommt Patronen. */
    private static void ankauf(MerchantOffers o, ItemLike ware, int menge, int patronen, int vorrat) {
        if (ware.asItem() != Items.AIR) {
            o.add(new MerchantOffer(new ItemCost(ware, menge), Optional.empty(), new ItemStack(patrone(), patronen),
                    vorrat, 1, 0.05F));
        }
    }

    public static void fuellen(String sortiment, MerchantOffers o) {
        switch (sortiment) {
            case VERSORGER -> {
                kauf(o, 3, ModRegistry.KONSERVENDOSE.get(), 2, 12);
                kauf(o, 2, Items.BREAD, 3, 12);
                kauf(o, 3, Items.COOKED_BEEF, 4, 8);
                kauf(o, 2, mod("toughasnails:purified_water_bottle"), 2, 12);
                kauf(o, 1, Items.WHEAT_SEEDS, 8, 6);
                kauf(o, 2, Items.POTATO, 6, 6);
                kauf(o, 2, Items.TORCH, 8, 12);
                ankauf(o, Items.WHEAT, 16, 2, 16);
                ankauf(o, Items.POTATO, 12, 2, 16);
                ankauf(o, Items.LEATHER, 6, 3, 12);
                ankauf(o, Items.BEEF, 8, 2, 12);
                ankauf(o, Items.ROTTEN_FLESH, 32, 1, 16);
            }
            case WAFFEN -> {
                kauf(o, 4, Haendler.tacz("ammo", "AmmoId", "9mm", 16), 12);
                kauf(o, 5, Haendler.tacz("ammo", "AmmoId", "12g", 8), 12);
                kauf(o, 4, Haendler.tacz("ammo", "AmmoId", "45acp", 12), 12);
                kauf(o, 9, Haendler.tacz("ammo", "AmmoId", "556x45", 30), 8);
                kauf(o, 9, Haendler.tacz("ammo", "AmmoId", "762x39", 30), 8);
                kauf(o, 18, Haendler.tacz("modern_kinetic_gun", "GunId", "glock_17", 1), 2);
                kauf(o, 30, Haendler.tacz("modern_kinetic_gun", "GunId", "m870", 1), 1);
                kauf(o, 40, Haendler.tacz("modern_kinetic_gun", "GunId", "kar98", 1), 1);
                kauf(o, 55, Haendler.tacz("modern_kinetic_gun", "GunId", "ak47", 1), 1);
                kauf(o, 20, ModRegistry.GASMASKE.get(), 1, 2);
                kauf(o, 4, ModRegistry.FILTER_ITEM.get(), 1, 8);
                kauf(o, 6, de.deadzonemc.waffen.Waffenpflege.PFLEGESET.get(), 1, 4);
                ankauf(o, mod("deadzonemc:p9"), 1, 10, 6);
                ankauf(o, mod("deadzonemc:munition_9mm"), 30, 3, 12);
            }
            case TECHNIK -> {
                kauf(o, 3, ModRegistry.BATTERIE.get(), 2, 8);
                kauf(o, 15, ModRegistry.GEIGERZAEHLER.get(), 1, 2);
                kauf(o, 8, ModRegistry.SCHATZKARTE.get(), 1, 3);
                kauf(o, 30, ModRegistry.GENERATOR_ITEM.get(), 1, 1);
                kauf(o, 2, Items.LANTERN, 2, 8);
                kauf(o, 4, Items.IRON_INGOT, 4, 8);
                ankauf(o, ModRegistry.SCHROTT.get(), 16, 3, 16);
                ankauf(o, ModRegistry.ELEKTRONIK.get(), 4, 4, 12);
                ankauf(o, ModRegistry.BATTERIE.get(), 2, 2, 12);
                ankauf(o, Items.IRON_INGOT, 8, 4, 12);
                ankauf(o, Items.GOLD_INGOT, 4, 3, 12);
                // Zone: Ausrüstung für Anomalienfelder, Artefakte werden gut bezahlt
                kauf(o, 2, de.deadzonemc.zone.Zone.BOLZEN.get(), 8, 8);
                kauf(o, 25, de.deadzonemc.zone.Zone.DETEKTOR.get(), 1, 1);
                ankauf(o, de.deadzonemc.zone.Zone.artefakt(de.deadzonemc.zone.ArtefaktItem.Art.QUALLE), 1, 22, 4);
                ankauf(o, de.deadzonemc.zone.Zone.artefakt(de.deadzonemc.zone.ArtefaktItem.Art.FEUERKUGEL), 1, 24, 4);
                ankauf(o, de.deadzonemc.zone.Zone.artefakt(de.deadzonemc.zone.ArtefaktItem.Art.BLITZKERN), 1, 30, 4);
                ankauf(o, de.deadzonemc.zone.Zone.artefakt(de.deadzonemc.zone.ArtefaktItem.Art.STEINBLUT), 1, 28, 4);
                ankauf(o, de.deadzonemc.zone.Zone.artefakt(de.deadzonemc.zone.ArtefaktItem.Art.SEELENKRISTALL), 1, 60, 2);
            }
            case ARZT -> {
                kauf(o, 3, ModRegistry.VERBAND.get(), 2, 12);
                kauf(o, 8, ModRegistry.ANTIBIOTIKA.get(), 1, 4);
                kauf(o, 5, ModRegistry.SCHMERZMITTEL.get(), 1, 6);
                kauf(o, 7, ModRegistry.ANTI_RAD.get(), 1, 4);
                kauf(o, 3, ModRegistry.WODKA.get(), 1, 6);
                kauf(o, 4, Items.GOLDEN_CARROT, 2, 6);
                ankauf(o, ModRegistry.STOFFRESTE.get(), 6, 2, 16);
                ankauf(o, Items.SPIDER_EYE, 4, 2, 12);
                ankauf(o, Items.HONEY_BOTTLE, 3, 3, 12);
                ankauf(o, Items.GLASS_BOTTLE, 6, 1, 12);
            }
            default -> {
            }
        }
    }
}
