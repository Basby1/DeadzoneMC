package de.deadzonemc.verzauberung;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Zonenwerkbank: links oben der Gegenstand, darunter das Material. Rechts stehen alle Zonen-Verzauberungen, die
 * zum Gegenstand passen; ein Klick hebt die Stufe um eins (Kosten: Erfahrungsstufen + Material).
 */
public class ZonenwerkbankMenu extends AbstractContainerMenu {

    public static final int SLOT_GEGENSTAND = 0;
    public static final int SLOT_MATERIAL = 1;

    private final Container feld = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
        }
    };
    private final ContainerLevelAccess zugang;
    private final Player spieler;

    public ZonenwerkbankMenu(int id, Inventory inv) {
        this(id, inv, ContainerLevelAccess.NULL);
    }

    public ZonenwerkbankMenu(int id, Inventory inv, ContainerLevelAccess zugang) {
        super(Zonenverzauberung.MENU.get(), id);
        this.zugang = zugang;
        this.spieler = inv.player;
        addSlot(new Slot(feld, SLOT_GEGENSTAND, 17, 22) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(feld, SLOT_MATERIAL, 17, 58));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, c + r * 9 + 9, 35 + c * 18, 118 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 35 + c * 18, 176));
        }
    }

    public ItemStack gegenstand() {
        return feld.getItem(SLOT_GEGENSTAND);
    }

    public ItemStack material() {
        return feld.getItem(SLOT_MATERIAL);
    }

    /** Alle Zonen-Verzauberungen, die auf den Gegenstand passen – auf Client und Server in derselben Reihenfolge. */
    public static List<Holder<Enchantment>> passende(RegistryAccess zugriff, ItemStack s) {
        List<Holder<Enchantment>> l = new ArrayList<>();
        if (s.isEmpty()) {
            return l;
        }
        for (Holder<Enchantment> h : zugriff.registryOrThrow(Registries.ENCHANTMENT).getTagOrEmpty(Zonenverzauberung.ZONENTISCH)) {
            if (h.value().canEnchant(s)) {
                l.add(h);
            }
        }
        l.sort(Comparator.comparing(h -> h.unwrapKey().map(k -> k.location().getPath()).orElse("")));
        return l;
    }

    public static String id(Holder<Enchantment> h) {
        return h.unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    public static int kostenStufen(int neueStufe) {
        return 2 + neueStufe * 3;
    }

    /** Grund, warum es nicht geht (null = geht). */
    public static Component hindernis(Player p, ItemStack s, ItemStack mat, Holder<Enchantment> h) {
        int jetzt = EnchantmentHelper.getItemEnchantmentLevel(h, s);
        if (jetzt >= h.value().getMaxLevel()) {
            return Component.translatable("verzauberung.deadzonemc.voll");
        }
        ItemEnchantments vorhanden = EnchantmentHelper.getEnchantmentsForCrafting(s);
        for (Holder<Enchantment> anderer : vorhanden.keySet()) {
            if (!anderer.equals(h) && !Enchantment.areCompatible(anderer, h)) {
                return Component.translatable("verzauberung.deadzonemc.unvertraeglich");
            }
        }
        int neu = jetzt + 1;
        Item noetig = Zonenverzauberung.material(id(h));
        boolean genugMaterial = mat.is(noetig) && mat.getCount() >= neu;
        boolean genugXp = p.getAbilities().instabuild || p.experienceLevel >= kostenStufen(neu);
        if (!genugMaterial || !genugXp) {
            return Component.translatable("verzauberung.deadzonemc.kosten", kostenStufen(neu), neu, noetig.getDescription());
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player p, int knopf) {
        ItemStack s = gegenstand();
        List<Holder<Enchantment>> liste = passende(p.level().registryAccess(), s);
        if (knopf < 0 || knopf >= liste.size()) {
            return false;
        }
        Holder<Enchantment> h = liste.get(knopf);
        if (hindernis(p, s, material(), h) != null) {
            return false;
        }
        int neu = EnchantmentHelper.getItemEnchantmentLevel(h, s) + 1;
        if (!p.level().isClientSide) {
            if (!p.getAbilities().instabuild) {
                p.giveExperienceLevels(-kostenStufen(neu));
                material().shrink(neu);
            }
            s.enchant(h, neu);
            feld.setChanged();
            zugang.execute((level, pos) -> level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F,
                    0.8F + level.random.nextFloat() * 0.3F));
            p.displayClientMessage(Component.translatable("verzauberung.deadzonemc.fertig", s.getHoverName()), true);
            broadcastChanges();
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack s = slot.getItem();
        ItemStack kopie = s.copy();
        if (index < 2) {
            if (!moveItemStackTo(s, 2, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean istMaterial = Zonenverzauberung.MATERIAL.values().stream()
                    .anyMatch(m -> s.is(de.deadzonemc.ausruestung.Ausruestung.mat(m)));
            if (istMaterial && !moveItemStackTo(s, SLOT_MATERIAL, SLOT_MATERIAL + 1, false)) {
                return ItemStack.EMPTY;
            }
            if (!istMaterial) {
                if (slots.get(SLOT_GEGENSTAND).hasItem()) {
                    return ItemStack.EMPTY;
                }
                ItemStack eins = s.split(1);
                slots.get(SLOT_GEGENSTAND).setByPlayer(eins);
            }
        }
        if (s.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return kopie;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        zugang.execute((level, pos) -> clearContainer(p, feld));
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(zugang, p, Zonenverzauberung.WERKBANK.get());
    }
}
