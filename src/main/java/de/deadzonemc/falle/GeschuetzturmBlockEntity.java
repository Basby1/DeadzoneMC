package de.deadzonemc.falle;

import de.deadzonemc.monster.Laerm;
import de.deadzonemc.waffen.feuer.Feuerwaffen;
import de.deadzonemc.waffen.feuer.Kaliber;
import de.deadzonemc.waffen.feuer.MunitionItem;
import de.deadzonemc.waffen.feuer.WaffenArt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Geschützturm: schießt selbstständig auf Infizierte und Monster in 20 Blöcken, die er sehen kann. Munition (9 mm,
 * 5,56 oder 7,62) kommt in sein Magazin (3×3). Jeder Schuss ist laut – ein Turm schützt, lockt aber auch an.
 */
public class GeschuetzturmBlockEntity extends BaseContainerBlockEntity {

    public static final double REICHWEITE = 20;
    private NonNullList<ItemStack> inhalt = NonNullList.withSize(9, ItemStack.EMPTY);
    /** Blickrichtung des Kopfes (Grad), auf dem Client geglättet. */
    public float drehung;
    public float drehungAlt;
    private float zielDrehung;
    private int abklingen;
    public int feuer;

    public GeschuetzturmBlockEntity(BlockPos pos, BlockState zustand) {
        super(Fallen.GESCHUETZTURM_BE.get(), pos, zustand);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState zustand, GeschuetzturmBlockEntity t) {
        if (--t.abklingen > 0 || !(level instanceof ServerLevel sl)) {
            return;
        }
        t.abklingen = 5;
        Vec3 kopf = Vec3.atCenterOf(pos).add(0, 0.35, 0);
        LivingEntity ziel = null;
        double best = REICHWEITE * REICHWEITE;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(pos).inflate(REICHWEITE),
                m -> m instanceof Enemy && m.isAlive() && !m.isInvisible())) {
            double d = m.distanceToSqr(kopf);
            if (d < best && sicht(sl, kopf, m)) {
                best = d;
                ziel = m;
            }
        }
        if (ziel == null) {
            return;
        }
        int slot = -1;
        Kaliber k = null;
        for (int i = 0; i < t.inhalt.size(); i++) {
            if (t.inhalt.get(i).getItem() instanceof MunitionItem mi
                    && (mi.kaliber == Kaliber.NEUN_MM || mi.kaliber == Kaliber.F556 || mi.kaliber == Kaliber.F762)) {
                slot = i;
                k = mi.kaliber;
                break;
            }
        }
        Vec3 auge = ziel.getEyePosition().add(0, -0.2, 0);
        Vec3 dir = auge.subtract(kopf);
        t.zielDrehung = (float) Math.toDegrees(Math.atan2(-dir.x, -dir.z));
        t.drehung = t.zielDrehung;
        if (slot < 0) {
            sl.sendBlockUpdated(pos, zustand, zustand, 3);
            if (sl.getGameTime() % 60 == 0) {
                sl.playSound(null, pos, Feuerwaffen.LEER.get(), SoundSource.BLOCKS, 0.8F, 1.3F);
            }
            return;
        }
        t.inhalt.get(slot).shrink(1);
        t.setChanged();
        float schaden = k == Kaliber.NEUN_MM ? 4.5F : k == Kaliber.F556 ? 6.0F : 7.5F;
        t.abklingen = k == Kaliber.NEUN_MM ? 5 : k == Kaliber.F556 ? 6 : 9;
        ziel.invulnerableTime = 0;
        ziel.hurt(new DamageSource(sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(de.deadzonemc.waffen.feuer.Schiessen.SCHUSS)), schaden);
        ziel.invulnerableTime = 0;
        sl.playSound(null, pos, Feuerwaffen.SCHUSS.get(k == Kaliber.NEUN_MM ? WaffenArt.MP9 : WaffenArt.STURMGEWEHR).get(),
                SoundSource.BLOCKS, 2.2F, 0.95F + sl.random.nextFloat() * 0.1F);
        Vec3 muendung = kopf.add(dir.normalize().scale(0.7));
        sl.sendParticles(ParticleTypes.FLAME, muendung.x, muendung.y, muendung.z, 1, 0.02, 0.02, 0.02, 0.01);
        sl.sendParticles(ParticleTypes.SMOKE, muendung.x, muendung.y, muendung.z, 2, 0.03, 0.03, 0.03, 0.01);
        sl.sendParticles(ParticleTypes.CRIT, auge.x, auge.y, auge.z, 4, 0.1, 0.1, 0.1, 0.1);
        Laerm.machen(sl, kopf, 40);
        t.feuer = 2;
        sl.sendBlockUpdated(pos, zustand, zustand, 3);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState zustand, GeschuetzturmBlockEntity t) {
        t.drehungAlt = t.drehung;
        float d = Mth.wrapDegrees(t.zielDrehung - t.drehung);
        t.drehung += Mth.clamp(d, -25, 25);
        if (t.feuer > 0) {
            t.feuer--;
        }
        if (level.getGameTime() % 40 == 0 && t.zielDrehung == t.drehung) {
            t.zielDrehung += 30; // schaut sich um
        }
    }

    private static boolean sicht(ServerLevel sl, Vec3 von, LivingEntity e) {
        return sl.clip(new ClipContext(von, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.deadzonemc.geschuetzturm");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inhalt;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> liste) {
        inhalt = liste;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new DispenserMenu(id, inv, this);
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack s) {
        return s.getItem() instanceof MunitionItem;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        inhalt = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inhalt, p);
        zielDrehung = tag.getFloat("Drehung");
        feuer = tag.getBoolean("Feuer") ? 2 : feuer;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        ContainerHelper.saveAllItems(tag, inhalt, p);
        tag.putFloat("Drehung", zielDrehung);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Drehung", zielDrehung);
        tag.putBoolean("Feuer", feuer > 0);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
