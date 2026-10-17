package de.deadzonemc.basis;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/** /deadzone basis stufe [sicher|belagerung|gnadenlos] und /deadzone basis info */
public final class Basisbefehle {

    private Basisbefehle() {
    }

    public static void registrieren(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("deadzone")
                .then(Commands.literal("klasse").executes(ctx -> {
                    de.deadzonemc.klasse.Klassen.oeffnen(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("basis")
                        .then(Commands.literal("info").executes(ctx -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            ctx.getSource().sendSuccess(() -> Component.translatable("basis.deadzonemc.befehl_info",
                                    Component.translatable("basis.deadzonemc.stufe." + Basen.stufe(level).id()),
                                    Basen.holen(level).alle().size(),
                                    Basisschutz.belagerungsnacht(level) ? "ja" : "nein"), false);
                            return 1;
                        }))
                        .then(Commands.literal("stufe")
                                .requires(q -> q.hasPermission(2))
                                .then(Commands.argument("stufe", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Sicherheit.values()).map(Sicherheit::id), b))
                                        .executes(ctx -> {
                                            Sicherheit s = Sicherheit.ausId(StringArgumentType.getString(ctx, "stufe"));
                                            if (s == null) {
                                                ctx.getSource().sendFailure(Component.literal("sicher, belagerung oder gnadenlos"));
                                                return 0;
                                            }
                                            Basen.stufeSetzen(ctx.getSource().getLevel(), s);
                                            ctx.getSource().sendSuccess(() -> Component.translatable("basis.deadzonemc.stufe_gesetzt",
                                                    Component.translatable("basis.deadzonemc.stufe." + s.id())), true);
                                            return 1;
                                        })))));
    }
}
