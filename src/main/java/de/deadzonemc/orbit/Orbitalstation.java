package de.deadzonemc.orbit;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.geschichte.Fortschritt;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.boss.DerUrsprung;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Die Orbitalstation im End: Statt des Enderdrachen steht auf der Hauptinsel die Station mit dem Reaktorring.
 * Questreihe: Lebenserhaltung starten (Konsole + 4 Elektronik), vier Energieknoten mit Kernsplittern hochfahren
 * (jeder Knoten spielt ein Logbuch der KI ab), dann erwacht der Ursprung mit einer langen Auftrittssequenz.
 * Der Rückweg (Endportal) ist von Anfang an offen.
 */
public final class Orbitalstation {

    private Orbitalstation() {
    }

    public static final BlockPos MITTE = new BlockPos(0, 0, 90);
    private static final int R = 28;

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredBlock<StationsBlock> KONSOLE = BLOECKE.register("lebenserhaltung",
            () -> new StationsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.METAL).lightLevel(s -> s.getValue(StationsBlock.AKTIV) ? 12 : 3), true));
    public static final DeferredBlock<StationsBlock> KNOTEN = BLOECKE.register("energieknoten",
            () -> new StationsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F)
                    .sound(SoundType.AMETHYST).lightLevel(s -> s.getValue(StationsBlock.AKTIV) ? 15 : 2), false));
    public static final DeferredItem<BlockItem> KONSOLE_ITEM = ITEMS.registerSimpleBlockItem(KONSOLE);
    public static final DeferredItem<BlockItem> KNOTEN_ITEM = ITEMS.registerSimpleBlockItem(KNOTEN);

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Orbitalstation::beitritt);
        NeoForge.EVENT_BUS.addListener(Orbitalstation::tick);
    }

    // ---- Kein Enderdrache -------------------------------------------------------------------

    private static void beitritt(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof EnderDragon drache && event.getLevel() instanceof ServerLevel sl
                && sl.dimension() == Level.END) {
            // Der Drache gilt als besiegt: kein Bosskampf mit ihm, das Rückkehrportal ist offen
            sl.getServer().execute(() -> {
                var kampf = sl.getDragonFight();
                if (kampf != null) {
                    kampf.setDragonKilled(drache);
                }
                drache.discard();
            });
        }
    }

    // ---- Station bauen und Ablauf -----------------------------------------------------------

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 3) {
            return;
        }
        ServerLevel end = server.getLevel(Level.END);
        if (end == null || end.players().isEmpty()) {
            return;
        }
        Fortschritt f = Fortschritt.von(server);
        if (!f.hat("station_gebaut")) {
            end.getChunk(MITTE.getX() >> 4, MITTE.getZ() >> 4);
            int y = hoehe(end);
            bauen(end, y);
            f.merken("station_gebaut");
            Stationsausbau.bauen(end, y, MITTE.getX(), MITTE.getZ(), R);
            f.merken("station_ausbau_1");
        } else if (!f.hat("station_ausbau_1")) {
            // Bestehende Welten: Station steht schon – Plattformhöhe am Kraterkern suchen und ausbauen
            for (int y = end.getMaxBuildHeight() - 1; y > end.getMinBuildHeight(); y--) {
                if (end.getBlockState(new BlockPos(MITTE.getX(), y, MITTE.getZ())).is(Blocks.CRYING_OBSIDIAN)) {
                    Stationsausbau.bauen(end, y, MITTE.getX(), MITTE.getZ(), R);
                    break;
                }
            }
            f.merken("station_ausbau_1");
        }
        if (f.hat("knoten_alle") && !f.hat("ursprung_erwacht")) {
            erwachen(server, end, f);
        }
    }

    public static int hoehe(ServerLevel end) {
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                end.getChunk((MITTE.getX() + dx) >> 4, (MITTE.getZ() + dz) >> 4);
            }
        }
        int y = end.getHeight(Heightmap.Types.WORLD_SURFACE, MITTE.getX(), MITTE.getZ());
        return y < 40 ? 64 : y;
    }

    private static void bauen(ServerLevel w, int y) {
        bauen(w, y, MITTE.getX(), MITTE.getZ());
    }

    /** Für den Selbsttest: die Station an beliebiger Stelle errichten. */
    public static void testBauen(ServerLevel w, BlockPos mitte) {
        bauen(w, mitte.getY(), mitte.getX(), mitte.getZ());
        Stationsausbau.bauen(w, mitte.getY(), mitte.getX(), mitte.getZ(), R);
    }

    private static void bauen(ServerLevel w, int y, int cx, int cz) {
        BlockState boden = Blocks.WHITE_CONCRETE.defaultBlockState();
        BlockState rand = Blocks.LIGHT_GRAY_CONCRETE.defaultBlockState();
        BlockState wand = Blocks.IRON_BLOCK.defaultBlockState();
        BlockState glas = Blocks.CYAN_STAINED_GLASS.defaultBlockState();
        BlockState licht = Blocks.SEA_LANTERN.defaultBlockState();
        BlockState stuetze = Blocks.END_STONE_BRICKS.defaultBlockState();
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > R + 0.5) {
                    continue;
                }
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                BlockState b = (dx % 6 == 0 && dz % 6 == 0) ? licht : d > R - 2 ? rand : boden;
                w.setBlock(p, b, Block.UPDATE_CLIENTS);
                // nichts schwebt: Stützen bis zur Insel (oder ins Leere, dann eben Station am Felsen)
                BlockPos u = p.below();
                for (int i = 0; i < 24 && w.getBlockState(u).isAir() && (Math.abs(dx) % 5 == 0 || d > R - 1); i++) {
                    w.setBlock(u, stuetze, Block.UPDATE_CLIENTS);
                    u = u.below();
                }
                for (int h = 1; h <= 7; h++) {
                    w.setBlock(p.above(h), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                // Außenwand mit Fenstern, Tor im Süden (Richtung +z) und Norden
                if (d > R - 0.5 && !(Math.abs(dx) <= 2)) {
                    for (int h = 1; h <= 5; h++) {
                        w.setBlock(p.above(h), h == 3 || h == 4 ? glas : wand, Block.UPDATE_CLIENTS);
                    }
                }
                // Reaktorring (Radius 8): erhöhte Kante mit violettem Glas
                if (d > 7.5 && d < 9) {
                    w.setBlock(p.above(1), Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                    w.setBlock(p.above(2), (dx + dz) % 3 == 0 ? Blocks.PURPLE_STAINED_GLASS.defaultBlockState()
                            : Blocks.CRYING_OBSIDIAN.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                if (d < 7.5) {
                    w.setBlock(p, d < 3 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.OBSIDIAN.defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
        // Deckungssäulen gegen den Leerepuls
        for (int i = 0; i < 6; i++) {
            double wk = 2 * Math.PI * i / 6 + 0.5;
            int px = cx + (int) Math.round(Math.cos(wk) * 15);
            int pz = cz + (int) Math.round(Math.sin(wk) * 15);
            for (int h = 1; h <= 6; h++) {
                for (int ox = 0; ox <= 1; ox++) {
                    for (int oz = 0; oz <= 1; oz++) {
                        w.setBlock(new BlockPos(px + ox, y + h, pz + oz), h == 6 ? licht : Blocks.QUARTZ_PILLAR.defaultBlockState(),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // Vier Energieknoten (N, O, S, W) auf Sockeln
        int[][] knoten = {{0, -21}, {21, 0}, {0, 21}, {-21, 0}};
        for (int[] k : knoten) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    w.setBlock(new BlockPos(cx + k[0] + ox, y + 1, cz + k[1] + oz), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
            w.setBlock(new BlockPos(cx + k[0], y + 2, cz + k[1]), KNOTEN.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        // Steg vom Nordtor zur Hauptinsel, damit man trockenen Fußes hinkommt
        for (int z = cz - R - 1; z > cz - R - 120; z--) {
            boolean angekommen = true;
            for (int ox = -1; ox <= 1; ox++) {
                int g = w.getHeight(Heightmap.Types.WORLD_SURFACE, cx + ox, z);
                if (g < y - 1) {
                    angekommen = false;
                    w.setBlock(new BlockPos(cx + ox, y, z), ox == 0 ? boden : rand, Block.UPDATE_CLIENTS);
                }
            }
            if (angekommen) {
                break;
            }
        }
        // Lebenserhaltung am Südtor
        w.setBlock(new BlockPos(cx + 3, y + 1, cz + 24), KONSOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        w.setBlock(new BlockPos(cx + 3, y + 2, cz + 24), Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState(), Block.UPDATE_CLIENTS);
        // Logbücher in Spinden
        String[] logs = {"orbit_log_1", "orbit_log_2"};
        for (int i = 0; i < logs.length; i++) {
            BlockPos kp = new BlockPos(cx - 3 - i * 2, y + 1, cz + 25);
            w.setBlock(kp, Blocks.BARREL.defaultBlockState(), Block.UPDATE_CLIENTS);
            if (w.getBlockEntity(kp) instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity fass) {
                fass.setItem(0, de.deadzonemc.item.NotizItem.erstellen(logs[i]));
                fass.setItem(1, new net.minecraft.world.item.ItemStack(ModRegistry.ELEKTRONIK.get(), 2));
            }
        }
    }

    /** Ein Spieler benutzt einen Stationsblock (Konsole oder Knoten). */
    public static void benutzt(ServerPlayer p, BlockPos pos, boolean konsole, boolean schonAktiv) {
        Fortschritt f = Fortschritt.von(p.server);
        if (schonAktiv) {
            return;
        }
        if (konsole) {
            Item e = ModRegistry.ELEKTRONIK.get();
            if (p.getInventory().countItem(e) < 4 && !p.isCreative()) {
                p.sendSystemMessage(Component.translatable("orbit.deadzonemc.konsole.fehlt").withStyle(ChatFormatting.GRAY));
                return;
            }
            entnehmen(p, e, 4);
            p.serverLevel().setBlockAndUpdate(pos, p.serverLevel().getBlockState(pos).setValue(StationsBlock.AKTIV, true));
            f.merken("lebenserhaltung");
            ki(p.server, "orbit.deadzonemc.ki.lebenserhaltung");
            p.serverLevel().playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.6F);
            return;
        }
        if (!f.hat("lebenserhaltung")) {
            p.sendSystemMessage(Component.translatable("orbit.deadzonemc.knoten.kein_strom").withStyle(ChatFormatting.GRAY));
            return;
        }
        Item splitter = de.deadzonemc.geschichte.Geschichte.KERNSPLITTER.get();
        if (p.getInventory().countItem(splitter) < 1 && !p.isCreative()) {
            p.sendSystemMessage(Component.translatable("orbit.deadzonemc.knoten.fehlt").withStyle(ChatFormatting.GRAY));
            return;
        }
        entnehmen(p, splitter, 1);
        ServerLevel w = p.serverLevel();
        w.setBlockAndUpdate(pos, w.getBlockState(pos).setValue(StationsBlock.AKTIV, true));
        w.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2.0F, 0.7F);
        w.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.3, 2, 0.3, 0.05);
        int n = 0;
        for (int i = 1; i <= 4; i++) {
            if (f.hat("knoten_" + i)) {
                n++;
            }
        }
        n++;
        f.merken("knoten_" + n);
        ki(p.server, "orbit.deadzonemc.ki.knoten_" + n);
        // Jeder Knoten weckt die Wächter der Station
        for (int i = 0; i < 2 + n; i++) {
            var m = EntityType.ENDERMAN.create(w);
            if (m != null) {
                m.moveTo(pos.getX() + w.random.nextInt(9) - 4, pos.getY(), pos.getZ() + w.random.nextInt(9) - 4, 0, 0);
                m.finalizeSpawn(w, w.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                m.setTarget(p);
                w.addFreshEntity(m);
            }
        }
        if (n >= 4) {
            f.merken("knoten_alle");
        }
    }

    private static void entnehmen(ServerPlayer p, Item item, int menge) {
        if (p.isCreative()) {
            return;
        }
        for (int i = 0; i < p.getInventory().getContainerSize() && menge > 0; i++) {
            var s = p.getInventory().getItem(i);
            if (s.is(item)) {
                int x = Math.min(menge, s.getCount());
                s.shrink(x);
                menge -= x;
            }
        }
    }

    private static void ki(MinecraftServer server, String schluessel) {
        Component z = Component.literal("[MUTTER] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.translatable(schluessel).withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(z);
            p.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, 1.0F, 0.5F);
        }
    }

    // ---- Auftritt des Ursprungs -----------------------------------------------------------------

    private static int sequenz = -1;

    private static void erwachen(MinecraftServer server, ServerLevel end, Fortschritt f) {
        sequenz++;
        int y = end.getHeight(Heightmap.Types.WORLD_SURFACE, MITTE.getX(), MITTE.getZ() + 12);
        BlockPos mitte = new BlockPos(MITTE.getX(), hoehe(end) + 1, MITTE.getZ());
        switch (sequenz) {
            case 0 -> ki(server, "orbit.deadzonemc.ki.erwachen_1");
            case 3 -> ki(server, "orbit.deadzonemc.ki.erwachen_2");
            case 6 -> ki(server, "orbit.deadzonemc.ki.erwachen_3");
            default -> {
            }
        }
        // Beben, Blitze und eine Säule aus Licht werden immer stärker
        for (ServerPlayer p : end.players()) {
            PacketDistributor.sendToPlayer(p, new de.deadzonemc.netz.SchreckPayload(-Math.min(4, 1 + sequenz / 2)));
        }
        end.sendParticles(ParticleTypes.REVERSE_PORTAL, mitte.getX() + 0.5, mitte.getY() + 4, mitte.getZ() + 0.5,
                60 + sequenz * 30, 3, 8, 3, 0.15);
        if (sequenz % 2 == 0) {
            LightningBolt b = EntityType.LIGHTNING_BOLT.create(end);
            if (b != null) {
                b.moveTo(mitte.getX() + end.random.nextInt(17) - 8, mitte.getY(), mitte.getZ() + end.random.nextInt(17) - 8);
                b.setVisualOnly(true);
                end.addFreshEntity(b);
            }
        }
        end.playSound(null, mitte, SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 6.0F, 0.5F + sequenz * 0.05F);
        if (sequenz < 10) {
            return;
        }
        DerUrsprung u = MonsterTypen.DER_URSPRUNG.get().create(end);
        if (u != null) {
            u.moveTo(mitte.getX() + 0.5, mitte.getY(), mitte.getZ() + 0.5, 180, 0);
            u.setPersistenceRequired();
            u.finalizeSpawn(end, end.getCurrentDifficultyAt(mitte), MobSpawnType.EVENT, null);
            end.addFreshEntity(u);
            u.auftreten();
            u.schildAufbauen(4, 21);
        }
        f.merken("ursprung_erwacht");
        sequenz = -1;
        if (y < 0) {
            return;
        }
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(KONSOLE_ITEM.get());
        ausgabe.accept(KNOTEN_ITEM.get());
    }
}
