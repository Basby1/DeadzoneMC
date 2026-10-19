package de.deadzonemc.monster.boss;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;


/**
 * Grundlage aller Bosse: große rote Bossleiste, sobald der Kampf beginnt, Auftritt mit eigener
 * Animation (währenddessen unverwundbar), Titel für alle in der Nähe, besondere Beute.
 */
public abstract class Boss extends DeadzoneMonster {

    private final ServerBossEvent leiste;
    private final RawAnimation auftritt;
    private int auftrittRest;
    private boolean kampf;
    private boolean aufgetreten;
    private int faehigkeit;

    // ---- Kampfmuster: Wuchtschlag, Stampfen, Ansturm, Raserei ----
    private enum Angriff { SCHLAG, STAMPFEN, ANSTURM, SPRUNG }

    private Angriff aktiv;
    private int ausholen;      // Ticks bis zum Einschlag (Vorwarnung)
    private int ansturmRest;   // Ticks, die der Ansturm noch dauert
    private int betaeubt;      // nach einem Ansturm gegen die Wand
    private int kampfPause = 60;
    private boolean wut;
    private int wutRest;
    private boolean letzteKraft;  // Phase 3 unter 20 %: Leiste blinkt, schneller, wütender
    private int sprungStart = -1; // Tick, in dem der Sprung begann (Landung = Stampfen)
    private int unerreichbar;     // wie lange das Ziel schon außer Reichweite ist (z. B. auf einem Turm)
    private static final net.minecraft.resources.ResourceLocation WUT_TEMPO =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(de.deadzonemc.DeadzoneMC.MODID, "boss_wut_tempo");
    private static final net.minecraft.resources.ResourceLocation WUT_KRAFT =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(de.deadzonemc.DeadzoneMC.MODID, "boss_wut_kraft");
    private static final net.minecraft.resources.ResourceLocation KAMPF_TEMPO =
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(de.deadzonemc.DeadzoneMC.MODID, "boss_kampf_tempo");

    protected Boss(EntityType<? extends Monster> typ, Level level, String art) {
        super(typ, level, art);
        this.leiste = new ServerBossEvent(getDisplayName().copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        this.leiste.setDarkenScreen(true);
        this.leiste.setPlayBossMusic(true);
        this.leiste.setVisible(false);
        this.auftritt = RawAnimation.begin().thenPlay("animation." + art + ".spawn");
        this.xpReward = 120;
        setPersistenceRequired();
    }

    /** Dauer der Auftritts-Animation in Ticks (währenddessen unverwundbar und reglos). */
    protected int auftrittDauer() {
        return 60;
    }

    /** Alle wie viele Ticks die Spezialfähigkeit kommt (0 = keine). */
    protected int faehigkeitTakt() {
        return 0;
    }

    /** Die Spezialfähigkeit (wird nur im Kampf und mit Ziel ausgelöst). */
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
    }

    /** Zusätzliche Beute zum Boss-Grundpaket. */
    protected void beute(ServerLevel level) {
    }

    /** Untertitel beim Auftritt. */
    protected abstract String untertitel();

    public boolean imAuftritt() {
        return auftrittRest > 0;
    }

    private int spottPause = 300;

