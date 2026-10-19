package de.deadzonemc.welt.deko;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Tote Landschaft: Spuren der Menschen an den Wänden. Plakate (Vermisst, Evakuierung, Quarantäne, Aufruf der
 * Wacht) und Kreidezeichen der Suchtrupps (Kreuz, Pfeil, Suchmarkierung, „HILFE“). Hängen in Städten und an Ruinen;
 * man kann sie auch selbst herstellen und in der Basis aufhängen.
 */
public final class Wanddeko {

    private Wanddeko() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.FEATURE, DeadzoneMC.MODID);

    /** Ein flaches Bild an der Wand (wie eine Leiter angebracht, aber nicht zum Klettern). */
    public static class Bild extends LadderBlock {
        public static final IntegerProperty MOTIV = IntegerProperty.create("motiv", 0, 3);

        public Bild(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(MOTIV, 0));
        }

        @Override
        public MapCodec<LadderBlock> codec() {
            return simpleCodec(Bild::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            super.createBlockStateDefinition(b);
            b.add(MOTIV);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            BlockState s = super.getStateForPlacement(ctx);
            return s == null ? null : s.setValue(MOTIV, ctx.getLevel().getRandom().nextInt(4));
        }
    }

    private static BlockBehaviour.Properties art(SoundType klang) {
        return BlockBehaviour.Properties.of().noCollission().instabreak().sound(klang).pushReaction(PushReaction.DESTROY).noOcclusion();
    }

    public static final DeferredBlock<Bild> PLAKAT = BLOECKE.register("plakat", () -> new Bild(art(SoundType.WOOL)));
    public static final DeferredBlock<Bild> KREIDEZEICHEN = BLOECKE.register("kreidezeichen", () -> new Bild(art(SoundType.CALCITE)));
    public static final DeferredItem<BlockItem> PLAKAT_ITEM = ITEMS.registerSimpleBlockItem(PLAKAT);
    public static final DeferredItem<BlockItem> KREIDEZEICHEN_ITEM = ITEMS.registerSimpleBlockItem(KREIDEZEICHEN);

    public static final net.neoforged.neoforge.registries.DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FEATURE =
            FEATURES.register("wanddeko", Feature_::new);

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
    }

    public static void tabFuellen(CreativeModeTab.Output aus) {
        aus.accept(PLAKAT_ITEM.get());
        aus.accept(KREIDEZEICHEN_ITEM.get());
    }

    /** Hängt an Hauswände (keine natürlichen Felsen) ein, zwei Plakate oder Kreidezeichen. */
    public static class Feature_ extends Feature<NoneFeatureConfiguration> {
        public Feature_() {
            super(NoneFeatureConfiguration.CODEC);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
            WorldGenLevel w = ctx.level();
            RandomSource r = ctx.random();
            BlockPos o = ctx.origin();
            int gesetzt = 0;
            for (int versuch = 0; versuch < 10 && gesetzt < 2; versuch++) {
                int x = (o.getX() & ~15) + r.nextInt(16);
                int z = (o.getZ() & ~15) + r.nextInt(16);
                int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos p = new BlockPos(x, y + r.nextInt(2), z);
                if (!w.getBlockState(p).isAir()) {
                    continue;
                }
                for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(r)) {
                    BlockPos wand = p.relative(d);
                    BlockState ws = w.getBlockState(wand);
                    if (!ws.isFaceSturdy(w, wand, d.getOpposite()) || natuerlich(ws)) {
                        continue;
                    }
                    Bild b = r.nextInt(3) == 0 ? KREIDEZEICHEN.get() : PLAKAT.get();
                    w.setBlock(p, b.defaultBlockState().setValue(LadderBlock.FACING, d.getOpposite())
                            .setValue(Bild.MOTIV, r.nextInt(4)), Block.UPDATE_CLIENTS);
                    gesetzt++;
                    break;
                }
            }
            return gesetzt > 0;
        }

        private static boolean natuerlich(BlockState s) {
            return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.LOGS)
                    || s.is(BlockTags.LEAVES) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE) || s.is(BlockTags.TERRACOTTA)
                    || s.is(net.minecraft.world.level.block.Blocks.GRAVEL) || s.is(net.minecraft.world.level.block.Blocks.CLAY);
        }
    }
}
