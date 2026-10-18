package de.deadzonemc.zone;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ein Anomalienfeld: zwei bis vier Anomalien derselben Art, der Boden drumherum gezeichnet – verbrannt,
 * zerwühlt, verätzt oder mit Kupfer überzogen. Wer aufpasst, erkennt das Feld, bevor er hineinläuft.
 */
public class AnomalieFeature extends Feature<NoneFeatureConfiguration> {

    private static final TagKey<Biome> VERSEUCHT = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "verseucht"));
    private static final TagKey<Biome> WUESTE = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "wueste"));
    private static final TagKey<Biome> SUMPF = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "sumpfwald"));

    public AnomalieFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource z = ctx.random();
        BlockPos start = ctx.origin();
        var biom = level.getBiome(start);
        AnomalieArt art;
        int w = z.nextInt(100);
        if (biom.is(WUESTE)) {
            art = w < 60 ? AnomalieArt.GLUTFELD : w < 85 ? AnomalieArt.ENTLADUNG : AnomalieArt.STRUDEL;
        } else if (biom.is(SUMPF)) {
            art = w < 65 ? AnomalieArt.SAEURE : AnomalieArt.STRUDEL;
        } else if (biom.is(VERSEUCHT)) {
            art = AnomalieArt.values()[z.nextInt(4)];
        } else {
            art = w < 35 ? AnomalieArt.STRUDEL : w < 60 ? AnomalieArt.ENTLADUNG : w < 80 ? AnomalieArt.GLUTFELD : AnomalieArt.SAEURE;
        }
        int anzahl = 2 + z.nextInt(3);
        int gesetzt = 0;
        Heightmap.Types karte = level instanceof net.minecraft.server.level.WorldGenRegion
                ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.WORLD_SURFACE;
        for (int i = 0; i < anzahl * 4 && gesetzt < anzahl; i++) {
            int x = start.getX() + z.nextInt(13) - 6;
            int zz = start.getZ() + z.nextInt(13) - 6;
            int y = level.getHeight(karte, x, zz);
            BlockPos pos = new BlockPos(x, y, zz);
            BlockPos unten = pos.below();
            if (!level.getBlockState(unten).isSolid() || !level.getFluidState(unten).isEmpty() || !level.isEmptyBlock(pos)) {
                continue;
            }
            level.setBlock(pos, Zone.ANOMALIE.get().defaultBlockState().setValue(AnomalieBlock.ART, art), 2);
            gesetzt++;
            zeichnen(level, z, unten, art);
        }
        return gesetzt > 0;
    }

    /** Spuren am Boden um eine Anomalie. */
    private static void zeichnen(WorldGenLevel level, RandomSource z, BlockPos mitte, AnomalieArt art) {
        int r = art == AnomalieArt.STRUDEL ? 3 : 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1 || z.nextInt(3) == 0) {
                    continue;
                }
                BlockPos b = mitte.offset(dx, 0, dz);
                BlockState alt = level.getBlockState(b);
                if (!alt.isSolid() || alt.hasBlockEntity() || !level.getBlockState(b.above()).canBeReplaced()) {
                    continue;
                }
                BlockState neu = switch (art) {
                    case STRUDEL -> z.nextBoolean() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
                    case GLUTFELD -> z.nextInt(4) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                            : z.nextBoolean() ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
                    case ENTLADUNG -> z.nextBoolean() ? Blocks.WEATHERED_COPPER.defaultBlockState()
                            : Blocks.OXIDIZED_COPPER.defaultBlockState();
                    case SAEURE -> z.nextBoolean() ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MUD.defaultBlockState();
                };
                level.setBlock(b, neu, 2);
                // Pflanzen darüber verdorren
                if (!level.isEmptyBlock(b.above()) && level.getBlockState(b.above()).canBeReplaced()) {
                    level.setBlock(b.above(), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        if (art == AnomalieArt.STRUDEL && z.nextInt(2) == 0) {
            BlockPos k = mitte.offset(z.nextInt(5) - 2, 1, z.nextInt(5) - 2);
            if (level.isEmptyBlock(k) && level.getBlockState(k.below()).isSolid()) {
                level.setBlock(k, Blocks.SKELETON_SKULL.defaultBlockState(), 2);
            }
        }
    }
}
