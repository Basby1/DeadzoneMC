package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Der Ursprung (Probe A-0) – Finale auf der Orbitalstation. Raid-Kampf in drei Phasen:
 * <ol>
 *     <li>Kristallschild: Solange Energiekristalle um ihn herum leuchten, ist er unverwundbar – sie müssen zerstört werden.
 *     Er wehrt sich mit Tentakelfeldern und Klauen-Kombos.</li>
 *     <li>Schild gefallen: dazu Leerepuls (nur Deckung hilft), Sog mit Nova (früh wegrennen), kreisende Leerestrahlen
 *     (drüberspringen) und Endermiten-Brut. Nach Nova, Strahlen und Kombo flackert das Auge – doppelter Schaden.</li>
 *     <li>Unter 25 %: zweiter Schild aus zwei Kristallen, alles wird schneller, Tentakel kommen in zwei Wellen.</li>
 * </ol>
 */
public class DerUrsprung extends Boss {

    private enum Muster { PULS, TENTAKEL, SOG, STRAHLEN, HIEBE, BRUT }

    private record Fleck(Vec3 pos, long zeit) {
    }

    private int phase = 1;
    private boolean zweiterSchild;
    private Muster laufend;
    private Muster zuletzt;
    private int t;
    private int erschoepft;
    private final List<Fleck> flecken = new ArrayList<>();
    private long nachwelle;
    private final Map<UUID, Integer> strahlTreffer = new HashMap<>();
    private double strahlWinkel;
    private int drehsinn = 1;
    private Vec3 hiebRichtung = new Vec3(1, 0, 0);
    private LivingEntity hiebZiel;
    private final Set<Muster> erklaert = new HashSet<>();
    private boolean augeErklaert;

