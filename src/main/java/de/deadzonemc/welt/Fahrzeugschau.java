package de.deadzonemc.welt;

import com.mojang.brigadier.context.CommandContext;
import de.deadzonemc.welt.bau.Baukasten;
import de.deadzonemc.welt.bau.Fahrzeuge;
import de.deadzonemc.welt.feature.AbsturzFeature;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * /deadzone fahrzeugschau – baut alle Fahrzeuge nebeneinander auf einer Wiese hoch über der Welt (zum Ansehen und
 * für die Testbilder). Mit einer Zahl dahinter springt man zu einem der Blickpunkte.
 */
public final class Fahrzeugschau {

    private Fahrzeugschau() {
    }

    /** Blickpunkte: Ziel (dx, dy, dz) und Kamera relativ zum Ziel, dazu ein Name. */
    private static final Object[][] BLICKE = {
            {4, 1, 2, -7, 5, -8, "autos_1"},
            {16, 1, 2, -7, 5, -8, "autos_2"},
            {31, 1, 2, -7, 5, -8, "autos_3"},
            {16, 1, 11, 6, 7, 14, "autos_ausgebrannt"},
            {50, 1, 3, -8, 5, -8, "traktor_loeschfahrzeug"},
            {10, 2, 35, -10, 7, -11, "panzer"},
            {60, 2, 35, -7, 5, -8, "lastwagen"},
            {60, 2, 35, -6, 16, -26, "konvoi"},
            {115, 2, 35, -16, 14, -20, "flugzeug"},
            {108, 2, 35, -4, 3, -5, "flugzeug_innen"},
            {60, 4, -40, -18, 14, -18, "frachter"},
    };

    public static int anzahlBlicke() {
        return BLICKE.length;
    }

    public static String blickName(int i) {
        return (String) BLICKE[i][6];
    }

    private static BlockPos ursprung(ServerLevel w) {
        BlockPos sp = w.getSharedSpawnPos();
        return new BlockPos(sp.getX() + 3000, 200, sp.getZ());
    }

    public static int befehl(CommandContext<CommandSourceStack> ctx, int blick) {
        ServerLevel w = ctx.getSource().getServer().overworld();
        BlockPos o = ursprung(w);
        if (blick < 0) {
            bauen(w, o);
            blick = 0;
        }
        if (ctx.getSource().getEntity() instanceof ServerPlayer p && blick < BLICKE.length) {
            Object[] b = BLICKE[blick];
            double zx = o.getX() + (int) b[0] + 0.5;
            double zy = o.getY() + (int) b[1];
            double zz = o.getZ() + (int) b[2] + 0.5;
            double kx = zx + (int) b[3];
            double ky = zy + (int) b[4];
            double kz = zz + (int) b[5];
            double dx = zx - kx;
            double dy = zy - ky;
            double dz = zz - kz;
            float gier = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90F;
            float neigung = (float) (-Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
            p.teleportTo(w, kx, ky, kz, gier, neigung);
            int nr = blick;
            ctx.getSource().sendSuccess(() -> Component.literal("Fahrzeugschau: " + blickName(nr)), false);
        }
        return 1;
    }

    /** Wiese, darauf alle Autos (oben normal, darunter ausgebrannt), Traktor, Löschfahrzeug und die großen Wracks. */
    public static void bauen(ServerLevel w, BlockPos o) {
        BlockState gras = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState erde = Blocks.DIRT.defaultBlockState();
        BlockState luft = Blocks.AIR.defaultBlockState();
        for (int x = -30; x <= 140; x++) {
            for (int z = -60; z <= 60; z++) {
                BlockPos p = o.offset(x, -1, z);
                w.setBlock(p.below(), erde, Block.UPDATE_CLIENTS);
                w.setBlock(p, gras, Block.UPDATE_CLIENTS);
                for (int h = 1; h <= 14; h++) {
                    if (!w.getBlockState(p.above(h)).isAir()) {
                        w.setBlock(p.above(h), luft, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        int n = Fahrzeuge.anzahlArten();
        for (int i = 0; i < n; i++) {
            int x = o.getX() + i * 6;
            Baukasten heil = new Baukasten(w, null, x, o.getZ(), x + 2, o.getZ() + 4, o.getY(), Direction.NORTH, 1000L + i);
            Fahrzeuge.testAuto(heil, i, false);
            Baukasten brand = new Baukasten(w, null, x, o.getZ() + 9, x + 2, o.getZ() + 13, o.getY(), Direction.NORTH, 2000L + i);
            Fahrzeuge.testAuto(brand, i, true);
        }
        Fahrzeuge.traktor(new Baukasten(w, null, o.getX() + 45, o.getZ(), o.getX() + 47, o.getZ() + 3, o.getY(),
                Direction.NORTH, 3001L), 3001L);
        Fahrzeuge.loeschfahrzeug(new Baukasten(w, null, o.getX() + 51, o.getZ(), o.getX() + 53, o.getZ() + 6, o.getY(),
                Direction.NORTH, 3002L), 3002L);
        AbsturzFeature.testBauen(w, w.random, o.offset(10, 0, 35), AbsturzFeature.Art.PANZER, Direction.EAST);
        AbsturzFeature.testBauen(w, w.random, o.offset(60, 0, 35), AbsturzFeature.Art.KONVOI, Direction.EAST);
        AbsturzFeature.testBauen(w, w.random, o.offset(115, 0, 35), AbsturzFeature.Art.FLUGZEUG, Direction.EAST);
        AbsturzFeature.testBauen(w, w.random, o.offset(60, 0, -40), AbsturzFeature.Art.FRACHTER, Direction.EAST);
    }
}
