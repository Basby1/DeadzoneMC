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

    protected Boss(EntityType<? extends Monster> typ, Level level, String art) {
        super(typ, level, art);
        this.leiste = new ServerBossEvent(getDisplayName().copy().withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
        this.leiste.setDarkenScreen(true);
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

    /** Startet den Auftritt (Animation, Titel, Gebrüll). */
    public void auftreten() {
        auftrittRest = auftrittDauer();
        triggerAnim("auftritt", "spawn");
        if (level() instanceof ServerLevel sl) {
            SoundEvent brull = MonsterSounds.holen(art, "brullen");
            if (brull != null) {
                sl.playSound(null, getX(), getY(), getZ(), brull, SoundSource.HOSTILE, 4.0F, 1.0F);
            }
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 64 * 64) {
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
        int takt = faehigkeitTakt();
        if (takt > 0 && ziel != null && ziel.isAlive() && !imAuftritt() && ++faehigkeit >= takt) {
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
        return super.hurt(quelle, menge);
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
            Component name = getDisplayName();
            for (ServerPlayer p : sl.players()) {
                if (p.distanceToSqr(this) < 96 * 96) {
                    p.sendSystemMessage(Component.literal("☠ ").withStyle(ChatFormatting.DARK_RED)
                            .append(name.copy().withStyle(ChatFormatting.RED)).append(Component.literal(" ist gefallen.")
                                    .withStyle(ChatFormatting.GRAY)));
                }
            }
        }
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
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        kampf = tag.getBoolean("Kampf");
        aufgetreten = tag.getBoolean("Aufgetreten");
        if (hasCustomName()) {
            leiste.setName(getDisplayName());
        }
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }
}
