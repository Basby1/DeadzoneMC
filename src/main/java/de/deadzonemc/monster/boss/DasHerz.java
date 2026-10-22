package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterSounds;
import de.deadzonemc.monster.MonsterTypen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.List;

/**
 * Das Herz der Zone – Endboss des Nethers. Steht fest im Glutsee (bewegt sich nicht), dreht sich aber zu seinen Zielen.
 * Jeder große Angriff wird sichtbar angekündigt – wer liest, kann ausweichen, wer zögert, brennt:
 * <ul>
 *     <li>Glutsäulen: der Boden unter den Spielern glüht, kurz darauf schießt Glut hervor (weglaufen).</li>
 *     <li>Glutwelle: das Herz rammt beide Arme in den See, ein Feuerring rast nach außen (drüberspringen). Danach liegt der Kern frei.</li>
 *     <li>Armfeger: ein Halbkreis vor ihm glüht, dann fegt ein Arm hindurch (hinter es oder weit weg). In Raserei fegt es danach nach hinten.</li>
 *     <li>Glutstrahl: ein Feuerstrahl, der dem Ziel langsam folgt (quer laufen oder hinter Stein in Deckung). Danach liegt der Kern frei.</li>
 *     <li>Glutregen (ab Raserei): Brocken stürzen auf markierte Stellen.</li>
 *     <li>Glutleiber steigen aus dem See.</li>
 * </ul>
 * Die Schwachstelle ist der Kern in der offenen Brust: Treffer von vorn wirken stärker, von hinten prallen sie am
 * verkohlten Panzer ab. Nach Glutwelle und Glutstrahl ist das Herz kurz erschöpft – dann trifft man den Kern voll.
 */
public class DasHerz extends Boss {

    private enum Muster { SAEULEN, WELLE, FEGEN, STRAHL, REGEN, BRUT }

    private record Saeule(BlockPos pos, long zeit) {
    }

    private record Brocken(Vec3 pos, long zeit) {
    }

    private static final class Ring {
        final int start;
        final Set<UUID> getroffen = new HashSet<>();

        Ring(int start) {
            this.start = start;
        }
    }

    private final List<Saeule> saeulen = new ArrayList<>();
    private final List<Brocken> brocken = new ArrayList<>();
    private final List<Ring> ringe = new ArrayList<>();
    private final Map<UUID, Integer> strahlTreffer = new HashMap<>();
    private final Set<Muster> erklaert = new HashSet<>();

    private Muster laufend;
    private Muster zuletzt;
    private int t;
    private int erschoepft;
    private Vec3 richtung = new Vec3(1, 0, 0);
    private double winkel;
    private int seite = 1;
    private LivingEntity strahlZiel;
    private boolean kernErklaert;

