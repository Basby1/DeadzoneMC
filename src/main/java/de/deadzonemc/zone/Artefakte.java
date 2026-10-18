package de.deadzonemc.zone;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.strahlung.Strahlung;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumSet;
import java.util.Set;

/** Wirkung getragener Artefakte und der Anomalie-Detektor. */
public final class Artefakte {

    private static final ResourceLocation LEBEN = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "seelenkristall");

    private Artefakte() {
    }

    public static Set<ArtefaktItem.Art> getragen(Player p) {
        Set<ArtefaktItem.Art> arten = EnumSet.noneOf(ArtefaktItem.Art.class);
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() instanceof ArtefaktItem a) {
                arten.add(a.art());
            }
        }
        if (p.getOffhandItem().getItem() instanceof ArtefaktItem a) {
            arten.add(a.art());
        }
        return arten;
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        long zeit = p.level().getGameTime();
        if (zeit % 10 == 3 && haeltDetektor(p)) {
            detektor(p);
        }
        if (zeit % 20 != 11) {
            return;
        }
        Set<ArtefaktItem.Art> arten = getragen(p);
        float strahlung = 0;
        for (ArtefaktItem.Art a : arten) {
            strahlung += a.strahlung();
            switch (a) {
                case FEUERKUGEL -> {
                    p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 50, 0, true, false, true));
                    p.causeFoodExhaustion(0.35F);
                }
                case BLITZKERN -> {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 0, true, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 50, 0, true, false, true));
                }
                case STEINBLUT -> {
                    if (zeit % 100 == 11 && p.getHealth() < p.getMaxHealth()) {
                        p.heal(1.0F);
                    }
                }
                case QUALLE, SEELENKRISTALL -> {
                }
            }
        }
        if (strahlung > 0) {
            Strahlung.bestrahlen(p, strahlung);
        }
        AttributeInstance leben = p.getAttribute(Attributes.MAX_HEALTH);
        if (leben != null) {
            boolean soll = arten.contains(ArtefaktItem.Art.SEELENKRISTALL);
            if (soll && !leben.hasModifier(LEBEN)) {
                leben.addTransientModifier(new AttributeModifier(LEBEN, 8.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (!soll && leben.hasModifier(LEBEN)) {
                leben.removeModifier(LEBEN);
                if (p.getHealth() > p.getMaxHealth()) {
                    p.setHealth(p.getMaxHealth());
                }
            }
        }
    }

    public static void fallen(LivingFallEvent event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && getragen(p).contains(ArtefaktItem.Art.QUALLE)) {
            event.setDistance(Math.max(0, event.getDistance() - 6));
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.4F);
        }
    }

    // ---- Detektor --------------------------------------------------------------------------

    private static boolean haeltDetektor(Player p) {
        return p.getMainHandItem().is(Zone.DETEKTOR.get()) || p.getOffhandItem().is(Zone.DETEKTOR.get());
    }

    /** Nächste Anomalie im Umkreis (oder null). */
    public static AnomalieBlockEntity naechste(ServerLevel level, BlockPos um, int radius) {
        AnomalieBlockEntity best = null;
        double bestD = (double) radius * radius;
        ChunkPos c = new ChunkPos(um);
        int r = (radius >> 4) + 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!level.hasChunk(c.x + dx, c.z + dz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(c.x + dx, c.z + dz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof AnomalieBlockEntity a) {
                        double d = be.getBlockPos().distSqr(um);
                        if (d < bestD) {
                            bestD = d;
                            best = a;
                        }
                    }
                }
            }
        }
        return best;
    }

    private static void detektor(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        AnomalieBlockEntity a = naechste(level, p.blockPosition(), 28);
        ItemEntity artefakt = null;
        double artD = 24 * 24;
        for (ItemEntity i : level.getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(24),
                i -> i.getItem().getItem() instanceof ArtefaktItem)) {
            double d = i.distanceToSqr(p);
            if (d < artD) {
                artD = d;
                artefakt = i;
            }
        }
        if (a == null && artefakt == null) {
            p.displayClientMessage(Component.translatable("zone.deadzonemc.detektor.ruhig").withStyle(ChatFormatting.DARK_GRAY), true);
            return;
        }
        Component zeile = Component.empty();
        if (a != null) {
            double d = Math.sqrt(a.getBlockPos().distToCenterSqr(p.position()));
            zeile = Component.translatable("zone.deadzonemc.detektor.anomalie",
                    Component.translatable("zone.deadzonemc.anomalie." + a.art().id()), (int) Math.round(d))
                    .withStyle(d < 5 ? ChatFormatting.RED : ChatFormatting.GOLD);
            // Piepen: je näher, desto öfter und höher
            int takt = d < 4 ? 1 : d < 8 ? 2 : d < 14 ? 3 : 4;
            if ((level.getGameTime() / 10) % takt == 0) {
                p.playNotifySound(SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F,
                        (float) Math.max(0.6, 2.0 - d / 20.0));
            }
        }
        if (artefakt != null) {
            int d = (int) Math.round(Math.sqrt(artD));
            Component teil = Component.translatable("zone.deadzonemc.detektor.artefakt", d).withStyle(ChatFormatting.AQUA);
            zeile = a == null ? teil : zeile.copy().append(Component.literal("  ·  ").withStyle(ChatFormatting.DARK_GRAY)).append(teil);
            if (level.getGameTime() % 20 == 3) {
                p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        }
        p.displayClientMessage(zeile, true);
    }
}
