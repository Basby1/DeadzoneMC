package de.deadzonemc.strahlung;

import com.mojang.serialization.Codec;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Strahlung und Giftgas wie in Stalker und Metro: Verseuchte Gebiete und strahlende Fässer erhöhen die
 * Dosis. Eine Gasmaske mit Filter schützt, Anti-Rad-Tabletten senken die Dosis, der Geigerzähler zeigt sie an.
 */
public final class Strahlung {

    private Strahlung() {
    }

    public static final int MAX = 1000;

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> DOSIS = ModRegistry.STRAHLENDOSIS;

    private static final ResourceKey<Biome> MOOR = biom("verseuchtes_moor");
    private static final ResourceKey<Biome> OEDLAND = biom("oedland");
    private static final ResourceKey<Biome> TOTES_MEER = biom("totes_meer");
    private static final net.minecraft.tags.TagKey<Biome> VERSEUCHT = net.minecraft.tags.TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "verseucht"));
    private static final net.minecraft.tags.TagKey<Biome> GIFTGAS = net.minecraft.tags.TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "giftgas"));

    /** Letzte gemessene Rate je Spieler (für das Knacken des Geigerzählers jeden Tick). */
    private static final Map<UUID, Double> RATE = new HashMap<>();

    private static ResourceKey<Biome> biom(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, name));
    }

    public static float dosis(Player p) {
        return p.getData(DOSIS);
    }

    /** Zusätzliche Strahlung von außen (Seuchenträger, Der Strahlende). Talente und Maske schützen. */
    public static void bestrahlen(Player p, float menge) {
        if (p.isCreative() || p.isSpectator()) {
            return;
        }
        net.minecraft.world.item.ItemStack maske = p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (maske.is(ModRegistry.GASMASKE.get()) && maske.getOrDefault(ModRegistry.FILTER.get(), 0) > 0) {
            menge *= 0.3F;
        }
        menge *= de.deadzonemc.klasse.Talente.strahlenfaktor(p) * de.deadzonemc.verzauberung.Zonenverzauberung.strahlenFaktor(p)
                * de.deadzonemc.bossmod.Bossmods.strahlung(p);
        if (de.deadzonemc.ausruestung.Satzboni.schutzanzug(p)) {
            menge *= 0.2F;
        }
        p.setData(DOSIS, Math.min(MAX, p.getData(DOSIS) + menge));
    }

    public static void senken(Player p, float menge) {
        p.setData(DOSIS, Math.max(0, p.getData(DOSIS) - menge));
    }

    /** Strahlung pro Sekunde an der Position des Spielers. */
    private static double rate(ServerLevel level, Player p) {
        BlockPos pos = p.blockPosition();
        double r = 0;
        var biom = level.getBiome(pos);
        if (biom.is(MOOR)) {
            r += 1.5;
        } else if (biom.is(VERSEUCHT)) {
            r += 0.4;
        } else if (biom.is(OEDLAND)) {
            r += 0.5;
        } else if (biom.is(TOTES_MEER) && p.isInWater()) {
            r += 2.0;
        }
        for (BlockPos b : BlockPos.betweenClosed(pos.offset(-6, -4, -6), pos.offset(6, 4, 6))) {
            if (level.getBlockState(b).is(ModRegistry.STRAHLENFASS.get())) {
                double d2 = b.distToCenterSqr(p.getX(), p.getY() + 1, p.getZ());
                r += 14.0 / (d2 + 1);
            }
        }
        return r;
    }

    private static boolean giftgas(ServerLevel level, Player p) {
        var biom = level.getBiome(p.blockPosition());
        return biom.is(MOOR) || biom.is(GIFTGAS);
    }

    public static boolean haeltGeigerzaehler(Player p) {
        return p.getMainHandItem().is(ModRegistry.GEIGERZAEHLER.get()) || p.getOffhandItem().is(ModRegistry.GEIGERZAEHLER.get());
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) {
            return;
        }
        // Knacken des Geigerzählers: jeden Tick, je nach Rate
        if (haeltGeigerzaehler(p)) {
            double r = RATE.getOrDefault(p.getUUID(), 0.0);
            if (r > 0.05 && p.getRandom().nextDouble() < Math.min(0.9, r / 3.0)) {
                p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.PLAYERS, 0.35F, 1.7F + p.getRandom().nextFloat() * 0.5F);
            }
        }
        if (p.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = p.serverLevel();
        double r = rate(level, p);
        boolean gas = giftgas(level, p);
        ItemStack maske = p.getItemBySlot(EquipmentSlot.HEAD);
        int filter = maske.is(ModRegistry.GASMASKE.get()) ? maske.getOrDefault(ModRegistry.FILTER.get(), 0) : 0;
        boolean geschuetzt = filter > 0;
        if (geschuetzt && (r > 0 || gas)) {
            int rest = Math.max(0, filter - 20);
            maske.set(ModRegistry.FILTER.get(), rest);
            if (rest == 0) {
                p.displayClientMessage(Component.translatable("strahlung.deadzonemc.filter_leer").withStyle(ChatFormatting.RED), true);
                p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        }
        if (geschuetzt) {
            r *= 0.2;
        }
        if (r > 0) {
            r *= de.deadzonemc.klasse.Talente.strahlenfaktor(p) * de.deadzonemc.verzauberung.Zonenverzauberung.strahlenFaktor(p)
                    * de.deadzonemc.bossmod.Bossmods.strahlung(p);
            if (de.deadzonemc.ausruestung.Satzboni.schutzanzug(p)) {
                r *= 0.2F;
            }
        }
        RATE.put(p.getUUID(), r);
        if (gas && !geschuetzt) {
            p.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            if (p.tickCount % 200 == 0) {
                p.displayClientMessage(Component.translatable("strahlung.deadzonemc.giftgas").withStyle(ChatFormatting.DARK_GREEN), true);
            }
        }
        float dosis = p.getData(DOSIS);
        dosis = (float) Math.max(0, Math.min(MAX, dosis + r - (r < 0.01 ? 0.1 : 0)));
        p.setData(DOSIS, dosis);
        wirken(p, dosis);
        if (haeltGeigerzaehler(p)) {
            ChatFormatting farbe = dosis >= 750 ? ChatFormatting.DARK_RED : dosis >= 500 ? ChatFormatting.RED
                    : dosis >= 250 ? ChatFormatting.GOLD : ChatFormatting.GREEN;
            p.displayClientMessage(Component.translatable("strahlung.deadzonemc.anzeige",
                    String.format("%.1f", r), Math.round(dosis), MAX).withStyle(farbe), true);
        }
    }

    private static void wirken(ServerPlayer p, float dosis) {
        var z = p.getRandom();
        if (dosis >= 250 && z.nextInt(40) == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 0));
        }
        if (dosis >= 500) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false, true));
            if (z.nextInt(20) == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 0));
            }
        }
        if (dosis >= 750 && z.nextInt(8) == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
        }
        if (dosis >= MAX) {
            p.hurt(p.damageSources().magic(), 2.0F);
        }
    }
}
