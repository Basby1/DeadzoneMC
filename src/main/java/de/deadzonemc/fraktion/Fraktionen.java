package de.deadzonemc.fraktion;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Fraktion;
import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Die sechs neuen Fraktionen: je eine Questreihe aus drei Aufträgen, Ansehen und ein dauerhafter Vorteil.
 * <ul>
 *     <li>Geier (Banditen): erst feindlich, bestechlich (10 Militärpatronen) – Vorteil: Schwarzmarkt-Paket alle 3 Tage</li>
 *     <li>Totenflüsterer: Infizierte lassen dich in Ruhe, solange du sie nicht angreifst</li>
 *     <li>Kinder des Waldes: im Wald heilst du von selbst</li>
 *     <li>Sandwanderer: in der Wüste schneller, alle 2 Tage Wasser</li>
 *     <li>Tiefenvolk: unter Wasser atmen und schnell schwimmen</li>
 *     <li>Hüter des Himmels: Gleitschirm (kein Fallschaden aus großer Höhe) und Elytren</li>
 * </ul>
 */
public final class Fraktionen {

    private Fraktionen() {
    }

    public enum Art { BRINGEN, JAGEN }

    /** Ein Auftrag einer Fraktion: Texte fquest.deadzonemc.<fraktion>.<nr>.titel/.text */
    public record Auftrag(Art art, String ziel, int anzahl, int patronen) {
    }

    public static final Map<Fraktion, List<Auftrag>> REIHEN = new HashMap<>();

