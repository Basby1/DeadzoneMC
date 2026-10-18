package de.deadzonemc.monster;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Optional;
import java.util.UUID;

/**
 * Die Mimik: trägt Haut und Namen eines Mitspielers (allein im Spiel: deine eigene) und folgt dir wie er – mit
 * etwas Abstand, immer ein bisschen zu still. Ist der echte Mitspieler weit weg und du allein mit ihr, reißt ihr
 * Gesicht auf, sie schreit und greift an. Wer sie vorher schlägt, löst es sofort aus.
 */
public class Mimik extends DeadzoneMonster {

    private static final EntityDataAccessor<Boolean> ENTHUELLT = SynchedEntityData.defineId(Mimik.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> VORBILD = SynchedEntityData.defineId(Mimik.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final net.minecraft.resources.ResourceLocation TEMPO = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
            de.deadzonemc.DeadzoneMC.MODID, "mimik_tempo");

    private UUID opfer;
    private int alleinZeit;

    public Mimik(EntityType<? extends Monster> typ, Level level) {
        super(typ, level, "mimik");
    }

    public static AttributeSupplier.Builder attribute() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ATTACK_DAMAGE, 9)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ENTHUELLT, false);
        b.define(VORBILD, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // getarnt greift sie niemanden von sich aus an
        targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal);
    }

    @Override
    protected boolean hatLauf() {
        return false;
    }

    @Override
    protected boolean hatSpezial() {
        return true;
    }

    public boolean enthuellt() {
        return entityData.get(ENTHUELLT);
    }

    public Optional<UUID> vorbild() {
        return entityData.get(VORBILD);
    }

    public void tarnen(Player vorbild, Player ziel) {
        entityData.set(VORBILD, Optional.of(vorbild.getUUID()));
        opfer = ziel.getUUID();
        setCustomName(vorbild.getName().copy());
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    private void enthuellen(ServerLevel sl) {
        if (enthuellt()) {
            return;
        }
        entityData.set(ENTHUELLT, true);
        setCustomName(null);
        setCustomNameVisible(false);
        triggerAnim("aktion", "special");
        playSound(MonsterSounds.holen("schreier", "schrei"), 5.0F, 0.7F);
        playSound(MonsterSounds.holen("lauerer", "schreck"), 4.0F, 0.8F);
        sl.sendParticles(ParticleTypes.SQUID_INK, getX(), getEyeY(), getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        var tempo = getAttribute(Attributes.MOVEMENT_SPEED);
        if (tempo != null && !tempo.hasModifier(TEMPO)) {
            tempo.addPermanentModifier(new AttributeModifier(TEMPO, 0.45, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        Player p = opfer == null ? null : sl.getPlayerByUUID(opfer);
        if (p == null) {
            p = sl.getNearestPlayer(this, 32);
        }
        if (p instanceof ServerPlayer sp) {
            setTarget(sp);
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new de.deadzonemc.netz.SchreckPayload(3));
        }
    }

    @Override
    public boolean hurt(DamageSource quelle, float menge) {
        if (!enthuellt() && level() instanceof ServerLevel sl && quelle.getEntity() instanceof Player) {
            enthuellen(sl);
        }
        return super.hurt(quelle, menge);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl) || enthuellt()) {
            return;
        }
        Player ziel = opfer == null ? null : sl.getPlayerByUUID(opfer);
        if (ziel == null || !ziel.isAlive()) {
            if (tickCount > 200) {
                discard(); // niemand mehr da, den sie nachahmen kann
            }
            return;
        }
        // folgt mit Abstand, schaut dich an, bleibt stehen, wenn du dich umdrehst
        double d = distanceTo(ziel);
        getLookControl().setLookAt(ziel);
        boolean siehtMich = ziel.getLookAngle().dot(position().subtract(ziel.getEyePosition()).normalize()) > 0.8;
        if (siehtMich || d < 6) {
            getNavigation().stop();
        } else if (d > 10 && tickCount % 20 == 0) {
            getNavigation().moveTo(ziel, 0.9);
        }
        // Allein mit ihr? Das Vorbild ist weit weg (oder gar nicht online) und kein anderer echter Spieler in der Nähe
        Player echt = vorbild().map(sl::getPlayerByUUID).orElse(null);
        boolean vorbildWeg = echt == null || echt == ziel || echt.distanceTo(ziel) > 48;
        boolean allein = sl.players().stream().noneMatch(p -> p != ziel && DeadzoneMonster.echterSpieler(p) && p.distanceTo(ziel) < 32);
        if (vorbildWeg && allein && d < 14) {
            if (++alleinZeit > 200 + random.nextInt(200)) {
                enthuellen(sl);
            }
        } else {
            alleinZeit = Math.max(0, alleinZeit - 2);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enthuellt", enthuellt());
        vorbild().ifPresent(u -> tag.putUUID("Vorbild", u));
        if (opfer != null) {
            tag.putUUID("Opfer", opfer);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ENTHUELLT, tag.getBoolean("Enthuellt"));
        if (tag.hasUUID("Vorbild")) {
            entityData.set(VORBILD, Optional.of(tag.getUUID("Vorbild")));
        }
        if (tag.hasUUID("Opfer")) {
            opfer = tag.getUUID("Opfer");
        }
    }

    // ---- Erscheinen ----------------------------------------------------------------------------

    /** Nachts selten: Eine Mimik taucht hinter einem Spieler auf, in der Gestalt eines Mitspielers. */
    public static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 2400 != 1234) {
            return;
        }
        ServerLevel w = event.getServer().overworld();
        if (!w.isNight() || w.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return;
        }
        var spieler = w.players().stream().filter(DeadzoneMonster::echterSpieler).toList();
        for (ServerPlayer p : spieler) {
            if (w.random.nextFloat() > (spieler.size() > 1 ? 0.06F : 0.03F) || SpawnSchutz.geschuetzt(p)
                    || de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(w, p.blockPosition())) {
                continue;
            }
            if (!w.getEntitiesOfClass(Mimik.class, p.getBoundingBox().inflate(96)).isEmpty()) {
                continue;
            }
            Player vorbild = spieler.size() > 1 ? spieler.stream().filter(x -> x != p).findAny().orElse(p) : p;
            float yaw = p.getYRot() + 180 + (w.random.nextFloat() - 0.5F) * 60;
            double ab = 18 + w.random.nextDouble() * 6;
            int x = Mth.floor(p.getX() - Math.sin(Math.toRadians(yaw)) * ab);
            int z = Mth.floor(p.getZ() + Math.cos(Math.toRadians(yaw)) * ab);
            if (!w.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int y = w.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Mimik m = MonsterTypen.MIMIK.get().create(w);
            if (m == null) {
                continue;
            }
            m.moveTo(x + 0.5, y, z + 0.5, w.random.nextFloat() * 360, 0);
            if (!w.noCollision(m) || p.hasLineOfSight(m)) {
                continue;
            }
            m.finalizeSpawn(w, w.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.EVENT, null);
            m.tarnen(vorbild, p);
            w.addFreshEntity(m);
        }
    }

    @SuppressWarnings("unused")
    private static Component titel() {
        return Component.literal("Mimik").withStyle(ChatFormatting.DARK_RED);
    }
}
