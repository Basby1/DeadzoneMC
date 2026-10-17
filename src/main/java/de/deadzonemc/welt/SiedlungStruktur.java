package de.deadzonemc.welt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.deadzonemc.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;

/**
 * Eine verlassene Siedlung: eine Straße mit Häusern zu beiden Seiten – oder ein einzelnes Haus
 * irgendwo im Nirgendwo.
 */
public class SiedlungStruktur extends Structure {

    public static final MapCodec<SiedlungStruktur> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            Codec.intRange(1, 16).fieldOf("min_haeuser").forGetter(s -> s.minHaeuser),
            Codec.intRange(1, 16).fieldOf("max_haeuser").forGetter(s -> s.maxHaeuser),
            Codec.BOOL.fieldOf("strasse").forGetter(s -> s.strasse)
    ).apply(instance, SiedlungStruktur::new));

    private final int minHaeuser;
    private final int maxHaeuser;
    private final boolean strasse;

    public SiedlungStruktur(StructureSettings settings, int minHaeuser, int maxHaeuser, boolean strasse) {
        super(settings);
        this.minHaeuser = minHaeuser;
        this.maxHaeuser = Math.max(minHaeuser, maxHaeuser);
        this.strasse = strasse;
    }

    @Override
    public StructureType<?> type() {
        return ModRegistry.SIEDLUNG.get();
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int mx = context.chunkPos().getMiddleBlockX();
        int mz = context.chunkPos().getMiddleBlockZ();
        int y = oberflaeche(context, mx, mz);
        int meer = context.chunkGenerator().getSeaLevel();
        if (y < meer) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(mx, y, mz),
                builder -> aufbauen(builder, context, mx, mz, y)));
    }

    private static int oberflaeche(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
    }

    private void aufbauen(StructurePiecesBuilder builder, GenerationContext context, int mx, int mz, int y) {
        WorldgenRandom zufall = context.random();
        int meer = context.chunkGenerator().getSeaLevel();
        int anzahl = minHaeuser + zufall.nextInt(maxHaeuser - minHaeuser + 1);

        if (!strasse) {
            haus(builder, context, zufall, mx - 4, mz - 4, zufall.nextInt(4), meer, false);
            return;
        }

        boolean entlangX = zufall.nextBoolean();
        int proSeite = (anzahl + 1) / 2;
        int[] cursor = {0, 0};
        int gesamt = 0;
        // erst die Längen planen, damit die Straße mittig liegt
        int[][] plan = new int[anzahl][4]; // breiteLaengs, tiefeQuer, abstand, vorgarten
        for (int i = 0; i < anzahl; i++) {
            plan[i][0] = 7 + zufall.nextInt(5);
            plan[i][1] = 7 + zufall.nextInt(5);
            plan[i][2] = 3 + zufall.nextInt(5);
            plan[i][3] = 3 + zufall.nextInt(3);
        }
        int[] laenge = {0, 0};
        for (int i = 0; i < anzahl; i++) {
            laenge[i % 2] += plan[i][0] + plan[i][2];
        }
        gesamt = Math.max(laenge[0], laenge[1]);
        int start = -gesamt / 2;
        cursor[0] = start;
        cursor[1] = start + plan[0][0] / 2;

        for (int i = 0; i < anzahl; i++) {
            int seite = i % 2; // 0 = Plus-Seite, 1 = Minus-Seite
            int laengs = cursor[seite];
            cursor[seite] += plan[i][0] + plan[i][2];
            int quer = seite == 0 ? 3 + plan[i][3] : -(3 + plan[i][3]) - plan[i][1] + 1;
            int bx = entlangX ? mx + laengs : mx + quer;
            int bz = entlangX ? mz + quer : mz + laengs;
            int breite = entlangX ? plan[i][0] : plan[i][1];
            int tiefe = entlangX ? plan[i][1] : plan[i][0];
            // Tür zeigt zur Straße
            int tuer;
            if (entlangX) {
                tuer = seite == 0 ? 0 : 1; // Plus-Seite liegt südlich der Straße -> Tür nach Norden
            } else {
                tuer = seite == 0 ? 2 : 3;
            }
            hausMitMass(builder, context, zufall, bx, bz, breite, tiefe, tuer, meer);
        }
        builder.addPiece(new StrassePiece(entlangX, entlangX ? mz : mx,
                (entlangX ? mx : mz) + start - 4, (entlangX ? mx : mz) + start + gesamt + 4, y, zufall.nextLong()));
    }

    private void haus(StructurePiecesBuilder builder, GenerationContext context, WorldgenRandom zufall,
                      int x0, int z0, int tuer, int meer, boolean unused) {
        hausMitMass(builder, context, zufall, x0, z0, 7 + zufall.nextInt(5), 7 + zufall.nextInt(5), tuer, meer);
    }

    private void hausMitMass(StructurePiecesBuilder builder, GenerationContext context, WorldgenRandom zufall,
                             int x0, int z0, int breite, int tiefe, int tuer, int meer) {
        int y = oberflaeche(context, x0 + breite / 2, z0 + tiefe / 2);
        // Würfeln immer gleich oft, damit spätere Häuser nicht von übersprungenen abhängen
        int stockwerke = zufall.nextInt(3) == 0 ? 2 : 1;
        boolean keller = zufall.nextInt(3) == 0;
        int material = zufall.nextInt(6);
        float verfall = 0.15F + zufall.nextFloat() * 0.35F;
        long saat = zufall.nextLong();
        if (y < meer) {
            return; // nicht ins Wasser bauen
        }
        builder.addPiece(new HausPiece(x0, y, z0, breite, tiefe, stockwerke, keller, tuer, material, verfall, saat));
    }
}
