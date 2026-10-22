package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Das Wrack der Kilo-2 am Strand (Boot-Intro): Der Bug liegt auf dem Sand, das Heck im Wasser. Grauer Rumpf mit rotem
 * Unterwasseranstrich und einem Loch in der Flanke, Steuerhaus mit zerbrochenen Scheiben, umgestürzter Mast,
 * Außenborder, Bänke, Ladung mit Beute, Treibgut ringsum.
 */
public final class Bootswrack {

    private Bootswrack() {
    }

    private record Rahmen(ServerLevel w, BlockPos o, Direction lang, Direction quer, int[] boden) {
        /** l: 0 = Uferlinie, + = Richtung Meer; b: quer; h: über dem Rumpfboden dieses Abschnitts. */
        BlockPos pos(int l, int b, int h) {
            return o.relative(lang, l).relative(quer, b).atY(boden[Math.max(0, Math.min(boden.length - 1, l + 6))] + h);
        }

        void setzen(int l, int b, int h, BlockState s) {
            w.setBlock(pos(l, b, h), s, Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState tr(Block b, Direction hoch, boolean kopf) {
        return b.defaultBlockState().setValue(StairBlock.FACING, hoch).setValue(StairBlock.HALF, kopf ? Half.TOP : Half.BOTTOM);
    }

    private static BlockState st(Block b, boolean oben) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, oben ? SlabType.TOP : SlabType.BOTTOM);
    }

