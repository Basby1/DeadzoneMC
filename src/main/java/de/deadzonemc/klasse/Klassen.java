package de.deadzonemc.klasse;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.ModRegistry;
import de.deadzonemc.basis.Basen;
import de.deadzonemc.monster.DeadzoneMonster;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Klassen: Auswahl, passive Vorteile und Schwächen, aktive Fähigkeiten (Taste V). */
public final class Klassen {

    private Klassen() {
    }

    private static final ResourceLocation MOD_LEBEN = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_leben");
    private static final ResourceLocation MOD_RUESTUNG = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_ruestung");
    private static final ResourceLocation MOD_TEMPO = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_tempo");

    private static final Map<String, Long> ABKLINGEN = new HashMap<>();
    /** Aktive Lärmköder: Position und Ende (Spielzeit). */
    private static final List<long[]> KOEDER = new ArrayList<>();

    private static final ResourceLocation MOD_RUECKSTOSS = ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "klasse_rueckstoss");
    /** Welche der eigenen Klassen gerade die Fähigkeit auf V liefert (Index in den aktiven Klassen). */
    private static final Map<UUID, Integer> GEWAEHLT = new HashMap<>();

    // ---- Mehrfachklassen ------------------------------------------------------------------------
    // Allein spielt man 3 Klassen, zu zweit 2, ab drei Spielern 1. Wer mehr Klassen gewählt hat, als gerade
    // Plätze frei sind, behält sie – die hinteren ruhen nur, bis wieder weniger Spieler online sind.

    /** Alle gewählten Klassen in der gewählten Reihenfolge. */
    public static List<Klasse> gewaehlt(Player p) {
        List<Klasse> l = new ArrayList<>();
        for (String id : p.getData(ModRegistry.KLASSE).split(",")) {
            Klasse k = Klasse.ausId(id.trim());
            if (k != null && !l.contains(k)) {
                l.add(k);
            }
        }
        return l;
    }

    /** Wie viele Klassen gerade gleichzeitig wirken dürfen. */
    public static int plaetze(net.minecraft.server.MinecraftServer server) {
        if (server == null) {
            return 1;
        }
        long n = server.getPlayerList().getPlayers().stream().filter(x -> !x.isSpectator()).count();
        return n <= 1 ? 3 : n == 2 ? 2 : 1;
    }

    /** Die gerade wirkenden Klassen. */
    public static List<Klasse> aktiv(Player p) {
        List<Klasse> l = gewaehlt(p);
        int n = p.getServer() != null ? plaetze(p.getServer()) : 3;
        return l.size() > n ? l.subList(0, n) : l;
    }

    public static boolean hat(Player p, Klasse k) {
        return k != null && aktiv(p).contains(k);
    }

    /** Wirken beide Klassen gerade bei diesem Spieler (Kombination)? */
    public static boolean kombi(Player p, Klasse a, Klasse b) {
        List<Klasse> l = aktiv(p);
        return l.contains(a) && l.contains(b);
    }

    /** Hauptklasse (die erste der wirkenden) – für Stellen, die nur eine Klasse kennen. */
    public static Klasse klasse(Player p) {
        List<Klasse> l = aktiv(p);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Für den Client: "brecher,schuetze;2" (gewählte Klassen; Plätze). */
    private static String kodieren(ServerPlayer p) {
        return p.getData(ModRegistry.KLASSE) + ";" + plaetze(p.getServer());
    }

    /** Spielerzahl hat sich geändert: Werte neu setzen und allen Bescheid geben. */
    public static void allenNeu(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer x : server.getPlayerList().getPlayers()) {
            anwenden(x);
            PacketDistributor.sendToPlayer(x, new KlassePayload(kodieren(x), false));
        }
    }

    public static void verlassen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getServer() != null) {
            var server = p.getServer();
            server.execute(() -> allenNeu(server));
        }
    }

    // ---- Wahl -------------------------------------------------------------------------------

    public static void empfangen(KlassePayload n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) {
                return;
            }
            if (n.klasse().equals("!")) {
                if (p.isShiftKeyDown()) {
                    wechseln(p);
                } else {
                    faehigkeit(p);
                }
                return;
            }
            List<Klasse> neu = new ArrayList<>();
            for (String id : n.klasse().split(",")) {
                Klasse k = Klasse.ausId(id.trim());
                if (k != null && !neu.contains(k)) {
                    neu.add(k);
                }
            }
            int frei = plaetze(p.getServer());
            if (neu.isEmpty()) {
                return;
            }
            if (neu.size() > frei) {
                p.displayClientMessage(Component.translatable("klasse.deadzonemc.zu_viele", frei).withStyle(ChatFormatting.RED), false);
                return;
            }
            if (!gewaehlt(p).isEmpty() && !inEigenerBasis(p) && !p.isCreative()) {
                p.displayClientMessage(Component.translatable("klasse.deadzonemc.nur_basis").withStyle(ChatFormatting.RED), false);
                return;
            }
            p.setData(ModRegistry.KLASSE, String.join(",", neu.stream().map(Klasse::id).toList()));
            GEWAEHLT.remove(p.getUUID());
            anwenden(p);
            Component namen = Component.empty();
            for (int i = 0; i < neu.size(); i++) {
                if (i > 0) {
                    namen = namen.copy().append(" + ");
                }
                namen = namen.copy().append(Component.translatable("klasse.deadzonemc." + neu.get(i).id()));
            }
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.gewaehlt", namen).withStyle(ChatFormatting.GOLD), false);
            PacketDistributor.sendToPlayer(p, new KlassePayload(kodieren(p), false));
        });
    }

    private static boolean inEigenerBasis(ServerPlayer p) {
        return Basen.holen(p.serverLevel()).basisBei(p.getX(), p.getZ(), 0) != null;
    }

    public static void oeffnen(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new KlassePayload(kodieren(p), true));
    }

    public static void betreten(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            anwenden(p);
            if (gewaehlt(p).isEmpty()) {
                oeffnen(p);
            }
            // Ein Spieler mehr: bei allen ändert sich womöglich die Zahl der wirkenden Klassen
            allenNeu(p.getServer());
        }
    }

    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            anwenden(p);
            p.setHealth(p.getMaxHealth());
        }
    }

    // ---- Passive Werte ------------------------------------------------------------------------

    private static void setze(AttributeInstance a, ResourceLocation id, double wert, AttributeModifier.Operation op) {
        if (a == null) {
            return;
        }
        a.removeModifier(id);
        if (wert != 0) {
            a.addPermanentModifier(new AttributeModifier(id, wert, op));
        }
    }

    public static void anwenden(ServerPlayer p) {
        List<Klasse> aktiv = aktiv(p);
        double leben = 0;
        double ruestung = 0;
        double tempo = 0;
        double rueckstoss = 0;
        if (aktiv.contains(Klasse.BRECHER)) {
            leben += 4;
            ruestung += 2;
        }
        // Schwächen nur mit einer einzigen Klasse – wer wenige ist, kann sich keine Schwächen leisten
        if (aktiv.size() == 1) {
            switch (aktiv.get(0)) {
                case SCHUETZE, SPAEHER -> leben -= 4;
                case TECHNIKER -> tempo -= 0.06;
                default -> {
                }
            }
        }
        // Kombinationen
        if (kombi(p, Klasse.BRECHER, Klasse.TECHNIKER)) {
            ruestung += 2; // Bollwerk
        }
        if (kombi(p, Klasse.SANITAETER, Klasse.TECHNIKER)) {
            leben += 4; // Feldlazarett
        }
        if (kombi(p, Klasse.BRECHER, Klasse.SPAEHER)) {
            tempo += 0.05; // Raubtier
        }
        if (kombi(p, Klasse.BRECHER, Klasse.SCHUETZE)) {
            rueckstoss += 0.25; // Sturmtruppe
        }
        setze(p.getAttribute(Attributes.MAX_HEALTH), MOD_LEBEN, leben, AttributeModifier.Operation.ADD_VALUE);
        setze(p.getAttribute(Attributes.ARMOR), MOD_RUESTUNG, ruestung, AttributeModifier.Operation.ADD_VALUE);
        setze(p.getAttribute(Attributes.MOVEMENT_SPEED), MOD_TEMPO, tempo, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setze(p.getAttribute(Attributes.KNOCKBACK_RESISTANCE), MOD_RUECKSTOSS, rueckstoss, AttributeModifier.Operation.ADD_VALUE);
        if (p.getHealth() > p.getMaxHealth()) {
            p.setHealth(p.getMaxHealth());
        }
    }

    /** Nahkampf- und Fernkampfschaden je nach Klasse. */
    public static void schaden(LivingIncomingDamageEvent event) {
        Entity quelle = event.getSource().getEntity();
        if (!(quelle instanceof Player p)) {
            return;
        }
        List<Klasse> aktiv = aktiv(p);
        if (aktiv.isEmpty()) {
            return;
        }
        boolean nahkampf = event.getSource().getDirectEntity() == p;
        // Mit mehreren Klassen zählt jeweils die stärkste – Schwächen fallen weg
        float f = 0F;
        for (Klasse k : aktiv) {
            f = Math.max(f, switch (k) {
                case BRECHER -> nahkampf ? 1.3F : 0.8F;
                case SCHUETZE -> nahkampf ? 0.75F : 1.25F;
                case SANITAETER -> 0.9F;
                default -> 1.0F;
            });
        }
        if (aktiv.size() > 1) {
            f = Math.max(f, 1.0F);
        }
        event.setAmount(event.getAmount() * f);
    }

    /** Wie stark Heilung bei diesem Spieler wirkt (Sanitäter: 1,5-fach). */
    public static float heilfaktor(Player p) {
        return (hat(p, Klasse.SANITAETER) ? 1.5F : 1.0F) * Talente.heilfaktor(p);
    }

    // ---- Aktive Fähigkeiten ------------------------------------------------------------------

    /** Die Klasse, deren Fähigkeit gerade auf V liegt. */
    public static Klasse faehigkeitKlasse(Player p) {
        List<Klasse> l = aktiv(p);
        if (l.isEmpty()) {
            return null;
        }
        return l.get(Math.floorMod(GEWAEHLT.getOrDefault(p.getUUID(), 0), l.size()));
    }

    private static void wechseln(ServerPlayer p) {
        List<Klasse> l = aktiv(p);
        if (l.size() < 2) {
            faehigkeit(p);
            return;
        }
        GEWAEHLT.put(p.getUUID(), Math.floorMod(GEWAEHLT.getOrDefault(p.getUUID(), 0) + 1, l.size()));
        Klasse k = faehigkeitKlasse(p);
        p.displayClientMessage(Component.translatable("klasse.deadzonemc.faehigkeit_gewechselt",
                Component.translatable("klasse.deadzonemc." + k.id() + ".faehigkeit_name")).withStyle(ChatFormatting.YELLOW), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
    }

    private static void faehigkeit(ServerPlayer p) {
        Klasse k = faehigkeitKlasse(p);
        if (k == null || p.isSpectator()) {
            return;
        }
        long jetzt = p.serverLevel().getGameTime();
        long frei = ABKLINGEN.getOrDefault(p.getUUID() + k.id(), 0L);
        if (jetzt < frei) {
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.abklingen", (frei - jetzt) / 20 + 1)
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        boolean ok = switch (k) {
            case BRECHER -> ansturm(p);
            case SCHUETZE -> markieren(p);
            case SANITAETER -> adrenalin(p);
            case TECHNIKER -> koeder(p);
            case SPAEHER -> leuchtsignal(p);
        };
        if (ok) {
            ABKLINGEN.put(p.getUUID() + k.id(), jetzt + k.abklingzeit());
        }
    }

    private static boolean ansturm(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        Vec3 blick = p.getLookAngle().multiply(1, 0, 1).normalize();
        int getroffen = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(4.5), e -> e instanceof Monster)) {
            Vec3 zu = e.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            if (zu.dot(blick) > 0.3) {
                e.knockback(1.6, -zu.x, -zu.z);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                e.hurt(p.damageSources().playerAttack(p), 3.0F);
                getroffen++;
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + blick.x * 2, p.getY() + 1, p.getZ() + blick.z * 2, 3, 0.8, 0.3, 0.8, 0);
        return true;
    }

    private static boolean markieren(ServerPlayer p) {
        Vec3 auge = p.getEyePosition();
        Vec3 ende = auge.add(p.getLookAngle().scale(64));
        EntityHitResult treffer = ProjectileUtil.getEntityHitResult(p.level(), p, auge, ende,
                new AABB(auge, ende).inflate(1.5), e -> e instanceof LivingEntity && !(e instanceof Player), 0.6F);
        if (treffer == null || !(treffer.getEntity() instanceof LivingEntity ziel)) {
            p.displayClientMessage(Component.translatable("klasse.deadzonemc.kein_ziel").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ziel.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        ziel.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 0));
        p.serverLevel().playSound(null, ziel.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    private static boolean adrenalin(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (ServerPlayer m : level.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(6))) {
            m.heal(8.0F);
            m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            m.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            level.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + 1.8, m.getZ(), 5, 0.4, 0.3, 0.4, 0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.4F);
        return true;
    }

    private static boolean koeder(ServerPlayer p) {
        var treffer = p.pick(24, 1.0F, false);
        BlockPos pos = BlockPos.containing(treffer.getLocation());
        KOEDER.add(new long[]{pos.asLong(), p.serverLevel().getGameTime() + 400});
        p.displayClientMessage(Component.translatable("klasse.deadzonemc.koeder").withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    private static boolean leuchtsignal(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40), e -> e instanceof Monster)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 240, 0));
        }
        level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 6, p.getZ(), 40, 0.5, 0.5, 0.5, 0.15);
        level.playSound(null, p.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.2F, 0.8F);
        return true;
    }

    /** Lärmköder: lockt Monster in der Nähe an und piept dabei. */
    public static void tick(ServerTickEvent.Post event) {
        if (KOEDER.isEmpty()) {
            return;
        }
        ServerLevel level = event.getServer().overworld();
        long jetzt = level.getGameTime();
        Iterator<long[]> it = KOEDER.iterator();
        while (it.hasNext()) {
            long[] k = it.next();
            if (jetzt > k[1]) {
                it.remove();
                continue;
            }
            if (jetzt % 20 != 0) {
                continue;
            }
            BlockPos pos = BlockPos.of(k[0]);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 2.0F, 1.6F);
            level.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(pos).inflate(32))) {
                if (m.getTarget() == null || m instanceof DeadzoneMonster) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.1);
                }
            }
        }
    }
}
