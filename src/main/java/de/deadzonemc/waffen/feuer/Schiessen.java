package de.deadzonemc.waffen.feuer;

import de.deadzonemc.monster.Laerm;
import de.deadzonemc.waffen.Waffenpflege;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ein Schuss auf dem Server: Takt prüfen, Patrone verbrauchen, Verschleiß, Lärm (lockt Infizierte an – mit
 * Schalldämpfer viel weniger), Mündungsfeuer, Strahl verfolgen (Streuung, Schrot), Treffer mit Kopfschuss.
 */
public final class Schiessen {

    private static final Map<UUID, Long> LETZTER = new HashMap<>();
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> SCHUSS =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(de.deadzonemc.DeadzoneMC.MODID, "schuss"));

    private Schiessen() {
    }

    public static void empfangen(WaffenPayloads.Schuss n, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer p) {
                schuss(p, n.zielen());
            }
        });
    }

    public static void schuss(ServerPlayer p, boolean zielen) {
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof SchusswaffeItem w) || p.isSpectator()) {
            return;
        }
        WaffenArt a = w.art;
        ServerLevel lv = p.serverLevel();
        long jetzt = lv.getGameTime();
        Long vorher = LETZTER.get(p.getUUID());
        if (vorher != null && jetzt - vorher < a.takt - 1) {
            return;
        }
        if (Nachladen.laedt(p)) {
            if (a.einzeln && s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0) > 0) {
                Nachladen.abbrechen(p);
            } else {
                return;
            }
        }
        LETZTER.put(p.getUUID(), jetzt);
        if (Boolean.TRUE.equals(s.get(Waffenpflege.LADEHEMMUNG.get()))) {
            lv.playSound(null, p.blockPosition(), Feuerwaffen.LEER.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
            return;
        }
        int mag = s.getOrDefault(Feuerwaffen.MAGAZIN.get(), 0);
        if (mag <= 0) {
            lv.playSound(null, p.blockPosition(), Feuerwaffen.LEER.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            Nachladen.starten(p);
            return;
        }
        if (!p.isCreative()) {
            s.set(Feuerwaffen.MAGAZIN.get(), mag - 1);
        }
        Waffenpflege.nachSchuss(s, p);
        boolean leise = Feuerwaffen.hatAufsatz(s, "schalldaempfer");
        lv.playSound(null, p.getX(), p.getEyeY(), p.getZ(), leise ? Feuerwaffen.GEDAEMPFT.get() : Feuerwaffen.SCHUSS.get(a).get(),
                SoundSource.PLAYERS, leise ? 0.8F : 3.0F, 0.94F + p.getRandom().nextFloat() * 0.12F);
        Laerm.machen(lv, p.position(), leise ? a.laerm * 0.25 : a.laerm);
        Vec3 blick = p.getLookAngle();
        Vec3 auge = p.getEyePosition();
        Vec3 rechts = blick.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 muendung = auge.add(blick.scale(0.9)).add(rechts.scale(0.25)).add(0, -0.15, 0);
        if (!leise && a.kaliber != Kaliber.BOLZEN) {
            lv.sendParticles(ParticleTypes.FLAME, muendung.x, muendung.y, muendung.z, 2, 0.02, 0.02, 0.02, 0.01);
        }
        lv.sendParticles(ParticleTypes.SMOKE, muendung.x, muendung.y, muendung.z, 3, 0.03, 0.03, 0.03, 0.01);
        float streuung = zielen ? a.streuungZiel : a.streuung;
        if (p.isSprinting() || !p.onGround()) {
            streuung *= 1.8F;
        } else if (p.isShiftKeyDown()) {
            streuung *= 0.75F;
        }
        if (zielen && Feuerwaffen.hatAufsatz(s, "zielfernrohr")) {
            streuung *= 0.4F;
        }
        int bestes = 0;
        for (int i = 0; i < a.kugeln; i++) {
            bestes = Math.max(bestes, strahl(lv, p, a, auge, streue(blick, streuung, p.getRandom()), i == 0));
        }
        if (bestes > 0) {
            PacketDistributor.sendToPlayer(p, new WaffenPayloads.Rueckmeldung(bestes, 0));
        }
    }

    private static Vec3 streue(Vec3 richtung, float grad, net.minecraft.util.RandomSource r) {
        if (grad <= 0) {
            return richtung;
        }
        double rad = Math.toRadians(grad) * Math.sqrt(r.nextDouble());
        double wnk = r.nextDouble() * Math.PI * 2;
        Vec3 a = Math.abs(richtung.y) < 0.95 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = richtung.cross(a).normalize();
        Vec3 v = richtung.cross(u).normalize();
        return richtung.add(u.scale(Math.cos(wnk) * Math.tan(rad))).add(v.scale(Math.sin(wnk) * Math.tan(rad))).normalize();
    }

    /** Verfolgt einen Strahl. Rückgabe: 0 nichts, 1 Treffer, 2 Kopfschuss, 3 tödlich. */
    private static int strahl(ServerLevel lv, ServerPlayer p, WaffenArt a, Vec3 start, Vec3 richtung, boolean erster) {
        Vec3 ende = start.add(richtung.scale(a.reichweite));
        BlockHitResult block = lv.clip(new ClipContext(start, ende, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 bis = block.getType() == HitResult.Type.MISS ? ende : block.getLocation();
        EntityHitResult ent = ProjectileUtil.getEntityHitResult(lv, p, start, bis,
                new AABB(start, bis).inflate(1.0), e -> e.isPickable() && !e.isSpectator() && e != p && e.isAlive(), 0.0F);
        if (ent != null && ent.getEntity() instanceof LivingEntity ziel) {
            Vec3 treffpunkt = ent.getLocation();
            double dist = start.distanceTo(treffpunkt);
            float schaden = a.schaden;
            if (a.kugeln > 1) {
                schaden *= (float) Mth.clamp(1.25 - dist / a.reichweite, 0.3, 1.0);
            } else if (dist > a.reichweite * 0.6) {
                schaden *= 0.8F;
            }
            boolean kopf = treffpunkt.y >= ziel.getEyeY() - 0.28 && ziel.getBbHeight() > 1.2F;
            if (kopf) {
                schaden *= a.kopf;
            }
            ziel.invulnerableTime = 0;
            boolean getroffen = ziel.hurt(new net.minecraft.world.damagesource.DamageSource(lv.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(SCHUSS), p, p), schaden);
            ziel.invulnerableTime = 0;
            if (getroffen) {
                ziel.knockback(0.15 * Math.min(2, a.kugeln), -richtung.x, -richtung.z);
            }
            lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState()),
                    treffpunkt.x, treffpunkt.y, treffpunkt.z, kopf ? 10 : 5, 0.1, 0.1, 0.1, 0.05);
            if (!getroffen) {
                return 0;
            }
            return !ziel.isAlive() ? 3 : kopf ? 2 : 1;
        }
        if (block.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = block.getBlockPos();
            BlockState zustand = lv.getBlockState(pos);
            Vec3 h = block.getLocation();
            lv.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, zustand), h.x, h.y, h.z, 4, 0.05, 0.05, 0.05, 0.05);
            if (erster) {
                lv.playSound(null, pos, Feuerwaffen.EINSCHLAG.get(), SoundSource.BLOCKS, 0.6F, 0.9F + lv.random.nextFloat() * 0.3F);
            }
            if (zustand.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || zustand.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)
                    || zustand.is(BlockTags.IMPERMEABLE)) {
                if (!de.deadzonemc.zuflucht.ZufluchtSchutz.sicher(lv, pos)) {
                    lv.destroyBlock(pos, false, p);
                    Laerm.machen(lv, Vec3.atCenterOf(pos), 20);
                }
            }
        }
        return 0;
    }

    @SuppressWarnings("unused")
    private static boolean lebt(Entity e) {
        return e.isAlive();
    }
}