    /** Sprachzeile des Bosses (boss.deadzonemc.<art>.<fall>) an alle Spieler in der Nähe, mit seiner Stimme. */
    protected void sprechen(ServerLevel sl, String fall) {
        Component zeile = Component.literal("«").append(Component.translatable("boss.deadzonemc." + art + "." + fall)).append("»")
                .withStyle(ChatFormatting.ITALIC, fall.equals("wut") ? ChatFormatting.RED : ChatFormatting.GRAY);
        Component text = Component.literal("").append(getDisplayName().copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD))
                .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY)).append(zeile);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 64 * 64) {
                p.sendSystemMessage(text);
            }
        }
        SoundEvent stimme = MonsterSounds.holen(art, "ambient");
        if (stimme != null && !fall.equals("wut")) {
            sl.playSound(null, getX(), getY(), getZ(), stimme, SoundSource.HOSTILE, 3.0F, 0.85F);
        }
    }

    /** Startet den Auftritt (Animation, Titel, Gebrüll). */
    public void auftreten() {
        auftrittRest = auftrittDauer();
        if (level() instanceof ServerLevel sl0) {
            sprechen(sl0, "auftritt");
        }
        triggerAnim("auftritt", "spawn");
        if (level() instanceof ServerLevel sl) {
            SoundEvent brull = MonsterSounds.holen(art, "brullen");
            if (brull != null) {
                sl.playSound(null, getX(), getY(), getZ(), brull, SoundSource.HOSTILE, 4.0F, 1.0F);
            }
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.4F, 0.6F);
            beben(sl, 48, 2);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 60,
                    getBbWidth(), 0.4, getBbWidth(), 0.04);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 64 * 64) {
                    p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 50, 0, false, false));
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
                    p.connection.send(new ClientboundSetTitleTextPacket(getDisplayName().copy().withStyle(ChatFormatting.DARK_RED)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(untertitel()).withStyle(ChatFormatting.GRAY)));
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        AnimationController<Boss> start = new AnimationController<>(this, "auftritt", 0, s -> PlayState.STOP);
        start.triggerableAnim("spawn", auftritt);
        controllers.add(start);
        AnimationController<Boss> kampfAnim = new AnimationController<>(this, "kampf", 1, s -> PlayState.STOP);
        for (String n : new String[]{"schlag", "stampfen", "ansturm", "wut"}) {
            kampfAnim.triggerableAnim(n, RawAnimation.begin().thenPlay("animation." + art + "." + n));
        }
        controllers.add(kampfAnim);
    }

    // ---- Kampfmuster -------------------------------------------------------------------------

    /** Ob dieser Boss anstürmen kann (Schlund, Nebelmutter und der schießende Kommandant nicht). */
    protected boolean kannAnstuermen() {
        return true;
    }

    /** Ob dieser Boss Nahkampf-Wuchtangriffe macht. */
    protected boolean wuchtangriffe() {
        return true;
    }

    private float wucht() {
        return (float) getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
    }

    private void beben(ServerLevel sl, double radius, int staerke) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < radius * radius) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new de.deadzonemc.netz.SchreckPayload(-staerke));
            }
        }
    }

    private void kampfTick(ServerLevel sl, LivingEntity ziel) {
        if (betaeubt > 0) {
            betaeubt--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if (tickCount % 4 == 0) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, getX(), getEyeY() + 0.6, getZ(), 4, 0.4, 0.1, 0.4, 0.05);
            }
            return;
        }
        if (wutRest > 0) {
            wutRest--;
            getNavigation().stop();
            return;
        }
        // Phase 2: Raserei unter halber Lebensleiste
        if (!wut && getHealth() < getMaxHealth() * 0.5F) {
            wut = true;
            wutRest = 32;
            aktiv = null;
            triggerAnim("kampf", "wut");
            SoundEvent brull = MonsterSounds.holen(art, "brullen");
            if (brull != null) {
                sl.playSound(null, getX(), getY(), getZ(), brull, SoundSource.HOSTILE, 5.0F, 0.75F);
            }
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY(), getZ(), 12, 1, 0.6, 1, 0);
            beben(sl, 24, 3);
            schockwelle(sl, 9.0, 0.9F);
            zeitlupe(sl, 32);
            leiste.setColor(BossEvent.BossBarColor.PURPLE);
            leiste.setName(Component.literal("").append(getDisplayName().copy()).append(" · Raserei")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            var tempo = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (tempo != null && !tempo.hasModifier(WUT_TEMPO)) {
                tempo.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(WUT_TEMPO, 0.3,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            var kraft = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            if (kraft != null && !kraft.hasModifier(WUT_KRAFT)) {
                kraft.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(WUT_KRAFT, 0.25,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 64 * 64) {
                    p.displayClientMessage(Component.literal("").append(getDisplayName().copy()).append(" gerät in Raserei!")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
                }
            }
            sprechen(sl, "wut");
            return;
        }
        // Phase 3: letzte Kraft unter 20 %
        if (wut && !letzteKraft && getHealth() < getMaxHealth() * 0.2F) {
            letzteKraft = true;
            wutRest = 24;
            aktiv = null;
            triggerAnim("kampf", "wut");
            SoundEvent brull = MonsterSounds.holen(art, "brullen");
            if (brull != null) {
                sl.playSound(null, getX(), getY(), getZ(), brull, SoundSource.HOSTILE, 6.0F, 0.6F);
            }
            beben(sl, 32, 4);
            schockwelle(sl, 11.0, 1.1F);
            zeitlupe(sl, 24);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 64 * 64) {
                    p.displayClientMessage(Component.literal("").append(getDisplayName().copy())
                            .append(" bäumt sich ein letztes Mal auf!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                }
            }
            sprechen(sl, "spott." + (1 + random.nextInt(3)));
            return;
        }
        // Zwischendurch spricht (oder droht) der Boss
        if (--spottPause <= 0) {
            spottPause = 420 + random.nextInt(300);
            sprechen(sl, "spott." + (1 + random.nextInt(3)));
        }
        if (!wuchtangriffe()) {
            return;
        }
        double d = distanceTo(ziel);
        boolean erhoeht = ziel.getY() - getY() > 2.5;
        if (erhoeht || (getNavigation().isDone() && d > 5)) {
            unerreichbar++;
        } else {
            unerreichbar = Math.max(0, unerreichbar - 2);
        }
        if (aktiv == null) {
            if (--kampfPause > 0) {
                return;
            }
            if (kannSpringen() && d > 5 && d < 24 && (unerreichbar > 60 || (d > 9 && random.nextFloat() < 0.18F))) {
                aktiv = Angriff.SPRUNG;
                unerreichbar = 0;
            } else if (d < 4.5) {
                aktiv = random.nextFloat() < 0.6F ? Angriff.SCHLAG : Angriff.STAMPFEN;
            } else if (d < 7) {
                aktiv = Angriff.STAMPFEN;
            } else if (d < 18 && kannAnstuermen() && getSensing().hasLineOfSight(ziel)) {
                aktiv = Angriff.ANSTURM;
            } else {
                kampfPause = 10;
                return;
            }
            switch (aktiv) {
                case SCHLAG -> {
                    ausholen = 10;
                    triggerAnim("kampf", "schlag");
                }
                case STAMPFEN -> {
                    ausholen = 13;
                    triggerAnim("kampf", "stampfen");
                }
                case ANSTURM -> {
                    ausholen = 12;
                    ansturmRest = 18;
                }
                case SPRUNG -> {
                    ausholen = 14;
                    triggerAnim("kampf", "stampfen");
                }
            }
            SoundEvent knurr = MonsterSounds.holen(art, "ambient");
            if (knurr != null) {
                sl.playSound(null, getX(), getY(), getZ(), knurr, SoundSource.HOSTILE, 3.0F, 0.6F);
            }
            return;
        }
        getLookControl().setLookAt(ziel, 30, 30);
        if (ausholen > 0) {
            ausholen--;
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.3, 1, 0.3));
            if (aktiv == Angriff.ANSTURM && ausholen % 3 == 0) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, getX(), getY() + 0.2, getZ(), 3, 0.5, 0.1, 0.5, 0.02);
            }
            if (ausholen % 2 == 0) {
                warnen(sl, ziel);
            }
            if (ausholen == 0) {
                einschlag(sl, ziel);
            }
            return;
        }
        if (aktiv == Angriff.ANSTURM && ansturmRest > 0) {
            ansturmRest--;
            if (ansturmRest % 8 == 7) {
                triggerAnim("kampf", "ansturm");
            }
            Vec3 richtung = ziel.position().subtract(position()).multiply(1, 0, 1).normalize().scale(wut ? 0.95 : 0.8);
            setDeltaMovement(richtung.x, getDeltaMovement().y, richtung.z);
            if (getBoundingBox().inflate(0.6).intersects(ziel.getBoundingBox())) {
                ziel.hurt(damageSources().mobAttack(this), wucht() * 1.3F);
                Vec3 stoss = richtung.normalize().scale(2.2);
                ziel.setDeltaMovement(stoss.x, 0.6, stoss.z);
                ziel.hurtMarked = true;
                beben(sl, 10, 2);
                sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.ZOMBIE_ATTACK_IRON_DOOR,
                        SoundSource.HOSTILE, 2.0F, 0.5F);
                ansturmRest = 0;
            } else if (horizontalCollision) {
                // gegen die Wand gekracht: taumelt und ist verwundbar
                betaeubt = 50;
                ansturmRest = 0;
                sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.5F);
                beben(sl, 12, 1);
            }
            if (ansturmRest == 0) {
                aktiv = null;
                kampfPause = pause();
            }
            return;
        }
        aktiv = null;
        kampfPause = pause();
    }

    private int pause() {
        if (letzteKraft) {
            return 20 + random.nextInt(25);
        }
        return wut ? 30 + random.nextInt(30) : 50 + random.nextInt(45);
    }

    /** Partikel, die während des Kampfes um die Spieler treiben (null = keine). */
    protected net.minecraft.core.particles.ParticleOptions kampfPartikel() {
        return net.minecraft.core.particles.ParticleTypes.ASH;
    }

    /** Ob dieser Boss zu erhöhten oder fernen Zielen springen kann (wer anstürmt, springt auch). */
    protected boolean kannSpringen() {
        return kannAnstuermen();
    }

    /** Sichtbare Vorwarnung: wo gleich der Schlag, das Stampfen oder die Landung trifft. */
    private void warnen(ServerLevel sl, LivingEntity ziel) {
        var rot = new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.9F, 0.1F, 0.05F), 1.6F);
        if (aktiv == Angriff.STAMPFEN) {
            double radius = wut ? 7.5 : 6.0;
            for (int i = 0; i < 28; i++) {
                double w = i / 28.0 * Math.PI * 2;
                sl.sendParticles(rot, getX() + Math.cos(w) * radius, getY() + 0.15, getZ() + Math.sin(w) * radius, 1, 0, 0, 0, 0);
            }
        } else if (aktiv == Angriff.SCHLAG) {
            Vec3 blick = getLookAngle().multiply(1, 0, 1).normalize();
            for (double r = 1.0; r <= 4.5; r += 0.9) {
                for (int s = -2; s <= 2; s++) {
                    Vec3 seite = new Vec3(-blick.z, 0, blick.x).scale(s * r * 0.35);
                    Vec3 p = position().add(blick.scale(r)).add(seite);
                    sl.sendParticles(rot, p.x, getY() + 0.15, p.z, 1, 0, 0, 0, 0);
                }
            }
        } else if (aktiv == Angriff.ANSTURM || aktiv == Angriff.SPRUNG) {
            Vec3 zu = ziel.position().subtract(position());
            int n = (int) Math.min(24, zu.length());
            for (int i = 1; i <= n; i++) {
                Vec3 p = position().add(zu.scale(i / (double) n));
                sl.sendParticles(rot, p.x, p.y + 0.15, p.z, 1, 0, 0, 0, 0);
            }
            if (aktiv == Angriff.SPRUNG) {
                for (int i = 0; i < 20; i++) {
                    double w = i / 20.0 * Math.PI * 2;
                    sl.sendParticles(rot, ziel.getX() + Math.cos(w) * 3.5, ziel.getY() + 0.15, ziel.getZ() + Math.sin(w) * 3.5, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    /** Druckwelle beim Phasenwechsel: stößt alle in der Nähe zurück (ohne viel Schaden). */
    private void schockwelle(ServerLevel sl, double radius, float stoss) {
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 4, radius),
                e -> e != this && !(e instanceof Monster) && e.isAlive())) {
            Vec3 weg = e.position().subtract(position()).multiply(1, 0, 1);
            if (weg.lengthSqr() < 0.01) {
                weg = new Vec3(1, 0, 0);
            }
            double anteil = 1.0 - Math.min(1.0, e.distanceTo(this) / radius) * 0.5;
            weg = weg.normalize().scale(stoss * 1.6 * anteil);
            e.setDeltaMovement(weg.x, 0.5 * anteil, weg.z);
            e.hurtMarked = true;
            e.hurt(damageSources().mobAttack(this), 2.0F);
        }
        for (int i = 0; i < 48; i++) {
            double w = i / 48.0 * Math.PI * 2;
            for (double r = 2; r <= radius; r += 3) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        getX() + Math.cos(w) * r, getY() + 0.3, getZ() + Math.sin(w) * r, 1, 0, 0.05, 0, 0.08);
            }
        }
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.5F);
    }

    /** „Zeitlupe“ beim Phasenwechsel: Spieler in der Nähe werden kurz träge, alles wird dunkel. */
    private void zeitlupe(ServerLevel sl, int ticks) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(this) < 40 * 40) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, false, false));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, ticks + 20, 0, false, false));
            }
        }
    }

    private void einschlag(ServerLevel sl, LivingEntity ziel) {
        if (aktiv == Angriff.ANSTURM) {
            return; // Ansturm beginnt jetzt
        }
        if (aktiv == Angriff.SPRUNG) {
            // Sprung auf das Ziel zu – gelandet wird mit einem Stampfen (siehe springenTick)
            Vec3 zu = ziel.position().subtract(position());
            double h = Math.max(0, zu.y);
            setDeltaMovement(zu.x * 0.105, Math.min(1.7, 0.75 + h * 0.11 + Math.sqrt(zu.x * zu.x + zu.z * zu.z) * 0.02), zu.z * 0.105);
            hasImpulse = true;
            sprungStart = tickCount;
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0F, 0.6F);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 3, 0.8, 0.1, 0.8, 0);
            aktiv = null;
            kampfPause = 999; // erst nach der Landung geht es weiter
            return;
        }
        if (aktiv == Angriff.SCHLAG) {
            Vec3 blick = getLookAngle().multiply(1, 0, 1).normalize();
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5, 2, 4.5),
                    e -> e != this && !(e instanceof Monster) && e.isAlive())) {
                Vec3 zu = e.position().subtract(position()).multiply(1, 0, 1);
                if (zu.lengthSqr() < 0.01 || zu.normalize().dot(blick) > 0.3) {
                    e.hurt(damageSources().mobAttack(this), wucht() * 1.6F);
                    Vec3 stoss = zu.normalize().scale(1.6);
                    e.setDeltaMovement(stoss.x, 0.45, stoss.z);
                    e.hurtMarked = true;
                }
            }
            Vec3 vorn = position().add(blick.scale(2.2));
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, vorn.x, getY() + 1, vorn.z, 3, 0.6, 0.3, 0.6, 0);
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.HOSTILE, 2.5F, 0.4F);
            beben(sl, 10, 1);
        } else if (aktiv == Angriff.STAMPFEN) {
            double radius = wut ? 7.5 : 6.0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 3, radius),
                    e -> e != this && !(e instanceof Monster) && e.isAlive() && e.onGround())) {
                double dist = e.distanceTo(this);
                if (dist > radius) {
                    continue;
                }
                float anteil = (float) (1.0 - dist / radius * 0.6);
                e.hurt(damageSources().mobAttack(this), wucht() * 1.2F * anteil);
                Vec3 weg = e.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.2 * anteil);
                e.setDeltaMovement(weg.x, 0.75 * anteil, weg.z);
                e.hurtMarked = true;
            }
            var boden = sl.getBlockState(blockPosition().below());
            for (int i = 0; i < 36; i++) {
                double w = i / 36.0 * Math.PI * 2;
                for (double r = 1.5; r <= radius; r += 1.5) {
                    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK, boden.isAir() ? net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState() : boden),
                            getX() + Math.cos(w) * r, getY() + 0.1, getZ() + Math.sin(w) * r, 2, 0.1, 0.1, 0.1, 0.15);
                }
            }
            sl.playSound(null, getX(), getY(), getZ(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.6F, 0.6F);
            beben(sl, 16, 3);
        }
        aktiv = null;
        kampfPause = pause();
    }

    @Override
    public void aiStep() {
        if (auftrittRest > 0) {
            auftrittRest--;
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            getNavigation().stop();
        }
        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (!aufgetreten) {
            aufgetreten = true;
            auftreten();
        }
        LivingEntity ziel = getTarget();
        if (ziel != null && !kampf) {
            kampf = true;
        }
        if (kampf) {
            var tempo = getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (tempo != null && !tempo.hasModifier(KAMPF_TEMPO)) {
                tempo.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(KAMPF_TEMPO, 0.2,
                        net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        if (sprungStart >= 0) {
            if (tickCount - sprungStart > 4 && (onGround() || isInWater())) {
                sprungStart = -1;
                aktiv = Angriff.STAMPFEN;
                einschlag(sl, ziel != null ? ziel : this);
            } else if (tickCount - sprungStart > 80) {
                sprungStart = -1;
                kampfPause = pause();
            }
        }
        if (kampf && ziel != null && ziel.isAlive() && !imAuftritt() && sprungStart < 0) {
            kampfTick(sl, ziel);
        }
        // Kampfatmosphäre: jeder Boss füllt die Luft um die Kämpfenden mit seinem eigenen Staub, Schnee, Sporen …
        if (kampf && tickCount % 5 == 0) {
            var partikel = kampfPartikel();
            if (partikel != null) {
                for (ServerPlayer p : sl.players()) {
                    if (p.distanceToSqr(this) < 40 * 40) {
                        sl.sendParticles(p, partikel, false, p.getX(), p.getY() + 1.5, p.getZ(), wut ? 10 : 6, 7, 3, 7, 0.01);
                    }
                }
            }
        }
        if (letzteKraft && tickCount % 10 == 0) {
            leiste.setColor(tickCount % 20 == 0 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE);
        }
        if (kampf) {
            leiste.setVisible(true);
            leiste.setProgress(getHealth() / getMaxHealth());
            for (ServerPlayer p : sl.players()) {
                boolean nah = p.distanceToSqr(this) < 56 * 56 && p.isAlive();
                if (nah && !leiste.getPlayers().contains(p)) {
                    leiste.addPlayer(p);
                } else if (!nah && leiste.getPlayers().contains(p)) {
                    leiste.removePlayer(p);
                }
            }
        }
        int takt = faehigkeitTakt() * (wut ? 55 : 75) / 100;
        if (takt > 0 && ziel != null && ziel.isAlive() && !imAuftritt() && aktiv == null && betaeubt == 0
                && ++faehigkeit >= takt) {
            faehigkeit = 0;
            faehigkeit(sl, ziel);
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (imAuftritt() && !quelle.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (quelle.getEntity() instanceof Player) {
            kampf = true;
        }
        if (wutRest > 0 && !quelle.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false; // während des Brüllens unverwundbar
        }
        if (betaeubt > 0) {
            menge *= 1.5F; // taumelt nach dem Ansturm: jetzt zuschlagen!
        }
        return super.hurt(quelle, menge);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity ziel) {
        if (aktiv != null || betaeubt > 0 || wutRest > 0) {
            return false; // während eines Wuchtangriffs kein normaler Schlag
        }
        return super.doHurtTarget(ziel);
    }

    @Override
    public boolean causeFallDamage(float distanz, float faktor, DamageSource quelle) {
        return false;
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        if (level() instanceof ServerLevel sl) {
            leiste.removeAllPlayers();
            spawnAtLocation(new ItemStack(ModRegistry.MILITAERPATRONE.get(), 25 + random.nextInt(26)));
            spawnAtLocation(new ItemStack(ModRegistry.ANTIBIOTIKA.get(), 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModRegistry.ELEKTRONIK.get(), 2 + random.nextInt(4)));
            beute(sl);
            var trophaee = de.deadzonemc.bossmod.Bossmods.TROPHAEE.get(art);
            if (trophaee != null) {
                spawnAtLocation(new ItemStack(trophaee.get(), random.nextInt(4) == 0 ? 2 : 1));
            }
            var kopf = de.deadzonemc.bossmod.Bossmods.KOPF.get(art);
            boolean endboss = art.equals("das_herz") || art.equals("der_ursprung");
            if (kopf != null && (endboss || random.nextFloat() < 0.4F)) {
                spawnAtLocation(new ItemStack(kopf.get()));
            }
            sprechen(sl, "tod");
            Component name = getDisplayName();
            // Finale: Blitz (nur Optik), Explosionen, Lichtsäule über der Beute, Fanfare und Titel
            var blitz = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
            if (blitz != null) {
                blitz.moveTo(getX(), getY(), getZ());
                blitz.setVisualOnly(true);
                sl.addFreshEntity(blitz);
            }
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + getBbHeight() * 0.5, getZ(), 2,
                    getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0);
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + getBbHeight() * 0.5, getZ(), 120,
                    getBbWidth() * 0.5, getBbHeight() * 0.4, getBbWidth() * 0.5, 0.08);
            for (int i = 0; i < 30; i++) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + 0.5 + i * 0.5, getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            }
            beben(sl, 64, 4);
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 96 * 96) {
                    p.playNotifySound(net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 1.0F);
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 30));
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("BEZWUNGEN").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(name.copy().withStyle(ChatFormatting.GRAY)));
                }
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 96 * 96) {
                    p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED)
                            .append(name.copy().withStyle(ChatFormatting.RED)).append(Component.literal(" ist gefallen.")
                                    .withStyle(ChatFormatting.GRAY)));
                }
            }
        }
    }

    /** Beim Tod springt die Beute in einer Fontäne heraus statt still auf einem Haufen zu liegen. */
    @Override
    public net.minecraft.world.entity.item.ItemEntity spawnAtLocation(ItemStack stack, float hoehe) {
        var e = super.spawnAtLocation(stack, hoehe + (isDeadOrDying() ? getBbHeight() * 0.5F : 0));
        if (e != null && isDeadOrDying()) {
            e.setDeltaMovement(random.nextGaussian() * 0.16, 0.45 + random.nextFloat() * 0.3, random.nextGaussian() * 0.16);
            e.setGlowingTag(true);
        }
        return e;
    }

    @Override
    public void remove(RemovalReason grund) {
        leiste.removeAllPlayers();
        super.remove(grund);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        leiste.removePlayer(p);
    }

    @Override
    public boolean removeWhenFarAway(double abstand) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Kampf", kampf);
        tag.putBoolean("Aufgetreten", aufgetreten);
        tag.putBoolean("Wut", wut);
        tag.putBoolean("LetzteKraft", letzteKraft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        kampf = tag.getBoolean("Kampf");
        aufgetreten = tag.getBoolean("Aufgetreten");
        wut = tag.getBoolean("Wut");
        letzteKraft = tag.getBoolean("LetzteKraft");
        if (hasCustomName()) {
            leiste.setName(getDisplayName());
        }
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }
}
