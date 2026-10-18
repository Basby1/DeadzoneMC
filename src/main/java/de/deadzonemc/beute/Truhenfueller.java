package de.deadzonemc.beute;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.Staedte;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sicherheitsnetz für die Beute: Jede Truhe und jedes Fass, das die Welt erzeugt hat und das beim ersten Öffnen
 * leer und ohne Beutetabelle ist (etwa weil der Stadtgenerator sie ohne Tabelle gesetzt hat), bekommt eine Tabelle
 * passend zum Ort – Stadtwohnung, Laden, Keller, Schuppen … Selbst gesetzte Behälter der Spieler werden beim
 * Aufstellen markiert und nie befüllt.
 */
public final class Truhenfueller {

    private static final Logger LOG = LoggerFactory.getLogger("deadzonemc/Truhen");
    private static int gefuellt;

    private Truhenfueller() {
    }

    private static final String[] STADT = {"stadt_wohnung", "stadt_wohnung", "stadt_wohnung", "stadt_wohnung", "kueche",
            "kueche", "wohnzimmer", "wohnzimmer", "kleiderschrank", "buero", "buero", "stadt_laden", "stadt_laden",
            "badezimmer", "nachttisch", "werkzeug", "apotheke", "kinderzimmer", "polizei", "waffenkammer"};
    private static final String[] UMLAND = {"schuppen", "schuppen", "scheune", "wohnzimmer", "wohnzimmer", "kueche",
            "kueche", "keller", "werkzeug", "werkzeug", "ruine", "ruine", "nachttisch", "kleiderschrank"};
    private static final String[] UNTEN_STADT = {"keller", "keller", "keller", "metro", "metro", "werkzeug", "bunker"};
    private static final String[] UNTEN = {"keller", "keller", "werkzeug", "versteck"};

    public static void gesetzt(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player && event.getLevel() instanceof ServerLevel level) {
            BlockEntity be = level.getBlockEntity(event.getPos());
            if (be instanceof RandomizableContainerBlockEntity) {
                be.setData(ModRegistry.BERUEHRT.get(), true);
                be.setChanged();
            }
        }
    }

    /** Wird beim Rechtsklick auf einen Behälter aufgerufen – vor dem Öffnen, damit die Beute gleich ausgepackt wird. */
    public static void pruefen(ServerLevel level, BlockPos pos, BlockEntity be) {
        if (!(be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity)
                || !(be instanceof RandomizableContainerBlockEntity rc) || be.getData(ModRegistry.BERUEHRT.get())) {
            return;
        }
        be.setData(ModRegistry.BERUEHRT.get(), true);
        be.setChanged();
        if (rc.getLootTable() != null || !rc.isEmpty()) {
            return;
        }
        String name = tabelle(level, pos, level.random);
        rc.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kiste/" + name)), level.random.nextLong());
        rc.setChanged();
        if (gefuellt++ < 20) {
            LOG.info("Leerer Weltbehälter bei {} befüllt mit kiste/{}", pos.toShortString(), name);
        }
    }

    public static String tabelle(ServerLevel level, BlockPos pos, RandomSource r) {
        float f = Staedte.stadtFaktor(level, pos.getX() >> 4, pos.getZ() >> 4);
        boolean stadt = f > Staedte.schwelle();
        boolean unten = pos.getY() < level.getSeaLevel() - 6;
        String[] liste = unten ? (stadt ? UNTEN_STADT : UNTEN) : stadt ? STADT : UMLAND;
        return liste[r.nextInt(liste.length)];
    }
}
