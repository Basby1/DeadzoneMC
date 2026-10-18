package de.deadzonemc.geschichte;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.item.NotizItem;
import de.deadzonemc.monster.boss.Boss;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Die Hauptgeschichte in fünf Akten. Fortschritt gilt für die ganze Gruppe: Sobald irgendein Spieler das Ziel des
 * aktuellen Schritts erfüllt, geht es für alle weiter. Das Tagebuch (J) zeigt immer, was als Nächstes zu tun ist,
 * dazu ein Zielmarker mit Richtung und Entfernung. Nether und End sind ganz normal über Portale erreichbar.
 * Nach dem Finale läuft die Welt endlos weiter.
 */
public final class Geschichte {

    private Geschichte() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredItem<Item> KERNSPLITTER = ITEMS.registerSimpleItem("kernsplitter",
            new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.EPIC));

    /** Ein Schritt: id (Texte geschichte.deadzonemc.<id>.titel/.text), Akt, Ziel und was beim Start passiert. */
    public record Schritt(String id, int akt, BiPredicate<ServerPlayer, Fortschritt> ziel, Start start) {
    }

    @FunctionalInterface
    public interface Start {
        void ausfuehren(MinecraftServer server, Fortschritt f, ServerPlayer wer);
    }

    private static final Start NICHTS = (s, f, p) -> {
    };

    public static final List<Schritt> SCHRITTE = List.of(
            // Akt I – Die Abriegelung
            new Schritt("absturz", 1, (p, f) -> de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(p.serverLevel(), p.blockPosition()),
                    (s, f, p) -> struktur(s, f, p, "zuflucht")),
            new Schritt("verwalter", 1, (p, f) -> f.hat("verwalter"), NICHTS),
            new Schritt("funkgeraet", 1, (p, f) -> f.hat("funk"), NICHTS),
            // Akt II – Funkstille
            new Schritt("protokolle", 2, (p, f) -> {
                for (int i = 1; i <= 4; i++) {
                    if (!f.hat("protokoll_" + i)) {
                        return false;
                    }
                }
                return true;
            }, (s, f, p) -> kein(f)),
            new Schritt("kommandant", 2, (p, f) -> f.hat("tot:kommandant"), (s, f, p) -> ortWaehlen(s, f, p, 0)),
            // Akt III – Station Null
            new Schritt("station_null", 3, (p, f) -> de.deadzonemc.welt.station.StationNull.bei(p.serverLevel(), p.blockPosition()) != null,
                    (s, f, p) -> struktur(s, f, p, "station_null")),
            new Schritt("strahlende", 3, (p, f) -> f.hat("tot:strahlende"), NICHTS),
            new Schritt("hirte", 3, (p, f) -> f.hat("tot:hirte"), (s, f, p) -> {
                geben(s, NotizItem.erstellen("vahl_logbuch"));
                ortWaehlen(s, f, p, 0);
            }),
            // Akt IV – Herz der Zone
            new Schritt("riss", 4, (p, f) -> p.level().dimension() == Level.NETHER, (s, f, p) -> kein(f)),
            new Schritt("kernsplitter", 4, (p, f) -> anzahl(p, KERNSPLITTER.get()) >= 3, (s, f, p) -> kein(f)),
            new Schritt("herz", 4, (p, f) -> f.hat("tot:das_herz"), (s, f, p) -> ortWaehlen(s, f, p, 1)),
            // Akt V – Orbitalstation
            new Schritt("orbit", 5, (p, f) -> p.level().dimension() == Level.END, (s, f, p) -> station(f)),
            new Schritt("lebenserhaltung", 5, (p, f) -> f.hat("lebenserhaltung"), (s, f, p) -> station(f)),
            new Schritt("energieknoten", 5, (p, f) -> f.hat("knoten_alle"), (s, f, p) -> station(f)),
            new Schritt("ursprung", 5, (p, f) -> f.hat("tot:der_ursprung"), (s, f, p) -> station(f)),
            // Danach: die Welt läuft weiter
            new Schritt("epilog", 6, (p, f) -> false, (s, f, p) -> kein(f))
    );

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Geschichte::tick);
        NeoForge.EVENT_BUS.addListener(Geschichte::tod);
        NeoForge.EVENT_BUS.addListener(Geschichte::anmelden);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(KERNSPLITTER.get());
    }

    // ---- Ablauf -----------------------------------------------------------------------------

    private static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 9) {
            return;
        }
        Fortschritt f = Fortschritt.von(server);
        if (f.schritt >= SCHRITTE.size()) {
            return;
        }
        Schritt s = SCHRITTE.get(f.schritt);
        List<ServerPlayer> spieler = server.getPlayerList().getPlayers();
        if (spieler.isEmpty()) {
            return;
        }
        if (!f.gestartet) {
            f.gestartet = true;
            s.start().ausfuehren(server, f, spieler.get(0));
            f.setDirty();
            senden(server, f);
        }
        for (ServerPlayer p : spieler) {
            // Funkprotokolle zählen, sobald sie jemand bei sich hat
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (st.is(ModRegistry.NOTIZ_ITEM.get())) {
                    String id = NotizItem.id(st);
                    if (id != null && id.startsWith("funkprotokoll_")) {
                        f.merken("protokoll_" + id.substring("funkprotokoll_".length()));
                    }
                }
            }
            if (s.ziel().test(p, f)) {
                weiter(server, f, p);
                return;
            }
        }
        ortsGegner(server, f, s);
    }

    private static void weiter(MinecraftServer server, Fortschritt f, ServerPlayer wer) {
        Schritt alt = SCHRITTE.get(f.schritt);
        f.schritt++;
        f.gestartet = false;
        f.ziel = null;
        f.setDirty();
        Schritt neu = f.schritt < SCHRITTE.size() ? SCHRITTE.get(f.schritt) : null;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable("geschichte.deadzonemc." + alt.id() + ".titel").withStyle(ChatFormatting.GRAY)));
            Component abschluss = Component.translatable("geschichte.deadzonemc." + alt.id() + ".ende");
            p.sendSystemMessage(abschluss.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA));
            if (neu != null) {
                if (neu.akt() != alt.akt()) {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("geschichte.deadzonemc.akt." + neu.akt())
                            .withStyle(ChatFormatting.DARK_RED)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("geschichte.deadzonemc.akt." + neu.akt() + ".name")
                            .withStyle(ChatFormatting.GRAY)));
                    p.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.MASTER, 1.0F, 0.6F);
                } else {
                    p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.MASTER, 1.0F, 0.8F);
                }
                p.sendSystemMessage(Component.literal("➤ ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable("geschichte.deadzonemc." + neu.id() + ".titel").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("  (J: Tagebuch)").withStyle(ChatFormatting.DARK_GRAY)));
            }
        }
        senden(server, f);
    }

    /** Beim Verwalter einer Zuflucht (Auftraggeber): Gespräch und Funkgerät. Liefert true, wenn die Geschichte sprach. */
    public static boolean verwalter(ServerPlayer p) {
        Fortschritt f = Fortschritt.von(p.server);
        if (f.schritt >= SCHRITTE.size()) {
            return false;
        }
        String id = SCHRITTE.get(f.schritt).id();
        if (id.equals("verwalter")) {
            f.merken("verwalter");
            p.sendSystemMessage(Component.translatable("geschichte.deadzonemc.verwalter.rede").withStyle(ChatFormatting.GRAY));
            return true;
        }
        if (id.equals("funkgeraet")) {
            if (anzahl(p, ModRegistry.BATTERIE.get()) >= 3 && anzahl(p, ModRegistry.ELEKTRONIK.get()) >= 2) {
                nehmen(p, ModRegistry.BATTERIE.get(), 3);
                nehmen(p, ModRegistry.ELEKTRONIK.get(), 2);
                f.merken("funk");
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
                for (ServerPlayer x : p.server.getPlayerList().getPlayers()) {
                    x.sendSystemMessage(Component.translatable("geschichte.deadzonemc.funkgeraet.funkspruch")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
                }
            } else {
                p.sendSystemMessage(Component.translatable("geschichte.deadzonemc.funkgeraet.fehlt").withStyle(ChatFormatting.GRAY));
            }
            return true;
        }
        return false;
    }

    private static void tod(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) {
            return;
        }
        Entity e = event.getEntity();
        String typ = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
        if (typ.startsWith(DeadzoneMC.MODID + ":") && e instanceof Boss) {
            Fortschritt.von(sl.getServer()).merken("tot:" + typ.substring(DeadzoneMC.MODID.length() + 1));
        }
        // Kernsplitter: im Herz der Zone (Nether) tragen manche Wesen Splitter des Kerns in sich
        if (sl.dimension() == Level.NETHER && e instanceof Enemy && event.getSource().getEntity() instanceof Player
                && sl.random.nextFloat() < 0.12F) {
            e.spawnAtLocation(new ItemStack(KERNSPLITTER.get()));
        }
    }

    private static void anmelden(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Fortschritt f = Fortschritt.von(p.server);
            PacketDistributor.sendToPlayer(p, paket(f));
            if (f.schritt == 0 && !f.hat("intro:" + p.getUUID())) {
                f.merken("intro:" + p.getUUID());
                p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 90, 30));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("geschichte.deadzonemc.akt.1")
                        .withStyle(ChatFormatting.DARK_RED)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("geschichte.deadzonemc.akt.1.name")
                        .withStyle(ChatFormatting.GRAY)));
                p.sendSystemMessage(Component.translatable("geschichte.deadzonemc.intro", p.getName())
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }
        }
    }

    // ---- Hilfen -------------------------------------------------------------------------------

    private static GeschichtePayload paket(Fortschritt f) {
        BlockPos z = f.ziel;
        return new GeschichtePayload(f.schritt, z == null ? 0 : z.getX(), z == null ? 0 : z.getZ(), z == null ? -1 : f.zielWelt);
    }

    private static void senden(MinecraftServer server, Fortschritt f) {
        PacketDistributor.sendToAllPlayers(paket(f));
    }

    private static void station(Fortschritt f) {
        f.ziel = de.deadzonemc.orbit.Orbitalstation.MITTE;
        f.zielWelt = 2;
    }

    private static void kein(Fortschritt f) {
        f.ziel = null;
    }

    private static void struktur(MinecraftServer server, Fortschritt f, ServerPlayer p, String name) {
        ServerLevel ow = server.overworld();
        TagKey<Structure> tag = TagKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name));
        BlockPos von = p.level() == ow ? p.blockPosition() : ow.getSharedSpawnPos();
        BlockPos z = ow.findNearestMapStructure(tag, von, 80, false);
        f.ziel = z;
        f.zielWelt = 0;
    }

    /** Wählt einen Ort für einen Gegner der Geschichte, 320–420 Blöcke entfernt (welt: 0 Oberwelt, 1 Nether). */
    private static void ortWaehlen(MinecraftServer server, Fortschritt f, ServerPlayer p, int welt) {
        BlockPos von = p.blockPosition();
        double w = p.getRandom().nextDouble() * Math.PI * 2;
        double ab = welt == 1 ? 140 + p.getRandom().nextDouble() * 60 : 320 + p.getRandom().nextDouble() * 100;
        f.ziel = new BlockPos(Mth.floor(von.getX() + Math.cos(w) * ab), von.getY(), Mth.floor(von.getZ() + Math.sin(w) * ab));
        f.zielWelt = welt;
    }

    /** Am Zielort der Geschichte erscheint der Gegner, sobald jemand nah genug ist (und wieder, falls er verschwand). */
    private static void ortsGegner(MinecraftServer server, Fortschritt f, Schritt s) {
        String art = switch (s.id()) {
            case "kommandant" -> "kommandant";
            case "hirte" -> "hirte";
            case "herz" -> "das_herz";
            default -> null;
        };
        if (art == null || f.ziel == null) {
            return;
        }
        ServerLevel lv = f.zielWelt == 1 ? server.getLevel(Level.NETHER) : server.overworld();
        if (lv == null) {
            return;
        }
        EntityType<?> typ = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, art)).orElse(null);
        if (typ == null) {
            return;
        }
        for (ServerPlayer p : lv.players()) {
            double dx = p.getX() - f.ziel.getX();
            double dz = p.getZ() - f.ziel.getZ();
            if (dx * dx + dz * dz > 56 * 56) {
                continue;
            }
            if (!lv.getEntities(typ, new net.minecraft.world.phys.AABB(f.ziel).inflate(120, 80, 120), e -> e.isAlive()).isEmpty()) {
                return;
            }
            if (lv.getGameTime() - f.letzterSpawn < 1200) {
                return;
            }
            BlockPos wo = boden(lv, f.ziel, p);
            Entity e = typ.create(lv);
            if (!(e instanceof Boss b)) {
                return;
            }
            b.moveTo(wo.getX() + 0.5, wo.getY(), wo.getZ() + 0.5, lv.random.nextFloat() * 360, 0);
            b.setPersistenceRequired();
            EventHooks.finalizeMobSpawn(b, lv, lv.getCurrentDifficultyAt(wo), MobSpawnType.EVENT, null);
            lv.addFreshEntity(b);
            b.auftreten();
            f.letzterSpawn = lv.getGameTime();
            return;
        }
    }

    private static BlockPos boden(ServerLevel lv, BlockPos ziel, ServerPlayer p) {
        if (lv.dimension() == Level.NETHER) {
            // im Nether: auf Höhe des Spielers einen freien Platz suchen
            BlockPos b = new BlockPos(ziel.getX(), p.getBlockY(), ziel.getZ());
            for (int dy = 0; dy < 24; dy++) {
                for (int s : new int[]{dy, -dy}) {
                    BlockPos q = b.above(s);
                    if (lv.getBlockState(q).isAir() && lv.getBlockState(q.above()).isAir() && lv.getBlockState(q.below()).isSolid()) {
                        return q;
                    }
                }
            }
            return b;
        }
        int y = lv.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ziel.getX(), ziel.getZ());
        return new BlockPos(ziel.getX(), y, ziel.getZ());
    }

    private static void geben(MinecraftServer server, ItemStack st) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ItemStack k = st.copy();
            if (!p.getInventory().add(k)) {
                p.drop(k, false);
            }
        }
    }

    private static int anzahl(ServerPlayer p, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void nehmen(ServerPlayer p, Item item, int menge) {
        for (int i = 0; i < p.getInventory().getContainerSize() && menge > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                int x = Math.min(menge, s.getCount());
                s.shrink(x);
                menge -= x;
            }
        }
    }

    /** Für den Befehl /deadzone geschichte: aktuellen Schritt überspringen bzw. setzen. */
    public static void setzen(MinecraftServer server, int schritt) {
        Fortschritt f = Fortschritt.von(server);
        f.schritt = Mth.clamp(schritt, 0, SCHRITTE.size() - 1);
        f.gestartet = false;
        f.ziel = null;
        f.setDirty();
        senden(server, f);
    }
}
