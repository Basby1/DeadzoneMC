package de.deadzonemc.wetter;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.netz.WetterPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/** Steuert das Wetter der Deadzone auf dem Server: Wechsel, Regen, Schaden und Befehle. */
public final class WetterSystem {

    public static final ResourceKey<DamageType> SAURER_REGEN = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "saurer_regen"));
    public static final ResourceKey<DamageType> ASCHE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "asche"));

    private WetterSystem() {
    }

    // ---- Ereignisse --------------------------------------------------------------------------

    public static void serverGestartet(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // Das Wetter steuern wir selbst, Minecraft soll es nicht nebenbei umwerfen.
        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        ServerLevel oberwelt = server.overworld();
        regenAnpassen(oberwelt, WetterDaten.holen(oberwelt));
    }

    public static void serverTick(ServerTickEvent.Post event) {
        ServerLevel oberwelt = event.getServer().overworld();
        WetterDaten daten = WetterDaten.holen(oberwelt);
        if (daten.ticken(oberwelt.random)) {
            umschlagen(oberwelt, daten, true);
        }
        if (oberwelt.getGameTime() % 20 == 0) {
            for (ServerPlayer spieler : oberwelt.players()) {
                wetterWirkung(spieler, oberwelt, daten.art());
            }
        }
    }

    public static void spielerBetritt(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer spieler) {
            senden(spieler);
        }
    }

    public static void dimensionGewechselt(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer spieler) {
            senden(spieler);
        }
    }

    // ---- Wetter anwenden ---------------------------------------------------------------------

    private static void senden(ServerPlayer spieler) {
        WetterDaten daten = WetterDaten.holen(spieler.server.overworld());
        PacketDistributor.sendToPlayer(spieler, new WetterPayload(daten.art().ordinal(), daten.nebel()));
    }

    private static void umschlagen(ServerLevel oberwelt, WetterDaten daten, boolean ankuendigen) {
        regenAnpassen(oberwelt, daten);
        PacketDistributor.sendToAllPlayers(new WetterPayload(daten.art().ordinal(), daten.nebel()));
        if (ankuendigen) {
            Component meldung = Component.translatable("wetter.deadzonemc." + daten.art().id() + ".meldung")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            for (ServerPlayer spieler : oberwelt.players()) {
                spieler.displayClientMessage(meldung, true);
            }
        }
    }

    private static void regenAnpassen(ServerLevel oberwelt, WetterDaten daten) {
        if (daten.art().regen()) {
            oberwelt.setWeatherParameters(0, daten.rest() + 100, true, false);
        } else {
            oberwelt.setWeatherParameters(daten.rest() + 100, 0, false, false);
        }
    }

    /** Asche raubt draußen den Atem. Ein Helm schützt. */
    private static void wetterWirkung(ServerPlayer spieler, ServerLevel level, WetterArt art) {
        if (spieler.isCreative() || spieler.isSpectator()) {
            return;
        }
        BlockPos kopf = BlockPos.containing(spieler.getEyePosition());
        long zeit = level.getGameTime();
        // Saurer Regen macht keinen Schaden mehr (nur Stimmung) – das war zu nervig
        // Aschesturm schadet nicht mehr und bremst nicht (Rückmeldung: Debuffs nerven nur) – nur Sicht und Stimmung
    }

    /** Nutzt den Helm statt der Gesundheit ab. Ohne Helm: {@code false}. */
    private static boolean helmAbnutzen(ServerPlayer spieler, int menge) {
        ItemStack helm = spieler.getItemBySlot(EquipmentSlot.HEAD);
        if (helm.isEmpty() || !helm.isDamageableItem()) {
            return !helm.isEmpty();
        }
        helm.hurtAndBreak(menge, spieler, EquipmentSlot.HEAD);
        return true;
    }

    private static DamageSource schaden(Level level, ResourceKey<DamageType> typ) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(typ));
    }

    // ---- Befehle -----------------------------------------------------------------------------

    public static void befehleRegistrieren(RegisterCommandsEvent event) {
        String[] namen = Arrays.stream(WetterArt.values()).map(WetterArt::id).toArray(String[]::new);
        event.getDispatcher().register(Commands.literal("deadzone")
                .requires(quelle -> quelle.hasPermission(2))
                .then(Commands.literal("nebel")
                        .executes(ctx -> nebelSchalten(ctx, null))
                        .then(Commands.literal("an").executes(ctx -> nebelSchalten(ctx, true)))
                        .then(Commands.literal("aus").executes(ctx -> nebelSchalten(ctx, false))))
                .then(Commands.literal("wetter")
                        .executes(WetterSystem::wetterInfo)
                        .then(Commands.argument("art", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(namen, builder))
                                .executes(ctx -> wetterSetzen(ctx, -1))
                                .then(Commands.argument("minuten", IntegerArgumentType.integer(1, 600))
                                        .executes(ctx -> wetterSetzen(ctx,
                                                IntegerArgumentType.getInteger(ctx, "minuten") * 1200))))));
    }

    private static int nebelSchalten(CommandContext<CommandSourceStack> ctx, Boolean an) {
        ServerLevel oberwelt = ctx.getSource().getServer().overworld();
        WetterDaten daten = WetterDaten.holen(oberwelt);
        if (an != null) {
            daten.nebelSetzen(an);
            PacketDistributor.sendToAllPlayers(new WetterPayload(daten.art().ordinal(), daten.nebel()));
        }
        boolean jetzt = daten.nebel();
        ctx.getSource().sendSuccess(() -> Component.translatable(jetzt ? "befehl.deadzonemc.nebel.an"
                : "befehl.deadzonemc.nebel.aus"), an != null);
        return 1;
    }

    private static int wetterInfo(CommandContext<CommandSourceStack> ctx) {
        WetterDaten daten = WetterDaten.holen(ctx.getSource().getServer().overworld());
        int minuten = Math.max(1, daten.rest() / 1200);
        ctx.getSource().sendSuccess(() -> Component.translatable("befehl.deadzonemc.wetter.info",
                Component.translatable("wetter.deadzonemc." + daten.art().id()), minuten), false);
        return 1;
    }

    private static int wetterSetzen(CommandContext<CommandSourceStack> ctx, int dauer) {
        String id = StringArgumentType.getString(ctx, "art");
        WetterArt art = WetterArt.ausId(id);
        if (art == null) {
            ctx.getSource().sendFailure(Component.translatable("befehl.deadzonemc.wetter.unbekannt", id));
            return 0;
        }
        ServerLevel oberwelt = ctx.getSource().getServer().overworld();
        WetterDaten daten = WetterDaten.holen(oberwelt);
        daten.setzen(art, dauer, oberwelt.random);
        umschlagen(oberwelt, daten, true);
        ctx.getSource().sendSuccess(() -> Component.translatable("befehl.deadzonemc.wetter.gesetzt",
                Component.translatable("wetter.deadzonemc." + art.id())), true);
        return 1;
    }
}
