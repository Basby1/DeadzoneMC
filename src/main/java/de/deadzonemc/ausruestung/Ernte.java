package de.deadzonemc.ausruestung;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import java.util.function.Supplier;

/** Anbaubare Pflanze (Heilkraut, Baumwolle) – wächst wie Weizen auf Ackerboden. */
public class Ernte extends CropBlock {

    private final Supplier<ItemLike> samen;

    public Ernte(Supplier<ItemLike> samen, Properties p) {
        super(p);
        this.samen = samen;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return samen.get();
    }
}
