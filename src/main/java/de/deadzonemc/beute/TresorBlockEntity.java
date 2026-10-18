package de.deadzonemc.beute;

import de.deadzonemc.DeadzoneMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Inhalt eines Tresors. Beim ersten Knacken wird die Beutetabelle gewürfelt. Wer einmal geplündert hat,
 * bekommt nichts nach (der Merker übersteht das Neuladen) – außer über den {@link Nachschub}.
 */
public class TresorBlockEntity extends RandomizableContainerBlockEntity {

    public static final ResourceKey<LootTable> TABELLE = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "kiste/tresor"));

    private NonNullList<ItemStack> inhalt = NonNullList.withSize(27, ItemStack.EMPTY);
    private boolean geknackt;
    private boolean gepluendert;

    public TresorBlockEntity(BlockPos pos, BlockState state) {
        super(Beute.TRESOR_BE.get(), pos, state);
        setLootTable(TABELLE, 0L);
    }

    public boolean istGeknackt() {
        return geknackt;
    }

    public void knacken() {
        if (geknackt || level == null) {
            return;
        }
        geknackt = true;
        level.setBlock(worldPosition, getBlockState().setValue(TresorBlock.OFFEN, true), 3);
        level.playSound(null, worldPosition, Beute.TRESOR_AUF.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        setChanged();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return inhalt;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> neu) {
        inhalt = neu;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.deadzonemc.tresor");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inv) {
        return ChestMenu.threeRows(id, inv, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public void unpackLootTable(net.minecraft.world.entity.player.Player spieler) {
        if (getLootTable() != null) {
            gepluendert = true;
        }
        super.unpackLootTable(spieler);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.loadAdditional(tag, p);
        inhalt = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        geknackt = tag.getBoolean("geknackt");
        gepluendert = tag.getBoolean("gepluendert");
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, inhalt, p);
            if (gepluendert) {
                setLootTable(null);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider p) {
        super.saveAdditional(tag, p);
        tag.putBoolean("geknackt", geknackt);
        tag.putBoolean("gepluendert", gepluendert);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, inhalt, p);
        }
    }

    /** Für den Nachschub: Tresor schließt sich wieder und bekommt neue Beute. */
    public void auffuellen(long saat) {
        geknackt = false;
        gepluendert = false;
        setLootTable(TABELLE, saat);
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(TresorBlock.OFFEN, false), 3);
        }
        setChanged();
    }
}