    public DasHerz(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "das_herz");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 22)
                .add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected boolean kannAnstuermen() {
        return false;
    }

    @Override
    protected String untertitel() {
        return "Unter dir schlägt etwas. Laut wie ein Erdbeben.";
    }

    @Override
    protected int auftrittDauer() {
        return 100;
    }

    @Override
    protected int faehigkeitTakt() {
        return inLetzterKraft() ? 60 : inRaserei() ? 85 : 130;
    }

    @Override
    protected String[] musterAnims() {
        return new String[]{"welle", "welle_schnell", "fegen", "fegen_doppelt", "fegen_doppelt_schnell", "strahl", "regen", "erschoepft", "rufen"};
    }

    @Override
    protected boolean beschaeftigt() {
        return laufend != null || erschoepft > 0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private int stufe() {
        return inLetzterKraft() ? 2 : inRaserei() ? 1 : 0;
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (quelle.getEntity() instanceof Player p) {
            Vec3 blick = getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 zuIhm = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            boolean vorn = blick.dot(zuIhm) > 0.35;
            if (erschoepft > 0) {
                menge *= vorn ? 2.5F : 1.0F; // der Kern liegt frei
            } else {
                menge *= vorn ? 1.5F : 0.55F;
            }
        }
        return super.hurt(quelle, menge);
    }

    private void ton(net.minecraft.sounds.SoundEvent s, float laut, float hoehe) {
        if (s != null) {
            level().playSound(null, getX(), getY(), getZ(), s, SoundSource.HOSTILE, laut, hoehe);
        }
    }

    private void erklaeren(ServerLevel sl, Muster m, String text) {
        if (erklaert.add(m)) {
            Telegraf.hinweis(sl, this, text, ChatFormatting.GOLD);
        }
    }

    // ---- Auswahl --------------------------------------------------------------------------------

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        List<Muster> wahl = new ArrayList<>(List.of(Muster.SAEULEN, Muster.WELLE, Muster.FEGEN, Muster.STRAHL));
        if (stufe() >= 1) {
            wahl.add(Muster.REGEN);
        }
        boolean vornNah = false;
        Vec3 blick = getLookAngle().multiply(1, 0, 1).normalize();
        for (Player p : Telegraf.spieler(level, this, 11)) {
            if (blick.dot(p.position().subtract(position()).multiply(1, 0, 1).normalize()) > 0) {
                vornNah = true;
            }
        }
        if (vornNah) {
            wahl.add(Muster.FEGEN); // wer vor dem Kern steht, wird öfter gefegt
        }
        if (random.nextInt(4) == 0 && level.getEntitiesOfClass(Monster.class, getBoundingBox().inflate(24), m -> m != this).size() < 4) {
            wahl.add(Muster.BRUT);
        }
        wahl.removeIf(m -> m == zuletzt);
        Muster m = wahl.get(random.nextInt(wahl.size()));
        zuletzt = m;
        starten(level, m, ziel);
    }

    private void starten(ServerLevel sl, Muster m, LivingEntity ziel) {
        t = 0;
        laufend = m;
        richtung = ziel.position().subtract(position()).multiply(1, 0, 1).normalize();
        winkel = Telegraf.winkelZu(this, ziel);
        strahlZiel = ziel;
        switch (m) {
            case SAEULEN -> {
                triggerAnim("aktion", "special");
                int n = 3 + stufe();
                for (Player p : Telegraf.spieler(sl, this, 40)) {
                    for (int i = 0; i < n; i++) {
                        BlockPos b = p.blockPosition().offset(sl.random.nextInt(5) - 2, 0, sl.random.nextInt(5) - 2);
                        saeulen.add(new Saeule(b, sl.getGameTime() + 22 + i * 7));
                    }
                }
                ton(MonsterSounds.holen(art, "brullen"), 5.0F, 0.8F);
                erklaeren(sl, m, "Der Boden glüht – weg da!");
            }
            case WELLE -> {
                triggerAnim("muster", stufe() >= 2 ? "welle_schnell" : "welle");
                ringe.clear();
                ton(net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0F, 0.5F);
                ton(MonsterSounds.holen(art, "herzschlag"), 6.0F, 0.6F);
                erklaeren(sl, m, "Glutwelle – spring drüber!");
            }
            case FEGEN -> {
                seite = random.nextBoolean() ? 1 : -1;
                triggerAnim("muster", stufe() >= 2 ? "fegen_doppelt_schnell" : stufe() == 1 ? "fegen_doppelt" : "fegen");
                ton(net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_ATTACK, 4.0F, 0.5F);
                erklaeren(sl, m, "Es holt aus – raus aus dem Glühen!");
            }
            case STRAHL -> {
                strahlTreffer.clear();
                triggerAnim("muster", "strahl");
                ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 5.0F, 0.6F);
                erklaeren(sl, m, "Glutstrahl – quer laufen oder hinter Stein!");
            }
            case REGEN -> {
                triggerAnim("muster", "regen");
                brocken.clear();
                long jetzt = sl.getGameTime();
                int i = 0;
                for (Player p : Telegraf.spieler(sl, this, 40)) {
                    for (int k = 0; k < 3 + stufe(); k++) {
                        Vec3 q = p.position().add(sl.random.nextGaussian() * 3.5, 0, sl.random.nextGaussian() * 3.5);
                        brocken.add(new Brocken(new Vec3(q.x, p.getY(), q.z), jetzt + 30 + (i++) * 5));
                    }
                }
                for (int k = 0; k < 6; k++) {
                    double w = sl.random.nextDouble() * Math.PI * 2;
                    double r = 6 + sl.random.nextDouble() * 16;
                    brocken.add(new Brocken(position().add(Math.cos(w) * r, 0, Math.sin(w) * r), jetzt + 30 + (i++) * 5));
                }
                ton(MonsterSounds.holen(art, "brullen"), 6.0F, 0.6F);
                ton(net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.5F);
                erklaeren(sl, m, "Glutregen – meide die Kreise!");
            }
            case BRUT -> {
                triggerAnim("muster", "rufen");
                for (int i = 0; i < 2 + stufe(); i++) {
                    var g = MonsterTypen.GLUTLEIB.get().create(sl);
                    if (g == null) {
                        continue;
                    }
                    double w = sl.random.nextDouble() * Math.PI * 2;
                    g.moveTo(getX() + Math.cos(w) * 9, getY(), getZ() + Math.sin(w) * 9, sl.random.nextFloat() * 360, 0);
                    g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    g.setTarget(ziel);
                    sl.addFreshEntity(g);
                    sl.sendParticles(ParticleTypes.LAVA, g.getX(), g.getY(), g.getZ(), 20, 0.5, 0.2, 0.5, 0);
                }
                ton(MonsterSounds.holen(art, "brullen"), 5.0F, 1.0F);
            }
        }
    }

    // ---- Ablauf ---------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (tickCount % (stufe() >= 1 ? 18 : 30) == 0) {
            ton(MonsterSounds.holen(art, "herzschlag"), 3.0F, 1.0F);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 9, getZ(), 8, 1, 1, 1, 0.02);
        }
        if (bruellt() || isDeadOrDying()) {
            laufend = null; // Phasenwechsel bricht das Muster ab
            erschoepft = 0;
            ringe.clear();
        }
        if (laufend != null) {
            t++;
            if (laufend == Muster.FEGEN || laufend == Muster.STRAHL) {
                Vec3 r = laufend == Muster.FEGEN ? richtung : Telegraf.richtung(winkel);
                getLookControl().setLookAt(getX() + r.x * 10, getEyeY(), getZ() + r.z * 10, 60, 60);
            }
            boolean fertig = switch (laufend) {
                case SAEULEN, BRUT -> t >= 20;
                case WELLE -> welle(sl);
                case FEGEN -> fegen(sl);
                case STRAHL -> strahl(sl);
                case REGEN -> t >= 30 + brockenDauer();
            };
            if (fertig) {
                laufend = null;
            }
        } else if (erschoepft > 0) {
            erschoepft--;
            if (erschoepft % 4 == 0) {
                Vec3 kern = position().add(getLookAngle().multiply(1, 0, 1).normalize().scale(1.5)).add(0, getBbHeight() * 0.55, 0);
                sl.sendParticles(ParticleTypes.FLAME, kern.x, kern.y, kern.z, 10, 0.5, 0.5, 0.5, 0.04);
                sl.sendParticles(Telegraf.GLUT_HELL, kern.x, kern.y, kern.z, 4, 0.4, 0.4, 0.4, 0);
            }
        }
        saeulenTick(sl);
        brockenTick(sl);
    }

    private int brockenDauer() {
        return 5 * Math.max(1, brocken.size()) + 10;
    }

    private void erschoepfen(ServerLevel sl, int ticks) {
        erschoepft = ticks;
        triggerAnim("muster", "erschoepft");
        ton(MonsterSounds.holen(art, "herzschlag"), 6.0F, 0.5F);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() * 0.6, getZ(), 40, 2, 2, 2, 0.02);
        if (!kernErklaert) {
            kernErklaert = true;
            Telegraf.hinweis(sl, this, "Der Kern liegt frei – jetzt von vorn!", ChatFormatting.YELLOW);
        }
    }

    /** Glutwelle: 30 Ticks Aufladen, dann rasen ein bis drei Feuerringe nach außen. Nur wer in der Luft ist, bleibt heil. */
    private boolean welle(ServerLevel sl) {
        int aufladen = stufe() >= 2 ? 22 : 30;
        if (t < aufladen) {
            if (t % 3 == 0) {
                Telegraf.ring(sl, position(), 4.5 + (t % 6) * 0.2, Telegraf.GLUT);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 12, 3.5, 0.2, 3.5, 0.06);
            }
            return false;
        }
        if (t == aufladen) {
            for (int i = 0; i <= stufe(); i++) {
                ringe.add(new Ring(aufladen + i * 13));
            }
        }
        boolean aktiv = false;
        for (Ring r : ringe) {
            if (t < r.start) {
                aktiv = true;
                continue;
            }
            if (t == r.start) {
                beben(sl, 30, 2);
                ton(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 3.0F, 0.5F);
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
            }
            double radius = 3.5 + (t - r.start) * 0.85;
            if (radius > 28) {
                continue;
            }
            aktiv = true;
            int n = (int) (radius * 4);
            for (int i = 0; i < n; i++) {
                double w = i / (double) n * Math.PI * 2;
                sl.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(w) * radius, getY() + 0.3, getZ() + Math.sin(w) * radius, 1, 0, 0.15, 0, 0.01);
            }
            for (Player p : Telegraf.spieler(sl, this, 30)) {
                double d = Math.sqrt(p.distanceToSqr(getX(), p.getY(), getZ()));
                boolean amBoden = p.onGround() || p.getY() - getY() < 0.6;
                if (Math.abs(d - radius) < 1.0 && amBoden && Math.abs(p.getY() - getY()) < 4 && r.getroffen.add(p.getUUID())) {
                    p.hurt(damageSources().mobAttack(this), stufe() >= 1 ? 18 : 16);
                    p.igniteForSeconds(4);
                    Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                    p.setDeltaMovement(weg.x * 1.1, 0.6, weg.z * 1.1);
                    p.hurtMarked = true;
                }
            }
        }
        if (!aktiv) {
            ringe.clear();
            erschoepfen(sl, new int[]{50, 36, 26}[stufe()]);
            return true;
        }
        return false;
    }

    /** Armfeger: Halbkreis vorn glüht, dann fegt der Arm. In Raserei folgt ein zweiter Feger nach hinten. */
    private boolean fegen(ServerLevel sl) {
        int warn = stufe() >= 2 ? 13 : 18;
        int zweiter = warn + 18;
        boolean doppelt = stufe() >= 1;
        if (t < warn && t % 2 == 0) {
            Telegraf.kegel(sl, position(), richtung, 90, 13, Telegraf.GLUT);
        }
        if (t == warn) {
            schlagen(sl, richtung);
        }
        if (doppelt) {
            if (t > warn && t < zweiter && t % 2 == 0) {
                Telegraf.kegel(sl, position(), richtung.scale(-1), 90, 13, Telegraf.GLUT_HELL);
            }
            if (t == zweiter) {
                schlagen(sl, richtung.scale(-1));
            }
            if (t >= zweiter + 4) {
                erschoepfen(sl, stufe() >= 2 ? 14 : 20);
                return true;
            }
            return false;
        }
        if (t >= warn + 4) {
            erschoepfen(sl, 24);
            return true;
        }
        return false;
    }

    private void schlagen(ServerLevel sl, Vec3 r) {
        ton(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 4.0F, 0.4F);
        ton(MonsterSounds.holen(art, "brullen"), 4.0F, 1.1F);
        double basis = Math.atan2(r.z, r.x);
        for (int i = 0; i <= 12; i++) {
            double w = basis - Math.PI / 2 + Math.PI * i / 12;
            for (double d = 4; d <= 13; d += 3) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(w) * d, getY() + 1.2, getZ() + Math.sin(w) * d, 1, 0, 0, 0, 0);
            }
        }
        Vec3 quer = new Vec3(-r.z, 0, r.x).scale(seite);
        for (Player p : Telegraf.spieler(sl, this, 16)) {
            if (Telegraf.imKegel(position(), r, 92, 13.5, p)) {
                p.hurt(damageSources().mobAttack(this), stufe() >= 1 ? 22 : 20);
                p.setDeltaMovement(quer.x * 1.8 + r.x * 0.6, 0.55, quer.z * 1.8 + r.z * 0.6);
                p.hurtMarked = true;
            }
        }
        beben(sl, 16, 1);
    }

    /** Glutstrahl: Vorwarnlinie, dann ein Feuerstrahl, der dem Ziel langsam nachdreht. Stein hält ihn auf. */
    private boolean strahl(ServerLevel sl) {
        int warn = 26;
        int dauer = new int[]{60, 70, 80}[stufe()];
        double drehung = new double[]{1.3, 1.8, 2.2}[stufe()];
        if (strahlZiel != null && strahlZiel.isAlive() && t > warn / 2) {
            winkel = Telegraf.nachfuehren(winkel, Telegraf.winkelZu(this, strahlZiel), t < warn ? drehung * 0.5 : drehung);
        }
        Vec3 r = Telegraf.richtung(winkel);
        Vec3 start = position().add(r.scale(3.5)).add(0, 1.0, 0);
        Vec3 ende = start.add(r.scale(30));
        var treffer = sl.clip(new net.minecraft.world.level.ClipContext(start, ende, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this));
        if (treffer.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            ende = treffer.getLocation();
        }
        double laenge = ende.distanceTo(start);
        if (t < warn) {
            if (t % 2 == 0) {
                Telegraf.linie(sl, start, ende, Telegraf.GLUT, 1.0);
                sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + getBbHeight() * 0.8, getZ(), 6, 0.6, 0.6, 0.6, 0.05);
            }
            return false;
        }
        if (t == warn) {
            ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.6F);
            beben(sl, 24, 1);
        }
        if (t % 2 == 0) {
            Vec3 mund = position().add(r.scale(1.5)).add(0, getBbHeight() * 0.78, 0);
            Telegraf.linie(sl, mund, start, ParticleTypes.FLAME, 1.2);
            Telegraf.linie(sl, start, ende, ParticleTypes.FLAME, 0.7);
            sl.sendParticles(ParticleTypes.LAVA, ende.x, ende.y, ende.z, 3, 0.3, 0.2, 0.3, 0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, ende.x, ende.y + 0.5, ende.z, 2, 0.2, 0.2, 0.2, 0.02);
        }
        if (t % 10 == 0) {
            sl.playSound(null, ende.x, ende.y, ende.z, net.minecraft.sounds.SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 2.5F, 0.6F);
        }
        for (Player p : Telegraf.spieler(sl, this, 34)) {
            double quer = Telegraf.abstandZumStrahl(start, r, laenge, p.position());
            double dy = p.getY() - start.y;
            if (quer < 1.3 && dy > -2.2 && dy < 0.8) {
                int letzter = strahlTreffer.getOrDefault(p.getUUID(), -100);
                if (t - letzter >= 8) {
                    strahlTreffer.put(p.getUUID(), t);
                    p.invulnerableTime = 0;
                    p.hurt(damageSources().mobAttack(this), stufe() >= 1 ? 7 : 6);
                    p.igniteForSeconds(3);
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                }
            }
        }
        if (t >= warn + dauer) {
            erschoepfen(sl, new int[]{45, 32, 22}[stufe()]);
            return true;
        }
        return false;
    }

    private void saeulenTick(ServerLevel sl) {
        long jetzt = sl.getGameTime();
        float schaden = stufe() >= 1 ? 12 : 9;
        saeulen.removeIf(s -> {
            if (jetzt < s.zeit()) {
                if (jetzt % 3 == 0) {
                    sl.sendParticles(ParticleTypes.LAVA, s.pos().getX() + 0.5, s.pos().getY() + 0.1, s.pos().getZ() + 0.5, 2, 0.4, 0, 0.4, 0);
                    Telegraf.ring(sl, Vec3.atBottomCenterOf(s.pos()), 1.3, Telegraf.GLUT);
                }
                return false;
            }
            sl.sendParticles(ParticleTypes.FLAME, s.pos().getX() + 0.5, s.pos().getY() + 1.5, s.pos().getZ() + 0.5, 40, 0.3, 1.5, 0.3, 0.05);
            sl.playSound(null, s.pos(), net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4F, 0.6F);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s.pos()).inflate(1.3, 3, 1.3),
                    e -> e != this && !(e instanceof Monster))) {
                e.hurt(damageSources().onFire(), schaden);
                e.igniteForSeconds(5);
                e.push(0, 0.8, 0);
                e.hurtMarked = true;
            }
            return true;
        });
    }

    private void brockenTick(ServerLevel sl) {
        long jetzt = sl.getGameTime();
        brocken.removeIf(b -> {
            long rest = b.zeit() - jetzt;
            if (rest > 0) {
                if (rest % 3 == 0) {
                    Telegraf.ring(sl, b.pos(), 2.5, Telegraf.GLUT);
                }
                if (rest <= 16) {
                    double h = rest * 1.4;
                    sl.sendParticles(ParticleTypes.FLAME, b.pos().x, b.pos().y + h, b.pos().z, 8, 0.4, 0.4, 0.4, 0.01);
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE, b.pos().x, b.pos().y + h + 1, b.pos().z, 3, 0.3, 0.3, 0.3, 0.01);
                }
                return false;
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, b.pos().x, b.pos().y + 0.5, b.pos().z, 3, 1, 0.3, 1, 0);
            sl.sendParticles(ParticleTypes.LAVA, b.pos().x, b.pos().y + 0.3, b.pos().z, 12, 1, 0.2, 1, 0);
            sl.playSound(null, b.pos().x, b.pos().y, b.pos().z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.7F);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(b.pos(), b.pos()).inflate(2.5, 2.5, 2.5),
                    e -> e != this && !(e instanceof Monster))) {
                if (e.position().distanceTo(b.pos()) < 2.8) {
                    e.hurt(damageSources().mobAttack(this), 14);
                    e.igniteForSeconds(5);
                    Vec3 weg = e.position().subtract(b.pos()).multiply(1, 0, 1);
                    weg = weg.lengthSqr() < 0.01 ? new Vec3(0.5, 0, 0) : weg.normalize();
                    e.setDeltaMovement(weg.x * 0.9, 0.5, weg.z * 0.9);
                    e.hurtMarked = true;
                }
            }
            return true;
        });
    }

    @Override
    protected void beute(ServerLevel level) {
        spawnAtLocation(new ItemStack(de.deadzonemc.geschichte.Geschichte.KERNSPLITTER.get(), 4));
        spawnAtLocation(new ItemStack(de.deadzonemc.ausruestung.Ausruestung.mat("mutagen"), 6));
        spawnAtLocation(new ItemStack(de.deadzonemc.ausruestung.Ausruestung.mat("glutkern"), 8));
        spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 4));
        spawnAtLocation(new ItemStack(Items.NETHER_STAR));
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return net.minecraft.core.particles.ParticleTypes.LAVA;
    }
}
