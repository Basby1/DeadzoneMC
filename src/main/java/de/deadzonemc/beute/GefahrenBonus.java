package de.deadzonemc.beute;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.welt.Staedte;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Je gefährlicher der Ort, desto besser die Beute. Die Gefahrenstufe eines Behälters ergibt sich aus der Lage:
 * Umland (0) – Vorort (1) – Stadt (2) – Stadtkern (3); verseuchte Gebiete und tiefe Keller zählen eine Stufe
 * mehr. Pro Stufe gibt es zusätzliche Würfe aus den Bonustabellen, in denen auch die legendären Stücke liegen.
 * Außerdem werden besondere Funde markiert, damit der Spieler beim Öffnen eine Fundmeldung bekommt.
 */
public class GefahrenBonus extends LootModifier {

    public static final MapCodec<GefahrenBonus> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).apply(i, GefahrenBonus::new));

    private static final TagKey<Biome> VERSEUCHT = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "verseucht"));

    public GefahrenBonus(LootItemCondition[] bedingungen) {
        super(bedingungen);
    }

    private static ResourceKey<LootTable> tabelle(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "bonus/" + name));
    }

    /** Gefahrenstufe 0..4 eines Ortes. */
    public static int stufe(ServerLevel level, BlockPos pos) {
        int stufe = 0;
        float f = Staedte.stadtFaktor(level, pos.getX() >> 4, pos.getZ() >> 4);
        double s = Staedte.schwelle();
        if (f > s) {
            stufe = f < s * 2.2 ? 1 : f < s * 5 ? 2 : 3;
        }
        if (level.getBiome(pos).is(VERSEUCHT)) {
            stufe++;
        }
        if (pos.getY() < level.getSeaLevel() - 12) {
            stufe++;
        }
        return Math.min(4, stufe);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> beute, LootContext ctx) {
        ResourceLocation id = ctx.getQueriedLootTableId();
        if (!DeadzoneMC.MODID.equals(id.getNamespace()) || !id.getPath().startsWith("kiste/")) {
            return beute;
        }
        Vec3 ort = ctx.getParamOrNull(LootContextParams.ORIGIN);
        if (ort != null) {
            int st = stufe(ctx.getLevel(), BlockPos.containing(ort));
            var r = ctx.getRandom();
            switch (st) {
                case 0 -> {
                    if (r.nextFloat() < 0.15F) {
                        wuerfeln("gering", ctx, beute);
                    }
                }
                case 1 -> {
                    wuerfeln("gering", ctx, beute);
                    if (r.nextFloat() < 0.35F) {
                        wuerfeln("mittel", ctx, beute);
                    }
                }
                case 2 -> {
                    wuerfeln("mittel", ctx, beute);
                    if (r.nextFloat() < 0.5F) {
                        wuerfeln("gering", ctx, beute);
                    }
                }
                case 3 -> {
                    wuerfeln("hoch", ctx, beute);
                    wuerfeln("mittel", ctx, beute);
                }
                default -> {
                    wuerfeln("hoch", ctx, beute);
                    wuerfeln("hoch", ctx, beute);
                    wuerfeln("mittel", ctx, beute);
                }
            }
        }
        for (ItemStack s : beute) {
            if (besonders(s)) {
                s.set(Beute.FUND.get(), true);
            }
        }
        return beute;
    }

    private static void wuerfeln(String name, LootContext ctx, ObjectArrayList<ItemStack> beute) {
        ctx.getResolver().get(Registries.LOOT_TABLE, tabelle(name))
                .ifPresent(t -> t.value().getRandomItemsRaw(ctx, beute::add));
    }

    public static boolean besonders(ItemStack s) {
        return !s.isEmpty() && (s.getRarity().ordinal() >= Rarity.RARE.ordinal()
                || s.is(net.minecraft.tags.TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "fund"))));
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
