package de.deadzonemc.zone;

import de.deadzonemc.DeadzoneMC;
import de.deadzonemc.strahlung.Strahlung;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Die Emission: alle paar Tage bricht aus der Zone eine Welle aus Strahlung über das Land. Wer sie unter
 * freiem Himmel erlebt, stirbt. 90 Sekunden Vorwarnung per Funk, dann eine Minute Hölle. Danach haben die
 * Anomalien neue Artefakte ausgespuckt.
 */
public final class Emission extends SavedData {

    public static final ResourceKey<DamageType> SCHADEN = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(DeadzoneMC.MODID, "emission"));
    public static final int WARNUNG = 1800;
    public static final int DAUER = 1400;

    private static final Factory<Emission> FABRIK = new Factory<>(Emission::new, Emission::laden, null);

    private int stufe;      // 0 = ruhig, 1 = Warnung, 2 = Emission
    private int rest;
    private int nummer;
    private long naechste = -1;

    public static Emission holen(ServerLevel oberwelt) {
        return oberwelt.getDataStorage().computeIfAbsent(FABRIK, "deadzonemc_emission");
    }

    public static int nummer(ServerLevel level) {
        return holen(level.getServer().overworld()).nummer;
    }

    public static int stufe(ServerLevel level) {
        return holen(level.getServer().overworld()).stufe;
    }

    private static Emission laden(CompoundTag tag, HolderLookup.Provider p) {
        Emission e = new Emission();
        e.stufe = tag.getInt("Stufe");
        e.rest = tag.getInt("Rest");
        e.nummer = tag.getInt("Nummer");
        e.naechste = tag.contains("Naechste") ? tag.getLong("Naechste") : -1;
        return e;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider p) {
        tag.putInt("Stufe", stufe);
        tag.putInt("Rest", rest);
        tag.putInt("Nummer", nummer);
        tag.putLong("Naechste", naechste);
        return tag;
    }

    // ---- Ablauf ----------------------------------------------------------------------------

    public static void tick(ServerTickEvent.Post event) {
        ServerLevel welt = event.getServer().overworld();
        Emission e = holen(welt);
        long zeit = welt.getGameTime();
        if (e.naechste < 0) {
            // Die erste Emission frühestens nach drei Tagen
            e.naechste = zeit + 72000L + welt.random.nextInt(48000);
            e.setDirty();
        }
        if (e.stufe == 0) {
            if (zeit >= e.naechste && !welt.players().isEmpty()) {
                e.starten(welt);
            }
            return;
        }
        e.rest--;
        if (e.rest % 100 == 0) {
            e.setDirty();
        }
        if (e.stufe == 1) {
            if (e.rest % 400 == 0 && e.rest > 0) {
                melden(welt, Component.translatable("zone.deadzonemc.emission.countdown", e.rest / 20)
                        .withStyle(ChatFormatting.RED), true);
                for (ServerPlayer p : welt.players()) {
                    p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.AMBIENT, 1.0F, 0.6F);
                }
            }
            if (e.rest <= 0) {
                e.stufe = 2;
                e.rest = DAUER;
                e.senden();
                titel(welt, "zone.deadzonemc.emission.titel", "zone.deadzonemc.emission.jetzt");
                for (ServerPlayer p : welt.players()) {
                    p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.AMBIENT, 0.8F, 0.5F);
                }
            }
        } else if (e.stufe == 2) {
            e.wueten(welt);
            if (e.rest <= 0) {
                e.stufe = 0;
                e.nummer++;
                e.naechste = zeit + 48000L + welt.random.nextInt(72000);
                e.senden();
                melden(welt, Component.translatable("zone.deadzonemc.emission.ende").withStyle(ChatFormatting.GRAY), false);
            }
        }
        e.setDirty();
    }

    /** Startet die Vorwarnung (auch per Befehl). */
    public void starten(ServerLevel welt) {
        stufe = 1;
        rest = WARNUNG;
        setDirty();
        senden();
        titel(welt, "zone.deadzonemc.emission.titel", "zone.deadzonemc.emission.warnung");
        melden(welt, Component.translatable("zone.deadzonemc.emission.funk").withStyle(ChatFormatting.RED), false);
        for (ServerPlayer p : welt.players()) {
            p.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
        }
    }

    private void wueten(ServerLevel welt) {
        long zeit = welt.getGameTime();
        DamageSource quelle = new DamageSource(welt.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SCHADEN));
        for (ServerPlayer p : welt.players()) {
            if (p.isSpectator()) {
                continue;
            }
            // Blitze am Horizont
            if (zeit % 12 == 0 && welt.random.nextInt(3) == 0) {
                double w = welt.random.nextDouble() * Math.PI * 2;
                double r = 25 + welt.random.nextDouble() * 50;
                int x = (int) (p.getX() + Math.cos(w) * r);
                int z = (int) (p.getZ() + Math.sin(w) * r);
                if (welt.hasChunkAt(new BlockPos(x, 0, z))) {
                    LightningBolt blitz = EntityType.LIGHTNING_BOLT.create(welt);
                    if (blitz != null) {
                        blitz.moveTo(x + 0.5, welt.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z + 0.5);
                        blitz.setVisualOnly(true);
                        welt.addFreshEntity(blitz);
                    }
                }
            }
            if (p.isCreative() || zeit % 20 != 0) {
                continue;
            }
            boolean draussen = welt.canSeeSky(BlockPos.containing(p.getEyePosition())) && p.level() == welt;
            if (draussen) {
                p.hurt(quelle, 3.0F);
                Strahlung.bestrahlen(p, 12.0F);
                p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, true, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, true, false, false));
                p.displayClientMessage(Component.translatable("zone.deadzonemc.emission.draussen")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            } else if (zeit % 100 == 0) {
                p.displayClientMessage(Component.translatable("zone.deadzonemc.emission.drinnen", rest / 20)
                        .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    private void senden() {
        PacketDistributor.sendToAllPlayers(new EmissionPayload(stufe));
    }

    public static void spielerBetritt(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PacketDistributor.sendToPlayer(p, new EmissionPayload(holen(p.server.overworld()).stufe));
        }
    }

    private static void melden(ServerLevel welt, Component text, boolean leiste) {
        for (ServerPlayer p : welt.players()) {
            if (leiste) {
                p.displayClientMessage(text, true);
            } else {
                p.sendSystemMessage(text);
            }
        }
    }

    private static void titel(ServerLevel welt, String titel, String untertitel) {
        for (ServerPlayer p : welt.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(titel)
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(untertitel)
                    .withStyle(ChatFormatting.RED)));
        }
    }

    /** Für Tests: verkürzt die laufende Phase. */
    public void restSetzen(int ticks) {
        rest = ticks;
        setDirty();
    }

    /** Spielzeit der nächsten Emission (-1 = noch nicht geplant). */
    public long naechste() {
        return naechste;
    }

    public int stufe() {
        return stufe;
    }

    public int rest() {
        return rest;
    }

    public long naechste() {
        return naechste;
    }
}