    static {
        REIHEN.put(Fraktion.BANDITEN, List.of(
                new Auftrag(Art.BRINGEN, "#deadzonemc:schusswaffen", 1, 10),
                new Auftrag(Art.JAGEN, "deadzonemc:soldat", 3, 20),
                new Auftrag(Art.BRINGEN, "deadzonemc:elektronik", 6, 25)));
        REIHEN.put(Fraktion.TOTENFLUESTERER, List.of(
                new Auftrag(Art.BRINGEN, "deadzonemc:verseuchtes_gewebe", 8, 10),
                new Auftrag(Art.JAGEN, "deadzonemc:schreier", 3, 18),
                new Auftrag(Art.BRINGEN, "deadzonemc:mutagen", 2, 30)));
        REIHEN.put(Fraktion.WALDKINDER, List.of(
                new Auftrag(Art.BRINGEN, "deadzonemc:heilkraut", 10, 10),
                new Auftrag(Art.JAGEN, "deadzonemc:seuchenhund", 4, 16),
                new Auftrag(Art.JAGEN, "deadzonemc:brut", 3, 24)));
        REIHEN.put(Fraktion.SANDWANDERER, List.of(
                new Auftrag(Art.BRINGEN, "minecraft:glass_bottle", 8, 8),
                new Auftrag(Art.JAGEN, "deadzonemc:glutleib", 4, 18),
                new Auftrag(Art.BRINGEN, "deadzonemc:schrott", 24, 22)));
        REIHEN.put(Fraktion.TIEFENVOLK, List.of(
                new Auftrag(Art.BRINGEN, "minecraft:kelp", 32, 8),
                new Auftrag(Art.JAGEN, "deadzonemc:ertrunkener", 5, 18),
                new Auftrag(Art.BRINGEN, "minecraft:prismarine_shard", 8, 24)));
        REIHEN.put(Fraktion.HIMMELSHUETER, List.of(
                new Auftrag(Art.BRINGEN, "minecraft:feather", 16, 10),
                new Auftrag(Art.JAGEN, "deadzonemc:spaeherdrohne", 3, 22),
                new Auftrag(Art.BRINGEN, "deadzonemc:strahlenkristall", 6, 30)));
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Fraktionen::kill);
        NeoForge.EVENT_BUS.addListener(Fraktionen::ziel);
        NeoForge.EVENT_BUS.addListener(Fraktionen::tick);
    }

    // ---- Stand -------------------------------------------------------------------------------

    private static int wert(Player p, String k) {
        return p.getData(ModRegistry.FRAKTIONSQUESTS).getOrDefault(k, 0);
    }

    private static void setzen(Player p, String k, int v) {
        Map<String, Integer> neu = new HashMap<>(p.getData(ModRegistry.FRAKTIONSQUESTS));
        neu.put(k, v);
        p.setData(ModRegistry.FRAKTIONSQUESTS, neu);
    }

    public static int schritt(Player p, Fraktion f) {
        return wert(p, f.id());
    }

    /** Vorteil freigeschaltet: ganze Questreihe erledigt und Ansehen nicht verspielt. */
    public static boolean vorteil(Player p, Fraktion f) {
        return schritt(p, f) >= 3 && Ansehen.holen(p, f) >= 40;
    }

    public static boolean feindlich(Player p, Fraktion f) {
        return f == Fraktion.BANDITEN ? Ansehen.holen(p, f) < 15 : Ansehen.krieg(p, f);
    }

    // ---- Gespräch ----------------------------------------------------------------------------

    private static Component name(Fraktion f) {
        return Component.translatable("fraktion.deadzonemc." + f.id()).withStyle(ChatFormatting.GOLD);
    }

    private static void sagen(ServerPlayer p, Fraktion f, Component text) {
        p.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY).append(name(f))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY)).append(text.copy().withStyle(ChatFormatting.GRAY)));
    }

    /** Ein Spieler spricht ein Fraktionsmitglied an. */
    public static void sprechen(ServerPlayer p, Fraktion f) {
        List<Auftrag> reihe = REIHEN.get(f);
        if (reihe == null) {
            return;
        }
        if (f == Fraktion.BANDITEN && feindlich(p, f)) {
            ItemStack hand = p.getMainHandItem();
            if (hand.is(ModRegistry.MILITAERPATRONE.get()) && hand.getCount() >= 10) {
                hand.shrink(10);
                Ansehen.aendern(p, f, 25);
                sagen(p, f, Component.translatable("fquest.deadzonemc.banditen.bestochen"));
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.6F);
            } else {
                sagen(p, f, Component.translatable("fquest.deadzonemc.banditen.drohung"));
            }
            return;
        }
        if (Ansehen.krieg(p, f)) {
            sagen(p, f, Component.translatable("fquest.deadzonemc.krieg"));
            return;
        }
        int s = schritt(p, f);
        String basis = "fquest.deadzonemc." + f.id() + ".";
        if (s >= reihe.size()) {
            gabe(p, f);
            sagen(p, f, Component.translatable(basis + "fertig"));
            return;
        }
        Auftrag a = reihe.get(s);
        boolean erfuellt;
        if (a.art() == Art.BRINGEN) {
            erfuellt = zaehlen(p, a.ziel()) >= a.anzahl();
            if (erfuellt) {
                nehmen(p, a.ziel(), a.anzahl());
            }
        } else {
            erfuellt = wert(p, f.id() + "_n") >= a.anzahl();
        }
        if (!erfuellt) {
            int stand = a.art() == Art.JAGEN ? wert(p, f.id() + "_n") : zaehlen(p, a.ziel());
            sagen(p, f, Component.translatable(basis + (s + 1) + ".text"));
            p.sendSystemMessage(Component.literal("   ➤ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(basis + (s + 1) + ".titel").withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (" + Math.min(stand, a.anzahl()) + "/" + a.anzahl() + ")").withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        // Belohnung
        setzen(p, f.id(), s + 1);
        setzen(p, f.id() + "_n", 0);
        ItemStack lohn = new ItemStack(ModRegistry.MILITAERPATRONE.get(), a.patronen());
        if (!p.getInventory().add(lohn)) {
            p.drop(lohn, false);
        }
        Ansehen.aendern(p, f, s + 1 == reihe.size() ? 40 : 20);
        if (f == Fraktion.TOTENFLUESTERER) {
            Ansehen.aendern(p, Fraktion.WACHT, -10); // die Wacht hasst sie
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        sagen(p, f, Component.translatable(basis + (s + 1) + ".dank"));
        if (s + 1 == reihe.size()) {
            p.sendSystemMessage(Component.translatable("fquest.deadzonemc.vorteil",
                    Component.translatable(basis + "vorteil")).withStyle(ChatFormatting.GREEN));
            if (f == Fraktion.HIMMELSHUETER) {
                ItemStack fluegel = new ItemStack(Items.ELYTRA);
                if (!p.getInventory().add(fluegel)) {
                    p.drop(fluegel, false);
                }
            }
        }
    }

    /** Wiederkehrende Gaben (Geier: Schwarzmarkt, Sandwanderer: Wasser). */
    private static void gabe(ServerPlayer p, Fraktion f) {
        long tag = p.serverLevel().getDayTime() / 24000L;
        int abstand = f == Fraktion.BANDITEN ? 3 : f == Fraktion.SANDWANDERER ? 2 : 0;
        if (abstand == 0 || !vorteil(p, f) || tag - wert(p, f.id() + "_gabe") < abstand) {
            return;
        }
        setzen(p, f.id() + "_gabe", (int) tag);
        if (f == Fraktion.BANDITEN) {
            ServerLevel sl = p.serverLevel();
            LootTable t = sl.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kiste/waffenkammer")));
            LootParams lp = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, p.position())
                    .withParameter(LootContextParams.THIS_ENTITY, p).create(LootContextParamSets.CHEST);
            for (ItemStack st : t.getRandomItems(lp)) {
                if (!p.getInventory().add(st)) {
                    p.drop(st, false);
                }
            }
        } else {
            for (int i = 0; i < 4; i++) {
                ItemStack w = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,
                        net.minecraft.world.item.alchemy.Potions.WATER);
                if (!p.getInventory().add(w)) {
                    p.drop(w, false);
                }
            }
        }
        p.sendSystemMessage(Component.translatable("fquest.deadzonemc." + f.id() + ".gabe").withStyle(ChatFormatting.GREEN));
    }

    private static boolean passt(ItemStack s, String ziel) {
        if (ziel.startsWith("#")) {
            return s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(ziel.substring(1))));
        }
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ziel));
        return s.is(i);
    }

    private static int zaehlen(Player p, String ziel) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && passt(s, ziel)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void nehmen(Player p, String ziel, int menge) {
        for (int i = 0; i < p.getInventory().getContainerSize() && menge > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && passt(s, ziel)) {
                int x = Math.min(menge, s.getCount());
                s.shrink(x);
                menge -= x;
            }
        }
    }

    // ---- Zählen und Vorteile -----------------------------------------------------------------

    private static void kill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) {
            return;
        }
        String typ = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).toString();
        for (var e : REIHEN.entrySet()) {
            int s = schritt(p, e.getKey());
            if (s < e.getValue().size()) {
                Auftrag a = e.getValue().get(s);
                if (a.art() == Art.JAGEN && a.ziel().equals(typ)) {
                    int n = wert(p, e.getKey().id() + "_n") + 1;
                    setzen(p, e.getKey().id() + "_n", n);
                    if (n == a.anzahl()) {
                        p.displayClientMessage(Component.translatable("fquest.deadzonemc.erledigt", name(e.getKey()))
                                .withStyle(ChatFormatting.GREEN), true);
                    }
                }
            }
        }
    }

    /** Totenflüsterer-Vorteil: Infizierte greifen dich nicht von sich aus an. */
    private static void ziel(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof Player p && event.getEntity() instanceof DeadzoneMonster m
                && !(m instanceof de.deadzonemc.monster.boss.Boss) && !(m instanceof de.deadzonemc.monster.Mimik)
                && m.getLastHurtByMob() != p && vorteil(p, Fraktion.TOTENFLUESTERER)) {
            event.setCanceled(true);
        }
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 11) {
            return;
        }
        var biom = p.level().getBiome(p.blockPosition());
        if (vorteil(p, Fraktion.WALDKINDER) && biom.is(BiomeTags.IS_FOREST) && p.getHealth() < p.getMaxHealth()) {
            p.heal(1.0F);
        }
        if (vorteil(p, Fraktion.SANDWANDERER) && (biom.is(BiomeTags.HAS_DESERT_PYRAMID) || biom.is(BiomeTags.IS_BADLANDS))) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        }
        if (vorteil(p, Fraktion.TIEFENVOLK) && p.isInWater()) {
            p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 80, 0, true, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 80, 0, true, false, true));
        }
        if (vorteil(p, Fraktion.HIMMELSHUETER) && p.fallDistance > 6 && !p.isFallFlying()) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, true, false, true));
        }
    }

    @SuppressWarnings("unused")
    private static boolean istMob(Object o) {
        return o instanceof Mob;
    }
}
