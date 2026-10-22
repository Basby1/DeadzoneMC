package de.deadzonemc.monster;

import de.deadzonemc.welt.Staedte;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Wo welches Monster lebt (Rückmeldung 22.10.: „überall spawnen einfach alle Mobs, das wirkt zu random“).
 *
 * <p>Jede Stelle der Oberwelt gehört zu einer {@link Gegend}. Ein Monster erscheint natürlich nur in den Gegenden,
 * die zu ihm passen: Seuchenträger im verseuchten Land, Ertrunkene in Flüssen und Meeren, Wühler in Höhlen,
 * Scheintote und Späherdrohnen in Städten, Tarnfresser zwischen Bäumen. Gruppen und Herden haben ein
 * {@link Thema}: Eine Herde besteht aus Infizierten (mit höchstens einem Schreier oder einer Brut dabei), aus einem
 * Hunderudel, aus einem Trupp gefallener Soldaten – nicht aus einem bunten Durcheinander.</p>
 */
public final class Lebensraum {

    private Lebensraum() {
    }

    public enum Gegend {
        STADT, FELD, WALD, SUMPF, WUESTE, VERSEUCHT, SCHNEE, GEBIRGE, WASSER, HOEHLE
    }

    private static TagKey<Biome> tag(String ns, String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(ns, name));
    }

    private static final TagKey<Biome> SUMPF = tag("deadzonemc", "sumpf");
    private static final TagKey<Biome> VERSEUCHT = tag("deadzonemc", "verseucht");
    private static final TagKey<Biome> WUESTE = tag("deadzonemc", "wueste");
    private static final TagKey<Biome> SCHNEE = tag("deadzonemc", "schnee");
    private static final TagKey<Biome> GEWAESSER = tag("deadzonemc", "grosse_gewaesser");
    private static final TagKey<Biome> WALDLAND = tag("deadzonemc", "waldland");

    /** In welcher Gegend liegt diese Stelle? */
    public static Gegend gegend(ServerLevelAccessor level, BlockPos pos) {
        int oben = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (pos.getY() < oben - 6 && !level.canSeeSky(pos)) {
            return Gegend.HOEHLE;
        }
        if (Staedte.istStadt(level.getLevel(), pos.getX() >> 4, pos.getZ() >> 4)) {
            return Gegend.STADT;
        }
        Holder<Biome> b = level.getBiome(pos);
        if (b.is(GEWAESSER)) {
            return Gegend.WASSER;
        }
        if (b.is(SUMPF)) {
            return Gegend.SUMPF;
        }
        if (b.is(VERSEUCHT)) {
            return Gegend.VERSEUCHT;
        }
        if (b.is(WUESTE)) {
            return Gegend.WUESTE;
        }
        if (b.is(SCHNEE)) {
            return Gegend.SCHNEE;
        }
        if (b.is(BiomeTags.IS_MOUNTAIN) || b.is(BiomeTags.IS_HILL)) {
            return Gegend.GEBIRGE;
        }
        if (b.is(WALDLAND) || b.is(BiomeTags.IS_FOREST) || b.is(BiomeTags.IS_TAIGA) || b.is(BiomeTags.IS_JUNGLE)) {
            return Gegend.WALD;
        }
        return Gegend.FELD;
    }

    // ---- Wer lebt wo -----------------------------------------------------------------------------

    private static final Map<EntityType<?>, Set<Gegend>> HEIMAT = new HashMap<>();

    private static Set<Gegend> heimat(EntityType<?> typ) {
        if (HEIMAT.isEmpty()) {
            Gegend s = Gegend.STADT, f = Gegend.FELD, w = Gegend.WALD, su = Gegend.SUMPF, wu = Gegend.WUESTE,
                    v = Gegend.VERSEUCHT, sch = Gegend.SCHNEE, g = Gegend.GEBIRGE, wa = Gegend.WASSER, h = Gegend.HOEHLE;
            // Die Infizierten: überall an der Oberfläche
            HEIMAT.put(MonsterTypen.WANDERER.get(), EnumSet.of(s, f, w, su, wu, v, sch, g));
            // Stadt
            HEIMAT.put(MonsterTypen.SCHEINTOTER.get(), EnumSet.of(s));
            HEIMAT.put(MonsterTypen.SPAEHERDROHNE.get(), EnumSet.of(s));
            HEIMAT.put(MonsterTypen.SCHREIER.get(), EnumSet.of(s, f));
            HEIMAT.put(MonsterTypen.GEFALLENER_SOLDAT.get(), EnumSet.of(s, f));
            HEIMAT.put(MonsterTypen.JUGGERNAUT.get(), EnumSet.of(s, v));
            // Dunkle Ecken: Häuser, Keller, Höhlen
            HEIMAT.put(MonsterTypen.LAUERER.get(), EnumSet.of(s, h));
            HEIMAT.put(MonsterTypen.KRIECHER.get(), EnumSet.of(s, h, g));
            HEIMAT.put(MonsterTypen.WUEHLER.get(), EnumSet.of(h));
            // Kanäle, Keller, Moor
            HEIMAT.put(MonsterTypen.SCHWARM.get(), EnumSet.of(su, h));
            HEIMAT.put(MonsterTypen.BRUT.get(), EnumSet.of(su, v, h));
            // Wildnis
            HEIMAT.put(MonsterTypen.IMITATOR.get(), EnumSet.of(f, w, sch, g));
            HEIMAT.put(MonsterTypen.DER_STILLE.get(), EnumSet.of(s, w, sch));
            HEIMAT.put(MonsterTypen.NEBELWESEN.get(), EnumSet.of(s, f, w, su, sch, g));
            HEIMAT.put(MonsterTypen.SEUCHENHUND.get(), EnumSet.of(f, w, wu, sch, g));
            HEIMAT.put(MonsterTypen.LICHTMOTTE.get(), EnumSet.of(w, su));
            HEIMAT.put(MonsterTypen.SAUGER.get(), EnumSet.of(w, su));
            HEIMAT.put(MonsterTypen.TARNFRESSER.get(), EnumSet.of(w, su));
            // Strahlung und Hitze
            HEIMAT.put(MonsterTypen.SEUCHENTRAEGER.get(), EnumSet.of(v));
            HEIMAT.put(MonsterTypen.GLUTLEIB.get(), EnumSet.of(v, wu));
            // Große Gewässer
            HEIMAT.put(MonsterTypen.ERTRUNKENER.get(), EnumSet.of(wa));
        }
        return HEIMAT.get(typ);
    }

    /** Darf dieses Monster hier natürlich erscheinen? Spawner, Bauwerke und Ereignisse sind nicht betroffen. */
    public static boolean passt(EntityType<?> typ, ServerLevelAccessor level, MobSpawnType grund, BlockPos pos) {
        if (grund != MobSpawnType.NATURAL && grund != MobSpawnType.CHUNK_GENERATION) {
            return true;
        }
        if (level.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            return true;
        }
        Set<Gegend> erlaubt = heimat(typ);
        if (erlaubt == null) {
            return true;
        }
        Gegend g = gegend(level, pos);
        if (!erlaubt.contains(g)) {
            return false;
        }
        if (typ == MonsterTypen.ERTRUNKENER.get()) {
            // nur wo das Wasser wirklich tief ist
            return level.getFluidState(pos.above(2)).is(FluidTags.WATER);
        }
        return true;
    }

    // ---- Gruppen mit Thema -------------------------------------------------------------------------

    /**
     * Ein Gruppenthema: ein Kern (die meisten Mitglieder) und höchstens eine Beimischung mit fester Höchstzahl.
     */
    public record Thema(String name, Supplier<? extends EntityType<? extends DeadzoneMonster>> kern,
                        Supplier<? extends EntityType<? extends DeadzoneMonster>> beimischung, int hoechstensBei,
                        int minGroesse, boolean nurNachts, int gewicht, Set<Gegend> gegenden) {

        /** Die Arten für eine Gruppe mit {@code groesse} Mitgliedern. */
        public List<EntityType<? extends DeadzoneMonster>> mitglieder(RandomSource r, int groesse) {
            List<EntityType<? extends DeadzoneMonster>> l = new ArrayList<>();
            int n = Math.max(groesse, minGroesse);
            int bei = beimischung == null ? 0 : r.nextInt(hoechstensBei + 1);
            for (int i = 0; i < n; i++) {
                l.add(i < bei ? beimischung.get() : kern.get());
            }
            return l;
        }
    }

    private static final List<Thema> THEMEN = new ArrayList<>();

    private static List<Thema> themen() {
        if (THEMEN.isEmpty()) {
            Gegend s = Gegend.STADT, f = Gegend.FELD, w = Gegend.WALD, su = Gegend.SUMPF, wu = Gegend.WUESTE,
                    v = Gegend.VERSEUCHT, sch = Gegend.SCHNEE, g = Gegend.GEBIRGE;
            THEMEN.add(new Thema("infizierte", MonsterTypen.WANDERER, MonsterTypen.SCHREIER, 1, 1, false, 10,
                    EnumSet.of(s, f, w, sch, g)));
            THEMEN.add(new Thema("soldaten", MonsterTypen.GEFALLENER_SOLDAT, null, 0, 2, false, 3, EnumSet.of(s, f)));
            THEMEN.add(new Thema("rudel", MonsterTypen.SEUCHENHUND, null, 0, 3, false, 4, EnumSet.of(f, w, wu, sch, g)));
            THEMEN.add(new Thema("kriecher", MonsterTypen.KRIECHER, null, 0, 2, true, 2, EnumSet.of(s)));
            THEMEN.add(new Thema("moor", MonsterTypen.WANDERER, MonsterTypen.BRUT, 1, 1, false, 8, EnumSet.of(su)));
            THEMEN.add(new Thema("schwarm", MonsterTypen.SCHWARM, null, 0, 6, true, 3, EnumSet.of(su)));
            THEMEN.add(new Thema("brandopfer", MonsterTypen.WANDERER, MonsterTypen.GLUTLEIB, 2, 1, false, 8, EnumSet.of(wu)));
            THEMEN.add(new Thema("werksarbeiter", MonsterTypen.SEUCHENTRAEGER, MonsterTypen.GLUTLEIB, 1, 2, false, 8,
                    EnumSet.of(v)));
            THEMEN.add(new Thema("halde", MonsterTypen.WANDERER, MonsterTypen.SEUCHENTRAEGER, 2, 1, false, 4, EnumSet.of(v)));
        }
        return THEMEN;
    }

    /** Ein passendes Thema für eine Gruppe an dieser Stelle (Wasser und Höhle zählen wie Feld). */
    public static Thema thema(ServerLevel w, BlockPos pos, boolean stadt) {
        Gegend g = stadt ? Gegend.STADT : gegend(w, pos);
        if (g == Gegend.WASSER || g == Gegend.HOEHLE) {
            g = Gegend.FELD;
        }
        boolean nacht = w.isNight();
        List<Thema> moeglich = new ArrayList<>();
        int summe = 0;
        for (Thema t : themen()) {
            if (t.gegenden().contains(g) && (!t.nurNachts() || nacht)) {
                moeglich.add(t);
                summe += t.gewicht();
            }
        }
        int z = w.random.nextInt(Math.max(1, summe));
        for (Thema t : moeglich) {
            z -= t.gewicht();
            if (z < 0) {
                return t;
            }
        }
        return themen().get(0);
    }
}
