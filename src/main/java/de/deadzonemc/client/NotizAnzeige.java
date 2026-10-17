package de.deadzonemc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Zeigt eine Notiz wie ein Buch an – nur auf dem Client. */
public final class NotizAnzeige {

    private NotizAnzeige() {
    }

    public static void oeffnen(String id) {
        buchZeigen("notiz.deadzonemc." + id + ".");
    }

    /** Das Handbuch: alle Seiten handbuch.deadzonemc.1, .2, ... */
    public static void handbuch() {
        buchZeigen("handbuch.deadzonemc.");
    }

    private static void buchZeigen(String praefix) {
        List<Component> seiten = new ArrayList<>();
        for (int i = 1; i <= 60; i++) {
            String schluessel = praefix + i;
            if (!I18n.exists(schluessel)) {
                break;
            }
            seiten.add(Component.translatable(schluessel));
        }
        if (seiten.isEmpty()) {
            seiten.add(Component.translatable("notiz.deadzonemc.unleserlich"));
        }
        Minecraft.getInstance().setScreen(new BookViewScreen(new BookViewScreen.BookAccess(seiten)));
    }
}
