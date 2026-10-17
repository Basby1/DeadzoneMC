package de.deadzonemc.klasse;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.basis.Basen;
import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Klassen: Auswahl, passive Vorteile und Schwächen, aktive Fähigkeiten (Taste V). */
public final class Klassen {

    private Klassen() {
    }

    private static final ResourceLocation MOD_LEBEN = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_leben");
    private static final ResourceLocation MOD_RUESTUNG = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_ruestung");
    private static final ResourceLocation MOD_TEMPO = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_tempo");

    private static final Map<UUID, Long> ABKLINGEN = new HashMap<>();
    /** Aktive Lärmköder: Position und Ende (Spielzeit). */
    private static final List<long[]> KOEDER = new ArrayList<>();

    public static Klasse klasse(Player p) {
        return Klasse.ausId(p.getData(ModRegistry.KLASSE));
    }

    // ---- Wahl -------------------------------------------------------------------------------

    public static void empfangen(KlassePayload n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) {
                return;
            }
            if (n.klasse().equals("!")) {
                faehigkeit(p);
                return;
            }
            Klasse neu = Klasse.ausId(n.klasse());
            if (neu == null) {
                return;
            }
            Klasse alt = klasse(p);
            if (alt != null && !inEigenerBasis(p) && !p.isCreative()) {
                p.displayClientMessage(Component.translatable("klasse.deadzonemc.nur_basis").withStyle(ChatFormatting.RED), false);
                return;
            }
            p.setData(ModRegistry.KLASSE, neu.id());
            anwenden(p);
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.gewaehlt",
                    Component.translatable("klasse.deadzonemc." + neu.id())).withStyle(ChatFormatting.GOLD), false);
            PacketDistributor.sendToPlayer(p, new KlassePayload(neu.id(), false));
        });
    }

    private static boolean inEigenerBasis(ServerPlayer p) {
        return Basen.holen(p.serverLevel()).basisBei(p.getX(), p.getZ(), 0) != null;
    }

    public static void oeffnen(ServerPlayer p) {
        String k = p.getData(ModRegistry.KLASSE);
        PacketDistributor.sendToPlayer(p, new KlassePayload(k, true));
    }

    public static void betreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            anwenden(p);
            if (klasse(p) == null) {
                oeffnen(p);
            } else {
                PacketDistributor.sendToPlayer(p, new KlassePayload(p.getData(ModRegistry.KLASSE), false));
            }
        }
    }

    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            anwenden(p);
            p.setHealth(p.getMaxHealth());
        }
    }

    // ---- Passive Werte ------------------------------------------------------------------------

    private static void setze(AttributeInstance a, ResourceLocation id, double wert, AttributeModifier.Operation op) {
        if (a == null) {
            return;
        }
        a.removeModifier(id);
        if (wert != 0) {
            a.addPermanentModifier(new AttributeModifier(id, wert, op));
        }
    }

    public static void anwenden(ServerPlayer p) {
        Klasse k = klasse(p);
        double leben = 0;
        double ruestung = 0;
        double tempo = 0;
        if (k != null) {
            switch (k) {
                case BRECHER -> {
                    leben = 4;
                    ruestung = 2;
                }
                case SCHUETZE, SPAEHER -> leben = -4;
                case TECHNIKER -> tempo = -0.06;
                default -> {
                }
            }
        }
        setze(p.getAttribute(Attributes.MAX_HEALTH), MOD_LEBEN, leben, AttributeModifier.Operation.ADD_VALUE);
        setze(p.getAttribute(Attributes.ARMOR), MOD_RUESTUNG, ruestung, AttributeModifier.Operation.ADD_VALUE);
        setze(p.getAttribute(Attributes.MOVEMENT_SPEED), MOD_TEMPO, tempo, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    /** Nahkampf- und Fernkampfschaden je nach Klasse. */
    public static void schaden(LivingIncomingDamageEvent event) {
        Entity quelle = event.getSource().getEntity();
        if (!(quelle instanceof Player p)) {
            return;
        }
        Klasse k = klasse(p);
        if (k == null) {
            return;
        }
        boolean nahkampf = event.getSource().getDirectEntity() == p;
        float f = switch (k) {
            case BRECHER -> nahkampf ? 1.3F : 0.8F;
            case SCHUETZE -> nahkampf ? 0.75F : 1.25F;
            case SANITAETER -> 0.9F;
            default -> 1.0F;
        };
        event.setAmount(event.getAmount() * f);
    }

    /** Wie stark Heilung bei diesem Spieler wirkt (Sanitäter: 1,5-fach). */
    public static float heilfaktor(Player p) {
        return klasse(p) == Klasse.SANITAETER ? 1.5F : 1.0F;
    }

    // ---- Aktive Fähigkeiten ------------------------------------------------------------------

    private static void faehigkeit(ServerPlayer p) {
        Klasse k = klasse(p);
        if (k == null || p.isSpectator()) {
            return;
        }
        long jetzt = p.serverLevel().getGameTime();
        long frei = ABKLINGEN.getOrDefault(p.getUUID(), 0L);
        if (jetzt < frei) {
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.abklingen", (frei - jetzt) / 20 + 1)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean ok = switch (k) {
            case BRECHER -> ansturm(p);
            case SCHUETZE -> markieren(p);
            case SANITAETER -> adrenalin(p);
            case TECHNIKER -> koeder(p);
            case SPAEHER -> leuchtsignal(p);
        };
        if (ok) {
            ABKLINGEN.put(p.getUUID(), jetzt + k.abklingzeit());
        }
    }

    private static boolean ansturm(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 blick = p.getLookAngle().multiply(1, 0, 1).normalize();
        int getroffen = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5), e -> e instanceof Monster)) {
            Vec3 zu = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            if (zu.dot(blick) > 0.3) {
                e.knockback(1.6, -zu.x, -zu.z);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                e.hurt(p.damageSources().playerAttack(p), 3.0F);
                getroffen++;
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + blick.x * 2, p.getY() + 1, p.getZ() + blick.z * 2, 3, 0.8, 0.3, 0.8, 0);
        return true;
    }

    private static boolean markieren(ServerPlayer p) {
        Vec3 auge = p.getEyePosition();
        Vec3 ende = auge.add(p.getLookAngle().scale(64));
        EntityHitResult treffer = ProjectileUtil.getEntityHitResult(p.level(), p, auge, ende,
                new AABB(auge, ende).inflate(1.5), e -> e instanceof LivingEntity && !(e instanceof Player), 0.6F);
        if (treffer == null || !(treffer.getEntity() instanceof LivingEntity ziel)) {
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.kein_ziel").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ziel.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        ziel.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
        p.serverLevel().playSound(null, ziel.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    private static boolean adrenalin(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (ServerPlayer m : level.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(6))) {
            m.heal(8.0F);
            m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            m.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            level.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + 1.8, m.getZ(), 5, 0.4, 0.3, 0.4, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    private static boolean koeder(ServerPlayer p) {
        var treffer = p.pick(24, 1.0F, false);
        BlockPos pos = BlockPos.containing(treffer.getLocation());
        KOEDER.add(new long[]{pos.asLong(), p.serverLevel().getGameTime() + 400});
        p.displayClientMessage(Component.translatable("klasse.deadzonemc.koeder").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static boolean leuchtsignal(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40), e -> e instanceof Monster)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
        }
        level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 6, p.getZ(), 40, 0.5, 0.5, 0.5, 0.15);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Lärmköder: lockt Monster in der Nähe an und piept dabei. */
    public static void tick(ServerTickEvent.Post event) {
        if (KOEDER.isEmpty()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long jetzt = level.getGameTime();
        Iterator<long[]> it = KOEDER.iterator();
        while (it.hasNext()) {
            long[] k = it.next();
            if (jetzt > k[1]) {
                it.remove();
                continue;
            }
            if (jetzt % 20 != 0) {
                continue;
            }
            BlockPos pos = BlockPos.of(k[0]);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 2.0F, 1.6F);
            level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(32))) {
                if (m.getTarget() == null || m instanceof DeadzoneMonster) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.1);
                }
            }
        }
    }
}
