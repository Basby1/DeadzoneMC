package de.deadzonemc.welt.bau;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Baut in einem lokalen Koordinatensystem, als stünde man auf der Straße und schaute auf das Grundstück:
 * <ul>
 *     <li>lx: von links (0) nach rechts</li>
 *     <li>lz: von vorne (0, an der Straße) nach hinten</li>
 *     <li>ly: Höhe über dem Boden (0 = Bodenebene des Grundstücks)</li>
 * </ul>
 * Richtungen werden im Entwurf als {@link #V} (vorne, zur Straße), {@link #H} (hinten), {@link #L} (links)
 * und {@link #R} (rechts) angegeben; der Baukasten dreht Blöcke wie Treppen und Türen automatisch passend.
 * Gebaut wird nur innerhalb des gerade erzeugten Bereichs.
 */
public final class Baukasten {

    /** Entwurfsrichtungen (im Entwurf ist die Straße im Norden). */
    public static final Direction V = Direction.NORTH;
    public static final Direction H = Direction.SOUTH;
    public static final Direction R = Direction.WEST;
    public static final Direction L = Direction.EAST;

    public final WorldGenLevel level;
    private final BoundingBox bereich;
    private final int ox;
    private final int oy;
    private final int oz;
    private final Direction hinten;
    private final Direction rechts;
    private final Rotation drehung;
    public final int breite;
    public final int tiefe;
    public final long saat;

    public Baukasten(WorldGenLevel level, BoundingBox bereich, int minX, int minZ, int maxX, int maxZ, int y,
                     Direction vorne, long saat) {
        this.level = level;
        this.bereich = bereich;
        this.hinten = vorne.getOpposite();
        this.rechts = hinten.getClockWise();
        this.oy = y;
        this.ox = (rechts.getStepX() > 0 || hinten.getStepX() > 0) ? minX : maxX;
        this.oz = (rechts.getStepZ() > 0 || hinten.getStepZ() > 0) ? minZ : maxZ;
        this.drehung = switch (vorne) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        boolean breiteEntlangX = rechts.getAxis() == Direction.Axis.X;
        this.breite = breiteEntlangX ? maxX - minX + 1 : maxZ - minZ + 1;
        this.tiefe = breiteEntlangX ? maxZ - minZ + 1 : maxX - minX + 1;
        this.saat = saat;
    }

    // ---- Koordinaten -------------------------------------------------------------------------

    public int wx(int lx, int lz) {
        return ox + rechts.getStepX() * lx + hinten.getStepX() * lz;
    }

    public int wz(int lx, int lz) {
        return oz + rechts.getStepZ() * lx + hinten.getStepZ() * lz;
    }

    public int wy(int ly) {
        return oy + ly;
    }

    public BlockPos pos(int lx, int ly, int lz) {
        return new BlockPos(wx(lx, lz), wy(ly), wz(lx, lz));
    }

    /** Wandelt eine Entwurfsrichtung in die echte Richtung um. */
    public Direction welt(Direction entwurf) {
        return drehung.rotate(entwurf);
    }

    /** Schritt in lx-Richtung für eine Entwurfsrichtung (R = +1, L = -1). */
    public static int dx(Direction d) {
        return d == R ? 1 : d == L ? -1 : 0;
    }

    /** Schritt in lz-Richtung für eine Entwurfsrichtung (H = +1, V = -1). */
    public static int dz(Direction d) {
        return d == H ? 1 : d == V ? -1 : 0;
    }

    /**
     * Ein Unter-Baukasten für einen Teilbereich (z. B. ein Auto in der Garage, ein Schuppen im Garten).
     * {@code vorneLokal} ist die Richtung, in die der Teil "schaut", in Entwurfsrichtungen dieses Baukastens.
     */
    public Baukasten teil(int lx1, int lz1, int lx2, int lz2, int ly, Direction vorneLokal, long teilSaat) {
        int ax = wx(lx1, lz1);
        int az = wz(lx1, lz1);
        int bx = wx(lx2, lz2);
        int bz = wz(lx2, lz2);
        return new Baukasten(level, bereich, Math.min(ax, bx), Math.min(az, bz), Math.max(ax, bx), Math.max(az, bz),
                wy(ly), welt(vorneLokal), teilSaat);
    }

    // ---- Zufall, der überall gleich ist ------------------------------------------------------

    public double zufall(int lx, int ly, int lz) {
        return Bau.zufall(saat, wx(lx, lz), wy(ly), wz(lx, lz));
    }

    public boolean chance(int lx, int ly, int lz, double p) {
        return zufall(lx, ly, lz) < p;
    }

    // ---- Blöcke setzen -----------------------------------------------------------------------

    public boolean imBereich(int lx, int ly, int lz) {
        return bereich == null || bereich.isInside(wx(lx, lz), wy(ly), wz(lx, lz));
    }

    public void setze(int lx, int ly, int lz, BlockState zustand) {
        if (!imBereich(lx, ly, lz)) {
            return;
        }
        int y = wy(ly);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight() - 1) {
            return;
        }
        level.setBlock(pos(lx, ly, lz), zustand.rotate(drehung), Block.UPDATE_CLIENTS);
    }

    public void setze(int lx, int ly, int lz, Block block) {
        setze(lx, ly, lz, block.defaultBlockState());
    }

    public BlockState hole(int lx, int ly, int lz) {
        return level.getBlockState(pos(lx, ly, lz));
    }

    /** Setzt nur, wenn dort Luft oder etwas Ersetzbares ist (z. B. Gras). */
    public void setzeFrei(int lx, int ly, int lz, BlockState zustand) {
        if (imBereich(lx, ly, lz) && Bau.ersetzbar(hole(lx, ly, lz))) {
            setze(lx, ly, lz, zustand);
        }
    }

    public void fuelle(int x1, int y1, int z1, int x2, int y2, int z2, BlockState zustand) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    setze(x, y, z, zustand);
                }
            }
        }
    }

    public void fuelle(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fuelle(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    public void luft(int x1, int y1, int z1, int x2, int y2, int z2) {
        fuelle(x1, y1, z1, x2, y2, z2, Bau.LUFT);
    }

    /** Nur die Außenwände eines Rechtecks. */
    public void waende(int x1, int z1, int x2, int z2, int y1, int y2, BlockState zustand) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                setze(x, y, z1, zustand);
                setze(x, y, z2, zustand);
            }
            for (int z = z1 + 1; z < z2; z++) {
                setze(x1, y, z, zustand);
                setze(x2, y, z, zustand);
            }
        }
    }

    /** Füllt unter einer Fläche bis zum Boden auf (ab ly = -1 nach unten). */
    public void fundament(int x1, int z1, int x2, int z2, BlockState zustand) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                if (!imBereich(x, 0, z)) {
                    continue;
                }
                for (int y = -1; y > -18; y--) {
                    BlockState da = hole(x, y, z);
                    if (!(da.isAir() || da.canBeReplaced() || !da.getFluidState().isEmpty())) {
                        break;
                    }
                    setze(x, y, z, zustand);
                }
            }
        }
    }

    /** Macht eine Fläche zum Boden: oberste Schicht setzen, darüber freiräumen. */
    public void boden(int x1, int z1, int x2, int z2, BlockState oben, int freiBis) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                setze(x, 0, z, oben);
                for (int y = 1; y <= freiBis; y++) {
                    if (!hole(x, y, z).isAir()) {
                        setze(x, y, z, Bau.LUFT);
                    }
                }
            }
        }
        fundament(x1, z1, x2, z2, Blocks.DIRT.defaultBlockState());
    }

    // ---- Bauteile ----------------------------------------------------------------------------

    public void kiste(int lx, int ly, int lz, BlockState behaelter, String beute) {
        if (!imBereich(lx, ly, lz)) {
            return;
        }
        setze(lx, ly, lz, behaelter);
        BlockEntity be = level.getBlockEntity(pos(lx, ly, lz));
        if (be instanceof RandomizableContainer inhalt) {
            inhalt.setLootTable(Bau.beute(beute), Bau.mischen(saat, wx(lx, lz), wy(ly), wz(lx, lz)));
        }
    }

    /** Kiste, die mit der Vorderseite in Richtung {@code blick} zeigt. */
    public void truhe(int lx, int ly, int lz, Direction blick, String beute) {
        kiste(lx, ly, lz, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING, blick), beute);
    }

    public void fass(int lx, int ly, int lz, Direction blick, String beute) {
        kiste(lx, ly, lz, Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, blick), beute);
    }

    public void tuer(int lx, int ly, int lz, Direction blick, Block tuerBlock, boolean offen) {
        BlockState t = tuerBlock.defaultBlockState().setValue(DoorBlock.FACING, blick).setValue(DoorBlock.OPEN, offen);
        setze(lx, ly, lz, t.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        setze(lx, ly + 1, lz, t.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** Bett: Kopfende an (lx, lz), das Fußende liegt entgegen {@code kopfRichtung}. */
    public void bett(int lx, int ly, int lz, Direction kopfRichtung, Block bett) {
        BlockState b = bett.defaultBlockState().setValue(BedBlock.FACING, kopfRichtung);
        setze(lx, ly, lz, b.setValue(BedBlock.PART, BedPart.HEAD));
        setze(lx - dx(kopfRichtung), ly, lz - dz(kopfRichtung), b.setValue(BedBlock.PART, BedPart.FOOT));
    }

    public void leiter(int lx, int ly1, int ly2, int lz, Direction blick) {
        for (int y = ly1; y <= ly2; y++) {
            setze(lx, y, lz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, blick));
        }
    }

    /** Glasscheibe, die sich seitlich mit den Wänden verbindet. {@code quer} = Scheibe verläuft entlang lx. */
    public BlockState scheibe(Block glas, boolean entlangLx) {
        BlockState s = glas.defaultBlockState();
        return entlangLx
                ? s.setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true)
                : s.setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
    }

    /** Zaunstück, das sich zu beiden Seiten verbindet. */
    public BlockState zaun(Block zaun, boolean entlangLx) {
        return scheibe(zaun, entlangLx);
    }

    /** Schild an einer Wand, Schrift zeigt in Richtung {@code blick}. Bis zu vier Zeilen. */
    public void schild(int lx, int ly, int lz, Direction blick, Block wandschild, DyeColor farbe, String... zeilen) {
        if (!imBereich(lx, ly, lz)) {
            return;
        }
        setze(lx, ly, lz, wandschild.defaultBlockState().setValue(WallSignBlock.FACING, blick));
        if (level.getBlockEntity(pos(lx, ly, lz)) instanceof SignBlockEntity schild) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, zeilen.length); i++) {
                text = text.setMessage(i, Component.literal(zeilen[i]));
            }
            text = text.setColor(farbe);
            // Während der Weltgenerierung gehört das Schild noch zu keiner Welt: setText() würde abstürzen.
            // Deshalb wie bei Minecrafts eigenen Bauwerken über gespeicherte Daten setzen.
            CompoundTag daten = new CompoundTag();
            SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, text).result().ifPresent(t -> daten.put("front_text", t));
            daten.putBoolean("is_waxed", true);
            schild.loadWithComponents(daten, level.registryAccess());
        }
    }
}