    public DerUrsprung(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "der_ursprung");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1400)
                .add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.ATTACK_DAMAGE, 26)
                .add(Attributes.FOLLOW_RANGE, 80)
                .add(Attributes.ARMOR, 14)
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
        return "Probe A-0. Der Grund für alles.";
    }

    @Override
    protected int auftrittDauer() {
        return 160;
    }

    @Override
    protected int faehigkeitTakt() {
        return phase >= 3 ? 70 : phase == 2 ? 100 : 150;
    }

    @Override
    protected String[] musterAnims() {
        return new String[]{"beschwoeren", "sog", "sog_schnell", "strahlen", "hieb_links_0", "hieb_rechts_0", "doppelhieb_0",
                "hieb_links_1", "hieb_rechts_1", "doppelhieb_1", "hieb_links_2", "hieb_rechts_2", "doppelhieb_2", "erschoepft"};
    }

    @Override
    protected boolean beschaeftigt() {
        return laufend != null || erschoepft > 0;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    private List<EndCrystal> kristalle() {
        return level().getEntitiesOfClass(EndCrystal.class, getBoundingBox().inflate(48), EndCrystal::isAlive);
    }

    /** Stellt Energiekristalle im Kreis auf, die ihn mit Strahlen schützen. */
    public void schildAufbauen(int anzahl, double radius) {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        for (int i = 0; i < anzahl; i++) {
            double w = 2 * Math.PI * i / anzahl + 0.4;
            BlockPos b = BlockPos.containing(getX() + Math.cos(w) * radius, getY() + 2, getZ() + Math.sin(w) * radius);
            sl.setBlockAndUpdate(b.below(), net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
            EndCrystal k = new EndCrystal(sl, b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
            k.setShowBottom(true);
            k.setBeamTarget(blockPosition().above(8));
            sl.addFreshEntity(k);
            sl.sendParticles(ParticleTypes.END_ROD, k.getX(), k.getY() + 1, k.getZ(), 30, 0.3, 1, 0.3, 0.05);
        }
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 96 * 96) {
                p.sendSystemMessage(Component.translatable("ursprung.deadzonemc.schild").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (!quelle.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && !kristalle().isEmpty()) {
            if (level() instanceof ServerLevel sl && quelle.getEntity() instanceof Player) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 8, getZ(), 12, 2, 2, 2, 0.1);
            }
            return false;
        }
        if (erschoepft > 0 && quelle.getEntity() instanceof Player) {
            menge *= 2.0F; // das Auge flackert: jetzt sitzt jeder Treffer
        }
        return super.hurt(quelle, menge);
    }

    private int stufe() {
        return phase >= 3 ? 2 : phase == 2 ? 1 : 0;
    }

    private void ton(net.minecraft.sounds.SoundEvent s, float laut, float hoehe) {
        if (s != null) {
            level().playSound(null, getX(), getY(), getZ(), s, net.minecraft.sounds.SoundSource.HOSTILE, laut, hoehe);
        }
    }

    private void erklaeren(ServerLevel sl, Muster m, String text) {
        if (erklaert.add(m)) {
            Telegraf.hinweis(sl, this, text, ChatFormatting.LIGHT_PURPLE);
        }
    }

    private Player naechster(ServerLevel sl, double radius) {
        Player best = null;
        for (Player p : Telegraf.spieler(sl, this, radius)) {
            if (best == null || p.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = p;
            }
        }
        return best;
    }

    // ---- Auswahl --------------------------------------------------------------------------------

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        List<Muster> wahl = new ArrayList<>();
        wahl.add(Muster.TENTAKEL);
        if (naechster(level, 14) != null) {
            wahl.add(Muster.HIEBE);
            wahl.add(Muster.HIEBE);
        }
        if (phase >= 2) {
            wahl.addAll(List.of(Muster.PULS, Muster.SOG, Muster.STRAHLEN));
            if (random.nextInt(3) == 0 && level.getEntitiesOfClass(net.minecraft.world.entity.monster.Endermite.class,
                    getBoundingBox().inflate(30)).size() < 6) {
                wahl.add(Muster.BRUT);
            }
        }
        if (wahl.size() > 1) {
            wahl.removeIf(m -> m == zuletzt);
        }
        Muster m = wahl.get(random.nextInt(wahl.size()));
        zuletzt = m;
        starten(level, m, ziel);
    }

    private void starten(ServerLevel sl, Muster m, LivingEntity ziel) {
        t = 0;
        laufend = m;
        switch (m) {
            case PULS -> {
                triggerAnim("aktion", "special");
                ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 6.0F, 0.5F);
                erklaeren(sl, m, "Leerepuls – geh hinter einer Säule in Deckung!");
            }
            case TENTAKEL -> {
                triggerAnim("muster", "beschwoeren");
                fleckenSetzen(sl, new int[]{26, 22, 16}[stufe()], true);
                ton(MonsterSounds.holen(art, "brullen"), 5.0F, 1.1F);
                erklaeren(sl, m, "Der Boden reißt auf – raus aus den Kreisen!");
            }
            case SOG -> {
                triggerAnim("muster", stufe() >= 2 ? "sog_schnell" : "sog");
                ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_CHARGE, 6.0F, 0.4F);
                ton(MonsterSounds.holen(art, "puls"), 6.0F, 0.4F);
                erklaeren(sl, m, "Es zieht dich an – renn weg, so weit du kannst!");
            }
            case STRAHLEN -> {
                strahlTreffer.clear();
                drehsinn = -drehsinn;
                strahlWinkel = random.nextDouble() * 360;
                triggerAnim("muster", "strahlen");
                ton(net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 6.0F, 0.5F);
                erklaeren(sl, m, "Leerestrahlen – spring drüber, wenn sie kommen!");
            }
            case HIEBE -> {
                hiebZiel = naechster(sl, 14);
                if (hiebZiel == null) {
                    hiebZiel = ziel;
                }
                erklaeren(sl, m, "Klauen – zur Seite, zur Seite, zurück!");
            }
            case BRUT -> {
                triggerAnim("muster", "beschwoeren");
                for (int i = 0; i < 4; i++) {
                    var mite = EntityType.ENDERMITE.create(sl);
                    if (mite == null) {
                        continue;
                    }
                    double w = sl.random.nextDouble() * Math.PI * 2;
                    mite.moveTo(getX() + Math.cos(w) * 7, getY() + 1, getZ() + Math.sin(w) * 7, 0, 0);
                    mite.finalizeSpawn(sl, sl.getCurrentDifficultyAt(mite.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    mite.setTarget(ziel);
                    sl.addFreshEntity(mite);
                }
                ton(MonsterSounds.holen(art, "brullen"), 5.0F, 1.3F);
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
        float anteil = getHealth() / getMaxHealth();
        if (phase == 1 && kristalle().isEmpty() && tickCount > 200) {
            phase = 2;
            sprechen(sl, "wut");
        }
        if (phase == 2 && anteil < 0.25F && !zweiterSchild) {
            zweiterSchild = true;
            phase = 3;
            schildAufbauen(2, 14);
        }
        if (tickCount % 40 == 0) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 9, getZ(), 6, 1.5, 1.5, 1.5, 0.02);
        }
        // Wer in den Abgrund fällt, wird nicht zurückgeholt – aber der Ursprung bleibt auf seiner Plattform
        if (getY() < sl.getMinBuildHeight() + 10) {
            teleportTo(getX(), sl.getMinBuildHeight() + 70, getZ());
        }
        if (bruellt() || isDeadOrDying()) {
            laufend = null;
            erschoepft = 0;
        }
        if (laufend != null || erschoepft > 0) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
        }
        if (laufend != null) {
            t++;
            boolean fertig = switch (laufend) {
                case PULS -> puls(sl);
                case TENTAKEL -> flecken.isEmpty() && t > 10;
                case SOG -> sog(sl);
                case STRAHLEN -> strahlen(sl);
                case HIEBE -> hiebe(sl);
                case BRUT -> t >= 20;
            };
            if (fertig) {
                laufend = null;
            }
        } else if (erschoepft > 0) {
            erschoepft--;
            if (erschoepft % 4 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 9, getZ(), 5, 1.2, 1.2, 1.2, 0.01);
                sl.sendParticles(Telegraf.LEERE_HELL, getX(), getY() + 9, getZ(), 4, 1, 1, 1, 0);
            }
        }
        fleckenTick(sl);
    }

    private void erschoepfen(ServerLevel sl, int ticks) {
        erschoepft = ticks;
        triggerAnim("muster", "erschoepft");
        ton(net.minecraft.sounds.SoundEvents.BEACON_DEACTIVATE, 5.0F, 0.5F);
        if (!augeErklaert && kristalle().isEmpty()) {
            augeErklaert = true;
            Telegraf.hinweis(sl, this, "Das Auge flackert – jetzt zuschlagen!", ChatFormatting.YELLOW);
        }
    }

    /** Leerepuls: das Auge saugt Leere an, dann trifft der Puls jeden ohne Deckung. */
    private boolean puls(ServerLevel sl) {
        int warn = stufe() >= 2 ? 30 : 40;
        Vec3 auge = position().add(0, 9, 0);
        if (t < warn) {
            double streu = 2 + 10.0 * (warn - t) / warn;
            if (t % 2 == 0) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, auge.x, auge.y, auge.z, 40, streu, streu * 0.5, streu, 0.05);
                sl.sendParticles(Telegraf.LEERE_HELL, auge.x, auge.y, auge.z, 6, 0.8, 0.8, 0.8, 0);
            }
            if (t % 8 == 0) {
                ton(net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, 4.0F, 0.6F + t / (float) warn);
            }
            return false;
        }
        if (t == warn) {
            ton(MonsterSounds.holen(art, "puls"), 8.0F, 0.6F);
            ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 5.0F, 0.5F);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, auge.x, auge.y, auge.z, 400, 10, 4, 10, 0.2);
            for (Player p : Telegraf.spieler(sl, this, 40)) {
                boolean deckung = sl.clip(new net.minecraft.world.level.ClipContext(auge, p.getEyePosition(),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                        .getType() != net.minecraft.world.phys.HitResult.Type.MISS;
                if (!deckung) {
                    p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1));
                    p.hurt(damageSources().indirectMagic(this, this), stufe() >= 2 ? 14 : 10);
                }
            }
            beben(sl, 40, 2);
        }
        return t >= warn + 10;
    }

    private void fleckenSetzen(ServerLevel sl, int warn, boolean vorhersage) {
        long jetzt = sl.getGameTime();
        for (Player p : Telegraf.spieler(sl, this, 36)) {
            flecken.add(new Fleck(p.position(), jetzt + warn));
            if (vorhersage) {
                Vec3 lauf = new Vec3(p.getX() - p.xo, 0, p.getZ() - p.zo).scale(14);
                if (lauf.length() > 6) {
                    lauf = lauf.normalize().scale(6);
                }
                if (lauf.length() > 1.2) {
                    flecken.add(new Fleck(p.position().add(lauf), jetzt + warn + 4));
                }
            }
        }
        if (stufe() >= 2 && vorhersage) {
            nachwelle = jetzt + warn + 10;
        }
    }

    private void fleckenTick(ServerLevel sl) {
        long jetzt = sl.getGameTime();
        if (nachwelle > 0 && jetzt >= nachwelle) {
            nachwelle = 0;
            fleckenSetzen(sl, 14, false); // in der letzten Phase folgt eine zweite Welle dorthin, wo du jetzt stehst
        }
        float schaden = stufe() == 0 ? 12 : 15;
        flecken.removeIf(f -> {
            long rest = f.zeit() - jetzt;
            if (rest > 0) {
                if (rest % 3 == 0) {
                    Telegraf.ring(sl, f.pos(), 1.9, Telegraf.LEERE);
                    sl.sendParticles(ParticleTypes.PORTAL, f.pos().x, f.pos().y + 0.2, f.pos().z, 6, 0.8, 0.1, 0.8, 0.2);
                }
                return false;
            }
            sl.sendParticles(ParticleTypes.EXPLOSION, f.pos().x, f.pos().y + 0.5, f.pos().z, 2, 0.6, 0.3, 0.6, 0);
            for (int h = 0; h < 6; h++) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, f.pos().x, f.pos().y + h * 0.7, f.pos().z, 12, 0.4, 0.2, 0.4, 0.05);
            }
            sl.playSound(null, f.pos().x, f.pos().y, f.pos().z, net.minecraft.sounds.SoundEvents.EVOKER_FANGS_ATTACK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.5F);
            for (Player p : Telegraf.spieler(sl, this, 40)) {
                double d = Math.sqrt(p.distanceToSqr(f.pos().x, p.getY(), f.pos().z));
                if (d < 2.0 && Math.abs(p.getY() - f.pos().y) < 3) {
                    p.hurt(damageSources().mobAttack(this), schaden);
                    p.setDeltaMovement(p.getDeltaMovement().x, 1.1, p.getDeltaMovement().z);
                    p.hurtMarked = true;
                }
            }
            return true;
        });
    }

    /** Sog: zieht alle heran, dann bricht im Umkreis eine Leere-Nova los. Wer früh rennt, entkommt. */
    private boolean sog(ServerLevel sl) {
        int dauer = stufe() >= 2 ? 40 : 50;
        double radius = 9.5;
        if (t < dauer) {
            if (t % 2 == 0) {
                Telegraf.ring(sl, position(), radius, t > dauer - 12 ? Telegraf.LEERE_HELL : Telegraf.LEERE);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 4, getZ(), 30, 8, 3, 8, 0.1);
            }
            for (Player p : Telegraf.spieler(sl, this, 32)) {
                Vec3 zu = position().subtract(p.position()).multiply(1, 0, 1);
                if (zu.length() > 3.0) {
                    Vec3 zug = zu.normalize().scale(0.085);
                    p.push(zug.x, 0, zug.z);
                    p.hurtMarked = true;
                }
            }
            return false;
        }
        ton(net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM, 6.0F, 0.4F);
        ton(net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.5F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 2, 2, 0.5, 2, 0);
        sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 200, 1, 1, 1, 0.6);
        for (int i = 0; i < 3; i++) {
            Telegraf.ring(sl, position(), radius * (0.5 + i * 0.25), ParticleTypes.SONIC_BOOM);
        }
        beben(sl, 32, 3);
        for (Player p : Telegraf.spieler(sl, this, radius + 2)) {
            if (Math.sqrt(p.distanceToSqr(getX(), p.getY(), getZ())) < radius && Math.abs(p.getY() - getY()) < 6) {
                p.hurt(damageSources().mobAttack(this), stufe() >= 2 ? 28 : 24);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                Vec3 weg = p.position().subtract(position()).multiply(1, 0, 1).normalize();
                p.setDeltaMovement(weg.x * 2.4, 0.7, weg.z * 2.4);
                p.hurtMarked = true;
            }
        }
        erschoepfen(sl, stufe() >= 2 ? 42 : 60);
        return true;
    }

    /** Leerestrahlen: flache Strahlen kreisen über den Boden – im richtigen Moment springen. */
    private boolean strahlen(ServerLevel sl) {
        int warn = 24;
        int dauer = stufe() >= 2 ? 100 : 90;
        int n = stufe() >= 2 ? 6 : 4;
        double laenge = 28;
        if (t >= warn) {
            strahlWinkel += drehsinn * (stufe() >= 2 ? 2.6 : 2.0);
        }
        double hoehe = getY() + 0.4;
        for (int i = 0; i < n; i++) {
            Vec3 r = Telegraf.richtung(strahlWinkel + 360.0 * i / n);
            Vec3 von = new Vec3(getX(), hoehe, getZ()).add(r.scale(3.0));
            Vec3 bis = von.add(r.scale(laenge - 3.0));
            if (t < warn) {
                if (t % 3 == 0) {
                    Telegraf.linie(sl, von, bis, Telegraf.LEERE, 1.2);
                }
                continue;
            }
            if (t % 2 == 0) {
                Telegraf.linie(sl, von, bis, ParticleTypes.END_ROD, 1.0);
                sl.sendParticles(Telegraf.LEERE_HELL, bis.x, bis.y, bis.z, 2, 0.2, 0.2, 0.2, 0);
            }
            for (Player p : Telegraf.spieler(sl, this, laenge + 2)) {
                double dy = p.getY() - hoehe;
                if (dy < -1.5 || dy > 0.5) {
                    continue; // wer springt, ist drüber
                }
                if (Telegraf.abstandZumStrahl(von, r, laenge - 3.0, p.position()) < 0.9) {
                    int letzter = strahlTreffer.getOrDefault(p.getUUID(), -100);
                    if (t - letzter >= 12) {
                        strahlTreffer.put(p.getUUID(), t);
                        p.invulnerableTime = 0;
                        p.hurt(damageSources().indirectMagic(this, this), 8);
                        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                        Vec3 quer = new Vec3(-r.z, 0, r.x).scale(drehsinn * 0.6);
                        p.setDeltaMovement(quer.x, 0.3, quer.z);
                        p.hurtMarked = true;
                    }
                }
            }
        }
        if (t >= warn && t % 20 == 0) {
            ton(net.minecraft.sounds.SoundEvents.BEACON_AMBIENT, 5.0F, 0.5F);
        }
        if (t >= warn + dauer) {
            erschoepfen(sl, stufe() >= 2 ? 28 : 40);
            return true;
        }
        return false;
    }

    /** Klauen-Kombo: links, rechts, dann beide – jeder Hieb zielt neu, der letzte ist breiter und härter. */
    private boolean hiebe(ServerLevel sl) {
        int warn = new int[]{14, 12, 9}[stufe()];
        int takt = warn + 6;
        int nr = (t - 1) / takt;
        int imHieb = (t - 1) % takt;
        if (nr > 2) {
            erschoepfen(sl, new int[]{20, 30, 22}[stufe()]);
            return true;
        }
        boolean letzter = nr == 2;
        double halb = letzter ? 70 : 40;
        double weite = letzter ? 11 : 10;
        if (imHieb == 0) {
            if (hiebZiel != null && hiebZiel.isAlive()) {
                hiebRichtung = hiebZiel.position().subtract(position()).multiply(1, 0, 1).normalize();
            }
            triggerAnim("muster", (nr == 0 ? "hieb_links_" : nr == 1 ? "hieb_rechts_" : "doppelhieb_") + stufe());
            ton(net.minecraft.sounds.SoundEvents.EVOKER_PREPARE_ATTACK, 4.0F, letzter ? 0.4F : 0.6F);
        }
        getLookControl().setLookAt(getX() + hiebRichtung.x * 10, getEyeY(), getZ() + hiebRichtung.z * 10, 60, 60);
        if (imHieb < warn && imHieb % 2 == 0) {
            Telegraf.kegel(sl, position(), hiebRichtung, halb, weite, letzter ? Telegraf.LEERE_HELL : Telegraf.LEERE);
        }
        if (imHieb == warn) {
            ton(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, 4.0F, 0.4F);
            ton(MonsterSounds.holen(art, "brullen"), 3.0F, letzter ? 0.8F : 1.2F);
            for (double r = 3; r <= weite; r += 2.5) {
                Vec3 q = position().add(hiebRichtung.scale(r));
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, q.x, q.y + 1, q.z, 2, r * 0.3, 0.2, r * 0.3, 0);
            }
            if (letzter) {
                sl.sendParticles(ParticleTypes.EXPLOSION, getX() + hiebRichtung.x * 6, getY() + 0.5, getZ() + hiebRichtung.z * 6, 4, 2, 0.3, 2, 0);
                beben(sl, 20, 2);
            }
            for (Player p : Telegraf.spieler(sl, this, weite + 2)) {
                if (Telegraf.imKegel(position(), hiebRichtung, halb + 4, weite + 0.5, p)) {
                    p.hurt(damageSources().mobAttack(this), (letzter ? 26 : 18) + stufe() * 2);
                    p.setDeltaMovement(hiebRichtung.x * 1.5, 0.5, hiebRichtung.z * 1.5);
                    p.hurtMarked = true;
                }
            }
        }
        return false;
    }

    @Override
    protected void beute(ServerLevel level) {
        spawnAtLocation(new ItemStack(Items.ELYTRA));
        spawnAtLocation(new ItemStack(Items.NETHER_STAR, 2));
        spawnAtLocation(new ItemStack(de.deadzonemc.ausruestung.Ausruestung.mat("mutagen"), 10));
        spawnAtLocation(new ItemStack(de.deadzonemc.ausruestung.Ausruestung.mat("strahlenkristall"), 12));
        spawnAtLocation(new ItemStack(Items.DRAGON_EGG));
    }

    @SuppressWarnings("unused")
    private AABB bereich() {
        return getBoundingBox().inflate(40);
    }

    @Override
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL;
    }
}
