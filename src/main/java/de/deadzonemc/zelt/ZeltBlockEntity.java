package de.deadzonemc.zelt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Inhalt eines Zelts (18 Plätze) – bleibt beim Abbauen im Zelt-Gegenstand erhalten. */
public class ZeltBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> inhalt = NonNullList.withSize(18, ItemStack.EMPTY);

    public ZeltBlockEntity(BlockPos pos, BlockState zustand) {
        super(Zelte.ZELT_BE.get(), pos, zustand);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.deadzonemc.zelt");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inhalt;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> liste) {
        inhalt = liste;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return new ChestMenu(MenuType.GENERIC_9x2, id, inv, this, 2);
    }

    @Override
    public int getContainerSize() {
        return 18;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack s) {
        return !(s.getItem() instanceof net.minecraft.world.item.BlockItem b && b.getBlock() instanceof ZeltBlock);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        inhalt = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, inhalt, p);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        ContainerHelper.saveAllItems(tag, inhalt, p);
    }
}
