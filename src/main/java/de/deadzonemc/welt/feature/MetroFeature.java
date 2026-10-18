package de.deadzonemc.welt.feature;

import de.deadzonemc.welt.Bau;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Die Metro: Lost Cities legt unter den Städten Bahntunnel an. Dieses Feature findet die Gleise im Chunk und
 * macht daraus einen Ort mit Geschichte – flackernde Notlampen (oft kaputt), Schutt, Spinnweben, Ketten,
 * Blutspuren, liegengebliebene Waggons mit Beute und kleine Lager von Überlebenden.
 */
public class MetroFeature extends Feature<NoneFeatureConfiguration> {

    public MetroFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource z = ctx.random();
        int x0 = ctx.origin().getX() & ~15;
        int z0 = ctx.origin().getZ() & ~15;
        // Gleise suchen (grobes Raster reicht – Tunnel sind lang)
        BlockPos gleis = null;
        suche:
        for (int dx = 1; dx < 16; dx += 3) {
            for (int dz = 1; dz < 16; dz += 3) {
                for (int y = level.getMinBuildHeight() + 8; y < 100; y++) {
                    BlockPos p = new BlockPos(x0 + dx, y, z0 + dz);
                    if (level.getBlockState(p).getBlock() instanceof BaseRailBlock && level.isEmptyBlock(p.above())
                            && !level.canSeeSkyFromBelowWater(p)) {
                        gleis = p;
                        break suche;
                    }
                }
            }
        }
        if (gleis == null) {
            return false;
        }
        int y = gleis.getY();
        boolean entlangX = true;
        BlockState rail = level.getBlockState(gleis);
        if (rail.hasProperty(BlockStateProperties.RAIL_SHAPE_STRAIGHT)) {
            entlangX = rail.getValue(BlockStateProperties.RAIL_SHAPE_STRAIGHT) == RailShape.EAST_WEST;
        } else if (rail.hasProperty(BlockStateProperties.RAIL_SHAPE)) {
            entlangX = rail.getValue(BlockStateProperties.RAIL_SHAPE) == RailShape.EAST_WEST;
        }
        // Tunnel-Hohlraum in diesem Chunk ausschmücken
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos boden = new BlockPos(x0 + dx, y, z0 + dz);
                if (!level.isEmptyBlock(boden) && !(level.getBlockState(boden).getBlock() instanceof BaseRailBlock)) {
                    continue;
                }
                if (!level.getBlockState(boden.below()).isSolid()) {
                    continue;
                }
                int decke = deckenHoehe(level, boden);
                if (decke < 0) {
                    continue;
                }
                double r = z.nextDouble();
                boolean aufGleis = level.getBlockState(boden).getBlock() instanceof BaseRailBlock;
                if (!aufGleis) {
                    if (r < 0.05) {
                        level.setBlock(boden, z.nextBoolean() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLESTONE.defaultBlockState(), 2);
                    } else if (r < 0.08) {
                        level.setBlock(boden, Blocks.REDSTONE_WIRE.defaultBlockState(), 2); // Blutspur
                    } else if (r < 0.085) {
                        level.setBlock(boden, Blocks.SKELETON_SKULL.defaultBlockState()
                                .setValue(BlockStateProperties.ROTATION_16, z.nextInt(16)), 2);
                    } else if (r < 0.09) {
                        level.setBlock(boden, Blocks.BONE_BLOCK.defaultBlockState(), 2);
                    }
                }
                BlockPos oben = new BlockPos(boden.getX(), decke - 1, boden.getZ());
                double o = z.nextDouble();
                if (o < 0.08 && level.isEmptyBlock(oben)) {
                    level.setBlock(oben, Blocks.COBWEB.defaultBlockState(), 2);
                } else if (o < 0.1 && level.isEmptyBlock(oben)) {
                    level.setBlock(oben, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y), 2);
                }
            }
        }
        // Notlampen: ein paar hängende Laternen, viele sind ausgefallen
        for (int i = 0; i < 2; i++) {
            BlockPos l = new BlockPos(x0 + 2 + z.nextInt(12), y, z0 + 2 + z.nextInt(12));
            int decke = deckenHoehe(level, l);
            if (decke > 0 && z.nextFloat() < 0.45F) {
                BlockPos h = new BlockPos(l.getX(), decke - 1, l.getZ());
                if (level.isEmptyBlock(h)) {
                    level.setBlock(h, (z.nextBoolean() ? Blocks.SOUL_LANTERN : Blocks.LANTERN).defaultBlockState()
                            .setValue(LanternBlock.HANGING, true), 2);
                }
            }
        }
        double ereignis = z.nextDouble();
        if (ereignis < 0.16) {
            waggon(level, z, gleis, entlangX, x0, z0);
        } else if (ereignis < 0.26) {
            lager(level, z, gleis, entlangX, x0, z0);
        }
        return true;
    }

    /** Höhe der Tunneldecke über diesem Boden (oder -1, wenn offen oder zu hoch). */
    private static int deckenHoehe(WorldGenLevel level, BlockPos boden) {
        for (int h = 1; h < 9; h++) {
            if (!level.isEmptyBlock(boden.above(h)) && !(level.getBlockState(boden.above(h)).getBlock() instanceof BaseRailBlock)) {
                return h >= 2 ? boden.getY() + h : -1;
            }
        }
        return -1;
    }

    private static boolean imChunk(BlockPos p, int x0, int z0) {
        return p.getX() >= x0 && p.getX() < x0 + 16 && p.getZ() >= z0 && p.getZ() < z0 + 16;
    }

    /** Liegengebliebener Waggon auf den Gleisen: Eisenkasten mit offenen Türen, drinnen Beute und Tote. */
    private static void waggon(WorldGenLevel level, RandomSource z, BlockPos gleis, boolean entlangX, int x0, int z0) {
        int y = gleis.getY();
        int start = entlangX ? x0 + 2 : z0 + 2;
        int quer = entlangX ? gleis.getZ() : gleis.getX();
        for (int l = 0; l < 10; l++) {
            for (int q = -1; q <= 1; q++) {
                for (int h = 0; h <= 2; h++) {
                    BlockPos p = entlangX ? new BlockPos(start + l, y + h, quer + q) : new BlockPos(quer + q, y + h, start + l);
                    if (!imChunk(p, x0, z0)) {
                        continue;
                    }
                    boolean wand = l == 0 || l == 9 || q != 0 || h == 2;
                    boolean tuer = (l == 4 || l == 5) && q != 0 && h < 2;
                    boolean fenster = h == 1 && q != 0 && l % 3 == 1;
                    if (wand && !tuer) {
                        level.setBlock(p, fenster ? Blocks.IRON_BARS.defaultBlockState()
                                : (h == 2 ? Blocks.SMOOTH_STONE_SLAB.defaultBlockState() : Blocks.IRON_BLOCK.defaultBlockState()), 2);
                    } else if (!tuer) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        BlockPos kiste = entlangX ? new BlockPos(start + 2, y, quer) : new BlockPos(quer, y, start + 2);
        if (imChunk(kiste, x0, z0)) {
            level.setBlock(kiste, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(kiste) instanceof RandomizableContainerBlockEntity b) {
                b.setLootTable(Bau.beute("metro"), z.nextLong());
            }
        }
        BlockPos tot = entlangX ? new BlockPos(start + 7, y, quer) : new BlockPos(quer, y, start + 7);
        if (imChunk(tot, x0, z0)) {
            level.setBlock(tot, Blocks.SKELETON_SKULL.defaultBlockState(), 2);
        }
    }

    /** Kleines Lager von Überlebenden neben den Gleisen: Feuerstelle (aus), Schlafsack, Fass mit Vorräten. */
    private static void lager(WorldGenLevel level, RandomSource z, BlockPos gleis, boolean entlangX, int x0, int z0) {
        for (int seite : new int[]{2, -2, 3, -3}) {
            BlockPos m = entlangX ? gleis.offset(z.nextInt(5) - 2, 0, seite) : gleis.offset(seite, 0, z.nextInt(5) - 2);
            if (!imChunk(m, x0, z0) || !level.isEmptyBlock(m) || !level.getBlockState(m.below()).isSolid()) {
                continue;
            }
            level.setBlock(m, Blocks.CAMPFIRE.defaultBlockState().setValue(BlockStateProperties.LIT, false), 2);
            Direction d = entlangX ? Direction.EAST : Direction.SOUTH;
            BlockPos fass = m.relative(d);
            if (imChunk(fass, x0, z0) && level.isEmptyBlock(fass)) {
                level.setBlock(fass, Blocks.BARREL.defaultBlockState(), 2);
                if (level.getBlockEntity(fass) instanceof RandomizableContainerBlockEntity b) {
                    b.setLootTable(Bau.beute("metro"), z.nextLong());
                }
            }
            BlockPos decke = m.relative(d.getOpposite());
            if (imChunk(decke, x0, z0) && level.isEmptyBlock(decke)) {
                level.setBlock(decke, Blocks.BROWN_CARPET.defaultBlockState(), 2);
            }
            BlockPos licht = m.relative(d, 2);
            if (imChunk(licht, x0, z0) && level.isEmptyBlock(licht)) {
                level.setBlock(licht, Blocks.LANTERN.defaultBlockState(), 2);
            }
            return;
        }
    }
}
