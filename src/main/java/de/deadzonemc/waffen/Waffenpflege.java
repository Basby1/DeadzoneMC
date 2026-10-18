package de.deadzonemc.waffen;

import com.mojang.serialization.Codec;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Waffenpflege: Schusswaffen (TaCZ) verschleißen mit jedem Schuss. Ab etwa 40 % Verschleiß drohen Ladehemmungen –
 * die Waffe schießt dann nicht mehr, bis man sie durchlädt (Schleichen + Hand tauschen, Taste F). Ein Pflegeset
 * macht die Waffe wieder wie neu.
 */
public final class Waffenpflege {

    private static final Logger LOG = com.mojang.logging.LogUtils.getLogger();

    private Waffenpflege() {
    }

    public static final DeferredRegister<DataComponentType<?>> KOMPONENTEN =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);

    /** Verschleiß in Promille (0 = neu, 1000 = Schrott). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VERSCHLEISS =
            KOMPONENTEN.register("verschleiss", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());
    /** Ob die Waffe gerade eine Ladehemmung hat. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LADEHEMMUNG =
            KOMPONENTEN.register("ladehemmung", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());

    public static final DeferredItem<PflegesetItem> PFLEGESET = ITEMS.register("pflegeset",
            () -> new PflegesetItem(new Item.Properties().durability(6)));

    private static Method schuetze;
    private static Method waffe;
    private static Method seite;

    public static void registrieren(IEventBus modBus) {
        KOMPONENTEN.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Waffenpflege::durchladen);
        if (ModList.get().isLoaded("tacz")) {
            taczAnmelden();
        }
    }

    @SuppressWarnings("unchecked")
    private static void taczAnmelden() {
        try {
            Class<? extends Event> abzug = (Class<? extends Event>) Class.forName("com.tacz.guns.api.event.common.GunShootEvent");
            Class<? extends Event> schuss = (Class<? extends Event>) Class.forName("com.tacz.guns.api.event.common.GunFireEvent");
            schuetze = schuss.getMethod("getShooter");
            waffe = schuss.getMethod("getGunItemStack");
            seite = schuss.getMethod("getLogicalSide");
            Method abzugWaffe = abzug.getMethod("getGunItemStack");
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, abzug, e -> {
                try {
                    ItemStack s = (ItemStack) abzugWaffe.invoke(e);
                    if (s != null && Boolean.TRUE.equals(s.get(LADEHEMMUNG.get()))) {
                        ((ICancellableEvent) e).setCanceled(true);
                    }
                } catch (ReflectiveOperationException ex) {
                    LOG.debug("TaCZ-Abzug nicht lesbar", ex);
                }
            });
            NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, schuss, Waffenpflege::abgefeuert);
            LOG.info("[Deadzone] Waffenpflege für TaCZ aktiv");
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("[Deadzone] TaCZ-Ereignisse nicht gefunden, Waffenpflege aus: {}", e.toString());
        }
    }

    private static void abgefeuert(Event e) {
        try {
            if (((ICancellableEvent) e).isCanceled() || !"SERVER".equals(String.valueOf(seite.invoke(e)))) {
                return;
            }
            ItemStack s = (ItemStack) waffe.invoke(e);
            LivingEntity wer = (LivingEntity) schuetze.invoke(e);
            if (s == null || s.isEmpty()) {
                return;
            }
            if (Boolean.TRUE.equals(s.get(LADEHEMMUNG.get()))) {
                ((ICancellableEvent) e).setCanceled(true);
                return;
            }
            int v = Math.min(1000, s.getOrDefault(VERSCHLEISS.get(), 0) + 3);
            s.set(VERSCHLEISS.get(), v);
            double chance = v < 400 ? 0 : (v - 400) / 600.0 * 0.07;
            if (wer.level().isRaining() && wer.level().canSeeSky(wer.blockPosition())) {
                chance += 0.01;
            }
            if (chance > 0 && wer.getRandom().nextDouble() < chance) {
                s.set(LADEHEMMUNG.get(), true);
                wer.level().playSound(null, wer.getX(), wer.getY(), wer.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.6F);
                if (wer instanceof ServerPlayer p) {
                    p.displayClientMessage(Component.translatable("waffe.deadzonemc.ladehemmung").withStyle(ChatFormatting.RED), true);
                }
            }
        } catch (ReflectiveOperationException ex) {
            LOG.debug("TaCZ-Schuss nicht lesbar", ex);
        }
    }

    public static boolean istSchusswaffe(ItemStack s) {
        return !s.isEmpty() && "tacz:modern_kinetic_gun".equals(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
    }

    /** Schleichen + Hand tauschen (F) mit einer Schusswaffe: Ladehemmung beheben. */
    private static void durchladen(LivingSwapItemsEvent.Hands event) {
        if (!(event.getEntity() instanceof Player p) || !p.isShiftKeyDown()) {
            return;
        }
        ItemStack s = p.getMainHandItem();
        if (!istSchusswaffe(s) || !Boolean.TRUE.equals(s.get(LADEHEMMUNG.get()))) {
            return;
        }
        event.setCanceled(true);
        if (!p.level().isClientSide) {
            s.remove(LADEHEMMUNG.get());
            p.getCooldowns().addCooldown(s.getItem(), 25);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS, 1.0F, 1.3F);
            p.displayClientMessage(Component.translatable("waffe.deadzonemc.durchgeladen").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Zustand als Text für den Tooltip. */
    public static Component zustand(ItemStack s) {
        int v = s.getOrDefault(VERSCHLEISS.get(), 0);
        int prozent = 100 - v / 10;
        ChatFormatting farbe = prozent > 70 ? ChatFormatting.GREEN : prozent > 45 ? ChatFormatting.YELLOW : ChatFormatting.RED;
        String stufe = prozent > 70 ? "gut" : prozent > 45 ? "abgenutzt" : prozent > 20 ? "verschlissen" : "kaputt";
        return Component.translatable("waffe.deadzonemc.zustand", prozent, Component.translatable("waffe.deadzonemc." + stufe))
                .withStyle(farbe);
    }
}
