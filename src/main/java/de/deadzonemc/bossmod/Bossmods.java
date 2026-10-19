package de.deadzonemc.bossmod;

import com.mojang.serialization.Codec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Boss-Modifikationen: Jeder Boss lässt eine Trophäe fallen. An der Zonenwerkbank wird sie in eine Waffe oder ein
 * Rüstungsteil eingebaut (ein Platz je Gegenstand) und wirkt dort je nach Art anders – in Waffen offensiv, in
 * Rüstungen schützend. Wer bestimmte Wirkungen will, muss gezielt bestimmte Bosse jagen.
 */
public final class Bossmods {

    /** Alle Bosse mit Trophäe (Reihenfolge = Kreativ-Tab). */
    public static final List<String> BOSSE = List.of("schlaechter", "erfrorene", "pilzvater", "nebelmutter", "schlund",
            "brutkoenigin", "strahlende", "hirte", "kommandant", "verschmolzener", "das_herz", "der_ursprung", "schwarmmutter", "pilot");

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<DataComponentType<?>> KOMPONENTEN =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DeadzoneMC.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> MOD =
            KOMPONENTEN.register("bossmod", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8).build());
    public static final Map<String, DeferredItem<TrophaeeItem>> TROPHAEE = new LinkedHashMap<>();
    /** Boss-Köpfe zum Aufhängen am Trophäenbrett – nur Bosse lassen Köpfe fallen. */
    public static final Map<String, DeferredItem<Item>> KOPF = new LinkedHashMap<>();

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BE =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);
    public static final net.neoforged.neoforge.registries.DeferredBlock<TrophaeenbrettBlock> BRETT = BLOECKE.register("trophaeenbrett",
            () -> new TrophaeenbrettBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                    .mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(1.5F).noOcclusion()
                    .sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BRETT_ITEM = ITEMS.registerSimpleBlockItem(BRETT);
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,
            net.minecraft.world.level.block.entity.BlockEntityType<TrophaeenbrettBlockEntity>> BRETT_BE = BE.register("trophaeenbrett",
            () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(TrophaeenbrettBlockEntity::new, BRETT.get()).build(null));

    static {
        for (String b : BOSSE) {
            TROPHAEE.put(b, ITEMS.register("trophaee_" + b, () -> new TrophaeeItem(b,
                    new Item.Properties().rarity(Rarity.EPIC).stacksTo(16))));
            KOPF.put(b, ITEMS.register("kopf_" + b, () -> new Item(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))));
        }
    }

    private Bossmods() {
    }

    public static void registrieren(IEventBus modBus) {
        ITEMS.register(modBus);
        KOMPONENTEN.register(modBus);
        BLOECKE.register(modBus);
        BE.register(modBus);
        NeoForge.EVENT_BUS.addListener(Bossmods::schaden);
        NeoForge.EVENT_BUS.addListener(Bossmods::tod);
        NeoForge.EVENT_BUS.addListener(Bossmods::tick);
        NeoForge.EVENT_BUS.addListener(Bossmods::attribute);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        TROPHAEE.values().forEach(t -> ausgabe.accept(t.get()));
        ausgabe.accept(BRETT_ITEM.get());
        KOPF.values().forEach(t -> ausgabe.accept(t.get()));
    }

    // ---- Abfragen -----------------------------------------------------------------------------

    public static String mod(ItemStack s) {
        return s.isEmpty() ? null : s.get(MOD.get());
    }

    /** Kann dieser Gegenstand eine Boss-Modifikation tragen? Waffen, Werkzeuge mit Schaden und Rüstungsteile. */
    public static boolean modbar(ItemStack s) {
        Item i = s.getItem();
        return s.getCount() == 1 && (i instanceof ArmorItem || i instanceof de.deadzonemc.waffen.feuer.SchusswaffeItem
                || i instanceof net.minecraft.world.item.TieredItem || i instanceof net.minecraft.world.item.ProjectileWeaponItem
                || i instanceof net.minecraft.world.item.TridentItem);
    }

    /** Wie viele getragene Rüstungsteile diese Modifikation haben. */
    public static int ruestung(LivingEntity e, String art) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (art.equals(mod(e.getItemBySlot(slot)))) {
                n++;
            }
        }
        return n;
    }

    /** Faktor auf Strahlungsdosis (Strahlenherz in der Rüstung: −20 % je Teil). */
    public static float strahlung(Player p) {
        return Math.max(0.1F, 1F - 0.2F * ruestung(p, "strahlende"));
    }

    /** Faktor auf Nachladezeit (Kommandantenabzeichen in der Waffe: −25 %). */
    public static float nachladen(ItemStack waffe) {
        return "kommandant".equals(mod(waffe)) ? 0.75F : 1F;
    }

    // ---- Wirkungen ----------------------------------------------------------------------------

    private static void schaden(LivingIncomingDamageEvent event) {
        LivingEntity ziel = event.getEntity();
        // Angreifer mit modifizierter Waffe
        if (event.getSource().getEntity() instanceof Player p && p != ziel) {
            String m = mod(p.getMainHandItem());
            if (m != null) {
                float menge = event.getAmount();
                switch (m) {
                    case "schlaechter" -> p.heal(Math.min(4F, menge * 0.15F));
                    case "erfrorene" -> ziel.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    case "pilzvater" -> ziel.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    case "nebelmutter" -> {
                        ziel.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                        if (ziel instanceof Mob mob && p.getRandom().nextFloat() < 0.25F) {
                            mob.setTarget(null);
                        }
                    }
                    case "schlund" -> {
                        if (ziel.getBbHeight() > 2.4F || ziel instanceof de.deadzonemc.monster.boss.Boss) {
                            event.setAmount(menge * 1.25F);
                        }
                    }
                    case "brutkoenigin" -> ziel.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                    case "strahlende" -> event.setAmount(menge + 2F);
                    case "hirte" -> ziel.knockback(0.6, p.getX() - ziel.getX(), p.getZ() - ziel.getZ());
                    case "kommandant" -> event.setAmount(menge * 1.1F);
                    case "das_herz" -> ziel.igniteForSeconds(4);
                    case "der_ursprung" -> event.setAmount(menge * 1.25F);
                    case "schwarmmutter" -> {
                        if (!splitter) {
                            splitter = true;
                            try {
                                for (LivingEntity daneben : ziel.level().getEntitiesOfClass(LivingEntity.class,
                                        ziel.getBoundingBox().inflate(2.5), e -> e != ziel && e != p && e instanceof net.minecraft.world.entity.monster.Enemy)) {
                                    daneben.hurt(p.damageSources().playerAttack(p), menge * 0.3F);
                                }
                            } finally {
                                splitter = false;
                            }
                        }
                    }
                    case "pilot" -> {
                        if (p.getMainHandItem().getItem() instanceof de.deadzonemc.waffen.feuer.SchusswaffeItem) {
                            event.setAmount(menge * 1.15F);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        // Verteidiger mit modifizierter Rüstung
        if (ziel instanceof Player p) {
            int frost = ruestung(p, "erfrorene");
            if (frost > 0 && event.getSource().getEntity() instanceof LivingEntity a && a != p) {
                a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + 20 * frost, 0));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                int brut = ruestung(p, "brutkoenigin");
                int leere = ruestung(p, "der_ursprung");
                if (leere >= 2) {
                    event.setCanceled(true);
                    return;
                }
                event.setAmount(event.getAmount() * Math.max(0F, 1F - 0.2F * brut - 0.5F * leere));
            }
            if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
                int glut = ruestung(p, "das_herz");
                if (glut >= 2) {
                    event.setCanceled(true);
                } else if (glut == 1) {
                    event.setAmount(event.getAmount() * 0.5F);
                }
            }
        }
    }

    /** Schutz gegen Kettenreaktionen beim Schwarmbiss (Folgeschaden löst sonst wieder Folgeschaden aus). */
    private static boolean splitter;

    private static void tod(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player p && event.getEntity() != p) {
            if ("verschmolzener".equals(mod(p.getMainHandItem()))) {
                p.heal(4F);
            }
            if (ruestung(p, "schlaechter") > 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 + 40 * ruestung(p, "schlaechter"), 0));
            }
        }
    }

    private static void tick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) {
            return;
        }
        // Notabsprung: wer im freien Fall schleicht, schwebt am Fallschirm
        if (p.fallDistance > 4 && p.isShiftKeyDown() && !p.onGround() && ruestung(p, "pilot") > 0) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false, true));
            p.fallDistance = 0;
        }
        if (p.tickCount % 20 != 3) {
            return;
        }
        if (ruestung(p, "erfrorene") > 0) {
            p.setTicksFrozen(0);
        }
        if (ruestung(p, "pilzvater") > 0) {
            p.removeEffect(MobEffects.POISON);
        }
        if (ruestung(p, "schwarmmutter") > 0 && p.level().getMaxLocalRawBrightness(p.blockPosition()) < 6) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
        }
        if (ruestung(p, "nebelmutter") > 0 && p.isShiftKeyDown()
                && p.level().getMaxLocalRawBrightness(p.blockPosition()) < 8) {
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, true));
        }
    }

    /** Werte-Modifikationen auf Rüstungsteilen: Rückstoßfestigkeit, Tempo, Rüstung, Lebenspunkte. */
    private static void attribute(ItemAttributeModifierEvent event) {
        ItemStack s = event.getItemStack();
        String m = mod(s);
        if (m == null || !(s.getItem() instanceof ArmorItem a)) {
            return;
        }
        EquipmentSlotGroup gruppe = EquipmentSlotGroup.bySlot(a.getEquipmentSlot());
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "bossmod_" + a.getEquipmentSlot().getName());
        switch (m) {
            case "schlund" -> event.addModifier(Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(id, 0.1, AttributeModifier.Operation.ADD_VALUE), gruppe);
            case "hirte" -> event.addModifier(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(id, 0.03, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), gruppe);
            case "kommandant" -> event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(id, 1.0, AttributeModifier.Operation.ADD_VALUE), gruppe);
            case "verschmolzener" -> event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(id, 2.0, AttributeModifier.Operation.ADD_VALUE), gruppe);
            default -> {
            }
        }
    }

    // ---- Anzeige ------------------------------------------------------------------------------

    public static void tooltip(ItemStack s, List<Component> zeilen) {
        String m = mod(s);
        if (m == null) {
            return;
        }
        boolean ruestung = s.getItem() instanceof ArmorItem;
        zeilen.add(Component.translatable("bossmod.deadzonemc.kopf", Component.translatable("item.deadzonemc.trophaee_" + m))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        zeilen.add(Component.literal(" ").append(Component.translatable("bossmod.deadzonemc." + m + (ruestung ? ".ruestung" : ".waffe")))
                .withStyle(ChatFormatting.DARK_PURPLE));
    }
}
