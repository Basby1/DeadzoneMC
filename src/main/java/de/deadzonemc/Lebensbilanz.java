package de.deadzonemc;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Wie in DayZ: Beim Tod sieht man, wie lange man in der Zone überlebt hat, wie viele Infizierte und Bosse man
 * erledigt hat und woran man gestorben ist. Danach beginnt die Zählung neu.
 */
public final class Lebensbilanz {

    private static final String START = "deadzonemc_leben_start";
    private static final String KILLS = "deadzonemc_leben_kills";
    private static final String BOSSE = "deadzonemc_leben_bosse";

    private Lebensbilanz() {
    }

    public static void registrieren() {
        NeoForge.EVENT_BUS.addListener(Lebensbilanz::betreten);
        NeoForge.EVENT_BUS.addListener(Lebensbilanz::tod);
    }

    private static void betreten(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && !p.getPersistentData().contains(START)) {
            p.getPersistentData().putLong(START, p.serverLevel().getGameTime());
        }
    }

    private static void tod(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            bilanz(p, event.getSource().getLocalizedDeathMessage(p));
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer p && event.getEntity() instanceof Enemy) {
            CompoundTag d = p.getPersistentData();
            d.putInt(KILLS, d.getInt(KILLS) + 1);
            if (event.getEntity() instanceof de.deadzonemc.monster.boss.Boss) {
                d.putInt(BOSSE, d.getInt(BOSSE) + 1);
            }
        }
    }

    private static void bilanz(ServerPlayer p, Component ursache) {
        CompoundTag d = p.getPersistentData();
        long start = d.contains(START) ? d.getLong(START) : p.serverLevel().getGameTime();
        long dauer = Math.max(0, p.serverLevel().getGameTime() - start);
        long tage = dauer / 24000L;
        long stunden = (dauer % 24000L) / 1000L;
        int kills = d.getInt(KILLS);
        int bosse = d.getInt(BOSSE);
        ChatFormatting grau = ChatFormatting.GRAY;
        p.sendSystemMessage(Component.literal("──── Dein Leben in der Zone ────").withStyle(ChatFormatting.DARK_RED));
        p.sendSystemMessage(Component.literal("Überlebt: ").withStyle(grau).append(Component.literal(
                tage + (tage == 1 ? " Tag, " : " Tage, ") + stunden + (stunden == 1 ? " Stunde" : " Stunden"))
                .withStyle(ChatFormatting.WHITE)));
        p.sendSystemMessage(Component.literal("Erledigt: ").withStyle(grau).append(Component.literal(
                kills + (kills == 1 ? " Infizierter" : " Infizierte") + (bosse > 0 ? " · " + bosse + (bosse == 1 ? " Boss" : " Bosse") : ""))
                .withStyle(ChatFormatting.WHITE)));
        p.sendSystemMessage(Component.literal("Todesursache: ").withStyle(grau).append(ursache.copy().withStyle(ChatFormatting.WHITE)));
        d.remove(START);
        d.remove(KILLS);
        d.remove(BOSSE);
    }
}
