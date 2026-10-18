package de.deadzonemc.zone;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Artefakt aus einer Anomalie. Wirkt, solange man es bei sich trägt – strahlt aber auch.
 * Händler in den Zufluchten zahlen gut dafür.
 */
public class ArtefaktItem extends Item {

    public enum Art {
        /** Aus dem Strudel: macht leicht, Stürze tun kaum noch weh. */
        QUALLE(0.25F),
        /** Aus dem Glutfeld: Feuer kann dir nichts, aber du hast ständig Hunger. */
        FEUERKUGEL(0.3F),
        /** Aus der Entladung: schneller laufen und arbeiten, strahlt stark. */
        BLITZKERN(0.6F),
        /** Aus dem Säurenebel: Wunden schließen sich langsam von selbst. */
        STEINBLUT(0.4F),
        /** Nur nach einer Emission: mehr Lebenskraft, aber gefährlich viel Strahlung. */
        SEELENKRISTALL(1.0F);

        private final float strahlung;

        Art(float strahlung) {
            this.strahlung = strahlung;
        }

        /** Strahlung pro Sekunde, solange man es trägt. */
        public float strahlung() {
            return strahlung;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final Art art;

    public ArtefaktItem(Art art, Properties eigenschaften) {
        super(eigenschaften.stacksTo(1).rarity(art == Art.SEELENKRISTALL ? Rarity.EPIC : Rarity.RARE));
        this.art = art;
    }

    public Art art() {
        return art;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> zeilen, TooltipFlag flag) {
        zeilen.add(Component.translatable("item.deadzonemc.artefakt_" + art.id() + ".gut").withStyle(ChatFormatting.GREEN));
        zeilen.add(Component.translatable("item.deadzonemc.artefakt_" + art.id() + ".schlecht").withStyle(ChatFormatting.RED));
        zeilen.add(Component.translatable("item.deadzonemc.artefakt.info").withStyle(ChatFormatting.DARK_GRAY));
    }
}
