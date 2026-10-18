package de.deadzonemc.beute;

import de.deadzonemc.monster.DeadzoneMonster;
import de.deadzonemc.monster.MonsterTypen;
import de.deadzonemc.monster.SpawnSchutz;
import de.deadzonemc.welt.Staedte;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Beutestufen: Wer ein Gebäude (oder die Metro) betritt, sieht dessen Stufe. Höhere Stufe = bessere Beute,
 * aber auch mehr Infizierte, die drinnen lauern. Nachts steigt jede Stufe um eins.
 */
public final class Beutestufe {

    private Beutestufe() {
    }

    private static final ChatFormatting[] FARBE = {ChatFormatting.GREEN, ChatFormatting.YELLOW, ChatFormatting.GOLD,
            ChatFormatting.RED, ChatFormatting.DARK_RED, ChatFormatting.DARK_PURPLE};
    private static final Map<UUID, String> ZULETZT = new HashMap<>();
    private static final Map<Long, Long> HINTERHALT = new HashMap<>();

    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 7 || p.isSpectator()) {
            return;
        }
        ServerLevel w = p.serverLevel();
        BlockPos kopf = BlockPos.containing(p.getEyePosition());
        String ort = null;
        if (!w.canSeeSky(kopf) && w.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            Integer strasse = Staedte.istStadt(w, p.chunkPosition().x, p.chunkPosition().z)
                    ? Staedte.strassenhoehe(w, p.chunkPosition().x, p.chunkPosition().z) : null;
            if (strasse != null) {
                ort = p.getBlockY() < strasse - 5 ? "metro" : "gebaeude";
            } else if (de.deadzonemc.welt.plan.Siedlungen.innerhalb(w.getSeed(), p.getBlockX(), p.getBlockZ())) {
                ort = "gebaeude";
            }
        }
        String vorher = ZULETZT.get(p.getUUID());
        if (ort == null) {
            ZULETZT.remove(p.getUUID());
            return;
        }
        int stufe = GefahrenBonus.stufe(w, p.blockPosition());
        String schluessel = ort + stufe;
        if (schluessel.equals(vorher)) {
            return;
        }
        ZULETZT.put(p.getUUID(), schluessel);
        Component name = Component.translatable(ort.equals("metro") ? "ort.deadzonemc.metro" : "beutestufe.deadzonemc.gebaeude");
        Component text = Component.translatable("beutestufe.deadzonemc.anzeige", name, "☠".repeat(Math.max(1, stufe)) + " " + stufe,
                Component.translatable("beutestufe.deadzonemc." + stufe))
                .withStyle(FARBE[stufe]);
        if (GefahrenBonus.nacht(w)) {
            text = text.copy().append(Component.translatable("beutestufe.deadzonemc.nacht").withStyle(ChatFormatting.DARK_GRAY));
        }
        p.displayClientMessage(text, true);
        if (vorher == null) {
            hinterhalt(w, p, stufe);
        }
    }

    /** Drinnen lauern Infizierte – je höher die Stufe, desto mehr (nicht öfter als alle 5 Minuten pro Gebäude). */
    private static void hinterhalt(ServerLevel w, ServerPlayer p, int stufe) {
        if (stufe < 1 || SpawnSchutz.geschuetzt(p) || !w.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        long chunk = p.chunkPosition().toLong();
        long jetzt = w.getGameTime();
        Long letzter = HINTERHALT.get(chunk);
        if (letzter != null && jetzt - letzter < 6000) {
            return;
        }
        HINTERHALT.put(chunk, jetzt);
        if (HINTERHALT.size() > 4000) {
            HINTERHALT.clear();
        }
        if (w.random.nextFloat() > 0.25F + stufe * 0.15F) {
            return;
        }
        int anzahl = 1 + w.random.nextInt(stufe + 1);
        for (int versuch = 0; versuch < 30 && anzahl > 0; versuch++) {
            int x = p.getBlockX() + w.random.nextInt(25) - 12;
            int z = p.getBlockZ() + w.random.nextInt(25) - 12;
            int y = p.getBlockY() + w.random.nextInt(9) - 4;
            BlockPos pos = new BlockPos(x, y, z);
            if (pos.distSqr(p.blockPosition()) < 36 || !w.getBlockState(pos.below()).isSolid()
                    || !w.getBlockState(pos).isAir() || !w.getBlockState(pos.above()).isAir() || w.canSeeSky(pos)) {
                continue;
            }
            Vec3 ziel = Vec3.atCenterOf(pos).add(0, 0.5, 0);
            if (w.clip(new ClipContext(p.getEyePosition(), ziel, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p)).getType()
                    == HitResult.Type.MISS) {
                continue; // nicht vor den Augen des Spielers erscheinen
            }
            EntityType<? extends DeadzoneMonster> typ = stufe >= 3 && w.random.nextInt(3) == 0
                    ? (w.random.nextBoolean() ? MonsterTypen.LAUERER.get() : MonsterTypen.KRIECHER.get())
                    : MonsterTypen.WANDERER.get();
            DeadzoneMonster m = typ.create(w);
            if (m == null) {
                continue;
            }
            m.moveTo(x + 0.5, y, z + 0.5, w.random.nextFloat() * 360F, 0F);
            m.finalizeSpawn(w, w.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            w.addFreshEntity(m);
            anzahl--;
        }
    }
}
