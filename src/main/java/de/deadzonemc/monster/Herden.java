package de.deadzonemc.monster;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.monster.boss.Boss;
import de.deadzonemc.monster.boss.BossSpawner;
import de.deadzonemc.welt.Staedte;
import de.deadzonemc.zuflucht.ZufluchtSchutz;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Statt einer festen Horde alle 7 Tage: wandernde Herden, die langsam übers Land ziehen, und zufällige
 * Überfälle. Was in einer Herde mitläuft, hängt von der Gegend ab (Stadt, Wald, Sumpf, Strahlenzone,
 * Wüste, Schnee) und von Tageszeit und Wetter.
 */
public final class Herden {

    private Herden() {
    }

    static TagKey<Biome> biomTag(String name) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name));
    }

    private static final TagKey<Biome> VERSEUCHT = biomTag("verseucht");
    private static final TagKey<Biome> WASSER = biomTag("wasser");
    private static final TagKey<Biome> WUESTE = biomTag("wueste");
    private static final TagKey<Biome> SCHNEE = biomTag("schnee");

    /** Eine wandernde Herde. */
    static final class Herde {
        final List<UUID> mitglieder = new ArrayList<>();
        Vec3 ziel;
        Vec3 richtung;
        boolean ueberfall;
        boolean fusionGeprueft;
        UUID hirte;

        Herde(Vec3 ziel, Vec3 richtung) {
            this.ziel = ziel;
            this.richtung = richtung;
        }
    }

    private static final List<Herde> HERDEN = new ArrayList<>();
    private static final List<Fusion> FUSIONEN = new ArrayList<>();

    // ---- Zusammensetzung ---------------------------------------------------------------------

    /** Welche Art als Nächstes in diese Herde kommt – je nach Gegend, Tageszeit und Wetter. */
    static EntityType<? extends DeadzoneMonster> mitglied(ServerLevel w, BlockPos pos, boolean stadt) {
        int r = w.random.nextInt(100);
        boolean nacht = w.isNight();
        var biom = w.getBiome(pos);
        if (DeadzoneMonster.dichterNebel(w) && r < 8) {
            return MonsterTypen.NEBELWESEN.get();
        }
        if (stadt) {
            if (r < 12) return MonsterTypen.SCHREIER.get();
            if (r < 24) return MonsterTypen.GEFALLENER_SOLDAT.get();
            if (r < 34) return MonsterTypen.KRIECHER.get();
            if (nacht && r < 40) return MonsterTypen.BRUT.get();
            return MonsterTypen.WANDERER.get();
        }
        if (biom.is(VERSEUCHT)) {
            if (r < 35) return MonsterTypen.SEUCHENTRAEGER.get();
            if (r < 55) return MonsterTypen.GLUTLEIB.get();
            return MonsterTypen.WANDERER.get();
        }
        if (biom.is(WASSER)) {
            if (r < 40) return MonsterTypen.ERTRUNKENER.get();
            if (r < 52) return MonsterTypen.BRUT.get();
            return MonsterTypen.WANDERER.get();
        }
        if (biom.is(WUESTE)) {
            if (r < 35) return MonsterTypen.GLUTLEIB.get();
            if (r < 55) return MonsterTypen.SEUCHENHUND.get();
            return MonsterTypen.WANDERER.get();
        }
        if (biom.is(SCHNEE)) {
            if (r < 25) return MonsterTypen.SEUCHENHUND.get();
            return MonsterTypen.WANDERER.get();
        }
        if (r < 22) return MonsterTypen.SEUCHENHUND.get();
        if (nacht && r < 32) return MonsterTypen.SAUGER.get();
        if (r < 38) return MonsterTypen.SCHREIER.get();
        return MonsterTypen.WANDERER.get();
    }

    // ---- Takt ---------------------------------------------------------------------------------

    public static void tick(ServerTickEvent.Post event) {
        ServerLevel w = event.getServer().overworld();
        int takt = event.getServer().getTickCount();
        Iterator<Fusion> it = FUSIONEN.iterator();
        while (it.hasNext()) {
            if (it.next().tick(w)) {
                it.remove();
            }
        }
        if (takt % 40 == 13) {
            herdenBewegen(w);
        }
        if (takt % 1200 != 600 || !w.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || w.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        for (ServerPlayer p : w.players()) {
            if (!DeadzoneMonster.echterSpieler(p) || ZufluchtSchutz.sicher(w, p.blockPosition())) {
                continue;
            }
            long nah = HERDEN.stream().filter(h -> h.ziel.distanceToSqr(p.position()) < 160 * 160).count();
            double chance = w.isNight() ? 0.35 : 0.2;
            if (DeadzoneMonster.dichterNebel(w)) {
                chance += 0.15;
            }
            if (nah < 2 && w.random.nextDouble() < chance) {
                herdeSpawnen(w, p.position(), p, false);
            }
            // Zufälliger Überfall: selten, nachts oder im Nebel, direkt auf den Spieler
            if ((w.isNight() || DeadzoneMonster.dichterNebel(w)) && w.random.nextInt(14) == 0) {
                ueberfall(w, p);
            }
        }
    }

    /** Spawnt eine Herde 70–100 Blöcke vom Spieler entfernt, die quer durch seine Gegend zieht. */
    public static Herde herdeSpawnen(ServerLevel w, Vec3 ort, ServerPlayer p, boolean ueberfall) {
        for (int versuch = 0; versuch < 12; versuch++) {
            double winkel = w.random.nextDouble() * Math.PI * 2;
            double abstand = ueberfall ? 42 + w.random.nextDouble() * 12 : 70 + w.random.nextDouble() * 30;
            int x = Mth.floor(ort.x + Math.cos(winkel) * abstand);
            int z = Mth.floor(ort.z + Math.sin(winkel) * abstand);
            if (!w.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos mitte = new BlockPos(x, y, z);
            if (!w.getFluidState(mitte.below()).isEmpty() && !w.getBiome(mitte).is(WASSER)) {
                continue;
            }
            if (ZufluchtSchutz.sicher(w, mitte)) {
                continue;
            }
            boolean stadt = Staedte.istStadt(w, x >> 4, z >> 4);
            int groesse = stadt ? 16 + w.random.nextInt(12) : 10 + w.random.nextInt(9);
            if (ueberfall) {
                groesse = 8 + w.random.nextInt(7);
            }
            // Richtung: grob am Spieler vorbei (oder direkt auf ihn beim Überfall)
            Vec3 zumSpieler = ort.subtract(Vec3.atCenterOf(mitte)).multiply(1, 0, 1).normalize();
            Vec3 richtung = ueberfall ? zumSpieler
                    : zumSpieler.yRot((float) ((w.random.nextDouble() - 0.5) * 1.2));
            Herde h = new Herde(ueberfall ? ort : Vec3.atCenterOf(mitte).add(richtung.scale(60)), richtung);
            h.ueberfall = ueberfall;
            int gespawnt = 0;
            for (int i = 0; i < groesse * 2 && gespawnt < groesse; i++) {
                BlockPos pos = boden(w, x + w.random.nextInt(11) - 5, z + w.random.nextInt(11) - 5);
                if (pos == null) {
                    continue;
                }
                EntityType<? extends DeadzoneMonster> typ = mitglied(w, pos, stadt);
                int anzahl = typ == MonsterTypen.SEUCHENHUND.get() ? 3 : 1; // Hunde kommen im Rudel
                for (int k = 0; k < anzahl && gespawnt < groesse; k++) {
                    DeadzoneMonster m = erschaffen(w, typ, pos, p);
                    if (m != null) {
                        h.mitglieder.add(m.getUUID());
                        gespawnt++;
                        if (ueberfall && p != null) {
                            m.setTarget(p);
                        }
                    }
                }
            }
            if (gespawnt == 0) {
                continue;
            }
            if (!ueberfall && w.random.nextInt(4) == 0) {
                DeadzoneMonster d = erschaffen(w, MonsterTypen.SPAEHERDROHNE.get(), new BlockPos(x, y + 10, z), p);
                if (d != null) {
                    h.mitglieder.add(d.getUUID());
                }
            }
            if (!ueberfall && !stadt && p != null && (w.isNight() || w.getDayTime() % 24000 > 11500)) {
                Boss hirte = BossSpawner.hirteFuerHerde(w, mitte, p);
                if (hirte != null) {
                    h.hirte = hirte.getUUID();
                    h.mitglieder.add(hirte.getUUID());
                }
            }
            HERDEN.add(h);
            return h;
        }
        return null;
    }

    private static DeadzoneMonster erschaffen(ServerLevel w, EntityType<? extends DeadzoneMonster> typ, BlockPos pos, ServerPlayer p) {
        DeadzoneMonster m = typ.create(w);
        if (m == null) {
            return null;
        }
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, w.random.nextFloat() * 360, 0);
        if (p != null && p.hasLineOfSight(m) && p.distanceToSqr(m) < 40 * 40) {
            return null;
        }
        EventHooks.finalizeMobSpawn(m, w, w.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        if (m.isSpawnCancelled()) {
            return null;
        }
        w.addFreshEntity(m);
        return m;
    }

    private static BlockPos boden(ServerLevel w, int x, int z) {
        if (!w.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (!w.getBlockState(pos.below()).isSolid() && w.getFluidState(pos.below()).isEmpty()) {
            return null;
        }
        return pos;
    }

    /** Ein Überfall: kündigt sich mit Geheul an, dann kommt eine Gruppe direkt auf den Spieler zu. */
    public static void ueberfall(ServerLevel w, ServerPlayer p) {
        Herde h = herdeSpawnen(w, p.position(), p, true);
        if (h != null) {
            p.displayClientMessage(Component.literal("Irgendwo in der Nähe heult etwas auf … sie haben deine Spur.")
                    .withStyle(ChatFormatting.DARK_RED), true);
            w.playSound(null, p.getX(), p.getY(), p.getZ(), MonsterSounds.holen("seuchenhund", "heulen"), SoundSource.HOSTILE, 1.5F, 0.6F);
        }
    }

    private static void herdenBewegen(ServerLevel w) {
        Iterator<Herde> it = HERDEN.iterator();
        while (it.hasNext()) {
            Herde h = it.next();
            List<DeadzoneMonster> lebend = new ArrayList<>();
            for (UUID id : h.mitglieder) {
                if (w.getEntity(id) instanceof DeadzoneMonster m && m.isAlive()) {
                    lebend.add(m);
                }
            }
            if (lebend.size() < 2) {
                it.remove();
                continue;
            }
            h.mitglieder.removeIf(id -> lebend.stream().noneMatch(m -> m.getUUID().equals(id)));
            // Schwerpunkt der Herde
            Vec3 mitte = Vec3.ZERO;
            for (DeadzoneMonster m : lebend) {
                mitte = mitte.add(m.position());
            }
            mitte = mitte.scale(1.0 / lebend.size());
            if (mitte.distanceToSqr(h.ziel) < 12 * 12) {
                // weiterziehen, leicht schwenkend
                h.richtung = h.richtung.yRot((float) ((w.random.nextDouble() - 0.5) * 0.9));
                h.ziel = mitte.add(h.richtung.scale(50));
            }
            // Wer einen Spieler entdeckt, reißt die ganze Herde mit
            net.minecraft.world.entity.LivingEntity ziel = null;
            for (DeadzoneMonster m : lebend) {
                if (m.getTarget() instanceof ServerPlayer sp && DeadzoneMonster.echterSpieler(sp)) {
                    ziel = sp;
                    break;
                }
            }
            for (DeadzoneMonster m : lebend) {
                if (ziel != null) {
                    if (m.getTarget() == null && m.distanceToSqr(ziel) < 40 * 40) {
                        m.setTarget(ziel);
                    }
                } else if (m.getTarget() == null && !(m instanceof Flieger) && m.getNavigation().isDone()) {
                    double ox = (m.getUUID().getLeastSignificantBits() % 7) - 3;
                    double oz = (m.getUUID().getMostSignificantBits() % 7) - 3;
                    m.getNavigation().moveTo(h.ziel.x + ox, h.ziel.y, h.ziel.z + oz, 0.75);
                }
            }
            if (ziel instanceof ServerPlayer sp && !h.fusionGeprueft && w.isNight() && lebend.size() >= 12) {
                h.fusionGeprueft = true;
                if (BossSpawner.darf(w, sp, "verschmolzener") && w.random.nextInt(10) == 0) {
                    fusionStarten(w, lebend, sp);
                }
            }
        }
    }

    // ---- Fusion zum Verschmolzenen -------------------------------------------------------------

    static void fusionStarten(ServerLevel w, List<DeadzoneMonster> herde, ServerPlayer p) {
        List<DeadzoneMonster> teil = new ArrayList<>();
        for (DeadzoneMonster m : herde) {
            if (m instanceof Wanderer && teil.size() < 10) {
                teil.add(m);
            }
        }
        if (teil.size() < 6) {
            return;
        }
        Vec3 mitte = Vec3.ZERO;
        for (DeadzoneMonster m : teil) {
            mitte = mitte.add(m.position());
        }
        mitte = mitte.scale(1.0 / teil.size());
        BlockPos b = boden(w, Mth.floor(mitte.x), Mth.floor(mitte.z));
        if (b == null) {
            return;
        }
        BossSpawner.vermerken(w, p, "verschmolzener");
        FUSIONEN.add(new Fusion(teil, Vec3.atBottomCenterOf(b)));
        p.displayClientMessage(Component.literal("Die Infizierten taumeln zusammen … etwas stimmt nicht.")
                .withStyle(ChatFormatting.DARK_RED), true);
    }

    /** Die Infizierten taumeln zu einem Punkt, kriechen ineinander, und der Verschmolzene wächst heraus. */
    public static final class Fusion {
        private final List<DeadzoneMonster> teile;
        private final Vec3 mitte;
        private int zeit;
        private int verschluckt;

        public Fusion(List<DeadzoneMonster> teile, Vec3 mitte) {
            this.teile = new ArrayList<>(teile);
            this.mitte = mitte;
        }

        /** true, wenn fertig. */
        boolean tick(ServerLevel w) {
            zeit++;
            Iterator<DeadzoneMonster> it = teile.iterator();
            while (it.hasNext()) {
                DeadzoneMonster m = it.next();
                if (!m.isAlive()) {
                    it.remove();
                    continue;
                }
                m.setTarget(null);
                if (zeit % 10 == 0) {
                    m.getNavigation().moveTo(mitte.x, mitte.y, mitte.z, 1.0);
                }
                if (m.position().distanceToSqr(mitte) < 2.2 * 2.2 || zeit > 160) {
                    w.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                            m.getX(), m.getY() + 1, m.getZ(), 40, 0.4, 0.8, 0.4, 0.2);
                    w.playSound(null, m.getX(), m.getY(), m.getZ(), MonsterSounds.holen("verschmolzener", "fusion"),
                            SoundSource.HOSTILE, 2.0F, 0.7F + w.random.nextFloat() * 0.3F);
                    m.discard();
                    it.remove();
                    verschluckt++;
                }
            }
            if (zeit % 8 == 0 && verschluckt > 0) {
                w.sendParticles(ParticleTypes.DAMAGE_INDICATOR, mitte.x, mitte.y + 0.5, mitte.z, 8 + verschluckt * 2,
                        0.5 + verschluckt * 0.1, 0.4, 0.5 + verschluckt * 0.1, 0.1);
            }
            if (teile.isEmpty() || zeit > 200) {
                if (verschluckt >= 4) {
                    Boss boss = MonsterTypen.VERSCHMOLZENER.get().create(w);
                    if (boss != null) {
                        boss.moveTo(mitte.x, mitte.y, mitte.z, w.random.nextFloat() * 360, 0);
                        boss.finalizeSpawn(w, w.getCurrentDifficultyAt(BlockPos.containing(mitte)), MobSpawnType.EVENT, null);
                        w.addFreshEntity(boss);
                    }
                }
                return true;
            }
            return false;
        }
    }

    /** Für den Selbsttest: Arten und Zahl der Mitglieder einer Herde. */
    public static String beschreiben(ServerLevel w, Herde h) {
        java.util.Map<String, Integer> z = new java.util.TreeMap<>();
        for (UUID id : h.mitglieder) {
            if (w.getEntity(id) != null) {
                z.merge(w.getEntity(id).getType().toShortString(), 1, Integer::sum);
            }
        }
        return z.toString();
    }

    public static int anzahl() {
        return HERDEN.size();
    }

    public static void leeren() {
        HERDEN.clear();
        FUSIONEN.clear();
    }
}
