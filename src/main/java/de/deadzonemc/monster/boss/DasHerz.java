package de.deadzonemc.monster.boss;

import de.deadzonemc.monster.MonsterSounds;
import de.deadzonemc.monster.MonsterTypen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
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
import java.util.List;

/**
 * Das Herz der Zone – Endboss des Nethers. Steht fest im Glutsee (bewegt sich nicht) und kämpft mit drei Mustern:
 * Glutsäulen unter den Füßen der Spieler (kurz vorher glüht der Boden), Hitzewelle im Nahbereich, und es ruft
 * Glutleiber aus dem See. Die Schwachstelle ist der Kern in der offenen Brust: Treffer von vorn wirken stärker,
 * von hinten prallen sie am verkohlten Panzer ab. Ab halber Lebensleiste schlägt das Herz schneller.
 */
public class DasHerz extends Boss {

    private record Saeule(BlockPos pos, long zeit) {
    }

    private final List<Saeule> saeulen = new ArrayList<>();
    private int muster;

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
        return getHealth() < getMaxHealth() * 0.5F ? 110 : 170;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (quelle.getEntity() instanceof Player p) {
            Vec3 blick = getLookAngle().multiply(1, 0, 1).normalize();
            Vec3 zuIhm = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            boolean vorn = blick.dot(zuIhm) > 0.35;
            menge *= vorn ? 1.5F : 0.55F;
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        triggerAnim("aktion", "special");
        muster = (muster + 1) % 3;
        switch (muster) {
            case 0 -> {
                // Glutsäulen: unter jedem Spieler in Reichweite glüht der Boden auf, eine Sekunde später bricht Glut hervor
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40), Player::isAlive)) {
                    for (int i = 0; i < 3; i++) {
                        BlockPos b = p.blockPosition().offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
                        saeulen.add(new Saeule(b, level.getGameTime() + 22 + i * 8));
                    }
                }
                playSound(MonsterSounds.holen(art, "brullen"), 5.0F, 0.8F);
            }
            case 1 -> {
                // Hitzewelle: alles in 10 Blöcken wird zurückgeschleudert und entzündet
                level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 3, getZ(), 200, 6, 2, 6, 0.08);
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10))) {
                    Vec3 weg = p.position().subtract(position()).normalize().scale(2.0);
                    p.push(weg.x, 0.7, weg.z);
                    p.hurtMarked = true;
                    p.hurt(damageSources().mobAttack(this), 10);
                    p.igniteForSeconds(4);
                }
                playSound(MonsterSounds.holen(art, "herzschlag"), 6.0F, 0.7F);
            }
            default -> {
                // Aus dem Glutsee steigen Glutleiber
                for (int i = 0; i < 2 + (getHealth() < getMaxHealth() * 0.5F ? 1 : 0); i++) {
                    var g = MonsterTypen.GLUTLEIB.get().create(level);
                    if (g == null) {
                        continue;
                    }
                    double w = level.random.nextDouble() * Math.PI * 2;
                    g.moveTo(getX() + Math.cos(w) * 9, getY(), getZ() + Math.sin(w) * 9, level.random.nextFloat() * 360, 0);
                    g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    g.setTarget(ziel);
                    level.addFreshEntity(g);
                    level.sendParticles(ParticleTypes.LAVA, g.getX(), g.getY(), g.getZ(), 20, 0.5, 0.2, 0.5, 0);
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        setDeltaMovement(0, getDeltaMovement().y, 0);
        if (tickCount % (getHealth() < getMaxHealth() * 0.5F ? 18 : 30) == 0) {
            playSound(MonsterSounds.holen(art, "herzschlag"), 3.0F, 1.0F);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 9, getZ(), 8, 1, 1, 1, 0.02);
        }
        long jetzt = sl.getGameTime();
        saeulen.removeIf(s -> {
            if (jetzt < s.zeit()) {
                if (jetzt % 4 == 0) {
                    sl.sendParticles(ParticleTypes.LAVA, s.pos().getX() + 0.5, s.pos().getY() + 0.1, s.pos().getZ() + 0.5, 2, 0.4, 0, 0.4, 0);
                }
                return false;
            }
            sl.sendParticles(ParticleTypes.FLAME, s.pos().getX() + 0.5, s.pos().getY() + 1.5, s.pos().getZ() + 0.5, 40, 0.3, 1.5, 0.3, 0.05);
            sl.playSound(null, s.pos(), net.minecraft.sounds.SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4F, 0.6F);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s.pos()).inflate(1.3, 3, 1.3),
                    e -> e != this && !(e instanceof Monster))) {
                e.hurt(damageSources().onFire(), 9);
                e.igniteForSeconds(5);
                e.push(0, 0.8, 0);
                e.hurtMarked = true;
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
}
