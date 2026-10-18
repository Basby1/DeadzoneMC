package de.deadzonemc.mensch;

import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Ein angeheuerter Begleiter. Folgt seinem Spieler, kämpft gegen Infizierte und schießt, solange er Munition hat.
 *
 * <ul>
 *   <li>Rechtsklick: Status (Leben, Munition)</li>
 *   <li>Schleichen + Rechtsklick: Warten / Folgen umschalten</li>
 *   <li>Mit Munition (TaCZ) in der Hand: Munition geben · mit Konserve/Verband: heilen</li>
 *   <li>Mit Waffe (Schwert/Axt) in der Hand: Nahkampfwaffe tauschen</li>
 * </ul>
 */
public class Begleiter extends Mensch {

    private UUID besitzer;
    private boolean warten;
    private int munition;

    public Begleiter(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    public Fraktion fraktion() {
        return null;
    }

    public void besitzerSetzen(Player p) {
        besitzer = p.getUUID();
    }

    @Nullable
    public Player besitzer() {
        return besitzer == null ? null : level().getPlayerByUUID(besitzer);
    }

    public boolean gehoert(Player p) {
        return besitzer != null && besitzer.equals(p.getUUID());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Fernkampf(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(5, new Folgen(this));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new BesitzerVerteidigen(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this, Begleiter.class, Player.class));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                e -> e.distanceToSqr(this) < 14 * 14));
    }

    @Override
    public boolean canAttack(LivingEntity ziel) {
        return !(ziel instanceof Player) && !(ziel instanceof Begleiter) && super.canAttack(ziel);
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!gehoert(p)) {
            sagen(p, Component.translatable("begleiter.deadzonemc.fremd"));
            return InteractionResult.SUCCESS;
        }
        ItemStack s = p.getItemInHand(hand);
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
        if (p.isShiftKeyDown()) {
            warten = !warten;
            getNavigation().stop();
            sagen(p, Component.translatable(warten ? "begleiter.deadzonemc.warte" : "begleiter.deadzonemc.folge"));
        } else if (s.getItem() instanceof de.deadzonemc.waffen.feuer.MunitionItem) {
            int n = s.getCount();
            munition += n;
            s.shrink(n);
            playSound(SoundEvents.ARMOR_EQUIP_CHAIN.value(), 1.0F, 1.2F);
            sagen(p, Component.translatable("begleiter.deadzonemc.munition", munition));
        } else if ((s.is(ModRegistry.KONSERVENDOSE.get()) || s.is(ModRegistry.VERBAND.get())) && getHealth() < getMaxHealth()) {
            heal(s.is(ModRegistry.VERBAND.get()) ? 10 : 6);
            s.shrink(1);
            playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
            sagen(p, Component.translatable("begleiter.deadzonemc.danke"));
        } else if (s.getItem() instanceof net.minecraft.world.item.SwordItem || s.getItem() instanceof net.minecraft.world.item.AxeItem) {
            ItemStack alt = getItemBySlot(EquipmentSlot.MAINHAND);
            setItemSlot(EquipmentSlot.MAINHAND, s.split(1));
            setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            if (!alt.isEmpty() && !p.getInventory().add(alt)) {
                p.drop(alt, false);
            }
            sagen(p, Component.translatable("begleiter.deadzonemc.waffe"));
        } else {
            sagen(p, Component.translatable("begleiter.deadzonemc.status", (int) Math.ceil(getHealth()), (int) getMaxHealth(),
                    munition, Component.translatable(warten ? "begleiter.deadzonemc.status_warte" : "begleiter.deadzonemc.status_folge")));
        }
        return InteractionResult.SUCCESS;
    }

    private void sagen(Player p, Component text) {
        p.displayClientMessage(Component.literal("<").append(getDisplayName()).append("> ").append(text)
                .withStyle(ChatFormatting.WHITE), false);
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (quelle.getEntity() instanceof Player p && gehoert(p)) {
            return false; // kein Eigenbeschuss
        }
        return super.hurt(quelle, menge);
    }

    @Override
    public void die(DamageSource quelle) {
        super.die(quelle);
        Player p = besitzer();
        if (p != null && !level().isClientSide) {
            p.sendSystemMessage(Component.translatable("begleiter.deadzonemc.tot", getDisplayName()).withStyle(ChatFormatting.DARK_RED));
        }
        if (munition > 0 && !level().isClientSide) {
            ItemStack m = Haendler.tacz("ammo", "AmmoId", "9mm", Math.min(64, munition));
            spawnAtLocation(m);
        }
    }

    @Override
    public boolean removeWhenFarAway(double abstand) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (besitzer != null) {
            tag.putUUID("Besitzer", besitzer);
        }
        tag.putBoolean("Warten", warten);
        tag.putInt("Munition", munition);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Besitzer")) {
            besitzer = tag.getUUID("Besitzer");
        }
        warten = tag.getBoolean("Warten");
        munition = tag.getInt("Munition");
    }

    /** Wie viele Begleiter dieser Spieler schon hat (in geladenen Gebieten). */
    public static int anzahl(ServerLevel level, Player p) {
        int n = 0;
        for (var e : level.getAllEntities()) {
            if (e instanceof Begleiter b && b.isAlive() && b.gehoert(p)) {
                n++;
            }
        }
        return n;
    }

    // ---- Ziele -------------------------------------------------------------------------------

    /** Folgt dem Besitzer; ist er zu weit weg, holt er auf (teleportiert bei über 40 Blöcken). */
    static class Folgen extends Goal {
        private final Begleiter b;
        private int neu;

        Folgen(Begleiter b) {
            this.b = b;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player p = b.besitzer();
            return !b.warten && p != null && !p.isSpectator() && b.distanceToSqr(p) > 5 * 5 && b.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = b.besitzer();
            return !b.warten && p != null && b.distanceToSqr(p) > 3 * 3 && !b.getNavigation().isDone();
        }

        @Override
        public void tick() {
            Player p = b.besitzer();
            if (p == null || --neu > 0) {
                return;
            }
            neu = 10;
            b.getLookControl().setLookAt(p, 10, b.getMaxHeadXRot());
            if (b.distanceToSqr(p) > 40 * 40 && p.onGround()) {
                Vec3 hinter = p.position().subtract(p.getLookAngle().multiply(2, 0, 2));
                b.teleportTo(hinter.x, p.getY(), hinter.z);
                b.getNavigation().stop();
                return;
            }
            b.getNavigation().moveTo(p, b.distanceToSqr(p) > 12 * 12 ? 1.35 : 1.05);
        }

        @Override
        public void stop() {
            b.getNavigation().stop();
        }
    }

    /** Greift an, was den Besitzer angreift oder von ihm angegriffen wird. */
    static class BesitzerVerteidigen extends TargetGoal {
        private final Begleiter b;
        private LivingEntity feind;

        BesitzerVerteidigen(Begleiter b) {
            super(b, false);
            this.b = b;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player p = b.besitzer();
            if (p == null) {
                return false;
            }
            LivingEntity a = p.getLastHurtByMob();
            LivingEntity z = p.getLastHurtMob();
            feind = a != null && p.tickCount - p.getLastHurtByMobTimestamp() < 100 ? a
                    : z != null && p.tickCount - p.getLastHurtMobTimestamp() < 100 ? z : null;
            return feind != null && feind.isAlive() && !(feind instanceof Player) && !(feind instanceof Begleiter)
                    && b.distanceToSqr(feind) < 24 * 24;
        }

        @Override
        public void start() {
            b.setTarget(feind);
            super.start();
        }
    }

    /** Mit Munition: Abstand halten und schießen (wie die Soldaten). Ohne Munition übernimmt der Nahkampf. */
    static class Fernkampf extends Goal {
        private final Begleiter b;
        private int abklingen = 15;

        Fernkampf(Begleiter b) {
            this.b = b;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity z = b.getTarget();
            return b.munition > 0 && z != null && z.isAlive() && b.distanceToSqr(z) < 28 * 28;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            b.setAggressive(true);
        }

        @Override
        public void stop() {
            b.setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity z = b.getTarget();
            if (z == null) {
                return;
            }
            b.getLookControl().setLookAt(z, 30, 30);
            double d = b.distanceTo(z);
            boolean sicht = b.getSensing().hasLineOfSight(z);
            if (d > 14 || !sicht) {
                b.getNavigation().moveTo(z, 1.1);
            } else if (d < 5) {
                Vec3 weg = b.position().subtract(z.position()).normalize().scale(4);
                b.getNavigation().moveTo(b.getX() + weg.x, b.getY(), b.getZ() + weg.z, 1.2);
            } else {
                b.getNavigation().stop();
            }
            if (--abklingen <= 0 && sicht && b.level() instanceof ServerLevel sl) {
                abklingen = 16 + b.getRandom().nextInt(10);
                b.munition--;
                b.triggerAnim("aktion", "special");
                sl.playSound(null, b.getX(), b.getY(), b.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.NEUTRAL, 2.5F, 0.6F);
                Vec3 von = b.getEyePosition();
                Vec3 nach = z.getEyePosition().subtract(0, 0.4, 0);
                for (int i = 1; i < 10; i++) {
                    Vec3 pt = von.lerp(nach, i / 10.0);
                    sl.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
                }
                double treffer = Math.max(0.4, Math.min(0.92, 1.0 - d / 36.0));
                if (b.getRandom().nextDouble() < treffer) {
                    z.hurt(b.damageSources().mobAttack(b), 6.0F + b.getRandom().nextInt(3));
                }
                if (b.munition == 0) {
                    Player p = b.besitzer();
                    if (p != null && p.distanceToSqr(b) < 32 * 32) {
                        b.sagen(p, Component.translatable("begleiter.deadzonemc.leer"));
                    }
                }
            }
        }
    }
}
