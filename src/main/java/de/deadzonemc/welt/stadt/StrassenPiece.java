package de.deadzonemc.welt.stadt;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Ein Straßenabschnitt (höchstens ein paar Blöcke lang, auf einer Höhe): Asphalt mit Rissen,
 * Mittellinie, Zebrastreifen vor Kreuzungen, erhöhte Gehwege mit Bordstein und kaputte Laternen.
 * Außerhalb des Ortes: Landstraße mit Bankett.
 */
public class StrassenPiece extends StructurePiece {

    private final boolean entlangX;
    private final int quer;
    private final int halb;
    private final int gehweg;
    private final int von;
    private final int bis;
    private final int y;
    private final int yVor;
    private final int yNach;
    private final boolean imOrt;
    private final boolean mittellinie;
    /** Fahrbahnen anderer Straßen, die diesen Abschnitt kreuzen: je 4 Zahlen minX, minZ, maxX, maxZ. */
    private final int[] kreuzungen;
    private final long saat;

    public StrassenPiece(boolean entlangX, int quer, int halb, int gehweg, int von, int bis, int y, int yVor, int yNach,
                         boolean imOrt, boolean mittellinie, int[] kreuzungen, long saat) {
        super(ModRegistry.STRASSEN_TEIL.get(), 0, rahmen(entlangX, quer, halb + gehweg, von, bis, y));
        this.entlangX = entlangX;
        this.quer = quer;
        this.halb = halb;
        this.gehweg = gehweg;
        this.von = von;
        this.bis = bis;
        this.y = y;
        this.yVor = yVor;
        this.yNach = yNach;
        this.imOrt = imOrt;
        this.mittellinie = mittellinie;
        this.kreuzungen = kreuzungen;
        this.saat = saat;
    }

    public StrassenPiece(CompoundTag tag) {
        super(ModRegistry.STRASSEN_TEIL.get(), tag);
        this.entlangX = tag.getBoolean("x");
        this.quer = tag.getInt("quer");
        this.halb = tag.getInt("halb");
        this.gehweg = tag.getInt("geh");
        this.von = tag.getInt("von");
        this.bis = tag.getInt("bis");
        this.y = tag.getInt("y");
        this.yVor = tag.getInt("yv");
        this.yNach = tag.getInt("yn");
        this.imOrt = tag.getBoolean("ort");
        this.mittellinie = tag.getBoolean("mitte");
        this.kreuzungen = tag.getIntArray("kreuz");
        this.saat = tag.getLong("saat");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putBoolean("x", entlangX);
        tag.putInt("quer", quer);
        tag.putInt("halb", halb);
        tag.putInt("geh", gehweg);
        tag.putInt("von", von);
        tag.putInt("bis", bis);
        tag.putInt("y", y);
        tag.putInt("yv", yVor);
        tag.putInt("yn", yNach);
        tag.putBoolean("ort", imOrt);
        tag.putBoolean("mitte", mittellinie);
        tag.putIntArray("kreuz", kreuzungen);
        tag.putLong("saat", saat);
    }

    private static BoundingBox rahmen(boolean entlangX, int quer, int hb, int von, int bis, int y) {
        return entlangX
                ? new BoundingBox(von, y, quer - hb, bis, y + 6, quer + hb)
                : new BoundingBox(quer - hb, y, von, quer + hb, y + 6, bis);
    }

    private int wx(int a, int q) {
        return entlangX ? a : quer + q;
    }

    private int wz(int a, int q) {
        return entlangX ? quer + q : a;
    }

    /** Liegt der Punkt auf der Fahrbahn einer kreuzenden Straße? */
    private boolean aufKreuzung(int x, int z, int rand) {
        for (int i = 0; i + 3 < kreuzungen.length; i += 4) {
            if (x >= kreuzungen[i] - rand && x <= kreuzungen[i + 2] + rand
                    && z >= kreuzungen[i + 1] - rand && z <= kreuzungen[i + 3] + rand) {
                return true;
            }
        }
        return false;
    }

