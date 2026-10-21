package de.deadzonemc.funk;

import com.mojang.serialization.MapCodec;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.zone.Emission;
import de.deadzonemc.zone.Luftabwurf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Funkstation für die Basis. Rechtsklick hört den Funk ab:
 * <ul>
 * <li>Zonenfunk: wann die nächste Emission kommt (ungefähr), ob gerade ein Versorgungsabwurf unterwegs ist.</li>
 * <li>Die Stimme: Einmal pro Spieltag empfängt man eine neue Sendung eines Unbekannten – zwölf Sendungen,
 * die Stück für Stück erzählen, was in der Zone geschehen ist.</li>
 * </ul>
 * Man stellt sie selbst her oder findet sie in Funktürmen.
 */
public final class Funk {

    private Funk() {
    }

    public static final int SENDUNGEN = 12;
    private static final String INDEX = "deadzonemc_funkstimme";
    private static final String TAG = "deadzonemc_funkstimme_tag";

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);

    public static class Station extends HorizontalDirectionalBlock {
        public Station(BlockBehaviour.Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return simpleCodec(Station::new);
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
            if (!level.isClientSide && p instanceof ServerPlayer sp) {
                abhoeren(sp, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static final DeferredBlock<Station> STATION = BLOECKE.register("funkstation",
            () -> new Station(BlockBehaviour.Properties.of().strength(2.5F, 6F).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .lightLevel(s -> 4)));
    public static final DeferredItem<BlockItem> STATION_ITEM = ITEMS.registerSimpleBlockItem(STATION);

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
    }

    public static void tabFuellen(CreativeModeTab.Output aus) {
        aus.accept(STATION_ITEM.get());
    }

    private static void abhoeren(ServerPlayer p, BlockPos pos) {
        ServerLevel ow = p.server.overworld();
        long zeit = ow.getGameTime();
        p.serverLevel().playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 0.6F, 0.5F);
        p.sendSystemMessage(Component.translatable("funk.deadzonemc.kopf").withStyle(ChatFormatting.DARK_GREEN));
        // Zonenfunk: Emission
        Emission e = Emission.holen(ow);
        if (e.stufe() > 0) {
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.emission.jetzt").withStyle(ChatFormatting.RED));
        } else if (e.naechste() > zeit) {
            long minuten = Math.max(1, Math.round((e.naechste() - zeit) / 1200.0 / 5.0) * 5);
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.emission.bald", minuten).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.emission.ruhig").withStyle(ChatFormatting.GRAY));
        }
        // Versorgungsabwurf
        long[] abwurf = Luftabwurf.holen(ow).letzter();
        if (abwurf != null) {
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.abwurf", Math.round(abwurf[0] / 25.0) * 25,
                    Math.round(abwurf[1] / 25.0) * 25).withStyle(ChatFormatting.GOLD));
        }
        // Wem gehört das Gebiet gerade?
        p.sendSystemMessage(Component.translatable("funk.deadzonemc.gebiet",
                de.deadzonemc.mensch.Gebiete.name(de.deadzonemc.mensch.Gebiete.herr(ow, pos.getX(), pos.getZ()))).withStyle(ChatFormatting.GRAY));
        // Die Stimme: eine neue Sendung pro Spieltag
        CompoundTag d = p.getPersistentData();
        int index = d.getInt(INDEX);
        long tag = ow.getDayTime() / 24000L;
        boolean neu = index < SENDUNGEN && (index == 0 || d.getLong(TAG) < tag);
        if (neu) {
            index++;
            d.putInt(INDEX, index);
            d.putLong(TAG, tag);
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.stimme." + index).withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
            p.serverLevel().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 0.6F);
        } else if (index >= SENDUNGEN) {
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.stimme.stille").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            p.sendSystemMessage(Component.translatable("funk.deadzonemc.stimme.morgen").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
