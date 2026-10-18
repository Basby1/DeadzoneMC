package de.deadzonemc.zone;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.deadzonemc.DeadzoneMC;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/** Die Zone: Anomalien, Artefakte, Emission und Versorgungsabwürfe. */
public final class Zone {

    private Zone() {
    }

    public static final DeferredRegister.Blocks BLOECKE = DeferredRegister.createBlocks(DeadzoneMC.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DeadzoneMC.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BE_TYPEN =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<EntityType<?>> TYPEN = DeferredRegister.create(Registries.ENTITY_TYPE, DeadzoneMC.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, DeadzoneMC.MODID);

    public static final DeferredBlock<AnomalieBlock> ANOMALIE = BLOECKE.register("anomalie",
            () -> new AnomalieBlock(BlockBehaviour.Properties.of()
                    .strength(-1.0F, 3_600_000.0F)
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.WOOL)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnomalieBlockEntity>> ANOMALIE_BE =
            BE_TYPEN.register("anomalie", () -> BlockEntityType.Builder.of(AnomalieBlockEntity::new, ANOMALIE.get()).build(null));

    public static final Map<ArtefaktItem.Art, DeferredItem<ArtefaktItem>> ARTEFAKTE = new EnumMap<>(ArtefaktItem.Art.class);

    static {
        for (ArtefaktItem.Art a : ArtefaktItem.Art.values()) {
            ARTEFAKTE.put(a, ITEMS.register("artefakt_" + a.id(), () -> new ArtefaktItem(a, new Item.Properties())));
        }
    }

    public static final DeferredItem<BolzenItem> BOLZEN = ITEMS.register("bolzen", () -> new BolzenItem(new Item.Properties().stacksTo(32)));
    public static final DeferredItem<AnomalieDetektorItem> DETEKTOR = ITEMS.register("anomaliedetektor",
            () -> new AnomalieDetektorItem(new Item.Properties()));

    public static final DeferredHolder<EntityType<?>, EntityType<Bolzen>> BOLZEN_TYP = TYPEN.register("bolzen",
            () -> EntityType.Builder.<Bolzen>of(Bolzen::new, MobCategory.MISC).sized(0.25F, 0.25F)
                    .clientTrackingRange(6).updateInterval(10).build("bolzen"));
    public static final DeferredHolder<EntityType<?>, EntityType<Abwurfkiste>> ABWURFKISTE = TYPEN.register("abwurfkiste",
            () -> EntityType.Builder.<Abwurfkiste>of(Abwurfkiste::new, MobCategory.MISC).sized(1.0F, 1.0F)
                    .clientTrackingRange(16).updateInterval(2).build("abwurfkiste"));

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ANOMALIE_FEATURE =
            FEATURES.register("anomalie", AnomalieFeature::new);

    public static Item artefakt(ArtefaktItem.Art a) {
        return ARTEFAKTE.get(a).get();
    }

    public static void registrieren(IEventBus modBus) {
        BLOECKE.register(modBus);
        ITEMS.register(modBus);
        BE_TYPEN.register(modBus);
        TYPEN.register(modBus);
        FEATURES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Emission::tick);
        NeoForge.EVENT_BUS.addListener(Emission::spielerBetritt);
        NeoForge.EVENT_BUS.addListener(Luftabwurf::tick);
        NeoForge.EVENT_BUS.addListener(Artefakte::tick);
        NeoForge.EVENT_BUS.addListener(Artefakte::fallen);
        NeoForge.EVENT_BUS.addListener(Zone::befehle);
    }

    /** Für den Kreativ-Tab. */
    public static void tabFuellen(net.minecraft.world.item.CreativeModeTab.Output ausgabe) {
        ausgabe.accept(DETEKTOR.get());
        ausgabe.accept(BOLZEN.get());
        ARTEFAKTE.values().forEach(a -> ausgabe.accept(a.get()));
    }

    // ---- Befehle (nur für Operatoren) ------------------------------------------------------

    private static void befehle(RegisterCommandsEvent event) {
        String[] arten = Arrays.stream(AnomalieArt.values()).map(AnomalieArt::id).toArray(String[]::new);
        event.getDispatcher().register(Commands.literal("deadzone")
                .requires(q -> q.hasPermission(2))
                .then(Commands.literal("emission")
                        .executes(ctx -> {
                            ServerLevel welt = ctx.getSource().getServer().overworld();
                            Emission e = Emission.holen(welt);
                            long min = Math.max(0, (e.naechste() - welt.getGameTime()) / 1200);
                            ctx.getSource().sendSuccess(() -> Component.literal("Emission: Stufe " + e.stufe()
                                    + ", Nummer " + Emission.nummer(welt) + ", nächste in ca. " + min + " min"), false);
                            return 1;
                        })
                        .then(Commands.literal("start").executes(ctx -> {
                            ServerLevel welt = ctx.getSource().getServer().overworld();
                            Emission.holen(welt).starten(welt);
                            ctx.getSource().sendSuccess(() -> Component.literal("Emission angekündigt."), true);
                            return 1;
                        })))
                .then(Commands.literal("abwurf").executes(ctx -> {
                    ServerLevel welt = ctx.getSource().getServer().overworld();
                    BlockPos hier = BlockPos.containing(ctx.getSource().getPosition());
                    Luftabwurf.abwerfen(welt, welt.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types
                            .MOTION_BLOCKING_NO_LEAVES, hier.offset(8, 0, 8)));
                    ctx.getSource().sendSuccess(() -> Component.literal("Versorgungskiste abgeworfen."), true);
                    return 1;
                }))
                .then(Commands.literal("anomalie")
                        .then(Commands.argument("art", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(arten, b))
                                .executes(ctx -> {
                                    AnomalieArt art = null;
                                    for (AnomalieArt a : AnomalieArt.values()) {
                                        if (a.id().equals(StringArgumentType.getString(ctx, "art"))) {
                                            art = a;
                                        }
                                    }
                                    if (art == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unbekannte Anomalie"));
                                        return 0;
                                    }
                                    BlockPos pos = BlockPos.containing(ctx.getSource().getPosition()).offset(0, 0, 4);
                                    ctx.getSource().getLevel().setBlock(pos, ANOMALIE.get().defaultBlockState()
                                            .setValue(AnomalieBlock.ART, art), 3);
                                    AnomalieArt fertig = art;
                                    ctx.getSource().sendSuccess(() -> Component.literal("Anomalie " + fertig.id() + " bei "
                                            + pos.toShortString()), true);
                                    return 1;
                                }))));
    }
}
