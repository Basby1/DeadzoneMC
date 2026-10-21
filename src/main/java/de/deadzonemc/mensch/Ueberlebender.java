package de.deadzonemc.mensch;

import de.deadzonemc.ModRegistry;
import de.deadzonemc.item.NotizItem;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.Wanderer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Einzelne Überlebende sind unberechenbar: manche helfen, manche stehlen, manche sind infiziert und
 * verwandeln sich, manche sind Banditen. Hinweise: Husten, nervöses Verhalten, zu gute Ausrüstung.
 */
public class Ueberlebender extends Mensch {

    public enum Gesinnung { FREUNDLICH, DIEB, INFIZIERT, BANDIT }

    private static final String[] NAMEN = {"Jonas", "Mira", "Kalle", "Svenja", "Tobias", "Ilka", "Ben", "Hanne",
            "Matze", "Rieke", "Oskar", "Lene", "Paul", "Greta", "Emil", "Nora"};

    private Gesinnung gesinnung = Gesinnung.FREUNDLICH;
    private boolean geholfen = false;
    private ItemStack beute = ItemStack.EMPTY;
    private int verwandlung = -1;
    private int flucht = 0;

    public Ueberlebender(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    public Fraktion fraktion() {
        return null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3) {
            @Override
            public boolean canUse() {
                return gesinnung != Gesinnung.BANDIT && super.canUse();
            }
        });
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.0, 1.3));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance schw, MobSpawnType grund, SpawnGroupData daten) {
        SpawnGroupData erg = super.finalizeSpawn(level, schw, grund, daten);
        int w = random.nextInt(100);
        gesinnung = w < 40 ? Gesinnung.FREUNDLICH : w < 65 ? Gesinnung.DIEB : w < 85 ? Gesinnung.INFIZIERT : Gesinnung.BANDIT;
        setCustomName(Component.literal(NAMEN[random.nextInt(NAMEN.length)]));
        setCustomNameVisible(false);
        if (gesinnung == Gesinnung.INFIZIERT) {
            verwandlung = 2400 + random.nextInt(2400);
        }
        return erg;
    }

    /** Für Ereignisse (Hilferuf): sicher ein freundlicher Überlebender. */
    public void freundlichMachen() {
        gesinnung = Gesinnung.FREUNDLICH;
        verwandlung = 0;
    }

    private void sagen(Player p, String schluessel) {
        p.displayClientMessage(Component.literal("<").append(getDisplayName()).append("> ")
                .append(Component.translatable("ueberlebender.deadzonemc." + schluessel)).withStyle(ChatFormatting.WHITE), false);
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (level().isClientSide || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        getLookControl().setLookAt(p);
        switch (gesinnung) {
            case FREUNDLICH -> {
                ItemStack hand_ = p.getItemInHand(hand);
                if (hand_.is(ModRegistry.MILITAERPATRONE.get()) && level() instanceof ServerLevel sl) {
                    anheuern(sl, p, hand_);
                    return InteractionResult.SUCCESS;
                }
                if (geholfen) {
                    sagen(p, "freundlich_danach" + random.nextInt(2));
                } else {
                    geholfen = true;
                    sagen(p, "freundlich" + random.nextInt(3));
                    ItemStack geschenk = switch (random.nextInt(5)) {
                        case 0 -> new ItemStack(ModRegistry.VERBAND.get(), 2);
                        case 1 -> new ItemStack(ModRegistry.KONSERVENDOSE.get(), 2);
                        case 2 -> new ItemStack(ModRegistry.SCHATZKARTE.get());
                        case 3 -> new ItemStack(ModRegistry.FILTER_ITEM.get());
                        default -> NotizItem.erstellen("funkspruch");
                    };
                    if (!p.getInventory().add(geschenk)) {
                        p.drop(geschenk, false);
                    }
                }
            }
            case DIEB -> {
                sagen(p, "dieb" + random.nextInt(2));
                if (beute.isEmpty()) {
                    int slot = random.nextInt(36);
                    ItemStack s = p.getInventory().getItem(slot);
                    if (!s.isEmpty()) {
                        beute = s.split(Math.max(1, s.getCount() / 2));
                        flucht = 400;
                        playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
                    }
                }
            }
            case INFIZIERT -> {
                sagen(p, "infiziert" + random.nextInt(2));
                playSound(SoundEvents.PLAYER_HURT, 0.7F, 0.6F);
                verwandlung = Math.min(verwandlung, 100 + random.nextInt(200));
            }
            case BANDIT -> {
                sagen(p, "bandit");
                setTarget(p);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (flucht > 0) {
            flucht--;
            Player p = level().getNearestPlayer(this, 24);
            if (p != null && getNavigation().isDone()) {
                Vec3 weg = position().subtract(p.position()).normalize().scale(16);
                getNavigation().moveTo(getX() + weg.x, getY(), getZ() + weg.z, 1.4);
            }
            if (flucht == 0 && p == null) {
                discard(); // mitsamt der Beute verschwunden
            }
        }
        if (gesinnung == Gesinnung.INFIZIERT) {
            if (random.nextInt(300) == 0) {
                playSound(SoundEvents.PLAYER_HURT, 0.5F, 0.5F); // Husten
            }
            if (verwandlung > 0 && --verwandlung == 0) {
                verwandeln();
            }
        }
        if (gesinnung == Gesinnung.BANDIT && getTarget() == null) {
            // greift an, wenn man ihm den Rücken zukehrt
            Player p = level().getNearestPlayer(this, 5);
            if (p != null && !p.isCreative()) {
                Vec3 blick = p.getViewVector(1.0F);
                Vec3 zuMir = position().subtract(p.position()).normalize();
                if (blick.dot(zuMir) < -0.2) {
                    setTarget(p);
                }
            }
        }
    }

    /** Für 15 Militärpatronen schließt sich ein freundlicher Überlebender an (höchstens zwei Begleiter). */
    private void anheuern(ServerLevel sl, Player p, ItemStack patronen) {
        if (patronen.getCount() < 15) {
            sagen(p, "anheuern_zu_wenig");
            return;
        }
        if (Begleiter.anzahl(sl, p) >= 2) {
            sagen(p, "anheuern_voll");
            return;
        }
        Begleiter b = MenschTypen.BEGLEITER.get().create(sl);
        if (b == null) {
            return;
        }
        patronen.shrink(15);
        b.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        b.setCustomName(getCustomName());
        b.besitzerSetzen(p);
        sl.addFreshEntity(b);
        p.displayClientMessage(Component.literal("<").append(getDisplayName()).append("> ")
                .append(Component.translatable("ueberlebender.deadzonemc.angeheuert")).withStyle(ChatFormatting.WHITE), false);
        playSound(SoundEvents.VILLAGER_YES, 0.8F, 1.0F);
        discard();
    }

    private void verwandeln() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        Wanderer w = MonsterTypen.WANDERER.get().create(sl);
        if (w != null) {
            w.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
            w.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
            sl.addFreshEntity(w);
        }
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.05);
        sl.playSound(null, blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1.2F, 0.7F);
        discard();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource quelle, boolean vomSpieler) {
        super.dropCustomDeathLoot(level, quelle, vomSpieler);
        if (!beute.isEmpty()) {
            spawnAtLocation(beute);
            beute = ItemStack.EMPTY;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Gesinnung", gesinnung.name());
        tag.putBoolean("Geholfen", geholfen);
        tag.putInt("Verwandlung", verwandlung);
        if (!beute.isEmpty()) {
            tag.put("Beute", beute.save(registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        try {
            gesinnung = Gesinnung.valueOf(tag.getString("Gesinnung"));
        } catch (IllegalArgumentException e) {
            gesinnung = Gesinnung.FREUNDLICH;
        }
        geholfen = tag.getBoolean("Geholfen");
        verwandlung = tag.contains("Verwandlung") ? tag.getInt("Verwandlung") : -1;
        if (tag.contains("Beute")) {
            beute = ItemStack.parseOptional(registryAccess(), tag.getCompound("Beute"));
        }
    }
}
