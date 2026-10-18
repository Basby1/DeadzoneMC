package de.deadzonemc.orbit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Konsole der Lebenserhaltung bzw. Energieknoten der Orbitalstation (unzerstörbar, wird per Rechtsklick aktiviert). */
public class StationsBlock extends Block {

    public static final BooleanProperty AKTIV = BooleanProperty.create("aktiv");
    private final boolean konsole;

    public StationsBlock(Properties p, boolean konsole) {
        super(p);
        this.konsole = konsole;
        registerDefaultState(stateDefinition.any().setValue(AKTIV, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AKTIV);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState zustand, Level level, BlockPos pos, Player spieler, BlockHitResult treffer) {
        if (spieler instanceof ServerPlayer sp) {
            Orbitalstation.benutzt(sp, pos, konsole, zustand.getValue(AKTIV));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
