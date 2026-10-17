package de.deadzonemc.welt.stadt;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.plan.Grundstueck;
import de.deadzonemc.welt.plan.Hoehen;
import de.deadzonemc.welt.plan.Objekt;
import de.deadzonemc.welt.plan.Ortsplan;
import de.deadzonemc.welt.plan.PlanSpeicher;
import de.deadzonemc.welt.plan.Siedlung;
import de.deadzonemc.welt.plan.Siedlungen;
import de.deadzonemc.welt.plan.Strasse;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Eine 64×64 Blöcke große Kachel einer Siedlung. Die Welt ist in ein festes Raster solcher Kacheln
 * geteilt; jede Kachel baut genau die Straßenstücke, Grundstücke und Objekte des Ortsplans, die bei ihr
 * liegen. So können Siedlungen beliebig groß werden und passen trotzdem nahtlos zusammen.
 */
public class KachelStruktur extends Structure {

    public static final int GROESSE = 64;

    public static final MapCodec<KachelStruktur> CODEC = simpleCodec(KachelStruktur::new);

    public KachelStruktur(StructureSettings settings) {
        super(settings);
    }

    @Override
    public StructureType<?> type() {
        return ModRegistry.KACHEL.get();
    }

    /** Liefert eine Höhenquelle aus dem Generator (Oberkante Gelände, vor dem Einebnen). */
    public static Hoehen hoehen(ChunkGenerator generator, net.minecraft.world.level.LevelHeightAccessor hoehe,
                                net.minecraft.world.level.levelgen.RandomState zufall) {
        // Meeresboden statt Wasseroberfläche: so erkennt man Wasser daran, dass der Boden unter dem Meeresspiegel liegt.
        return (x, z) -> generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, hoehe, zufall) - 1;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x0 = context.chunkPos().getMinBlockX();
        int z0 = context.chunkPos().getMinBlockZ();
        int x1 = x0 + GROESSE - 1;
        int z1 = z0 + GROESSE - 1;
        long saat = context.seed();
        List<Siedlung> siedlungen = Siedlungen.beiPunkt(saat, x0 + GROESSE / 2, z0 + GROESSE / 2, GROESSE);
        if (siedlungen.isEmpty()) {
            return Optional.empty();
        }
        Hoehen h = hoehen(context.chunkGenerator(), context.heightAccessor(), context.randomState());
        int meer = context.chunkGenerator().getSeaLevel();

        List<StructurePiece> teile = new ArrayList<>();
        int stubY = meer;
        for (Siedlung s : siedlungen) {
            if (!s.typ().wirdGebaut()) {
                continue;
            }
            Ortsplan plan = PlanSpeicher.fertig(saat, s, h, meer);
            if (!plan.gueltig) {
                continue;
            }
            for (Strasse st : plan.strassen) {
                strasseZerlegen(plan, st, x0, z0, x1, z1, teile);
            }
            for (Grundstueck g : plan.grundstuecke) {
                if (innen(g.mitteX(), g.mitteZ(), x0, z0, x1, z1)) {
                    teile.add(new GebaeudePiece(g));
                    stubY = g.y;
                }
            }
            for (Objekt o : plan.objekte) {
                if (innen(o.x, o.z, x0, z0, x1, z1)) {
                    teile.add(new ObjektPiece(o));
                }
            }
        }
        if (teile.isEmpty()) {
            return Optional.empty();
        }
        BlockPos mitte = new BlockPos(x0 + GROESSE / 2, stubY, z0 + GROESSE / 2);
        return Optional.of(new GenerationStub(mitte, builder -> teile.forEach(builder::addPiece)));
    }

    private static boolean innen(int x, int z, int x0, int z0, int x1, int z1) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1;
    }

    /** Zerlegt den Teil der Straße, der zu dieser Kachel gehört, in Abschnitte gleicher Höhe. */
    private static void strasseZerlegen(Ortsplan plan, Strasse st, int x0, int z0, int x1, int z1,
                                        List<StructurePiece> teile) {
        // Die Kachel, in der die Mittellinie liegt, ist für die Straße zuständig.
        int querMin = st.entlangX ? z0 : x0;
        int querMax = st.entlangX ? z1 : x1;
        if (st.quer < querMin || st.quer > querMax) {
            return;
        }
        int laengsMin = Math.max(st.von, st.entlangX ? x0 : z0);
        int laengsMax = Math.min(st.bis, st.entlangX ? x1 : z1);
        if (laengsMin > laengsMax) {
            return;
        }
        // Fahrbahnen der anderen Straßen (für Kreuzungen)
        List<int[]> andere = new ArrayList<>();
        for (Strasse o : plan.strassen) {
            if (o != st && Strasse.ueberlappt(o.flaeche(), st.flaeche(), 6)) {
                andere.add(o.fahrbahn());
            }
        }
        int[] kreuz = new int[andere.size() * 4];
        for (int i = 0; i < andere.size(); i++) {
            System.arraycopy(andere.get(i), 0, kreuz, i * 4, 4);
        }

        int a = laengsMin;
        while (a <= laengsMax) {
            int y = st.hoeheBei(a);
            boolean ort = st.imOrt(a);
            int b = a;
            // gleiche Höhe und gleicher Abschnittstyp zusammenfassen, höchstens 16 lang
            while (b + 1 <= laengsMax && st.hoeheBei(b + 1) == y && st.imOrt(b + 1) == ort && b + 1 - a < 16) {
                b++;
            }
            int yVor = a > st.von ? st.hoeheBei(a - 1) : y;
            int yNach = b < st.bis ? st.hoeheBei(b + 1) : y;
            teile.add(new StrassenPiece(st.entlangX, st.quer, st.typ.halb, ort ? st.typ.gehweg : st.typ.gehweg,
                    a, b, y, yVor, yNach, ort, st.typ != Strasse.Typ.LANDSTRASSE, kreuz, st.saat ^ a));
            a = b + 1;
        }
    }
}
