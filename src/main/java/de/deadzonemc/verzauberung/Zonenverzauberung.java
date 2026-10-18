package de.deadzonemc.verzauberung;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ausruestung.Ausruestung;
import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Zonen-Verzauberungen: 26 eigene Verzauberungen (data/deadzonemc/enchantment), die es nur an der Zonenwerkbank
 * gibt. Was Vanilla-Komponenten können (Rückstoß, Schaden gegen Infizierte, Haltbarkeit, Attribute, Fallschutz),
 * steht in den JSON-Dateien; alles andere hier bzw. in den Waffen-Klassen.
 */
public final class Zonenverzauberung {

    private Zonenverzauberung() {
    }

    public static final TagKey<Enchantment> ZONENTISCH = TagKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "zonentisch"));

    /** Womit jede Verzauberung bezahlt wird (Material aus der Zone, Anzahl = neue Stufe). */
    public static final Map<String, String> MATERIAL = new LinkedHashMap<>();

    static {
        String[] paare = {"kopfjaeger", "strahlenkristall", "hohlspitz", "knochensplitter", "durchschlag", "panzerplatte",
                "lautlos", "gummi", "ruhige_hand", "sehne", "schnellladen", "klebeband", "trommel", "drohnenteile",
                "brandmunition", "glutkern", "wucht", "panzerplatte", "schaedelbrecher", "knochensplitter",
                "blutrausch", "verseuchtes_gewebe", "leise_hand", "mottenstaub", "robust", "naegel",
                "zaehigkeit", "mutantenhaut", "dornen_der_zone", "chitinplatte", "abwehrkraefte", "heilkraut",
                "strahlenschutz", "gummi", "schleicher", "mottenstaub", "fluchtinstinkt", "sehne",
                "leiser_lauf", "baumwolle", "polsterung", "sporenbeutel", "kletterer", "sehne",
                "dunkelsicht", "strahlenkristall", "vorahnung", "mottenstaub", "ruhepuls", "heilkraut",
                "glueckspilz", "mutagen"};
        for (int i = 0; i < paare.length; i += 2) {
            MATERIAL.put(paare[i], paare[i + 1]);
        }
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, DeadzoneMC.MODID);

    public static final DeferredBlock<ZonenwerkbankBlock> WERKBANK = BLOECKE.register("zonenwerkbank",
            () -> new ZonenwerkbankBlock(BlockBehaviour.Properties.of().strength(3.5F, 6.0F).sound(SoundType.METAL)
                    .lightLevel(s -> 3).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> WERKBANK_ITEM = ITEMS.register("zonenwerkbank",
            () -> new BlockItem(WERKBANK.get(), new Item.Properties()));
    public static final DeferredHolder<MenuType<?>, MenuType<ZonenwerkbankMenu>> MENU = MENUS.register("zonenwerkbank",
            () -> new MenuType<>(ZonenwerkbankMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::kill);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::schaden);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::nachSchaden);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::effekt);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::ziel);
        NeoForge.EVENT_BUS.addListener(Zonenverzauberung::tick);
    }

    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(WERKBANK_ITEM.get());
    }

    // ---- Nachschlagen ---------------------------------------------------------------------

    public static Holder<Enchantment> holder(RegistryAccess zugriff, String id) {
        return zugriff.registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, id)))
                .orElse(null);
    }

    /** Stufe einer Zonen-Verzauberung auf einem Gegenstand. */
    public static int stufe(RegistryAccess zugriff, ItemStack s, String id) {
        return stufe(s, id);
    }

    /** Stufe ohne Registerzugriff (über den Schlüssel der Verzauberung) – geht überall, auch im Tooltip. */
    public static int stufe(ItemStack s, String id) {
        if (s.isEmpty() || !s.isEnchanted()) {
            return 0;
        }
        for (var e : s.getEnchantments().entrySet()) {
            var k = e.getKey().unwrapKey();
            if (k.isPresent() && k.get().location().getNamespace().equals(DeadzoneMC.MODID)
                    && k.get().location().getPath().equals(id)) {
                return e.getIntValue();
            }
        }
        return 0;
    }

    /** Höchste Stufe an der Ausrüstung eines Wesens (Slots laut Verzauberung). */
    public static int stufe(LivingEntity wer, String id) {
        Holder<Enchantment> h = holder(wer.level().registryAccess(), id);
        return h == null ? 0 : EnchantmentHelper.getEnchantmentLevel(h, wer);
    }

    /** Summe über alle Rüstungsteile. */
    public static int summeRuestung(LivingEntity wer, String id) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            n += stufe(wer.level().registryAccess(), wer.getItemBySlot(slot), id);
        }
        return n;
    }

    /** Faktor für die Strahlung (Strahlenschutz): je Stufe -8 %, höchstens -80 %. */
    public static float strahlenFaktor(Player p) {
        return Math.max(0.2F, 1.0F - 0.08F * summeRuestung(p, "strahlenschutz"));
    }

    public static Item material(String id) {
        return Ausruestung.mat(MATERIAL.getOrDefault(id, "naegel"));
    }

    // ---- Wirkungen ------------------------------------------------------------------------

    /** Blutrausch: Kill mit der Nahkampfwaffe heilt. */
    private static void kill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p) {
            int s = stufe(p.level().registryAccess(), p.getMainHandItem(), "blutrausch");
            if (s > 0) {
                p.heal(2.0F * s);
            }
        }
    }

    private static void schaden(LivingIncomingDamageEvent event) {
        LivingEntity opfer = event.getEntity();
        // Leise Hand: geduckt von hinten
        if (event.getSource().getEntity() instanceof Player p && event.getSource().getDirectEntity() == p && p.isCrouching()) {
            int s = stufe(p.level().registryAccess(), p.getMainHandItem(), "leise_hand");
            if (s > 0) {
                Vec3 blick = opfer.getLookAngle().multiply(1, 0, 1);
                Vec3 zuMir = p.position().subtract(opfer.position()).multiply(1, 0, 1);
                if (blick.lengthSqr() > 0 && zuMir.lengthSqr() > 0 && blick.normalize().dot(zuMir.normalize()) < -0.2) {
                    event.setAmount(event.getAmount() * (1.0F + 0.4F * s));
                }
            }
        }
        // Dornen der Zone: Nahkampf-Angreifer nimmt Schaden
        if (opfer instanceof Player p && event.getSource().getDirectEntity() instanceof LivingEntity angreifer
                && angreifer == event.getSource().getEntity() && angreifer != p) {
            int s = stufe(p, "dornen_der_zone");
            if (s > 0) {
                angreifer.hurt(p.damageSources().thorns(p), 1.5F * s);
            }
        }
    }

    private static final Map<UUID, Long> FLUCHT = new HashMap<>();

    /** Fluchtinstinkt: unter 30 % Gesundheit ein Tempo-Schub. */
    private static void nachSchaden(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.getHealth() > p.getMaxHealth() * 0.3F || !p.isAlive()) {
            return;
        }
        int s = stufe(p, "fluchtinstinkt");
        long jetzt = p.serverLevel().getGameTime();
        if (s > 0 && jetzt - FLUCHT.getOrDefault(p.getUUID(), -10000L) > 1200) {
            FLUCHT.put(p.getUUID(), jetzt);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100 + 40 * s, s, false, false, true));
        }
    }

    /** Abwehrkräfte: Gift, Hunger, Verwesung prallen manchmal ab. */
    private static void effekt(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player p)) {
            return;
        }
        var e = event.getEffectInstance().getEffect();
        if (!e.equals(MobEffects.POISON) && !e.equals(MobEffects.HUNGER) && !e.equals(MobEffects.WITHER)) {
            return;
        }
        int s = stufe(p, "abwehrkraefte");
        if (s > 0 && p.getRandom().nextFloat() < 0.25F * s) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    /** Schleicher: geduckt wird man erst aus der Nähe bemerkt (außer man hat zugeschlagen). */
    private static void ziel(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player p) || !p.isCrouching()
                || !(event.getEntity() instanceof Mob m) || m.getLastHurtByMob() == p) {
            return;
        }
        int s = stufe(p, "schleicher");
        if (s > 0) {
            double reichweite = 14 - 3.5 * s; // 10,5 / 7 / 3,5 Blöcke
            if (m.distanceToSqr(p) > reichweite * reichweite) {
                event.setCanceled(true);
            }
        }
    }

    /** Kopfteile: Dunkelsicht, Vorahnung, Ruhepuls. */
    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7) {
            return;
        }
        ItemStack helm = p.getItemBySlot(EquipmentSlot.HEAD);
        if (helm.isEmpty() || !helm.isEnchanted()) {
            return;
        }
        RegistryAccess z = p.level().registryAccess();
        if (stufe(z, helm, "dunkelsicht") > 0 && p.isCrouching()
                && p.level().getBrightness(LightLayer.BLOCK, p.blockPosition()) < 5 && p.level().getMaxLocalRawBrightness(p.blockPosition()) < 6) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
        int vor = stufe(z, helm, "vorahnung");
        if (vor > 0) {
            double r = 8.0 * vor;
            for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> m.getTarget() == p)) {
                if (m instanceof DeadzoneMonster || m instanceof net.minecraft.world.entity.monster.Enemy) {
                    m.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
                }
            }
        }
        int puls = stufe(z, helm, "ruhepuls");
        if (puls > 0 && p.tickCount % 80 == 7 && p.getHealth() < p.getMaxHealth()
                && p.tickCount - p.getLastHurtByMobTimestamp() > 200 && p.getFoodData().getFoodLevel() > 6) {
            p.heal(0.5F * puls);
        }
    }
}
