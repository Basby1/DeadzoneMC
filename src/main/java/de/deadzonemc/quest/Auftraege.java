package de.deadzonemc.quest;

import com.mojang.brigadier.arguments.StringArgumentType;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.mensch.Ansehen;
import de.deadzonemc.mensch.Auftraggeber;
import de.deadzonemc.mensch.Fraktion;
import de.deadzonemc.welt.Staedte;
import de.deadzonemc.welt.Verstecke;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Auftragssystem der Zufluchten: Auftraggeber bieten täglich wechselnde Aufträge an (bringen, jagen,
 * erkunden). Belohnt wird mit Militärpatronen, Ansehen bei der Zuflucht und Skill-Erfahrung.
 * Angenommen wird per Klick im Chat (/auftrag), abgegeben bei jedem Auftraggeber.
 */
public final class Auftraege {

    public static final int MAX_AKTIV = 3;

    private Auftraege() {
    }

    // ---- Daten ------------------------------------------------------------------------------

    private static ListTag liste(ServerPlayer p) {
        return p.getData(ModRegistry.AUFTRAEGE).getList("aktiv", Tag.TAG_COMPOUND);
    }

    private static void speichern(ServerPlayer p, ListTag aktiv, int erledigtPlus) {
        CompoundTag neu = p.getData(ModRegistry.AUFTRAEGE).copy();
        neu.put("aktiv", aktiv);
        neu.putInt("erledigt", neu.getInt("erledigt") + erledigtPlus);
        p.setData(ModRegistry.AUFTRAEGE, neu);
    }

    public static int erledigt(ServerPlayer p) {
        return p.getData(ModRegistry.AUFTRAEGE).getInt("erledigt");
    }

    /** Die drei Angebote dieses Auftraggebers heute (wechseln jeden Spieltag). */
    public static List<Auftrag> angebote(Auftraggeber a) {
        long tag = a.level().getDayTime() / 24000L;
        Random r = new Random(a.getUUID().getLeastSignificantBits() ^ (tag * 0x9E3779B97F4A7C15L));
        List<Auftrag> pool = new ArrayList<>(Auftrag.ALLE);
        List<Auftrag> wahl = new ArrayList<>();
        while (wahl.size() < 3 && !pool.isEmpty()) {
            wahl.add(pool.remove(r.nextInt(pool.size())));
        }
        return wahl;
    }

    // ---- Gespräch mit dem Auftraggeber -------------------------------------------------------

