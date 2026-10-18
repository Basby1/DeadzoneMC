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

import java.util.List;

/**
 * Der Ursprung (Probe A-0) – Finale auf der Orbitalstation. Raid-Kampf in drei Phasen:
 * <ol>
 *     <li>Kristallschild: Solange Energiekristalle um ihn herum leuchten, ist er unverwundbar – sie müssen zerstört werden.</li>
 *     <li>Unter 60 %: Leerepuls (schwebende, wehrlose Spieler, Schaden), Tentakelschläge an den Zielen, Endermiten-Brut.</li>
 *     <li>Unter 25 %: zweiter Schild aus zwei Kristallen, Pulse kommen schneller.</li>
 * </ol>
 * Schadet nur dem, der sich versteckt (hinter Säulen in Deckung gehen hilft gegen den Puls).
 */
public class DerUrsprung extends Boss {

    private int phase = 1;
    private boolean zweiterSchild;
    private int muster;

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
        return phase >= 3 ? 90 : phase == 2 ? 130 : 180;
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
        return super.hurt(quelle, menge);
    }

    @Override
    protected void faehigkeit(ServerLevel level, LivingEntity ziel) {
        if (phase == 1) {
            return; // Phase 1: nur Schläge, der Schild steht
        }
        triggerAnim("aktion", "special");
        muster = (muster + 1) % 3;
        switch (muster) {
            case 0 -> {
                // Leerepuls: alle ohne Deckung schweben hilflos und nehmen Schaden
                playSound(MonsterSounds.holen(art, "puls"), 8.0F, 0.6F);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 8, getZ(), 400, 10, 4, 10, 0.2);
                Vec3 auge = position().add(0, 9, 0);
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40), Player::isAlive)) {
                    boolean deckung = level.clip(new net.minecraft.world.level.ClipContext(auge, p.getEyePosition(),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                            .getType() != net.minecraft.world.phys.HitResult.Type.MISS;
                    if (!deckung) {
                        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 50, 1));
                        p.hurt(damageSources().indirectMagic(this, this), phase >= 3 ? 9 : 6);
                    }
                }
            }
            case 1 -> {
                // Tentakelschlag: unter jedem Ziel bricht der Boden auf
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(36), Player::isAlive)) {
                    level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY(), p.getZ(), 3, 1, 0.5, 1, 0);
                    level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 80, 1, 1, 1, 0.5);
                    p.hurt(damageSources().mobAttack(this), 10);
                    p.push(0, 1.1, 0);
                    p.hurtMarked = true;
                }
                playSound(MonsterSounds.holen(art, "brullen"), 6.0F, 0.9F);
            }
            default -> {
                // Brut aus dem Riss
                for (int i = 0; i < 4; i++) {
                    var m = EntityType.ENDERMITE.create(level);
                    if (m == null) {
                        continue;
                    }
                    double w = level.random.nextDouble() * Math.PI * 2;
                    m.moveTo(getX() + Math.cos(w) * 7, getY() + 1, getZ() + Math.sin(w) * 7, 0, 0);
                    m.finalizeSpawn(level, level.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
                    m.setTarget(ziel);
                    level.addFreshEntity(m);
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