    public static void bauen(ServerLevel w, BlockPos ufer, Vec3 meer, RandomSource r) {
        Direction lang = Direction.getNearest(meer.x, 0, meer.z);
        Direction quer = lang.getClockWise();
        int see = w.getSeaLevel();
        // Rumpfboden je Abschnitt: folgt dem Gelände, im Wasser liegt das Heck auf Grund knapp unter der Oberfläche
        int[] boden = new int[18];
        for (int i = 0; i < boden.length; i++) {
            BlockPos p = ufer.relative(lang, i - 6);
            w.getChunk(p.getX() >> 4, p.getZ() >> 4);
            int y = w.getHeight(Heightmap.Types.OCEAN_FLOOR, p.getX(), p.getZ());
            boden[i] = Math.max(see - 2, Math.min(y, see + 2));
        }
        // Treppenstufen glätten: höchstens ein Block Unterschied zum Nachbarn
        for (int i = 1; i < boden.length; i++) {
            boden[i] = Math.max(boden[i - 1] - 1, Math.min(boden[i - 1] + 1, boden[i]));
        }
        Rahmen f = new Rahmen(w, ufer, lang, quer, boden);
        BlockState grau = Blocks.GRAY_CONCRETE.defaultBlockState();
        BlockState rot = Blocks.RED_TERRACOTTA.defaultBlockState();
        BlockState deck = st(Blocks.SPRUCE_SLAB, true);
        Block grauTreppe = Blocks.POLISHED_ANDESITE_STAIRS;
        BlockState luft = Bau.LUFT;
        // Rumpf l -4..8, b -2..2; Bug läuft spitz zu (l -5, -4)
        for (int l = -5; l <= 8; l++) {
            int halb = l == -5 ? 0 : l == -4 ? 1 : 2;
            for (int b = -halb; b <= halb; b++) {
                boolean rand = Math.abs(b) == halb;
                Direction innen = b < 0 ? quer : quer.getOpposite();
                f.setzen(l, b, 0, rot);
                for (int h = 1; h <= 2; h++) {
                    BlockState s;
                    if (rand) {
                        s = h == 2 && halb > 0 ? tr(grauTreppe, innen, false) : grau;
                        if (l <= -4 && h == 2) {
                            s = tr(grauTreppe, lang, false); // Bugspitze
                        }
                    } else {
                        s = h == 1 ? deck : luft;
                    }
                    f.setzen(l, b, h, s);
                }
                for (int h = 3; h <= 7; h++) {
                    f.setzen(l, b, h, luft);
                }
            }
        }
        // Loch in der Flanke (hier ist es aufgeschlagen)
        f.setzen(3, -2, 1, luft);
        f.setzen(4, -2, 1, luft);
        f.setzen(3, -2, 0, Blocks.WATER.defaultBlockState());
        // Steuerhaus l 0..2, b -2..2, h 2..4; Scheiben zerbrochen, Dach als Platten
        for (int l = 0; l <= 2; l++) {
            for (int b = -2; b <= 2; b++) {
                boolean wand = Math.abs(b) == 2 || l == 0 || l == 2;
                for (int h = 2; h <= 4; h++) {
                    BlockState s = wand ? grau : luft;
                    if (wand && h == 3) {
                        s = r.nextInt(3) == 0 ? luft : Blocks.TINTED_GLASS.defaultBlockState();
                    }
                    if (b == 2 && l == 1 && h <= 3) {
                        s = luft; // Tür
                    }
                    f.setzen(l, b, h, s);
                }
                f.setzen(l, b, 5, st(Blocks.SMOOTH_STONE_SLAB, false));
            }
        }
        f.setzen(1, 0, 6, Blocks.REDSTONE_LAMP.defaultBlockState()); // zerschlagener Suchscheinwerfer
        f.setzen(1, -1, 2, tr(Blocks.SPRUCE_STAIRS, lang, false)); // Steuerstand
        f.setzen(1, 1, 2, Blocks.LEVER.defaultBlockState());
        // Mast umgestürzt: liegt quer über dem Bug bis in den Sand
        for (int k = 0; k < 7; k++) {
            BlockPos p = f.pos(1 - k, 3 + k / 2, 3).atY(Math.max(f.pos(1 - k, 3 + k / 2, 0).getY() + 1,
                    w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, f.pos(1 - k, 3 + k / 2, 0).getX(), f.pos(1 - k, 3 + k / 2, 0).getZ())));
            w.setBlock(p, Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, lang.getAxis()), Block.UPDATE_CLIENTS);
        }
        // Bänke an Deck, Ladung mit Beute
        for (int l = 3; l <= 6; l++) {
            f.setzen(l, -1, 2, tr(Blocks.SPRUCE_STAIRS, quer.getOpposite(), false));
            f.setzen(l, 1, 2, tr(Blocks.SPRUCE_STAIRS, quer, false));
        }
        kiste(w, f.pos(7, 0, 2), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), "militaer", r);
        kiste(w, f.pos(7, 1, 2), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), "apotheke", r);
        kiste(w, f.pos(1, 0, 2), Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, lang.getOpposite()),
                r.nextInt(3) == 0 ? "waffenkammer" : "militaer", r);
        kiste(w, f.pos(6, -1, 3), Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), "werkzeug", r);
        // Außenborder am Heck
        for (int b : new int[]{-1, 1}) {
            for (int h = 0; h <= 2; h++) {
                f.setzen(9, b, h, h == 2 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.BLACK_CONCRETE.defaultBlockState());
            }
        }
        // Treibgut am Strand: leere Fässer, Planken, eine Rettungsweste, Seil
        for (int i = 0; i < 10; i++) {
            BlockPos p = ufer.relative(lang, -r.nextInt(9)).relative(quer, r.nextInt(17) - 8);
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
            BlockPos q = new BlockPos(p.getX(), y, p.getZ());
            if (!w.getBlockState(q).isAir() || !w.getFluidState(q.below()).isEmpty()) {
                continue;
            }
            BlockState s = switch (i % 5) {
                case 0 -> Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, quer);
                case 1 -> Blocks.SPRUCE_SLAB.defaultBlockState();
                case 2 -> Blocks.ORANGE_CARPET.defaultBlockState();
                case 3 -> Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, quer.getAxis());
                default -> st(Blocks.SMOOTH_STONE_SLAB, false);
            };
            w.setBlock(q, s, Block.UPDATE_CLIENTS);
        }
    }

    private static void kiste(ServerLevel w, BlockPos p, BlockState s, String tabelle, RandomSource r) {
        Bau.kiste(w, null, p.getX(), p.getY(), p.getZ(), s, Bau.beute(tabelle), r.nextLong());
    }
}