    public static void sprechen(ServerPlayer p, Auftraggeber a) {
        ListTag aktiv = liste(p);
        ListTag rest = new ListTag();
        int fertig = 0;
        for (int i = 0; i < aktiv.size(); i++) {
            CompoundTag t = aktiv.getCompound(i);
            Auftrag auftrag = Auftrag.holen(t.getString("id"));
            if (auftrag == null) {
                continue;
            }
            if (abgeben(p, auftrag, t)) {
                belohnen(p, auftrag);
                fertig++;
            } else {
                rest.add(t);
            }
        }
        speichern(p, rest, fertig);

        p.sendSystemMessage(Component.literal("── ").withStyle(ChatFormatting.DARK_GRAY)
                .append(a.getDisplayName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(" ──").withStyle(ChatFormatting.DARK_GRAY)));
        if (fertig == 0 && rest.isEmpty()) {
            p.sendSystemMessage(Component.literal("„Du siehst aus, als könntest du mit anpacken. Ich hätte da was:“")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        for (int i = 0; i < rest.size(); i++) {
            CompoundTag t = rest.getCompound(i);
            Auftrag auftrag = Auftrag.holen(t.getString("id"));
            p.sendSystemMessage(Component.literal(" ▸ ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(auftrag.titel()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" – " + stand(p, auftrag, t)).withStyle(ChatFormatting.GRAY))
                    .append(knopf(" [Abbrechen]", "/auftrag abbrechen " + auftrag.id(), ChatFormatting.RED, "Auftrag aufgeben")));
        }
        if (rest.size() >= MAX_AKTIV) {
            p.sendSystemMessage(Component.literal("„Erledige erst, was du schon zugesagt hast.“").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (Auftrag an : angebote(a)) {
            if (hat(rest, an.id())) {
                continue;
            }
            p.sendSystemMessage(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(an.titel()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(": " + an.text() + " ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("(" + ziel(an) + " · Lohn: " + an.patronen() + " Militärpatronen)")
                            .withStyle(ChatFormatting.DARK_AQUA))
                    .append(knopf(" [Annehmen]", "/auftrag annehmen " + an.id(), ChatFormatting.GREEN, "Auftrag annehmen")));
        }
    }

    private static MutableComponent knopf(String text, String befehl, ChatFormatting farbe, String hinweis) {
        return Component.literal(text).withStyle(s -> s.withColor(farbe).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, befehl))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hinweis))));
    }

    private static boolean hat(ListTag liste, String id) {
        for (int i = 0; i < liste.size(); i++) {
            if (liste.getCompound(i).getString("id").equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String ziel(Auftrag a) {
        return switch (a.art()) {
            case BRINGEN -> a.anzahl() + "× " + itemName(a.ziel());
            case JAGEN -> a.anzahl() + "× " + wesenName(a.ziel()) + " töten";
            case ERKUNDEN -> a.ziel().equals("stadt") ? "eine Stadt erkunden" : "ein Versteck finden";
        };
    }

    private static String itemName(String ziel) {
        if (ziel.startsWith("#")) {
            return ziel.equals("#minecraft:logs") ? "Holzstämme" : ziel.equals("#deadzonemc:artefakte") ? "Artefakt(e)"
                    : ziel.equals("#deadzonemc:munition") ? "Munition (beliebig)" : ziel.substring(1);
        }
        if (ziel.equals("tacz:ammo")) {
            return "Munition";
        }
        Item i = BuiltInRegistries.ITEM.get(ResourceLocation.parse(ziel));
        return new ItemStack(i).getHoverName().getString();
    }

    private static String wesenName(String ziel) {
        if (ziel.startsWith("#")) {
            return "Infizierte";
        }
        return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(ziel)).getDescription().getString();
    }

    private static String stand(ServerPlayer p, Auftrag a, CompoundTag t) {
        return switch (a.art()) {
            case BRINGEN -> zaehlen(p, a.ziel()) + "/" + a.anzahl() + " " + itemName(a.ziel());
            case JAGEN -> t.getInt("n") + "/" + a.anzahl() + " " + wesenName(a.ziel());
            case ERKUNDEN -> t.getBoolean("ok") ? "Ziel erreicht – melde dich zurück"
                    : "Ziel bei " + t.getInt("x") + " / " + t.getInt("z") + " (" + richtung(p, t.getInt("x"), t.getInt("z")) + ")";
        };
    }

    private static String richtung(ServerPlayer p, int x, int z) {
        double dx = x - p.getX();
        double dz = z - p.getZ();
        int abstand = (int) Math.sqrt(dx * dx + dz * dz);
        String[] namen = {"Süden", "Südwesten", "Westen", "Nordwesten", "Norden", "Nordosten", "Osten", "Südosten"};
        double winkel = Math.toDegrees(Math.atan2(-dx, dz));
        int i = (int) Math.floorMod(Math.round(winkel / 45.0), 8);
        return "noch " + abstand + " Blöcke Richtung " + namen[i];
    }

    // ---- Annehmen / Abbrechen ----------------------------------------------------------------

    public static void annehmen(ServerPlayer p, String id) {
        Auftrag a = Auftrag.holen(id);
        Auftraggeber geber = p.serverLevel().getEntitiesOfClass(Auftraggeber.class, p.getBoundingBox().inflate(10)).stream()
                .filter(g -> angebote(g).stream().anyMatch(x -> x.id().equals(id))).findFirst().orElse(null);
        if (a == null || geber == null) {
            p.sendSystemMessage(Component.literal("Dieser Auftrag wird hier gerade nicht angeboten.").withStyle(ChatFormatting.RED));
            return;
        }
        ListTag aktiv = liste(p);
        if (hat(aktiv, id)) {
            p.sendSystemMessage(Component.literal("Diesen Auftrag hast du schon.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (aktiv.size() >= MAX_AKTIV) {
            p.sendSystemMessage(Component.literal("Du hast schon " + MAX_AKTIV + " Aufträge.").withStyle(ChatFormatting.RED));
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putInt("n", 0);
        if (a.art() == Auftrag.Art.ERKUNDEN) {
            BlockPos ziel = zielSuchen(p, a.ziel());
            t.putInt("x", ziel.getX());
            t.putInt("z", ziel.getZ());
        }
        aktiv.add(t);
        speichern(p, aktiv, 0);
        p.sendSystemMessage(Component.literal("Auftrag angenommen: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(a.titel()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(" – " + stand(p, a, t)).withStyle(ChatFormatting.GRAY)));
        p.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void abbrechen(ServerPlayer p, String id) {
        ListTag aktiv = liste(p);
        ListTag rest = new ListTag();
        for (int i = 0; i < aktiv.size(); i++) {
            if (!aktiv.getCompound(i).getString("id").equals(id)) {
                rest.add(aktiv.getCompound(i));
            }
        }
        speichern(p, rest, 0);
        if (rest.size() < aktiv.size()) {
            Ansehen.aendern(p, Fraktion.ZUFLUCHT, -2);
            p.sendSystemMessage(Component.literal("Auftrag aufgegeben.").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void zeigen(ServerPlayer p) {
        ListTag aktiv = liste(p);
        if (aktiv.isEmpty()) {
            p.sendSystemMessage(Component.literal("Keine Aufträge. Auftraggeber findest du in den Zufluchten.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        for (int i = 0; i < aktiv.size(); i++) {
            CompoundTag t = aktiv.getCompound(i);
            Auftrag a = Auftrag.holen(t.getString("id"));
            if (a != null) {
                p.sendSystemMessage(Component.literal(" ▸ " + a.titel()).withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal(" – " + stand(p, a, t)).withStyle(ChatFormatting.GRAY)));
            }
        }
    }

    private static BlockPos zielSuchen(ServerPlayer p, String art) {
        ServerLevel welt = p.serverLevel();
        int px = p.getBlockX();
        int pz = p.getBlockZ();
        if (art.equals("versteck")) {
            BlockPos best = null;
            for (BlockPos v : Verstecke.umkreis(welt.getSeed(), px, pz, 2)) {
                double d = v.distSqr(new BlockPos(px, v.getY(), pz));
                if (d > 150 * 150 && (best == null || d < best.distSqr(new BlockPos(px, best.getY(), pz)))) {
                    best = v;
                }
            }
            if (best != null) {
                return best;
            }
        } else {
            for (int ring = 16; ring <= 56; ring += 4) {
                for (int i = 0; i < ring * 2; i++) {
                    double w = Math.PI * i / ring;
                    int cx = (px >> 4) + (int) (Math.cos(w) * ring);
                    int cz = (pz >> 4) + (int) (Math.sin(w) * ring);
                    if (Staedte.istStadt(welt, cx, cz)) {
                        return new BlockPos(cx * 16 + 8, 0, cz * 16 + 8);
                    }
                }
            }
        }
        double w = p.getRandom().nextDouble() * Math.PI * 2;
        return new BlockPos(px + (int) (Math.cos(w) * 500), 0, pz + (int) (Math.sin(w) * 500));
    }

    // ---- Fortschritt -------------------------------------------------------------------------

    private static int zaehlen(ServerPlayer p, String ziel) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) {
            if (passt(s, ziel)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean passt(ItemStack s, String ziel) {
        if (s.isEmpty()) {
            return false;
        }
        if (ziel.startsWith("#")) {
            return s.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(ziel.substring(1))));
        }
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(ziel);
    }

    private static boolean abgeben(ServerPlayer p, Auftrag a, CompoundTag t) {
        return switch (a.art()) {
            case JAGEN -> t.getInt("n") >= a.anzahl();
            case ERKUNDEN -> t.getBoolean("ok");
            case BRINGEN -> {
                if (zaehlen(p, a.ziel()) < a.anzahl()) {
                    yield false;
                }
                int fehlt = a.anzahl();
                for (ItemStack s : p.getInventory().items) {
                    if (fehlt > 0 && passt(s, a.ziel())) {
                        int weg = Math.min(fehlt, s.getCount());
                        s.shrink(weg);
                        fehlt -= weg;
                    }
                }
                yield true;
            }
        };
    }

    private static void belohnen(ServerPlayer p, Auftrag a) {
        ItemStack lohn = new ItemStack(ModRegistry.MILITAERPATRONE.get(), a.patronen());
        if (!p.getInventory().add(lohn)) {
            p.drop(lohn, false);
        }
        Ansehen.aendern(p, Fraktion.ZUFLUCHT, a.ansehen());
        var server = p.getServer();
        if (server != null && a.xp() > 0) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    "puffish_skills experience add " + p.getGameProfile().getName() + " deadzonemc:" + a.kategorie() + " " + a.xp());
        }
        p.sendSystemMessage(Component.literal("✔ Auftrag erledigt: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(a.titel()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("  +" + a.patronen() + " Militärpatronen, +" + a.xp() + " Erfahrung ("
                        + a.kategorie() + ")").withStyle(ChatFormatting.GRAY)));
        p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
    }

    public static void getoetet(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ListTag aktiv = liste(p);
        if (aktiv.isEmpty()) {
            return;
        }
        boolean geaendert = false;
        for (int i = 0; i < aktiv.size(); i++) {
            CompoundTag t = aktiv.getCompound(i);
            Auftrag a = Auftrag.holen(t.getString("id"));
            if (a == null || a.art() != Auftrag.Art.JAGEN || t.getInt("n") >= a.anzahl() || !trifft(event.getEntity(), a.ziel())) {
                continue;
            }
            t.putInt("n", t.getInt("n") + 1);
            geaendert = true;
            String text = a.titel() + ": " + t.getInt("n") + "/" + a.anzahl();
            if (t.getInt("n") >= a.anzahl()) {
                text += " – erledigt, zurück zur Zuflucht!";
            }
            p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW), true);
        }
        if (geaendert) {
            speichern(p, aktiv, 0);
        }
    }

    private static boolean trifft(Entity e, String ziel) {
        EntityType<?> typ = e.getType();
        if (ziel.startsWith("#")) {
            return typ.is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(ziel.substring(1))));
        }
        return BuiltInRegistries.ENTITY_TYPE.getKey(typ).toString().equals(ziel);
    }

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 40 != 0) {
            return;
        }
        ListTag aktiv = liste(p);
        boolean geaendert = false;
        for (int i = 0; i < aktiv.size(); i++) {
            CompoundTag t = aktiv.getCompound(i);
            Auftrag a = Auftrag.holen(t.getString("id"));
            if (a == null || a.art() != Auftrag.Art.ERKUNDEN || t.getBoolean("ok")) {
                continue;
            }
            double dx = t.getInt("x") - p.getX();
            double dz = t.getInt("z") - p.getZ();
            if (dx * dx + dz * dz < 24 * 24) {
                t.putBoolean("ok", true);
                geaendert = true;
                p.displayClientMessage(Component.literal(a.titel() + ": Ziel erreicht – zurück zur Zuflucht!")
                        .withStyle(ChatFormatting.YELLOW), true);
                p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        if (geaendert) {
            speichern(p, aktiv, 0);
        }
    }

    public static void befehle(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("auftrag")
                .executes(ctx -> {
                    zeigen(ctx.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("annehmen").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    annehmen(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
                    return 1;
                })))
                .then(Commands.literal("abbrechen").then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                    abbrechen(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"));
                    return 1;
                }))));
    }
}
