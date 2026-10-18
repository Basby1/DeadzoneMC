package de.deadzonemc.zone;

import de.deadzonemc.strahlung.Strahlung;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

/** Die Wirkung einer Anomalie: prüft regelmäßig, wer hineingerät, und lässt ab und zu ein Artefakt entstehen. */
public class AnomalieBlockEntity extends BlockEntity {

    private int pause;
    private int letzteEmission = -1;
    private long naechstesArtefakt;

    public AnomalieBlockEntity(BlockPos pos, BlockState state) {
        super(Zone.ANOMALIE_BE.get(), pos, state);
    }

    public AnomalieArt art() {
        return getBlockState().getValue(AnomalieBlock.ART);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AnomalieBlockEntity be) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (be.pause > 0) {
            be.pause--;
        }
        long zeit = sl.getGameTime();
        if ((zeit + pos.asLong()) % 4 != 0) {
            return;
        }
        AnomalieArt art = be.art();
        Vec3 mitte = Vec3.atCenterOf(pos);
        double reichweite = art == AnomalieArt.STRUDEL ? 4.5 : art == AnomalieArt.SAEURE ? 3.0 : 2.2;
        List<Entity> nah = sl.getEntities((Entity) null, new AABB(pos).inflate(reichweite),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof Player p && p.isCreative()));
        for (Entity e : nah) {
            if (e instanceof ItemEntity item && item.getItem().getItem() instanceof ArtefaktItem) {
                continue;
            }
            be.wirken(sl, mitte, e, art);
        }
        // Artefakte: nach jeder Emission sicher eines, sonst selten
        int emission = Emission.nummer(sl);
        if (be.letzteEmission < 0) {
            be.letzteEmission = emission;
            be.naechstesArtefakt = zeit + 24000L + sl.random.nextInt(48000);
            be.setChanged();
        }
        if ((emission != be.letzteEmission || zeit > be.naechstesArtefakt) && (zeit % 40 == 0)) {
            boolean nachEmission = emission != be.letzteEmission;
            be.letzteEmission = emission;
            be.naechstesArtefakt = zeit + 36000L + sl.random.nextInt(60000);
            be.setChanged();
            be.artefaktErzeugen(sl, nachEmission);
        }
    }

    private void wirken(ServerLevel sl, Vec3 mitte, Entity e, AnomalieArt art) {
        Vec3 zu = mitte.subtract(e.position().add(0, e.getBbHeight() * 0.3, 0));
        double abstand = zu.length();
        switch (art) {
            case STRUDEL -> {
                if (e instanceof Player p && p.getAbilities().flying) {
                    return;
                }
                double zug = 0.09 * (1.0 - Math.min(1.0, abstand / 4.5)) + 0.02;
                e.setDeltaMovement(e.getDeltaMovement().add(zu.normalize().scale(zug)));
                e.hurtMarked = true;
                if (abstand < 1.3 && pause == 0) {
                    pause = 30;
                    ausloesen(sl, true);
                    if (e instanceof LivingEntity le) {
                        le.hurt(sl.damageSources().fellOutOfWorld(), 7.0F);
                    } else if (e instanceof ItemEntity) {
                        e.discard();
                        return;
                    }
                    e.setDeltaMovement(sl.random.nextGaussian() * 0.3, 1.1, sl.random.nextGaussian() * 0.3);
                    e.hurtMarked = true;
                }
            }
            case GLUTFELD -> {
                if (abstand < 1.8 && pause == 0 && e instanceof LivingEntity le) {
                    pause = 20;
                    ausloesen(sl, true);
                    le.igniteForSeconds(6);
                    le.hurt(sl.damageSources().inFire(), 5.0F);
                }
            }
            case ENTLADUNG -> {
                if (abstand < 2.4 && pause == 0 && e instanceof LivingEntity le) {
                    pause = 40;
                    ausloesen(sl, true);
                    le.hurt(sl.damageSources().lightningBolt(), 8.0F);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                    Vec3 von = Vec3.atCenterOf(worldPosition);
                    Vec3 nach = le.position().add(0, le.getBbHeight() * 0.5, 0);
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = von.lerp(nach, i / 10.0).add(sl.random.nextGaussian() * 0.15, sl.random.nextGaussian() * 0.15,
                                sl.random.nextGaussian() * 0.15);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.1);
                    }
                }
            }
            case SAEURE -> {
                if (e instanceof LivingEntity le && sl.getGameTime() % 20 == 0) {
                    if (!(le instanceof Player p && maskeMitFilter(p))) {
                        le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                        le.hurt(sl.damageSources().magic(), 1.0F);
                    }
                    for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS,
                            EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                        ItemStack s = le.getItemBySlot(slot);
                        if (!s.isEmpty() && s.isDamageableItem()) {
                            s.hurtAndBreak(1, le, slot);
                        }
                    }
                    if (le instanceof Player p) {
                        Strahlung.bestrahlen(p, 3.0F);
                    }
                    if (pause == 0) {
                        pause = 60;
                        ausloesen(sl, false);
                    }
                }
            }
        }
    }

    private static boolean maskeMitFilter(Player p) {
        ItemStack maske = p.getItemBySlot(EquipmentSlot.HEAD);
        return maske.is(de.deadzonemc.ModRegistry.GASMASKE.get())
                && maske.getOrDefault(de.deadzonemc.ModRegistry.FILTER.get(), 0) > 0;
    }

    /** Sichtbarer Ausbruch – durch Opfer oder einen geworfenen Bolzen. */
    public void ausloesen(ServerLevel sl, boolean laut) {
        BlockPos pos = worldPosition;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        switch (art()) {
            case STRUDEL -> {
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.4F, 0.38F, 0.35F), 1.6F), x, y, z, 60, 1.2, 1.0, 1.2, 0.2);
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y + 0.5, z, 4, 0.6, 0.6, 0.6, 0);
                sl.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, laut ? 1.4F : 0.8F, 1.6F);
            }
            case GLUTFELD -> {
                sl.sendParticles(ParticleTypes.FLAME, x, y, z, 50, 0.5, 1.2, 0.5, 0.08);
                sl.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.4, 0.4, 0.4, 0);
                sl.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.5F, 0.6F);
            }
            case ENTLADUNG -> {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 80, 1.0, 1.0, 1.0, 0.3);
                sl.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
                sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 1.6F, 1.5F);
            }
            case SAEURE -> {
                sl.sendParticles(new DustParticleOptions(new Vector3f(0.55F, 0.85F, 0.2F), 2.2F), x, y, z, 70, 1.6, 0.8, 1.6, 0.02);
                sl.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.5F);
            }
        }
    }

    private void artefaktErzeugen(ServerLevel sl, boolean nachEmission) {
        AABB umgebung = new AABB(worldPosition).inflate(8);
        if (!sl.getEntitiesOfClass(ItemEntity.class, umgebung, i -> i.getItem().getItem() instanceof ArtefaktItem).isEmpty()) {
            return;
        }
        if (!nachEmission && sl.random.nextInt(3) != 0) {
            return;
        }
        ArtefaktItem.Art a = nachEmission && sl.random.nextInt(12) == 0 ? ArtefaktItem.Art.SEELENKRISTALL : art().artefakt();
        // Mitten in der Anomalie oder knapp daneben: wer es will, muss den richtigen Moment abpassen
        double w = sl.random.nextDouble() * Math.PI * 2;
        double r = 0.6 + sl.random.nextDouble() * 1.6;
        BlockPos ziel = BlockPos.containing(worldPosition.getX() + 0.5 + Math.cos(w) * r, worldPosition.getY(),
                worldPosition.getZ() + 0.5 + Math.sin(w) * r);
        for (int i = 0; i < 4 && !sl.getBlockState(ziel.below()).isSolid(); i++) {
            ziel = ziel.below();
        }
        ItemEntity item = new ItemEntity(sl, ziel.getX() + 0.5, ziel.getY() + 0.1, ziel.getZ() + 0.5,
                new ItemStack(Zone.artefakt(a)), 0, 0, 0);
        item.setUnlimitedLifetime();
        item.setPickUpDelay(10);
        sl.addFreshEntity(item);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("Emission", letzteEmission);
        tag.putLong("Naechstes", naechstesArtefakt);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        letzteEmission = tag.contains("Emission") ? tag.getInt("Emission") : -1;
        naechstesArtefakt = tag.getLong("Naechstes");
    }
}
