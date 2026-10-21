package de.deadzonemc.geschichte;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Fraktion;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Die Entscheidung in Akt III: Nach dem Sieg über den Strahlenden in der Reaktorhalle von Station Null entscheidet
 * die Gruppe (einmal, für alle):
 * <ul>
 * <li>Reaktor abschalten – Emissionen kommen seltener, die Wacht ist dankbar.</li>
 * <li>Kern bergen – sofort drei Kernsplitter (Abkürzung in Akt IV) und Strahlenkristalle; dafür kommen Emissionen
 * häufiger, und die Wacht wird feindlich.</li>
 * </ul>
 */
public final class Entscheidung {

    public static final String OFFEN = "entscheidung_offen";
    public static final String ABSCHALTEN = "entscheidung:abschalten";
    public static final String BERGEN = "entscheidung:bergen";

    private Entscheidung() {
    }

    /** Faktor auf den Abstand zwischen Emissionen. */
    public static double emissionsFaktor(MinecraftServer server) {
        Fortschritt f = Fortschritt.von(server);
        return f.hat(ABSCHALTEN) ? 1.5 : f.hat(BERGEN) ? 0.7 : 1.0;
    }

    /** Nach dem Tod des Strahlenden: die Wahl anbieten. */
    public static void anbieten(MinecraftServer server) {
        Fortschritt f = Fortschritt.von(server);
        if (f.hat(ABSCHALTEN) || f.hat(BERGEN)) {
            return;
        }
        f.merken(OFFEN);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            zeigen(p);
        }
    }

    public static void zeigen(ServerPlayer p) {
        p.sendSystemMessage(Component.translatable("entscheidung.deadzonemc.frage").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("  ").append(knopf("abschalten", ChatFormatting.AQUA))
                .append("   ").append(knopf("bergen", ChatFormatting.RED)));
    }

    private static Component knopf(String wahl, ChatFormatting farbe) {
        return Component.translatable("entscheidung.deadzonemc." + wahl + ".knopf").withStyle(s -> s.withColor(farbe).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/entscheidung " + wahl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.translatable("entscheidung.deadzonemc." + wahl + ".folgen"))));
    }

    /** Wer sich anmeldet, während die Wahl noch offen ist, bekommt sie wieder angezeigt. */
    public static void anmelden(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Fortschritt f = Fortschritt.von(p.server);
            if (f.hat(OFFEN) && !f.hat(ABSCHALTEN) && !f.hat(BERGEN)) {
                zeigen(p);
            }
        }
    }

    public static void befehl(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("entscheidung")
                .then(Commands.argument("wahl", StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(new String[]{"abschalten", "bergen"}, b))
                        .executes(ctx -> treffen(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "wahl")))));
    }

    private static int treffen(ServerPlayer p, String wahl) {
        Fortschritt f = Fortschritt.von(p.server);
        if (!f.hat(OFFEN) || f.hat(ABSCHALTEN) || f.hat(BERGEN)) {
            p.sendSystemMessage(Component.translatable("entscheidung.deadzonemc.nicht_jetzt").withStyle(ChatFormatting.GRAY));
            return 0;
        }
        boolean abschalten = wahl.equals("abschalten");
        if (!abschalten && !wahl.equals("bergen")) {
            return 0;
        }
        f.merken(abschalten ? ABSCHALTEN : BERGEN);
        for (ServerPlayer x : p.server.getPlayerList().getPlayers()) {
            x.sendSystemMessage(Component.translatable("entscheidung.deadzonemc." + wahl + ".ergebnis", p.getDisplayName())
                    .withStyle(abschalten ? ChatFormatting.AQUA : ChatFormatting.RED));
            Ansehen.aendern(x, Fraktion.WACHT, abschalten ? 25 : -40);
            x.playNotifySound(abschalten ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_POWER_SELECT, SoundSource.MASTER, 1.0F, 0.6F);
        }
        if (!abschalten) {
            geben(p, new ItemStack(Geschichte.KERNSPLITTER.get(), 3));
            geben(p, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("deadzonemc", "strahlenkristall")), 12));
        }
        return 1;
    }

    private static void geben(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s)) {
            p.drop(s, false);
        }
    }
}
