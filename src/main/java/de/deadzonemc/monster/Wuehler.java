package de.deadzonemc.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Blinder Gräber der Höhlen: liegt eingegraben im Fels und bricht unter dem Spieler hervor. Graben kann
 * er nur durch natürlichen Stein und Erde – auf gebauten Böden (Basis, Häuser) ist man sicher.
 */
public class Wuehler extends DeadzoneMonster {

    private boolean eingegraben;
    private int warnung;
    private Player opfer;
    private int ohneZiel;

    public Wuehler(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "wuehler");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 7)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.ARMOR, 4);
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    /** Natürlicher Boden, durch den ein Wühler graben kann. */
    public static boolean natuerlich(BlockState b) {
        return b.is(BlockTags.BASE_STONE_OVERWORLD) || b.is(BlockTags.DIRT) || b.is(BlockTags.SAND) || b.is(Blocks.GRAVEL)
                || b.is(Blocks.DEEPSLATE) || b.is(Blocks.TUFF) || b.is(Blocks.CALCITE) || b.is(Blocks.CLAY)
                || b.is(BlockTags.STONE_ORE_REPLACEABLES) || b.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    private int unterHimmel;

    private boolean aufNatur(BlockPos fuss) {
        return natuerlich(level().getBlockState(fuss.below()));
    }

    private void eingraben(ServerLevel sl) {
        triggerAnim("aktion", "special");
        playSound(MonsterSounds.holen(art, "graben"), 1.5F, 1.0F);
        krumen(sl, blockPosition());
        eingegraben = true;
        setInvisible(true);
        setSilent(true);
        setNoGravity(true);
        noPhysics = true;
        setTarget(null);
        getNavigation().stop();
        teleportTo(getX(), getY() - 2.5, getZ());
    }

    private void auftauchen(ServerLevel sl, BlockPos wo) {
        eingegraben = false;
        noPhysics = false;
        setNoGravity(false);
        setInvisible(false);
        setSilent(false);
        teleportTo(wo.getX() + 0.5, wo.getY(), wo.getZ() + 0.5);
        krumen(sl, wo);
        playSound(MonsterSounds.holen(art, "graben"), 2.0F, 0.7F);
        triggerAnim("aktion", "attack");
        if (opfer != null) {
            setTarget(opfer);
            if (distanceToSqr(opfer) < 4) {
                doHurtTarget(opfer);
            }
        }
        opfer = null;
        ohneZiel = 0;
    }

    private void krumen(ServerLevel sl, BlockPos pos) {
        BlockState b = sl.getBlockState(pos.below());
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, b.isAir() ? Blocks.DIRT.defaultBlockState() : b),
                pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 40, 0.6, 0.3, 0.6, 0.15);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (eingegraben) {
            getNavigation().stop();
            setDeltaMovement(0, 0, 0);
            if (opfer != null) {
                if (!opfer.isAlive() || !aufNatur(opfer.blockPosition()) || opfer.distanceToSqr(this) > 16 * 16
                        || sl.canSeeSky(opfer.blockPosition())) {
                    opfer = null; // auf gebautem Boden kann er nicht hin
                    return;
                }
                if (--warnung % 5 == 0) {
                    krumen(sl, opfer.blockPosition());
                }
                if (warnung <= 0) {
                    auftauchen(sl, opfer.blockPosition());
                }
            } else if (tickCount % 10 == 0) {
                for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(10, 6, 10), DeadzoneMonster::echterSpieler)) {
                    if (aufNatur(p.blockPosition()) && !p.isCrouching() && !sl.canSeeSky(p.blockPosition())) {
                        opfer = p;
                        warnung = 30;
                        playSound(MonsterSounds.holen(art, "graben"), 1.0F, 0.6F);
                        break;
                    }
                }
            }
            return;
        }
        // Ein Höhlenwesen: unter freiem Himmel taucht er sofort wieder ab (oder verschwindet in einer Spalte)
        if (sl.canSeeSky(blockPosition())) {
            setTarget(null);
            if (onGround() && aufNatur(blockPosition())) {
                eingraben(sl);
                return;
            }
            if (++unterHimmel > 60) {
                krumen(sl, blockPosition());
                discard();
            }
            return;
        }
        unterHimmel = 0;
        if (getTarget() != null && sl.canSeeSky(getTarget().blockPosition())) {
            setTarget(null); // nach draußen folgt er niemandem
        }
        if (getTarget() == null) {
            if (++ohneZiel > 200 && onGround() && aufNatur(blockPosition())) {
                eingraben(sl);
            }
        } else {
            ohneZiel = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (eingegraben) {
            return false;
        }
        return super.hurt(quelle, menge);
    }

    @Override
    public boolean isPushable() {
        return !eingegraben && super.isPushable();
    }

    @Override
    public boolean isPickable() {
        return !eingegraben && super.isPickable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Eingegraben", eingegraben);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        eingegraben = tag.getBoolean("Eingegraben");
        if (eingegraben) {
            setInvisible(true);
            setSilent(true);
            setNoGravity(true);
            noPhysics = true;
        }
    }
}
