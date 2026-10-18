package de.deadzonemc.verletzung;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Verletzungen wie in DayZ: Bisse und Hiebe können eine Blutung auslösen (hört erst mit einem Verband auf),
 * schwere Stürze brechen das Bein (nur eine Schiene hilft).
 */
public final class Verletzungen {

    private Verletzungen() {
    }

    public static final ResourceKey<DamageType> BLUTVERLUST = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "blutverlust"));

    public static final DeferredRegister<MobEffect> EFFEKTE = DeferredRegister.create(Registries.MOB_EFFECT, DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> BLUTUNG = EFFEKTE.register("blutung", Blutung::new);
    public static final DeferredHolder<MobEffect, MobEffect> KNOCHENBRUCH = EFFEKTE.register("knochenbruch",
            () -> new Bruch()
                    .addAttributeModifier(Attributes.MOVEMENT_SPEED,
                            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "knochenbruch_tempo"), -0.35,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                    .addAttributeModifier(Attributes.JUMP_STRENGTH,
                            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "knochenbruch_sprung"), -0.45,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredItem<SchieneItem> SCHIENE = ITEMS.register("schiene",
            () -> new SchieneItem(new Item.Properties().stacksTo(8)));

    public static void registrieren(IEventBus modBus) {
        EFFEKTE.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Verletzungen::verletzt);
    }

    private static void verletzt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isCreative() || event.getNewDamage() <= 0) {
            return;
        }
        DamageSource quelle = event.getSource();
        float schaden = event.getNewDamage();
        var zufall = p.getRandom();
        if (quelle.is(DamageTypeTags.IS_FALL)) {
            if (schaden >= 5 && zufall.nextFloat() < Math.min(0.85F, 0.25F + schaden * 0.06F) && !p.hasEffect(KNOCHENBRUCH)) {
                p.addEffect(new MobEffectInstance(KNOCHENBRUCH, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
                p.level().playSound(null, p.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.2F, 0.5F);
                p.displayClientMessage(Component.translatable("verletzung.deadzonemc.bruch").withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        if (quelle.getEntity() instanceof Enemy && quelle.getDirectEntity() == quelle.getEntity() && schaden >= 3) {
            float chance = 0.2F + (p.getItemBySlot(EquipmentSlot.CHEST).isEmpty() ? 0.12F : 0F) + Math.min(0.2F, schaden * 0.02F);
            if (zufall.nextFloat() < chance && !p.hasEffect(BLUTUNG)) {
                p.addEffect(new MobEffectInstance(BLUTUNG, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
                p.displayClientMessage(Component.translatable("verletzung.deadzonemc.blutung").withStyle(ChatFormatting.DARK_RED), true);
            }
        }
    }

    /** Verliert alle 5 Sekunden ein halbes Herz, bis ein Verband angelegt wird. */
    static class Blutung extends MobEffect {
        Blutung() {
            super(MobEffectCategory.HARMFUL, 0x8a0f0f);
        }

        @Override
        public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> kuren, MobEffectInstance instanz) {
            // Milch hilft nicht – nur Verband bzw. Schiene
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int dauer, int staerke) {
            return true;
        }

        @Override
        public boolean applyEffectTick(LivingEntity wer, int staerke) {
            if (wer.level() instanceof ServerLevel sl) {
                if (wer.tickCount % 10 == 0) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState()),
                            wer.getX(), wer.getY() + 0.4, wer.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                }
                if (wer.tickCount % 100 == 0) {
                    Holder<DamageType> typ = sl.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BLUTVERLUST);
                    wer.hurt(new DamageSource(typ), 1.0F);
                }
            }
            return true;
        }
    }

    /** Gebrochenes Bein: langsam, kaum springen (Wirkung über Attribute). */
    static class Bruch extends MobEffect {
        Bruch() {
            super(MobEffectCategory.HARMFUL, 0xd8d0b8);
        }

        @Override
        public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> kuren, MobEffectInstance instanz) {
            // Milch hilft nicht – nur Verband bzw. Schiene
        }
    }
}
