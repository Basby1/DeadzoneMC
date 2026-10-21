package de.deadzonemc.strom;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.basis.Basen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Strom-Netz der Basis: Was im Schutzbereich eines Notstromgenerators steht, hat Strom.
 * <ul>
 * <li>Scheinwerfer: leuchtet hell, solange er Strom hat (und blendet z. B. die Schwarmmutter).</li>
 * <li>Elektrozaun: unter Strom bekommen Monster, die ihn berühren, einen Schlag und werden zurückgeworfen.</li>
 * <li>Panzertür: Stahltür, die Infizierte nicht aufbrechen können – Spieler öffnen sie von Hand.</li>
 * </ul>
 */
public final class Strom {

    private Strom() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final BooleanProperty STROM = BooleanProperty.create("strom");

    /** Hat diese Stelle Strom (liegt sie im Schutzbereich eines Generators)? */
    public static boolean hatStrom(ServerLevel level, BlockPos pos) {
        return Basen.holen(level).basisBei(pos.getX() + 0.5, pos.getZ() + 0.5, 0) != null;
    }

    // ---- Scheinwerfer -------------------------------------------------------------------------
    public static class Scheinwerfer extends HorizontalDirectionalBlock {
        public Scheinwerfer(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STROM, false));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(Scheinwerfer::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING, STROM);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState alt, boolean bewegt) {
            level.scheduleTick(pos, this, 2);
        }

        @Override
        protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
            boolean an = hatStrom(level, pos);
            if (s.getValue(STROM) != an) {
                level.setBlock(pos, s.setValue(STROM, an), 3);
            }
            level.scheduleTick(pos, this, 100);
        }
    }

    // ---- Elektrozaun --------------------------------------------------------------------------
    public static class Elektrozaun extends IronBarsBlock {
        public Elektrozaun(BlockBehaviour.Properties p) {
            super(p);
        }

        @Override
        protected void onPlace(BlockState s, Level level, BlockPos pos, BlockState alt, boolean bewegt) {
            super.onPlace(s, level, pos, alt, bewegt);
            level.scheduleTick(pos, this, 10);
        }

        @Override
        protected void tick(BlockState s, ServerLevel level, BlockPos pos, RandomSource r) {
            level.scheduleTick(pos, this, 10);
            if (!hatStrom(level, pos)) {
                return;
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(0.45),
                    x -> x instanceof Enemy && x.isAlive())) {
                if (e.hurt(level.damageSources().lightningBolt(), 3.0F)) {
                    Vec3 weg = e.position().subtract(Vec3.atCenterOf(pos)).multiply(1, 0, 1).normalize();
                    e.push(weg.x * 0.7, 0.25, weg.z * 0.7);
                    e.hurtMarked = true;
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.4, 0.3, 0.1);
                    level.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.BLOCKS, 0.8F, 1.8F);
                }
            }
            if (r.nextInt(40) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
    }

    // ---- Panzertür ----------------------------------------------------------------------------
    public static class Panzertuer extends DoorBlock {
        public Panzertuer(BlockBehaviour.Properties p) {
            super(BlockSetType.IRON, p);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            setOpen(p, level, s, pos, !s.getValue(BlockStateProperties.OPEN));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final DeferredBlock<Scheinwerfer> SCHEINWERFER = BLOECKE.register("scheinwerfer",
            () -> new Scheinwerfer(BlockBehaviour.Properties.of().strength(1.5F, 6F).sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(STROM) ? 15 : 0)));
    public static final DeferredBlock<Elektrozaun> ELEKTROZAUN = BLOECKE.register("elektrozaun",
            () -> new Elektrozaun(BlockBehaviour.Properties.of().strength(4F, 6F).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops()));
    public static final DeferredBlock<Panzertuer> PANZERTUER = BLOECKE.register("panzertuer",
            () -> new Panzertuer(BlockBehaviour.Properties.of().strength(8F, 1200F).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> SCHEINWERFER_ITEM = ITEMS.registerSimpleBlockItem(SCHEINWERFER);
    public static final DeferredItem<BlockItem> ELEKTROZAUN_ITEM = ITEMS.registerSimpleBlockItem(ELEKTROZAUN);
    public static final DeferredItem<DoubleHighBlockItem> PANZERTUER_ITEM = ITEMS.register("panzertuer",
            () -> new DoubleHighBlockItem(PANZERTUER.get(), new Item.Properties()));

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
    }

    public static void tabFuellen(CreativeModeTab.Output aus) {
        aus.accept(SCHEINWERFER_ITEM.get());
        aus.accept(ELEKTROZAUN_ITEM.get());
        aus.accept(PANZERTUER_ITEM.get());
    }
}