    private static final Block[] RISSE = {Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE,
            Blocks.TUFF, Blocks.GRAVEL, Blocks.MOSS_BLOCK, Blocks.COARSE_DIRT};

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox bereich, ChunkPos chunkPos, BlockPos pivot) {
        int ganz = halb + gehweg;
        for (int a = von; a <= bis; a++) {
            boolean rampeHoch = (a == bis && yNach == y + 1) || (a == von && yVor == y + 1);
            for (int q = -ganz; q <= ganz; q++) {
                int x = wx(a, q);
                int z = wz(a, q);
                if (!bereich.isInside(x, y, z)) {
                    continue;
                }
                double w = Bau.zufall(saat, x, y, z);
                boolean fahrbahn = Math.abs(q) <= halb;
                boolean kreuzung = aufKreuzung(x, z, 0);
                // Unterbau auffüllen
                for (int yy = y - 1; yy > y - 12; yy--) {
                    BlockState da = Bau.holen(level, x, yy, z);
                    if (!(da.isAir() || da.canBeReplaced() || !da.getFluidState().isEmpty())) {
                        break;
                    }
                    Bau.setzen(level, bereich, x, yy, z, Blocks.STONE.defaultBlockState());
                }
                // Oberfläche
                BlockState oben;
                BlockState darauf = Bau.LUFT;
                if (fahrbahn || kreuzung) {
                    oben = asphalt(w);
                    if (!kreuzung && imOrt) {
                        oben = markierung(a, q, x, z, w, oben);
                    } else if (!kreuzung && !imOrt && Math.abs(q) == halb && Math.floorMod(a, 8) < 5 && w > 0.3) {
                        oben = Blocks.WHITE_CONCRETE.defaultBlockState(); // Randlinie der Landstraße
                    }
                    if (rampeHoch) {
                        darauf = Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                    }
                } else if (imOrt) {
                    // Gehweg: erhöht als Stufe, am Bordstein etwas dunkler
                    oben = Blocks.STONE.defaultBlockState();
                    Block platte = Math.abs(q) == halb + 1 ? Blocks.STONE_BRICK_SLAB : Blocks.SMOOTH_STONE_SLAB;
                    if (w < 0.06) {
                        platte = Blocks.COBBLESTONE_SLAB;
                    }
                    darauf = rampeHoch ? Blocks.SMOOTH_STONE.defaultBlockState()
                            : platte.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                    if (w > 0.97) {
                        oben = Blocks.GRASS_BLOCK.defaultBlockState(); // Gras bricht durch
                        darauf = Blocks.SHORT_GRASS.defaultBlockState();
                    }
                } else {
                    oben = w < 0.5 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(); // Bankett
                }
                Bau.setzen(level, bereich, x, y, z, oben);
                Bau.setzen(level, bereich, x, y + 1, z, darauf);
                for (int yy = y + 2; yy <= y + 6; yy++) {
                    if (!Bau.holen(level, x, yy, z).isAir()) {
                        Bau.setzen(level, bereich, x, yy, z, Bau.LUFT);
                    }
                }
            }
            // Laternen am äußeren Gehwegrand, abwechselnd links und rechts
            if (imOrt && gehweg > 0 && Math.floorMod(a, 14) == 0) {
                int seite = Math.floorMod(Math.floorDiv(a, 14), 2) == 0 ? 1 : -1;
                int q = seite * ganz;
                if (!aufKreuzung(wx(a, q), wz(a, q), 3)) {
                    laterne(level, bereich, wx(a, q), wz(a, q));
                }
            }
        }
    }

    private static BlockState asphalt(double w) {
        if (w < 0.10) {
            return RISSE[(int) (w * 1000) % RISSE.length].defaultBlockState();
        }
        return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }

    private BlockState markierung(int a, int q, int x, int z, double w, BlockState asphalt) {
        // Zebrastreifen kurz vor einer Kreuzung
        boolean vorKreuzung = aufKreuzung(x, z, 4) && !aufKreuzung(x, z, 1);
        if (vorKreuzung && Math.floorMod(q, 2) == 0) {
            return w < 0.8 ? Blocks.WHITE_CONCRETE.defaultBlockState() : asphalt;
        }
        if (mittellinie && q == 0 && Math.floorMod(a, 6) < 3 && !aufKreuzung(x, z, 6)) {
            return w < 0.75 ? Blocks.WHITE_CONCRETE.defaultBlockState() : asphalt;
        }
        return asphalt;
    }

    private void laterne(WorldGenLevel level, BoundingBox bereich, int x, int z) {
        if (!bereich.isInside(x, y, z)) {
            return;
        }
        boolean umgeknickt = Bau.chance(saat, x, y + 7, z, 0.3);
        int hoehe = umgeknickt ? 2 : 4;
        for (int i = 1; i <= hoehe; i++) {
            Bau.setzen(level, bereich, x, y + i, z, Blocks.ANDESITE_WALL.defaultBlockState());
        }
        if (!umgeknickt) {
            // Die Lampe ist aus – Strom gibt es keinen mehr.
            Bau.setzen(level, bereich, x, y + 5, z, Blocks.REDSTONE_LAMP.defaultBlockState());
        }
    }
}
