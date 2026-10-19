package de.deadzonemc.mensch;

import de.deadzonemc.ModRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Händler der Karawane: tauscht Beute gegen Beute, jedes Mal ein anderes Angebot. */
public class Haendler extends WanderingTrader implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Haendler(EntityType<? extends WanderingTrader> typ, Level level) {
        super(typ, level);
        setDespawnDelay(36000);
    }

    /** Früher TaCZ – jetzt eigene Waffen und Munition (Namen werden übersetzt). */
    public static ItemStack tacz(String item, String schluessel, String wert, int menge) {
        return de.deadzonemc.waffen.feuer.Feuerwaffen.ausTacz(item, wert, menge);
    }

    private static MerchantOffer angebot(ItemLike preis, int anzahl, ItemStack ware, int vorrat) {
        return new MerchantOffer(new ItemCost(preis, anzahl), Optional.empty(), ware, vorrat, 2, 0.05F);
    }

    private static MerchantOffer angebot(ItemLike preis, int anzahl, ItemLike preis2, int anzahl2, ItemStack ware, int vorrat) {
        return new MerchantOffer(new ItemCost(preis, anzahl), Optional.of(new ItemCost(preis2, anzahl2)), ware, vorrat, 2, 0.05F);
    }

    /** Festes Sortiment eines Händlers in einer Zuflucht (leer = Karawanenhändler mit wechselndem Angebot). */
    private String sortiment = "";
    private net.minecraft.core.BlockPos heimat;

    /** Macht ihn zum sesshaften Händler einer Zuflucht: festes Sortiment, verschwindet nie, bleibt am Stand. */
    public void zufluchtSetzen(String sortiment, net.minecraft.core.BlockPos stand) {
        this.sortiment = sortiment;
        this.heimat = stand;
        setDespawnDelay(0);
        restrictTo(stand, 4);
        setPersistenceRequired();
        getOffers().clear();
        updateTrades();
    }

    public String sortiment() {
        return sortiment;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.6));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Sortiment", sortiment);
        if (heimat != null) {
            tag.putLong("Stand", heimat.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        sortiment = tag.getString("Sortiment");
        if (tag.contains("Stand")) {
            heimat = net.minecraft.core.BlockPos.of(tag.getLong("Stand"));
            restrictTo(heimat, 4);
            setDespawnDelay(0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double abstand) {
        return heimat == null && super.removeWhenFarAway(abstand);
    }

    @Override
    protected void updateTrades() {
        if (!sortiment.isEmpty()) {
            de.deadzonemc.zuflucht.Sortimente.fuellen(sortiment, getOffers());
            return;
        }
        List<MerchantOffer> alle = new ArrayList<>();
        alle.add(angebot(ModRegistry.SCHROTT.get(), 4, new ItemStack(ModRegistry.ELEKTRONIK.get()), 8));
        alle.add(angebot(ModRegistry.ELEKTRONIK.get(), 3, new ItemStack(ModRegistry.FILTER_ITEM.get()), 4));
        alle.add(angebot(ModRegistry.KONSERVENDOSE.get(), 6, new ItemStack(ModRegistry.ANTIBIOTIKA.get()), 3));
        alle.add(angebot(ModRegistry.STOFFRESTE.get(), 4, new ItemStack(ModRegistry.VERBAND.get(), 2), 6));
        alle.add(angebot(ModRegistry.SCHROTT.get(), 10, new ItemStack(ModRegistry.SCHATZKARTE.get()), 2));
        alle.add(angebot(ModRegistry.BATTERIE.get(), 4, new ItemStack(ModRegistry.ANTI_RAD.get()), 3));
        alle.add(angebot(ModRegistry.ELEKTRONIK.get(), 12, new ItemStack(ModRegistry.GASMASKE.get()), 1));
        alle.add(angebot(ModRegistry.BATTERIE.get(), 2, ModRegistry.ELEKTRONIK.get(), 2, new ItemStack(ModRegistry.GEIGERZAEHLER.get()), 1));
        alle.add(angebot(Items.IRON_INGOT, 6, new ItemStack(ModRegistry.KONSERVENDOSE.get(), 4), 6));
        alle.add(angebot(ModRegistry.KONSERVENDOSE.get(), 3, new ItemStack(ModRegistry.WODKA.get()), 4));
        ItemStack munition9 = tacz("ammo", "AmmoId", "9mm", 16);
        if (!munition9.isEmpty()) {
            alle.add(angebot(ModRegistry.KONSERVENDOSE.get(), 5, munition9, 4));
            alle.add(angebot(ModRegistry.ELEKTRONIK.get(), 4, tacz("ammo", "AmmoId", "12g", 8), 4));
            ItemStack pistole = tacz("modern_kinetic_gun", "GunId", "glock_17", 1);
            alle.add(angebot(ModRegistry.ELEKTRONIK.get(), 16, ModRegistry.BATTERIE.get(), 6, pistole, 1));
        }
        Collections.shuffle(alle, new java.util.Random(getUUID().getLeastSignificantBits()));
        MerchantOffers angebote = getOffers();
        alle.stream().limit(6).forEach(angebote::add);
    }

    /**
     * Ruf bei Händlern: Stammkunden zahlen weniger (ab 25 Ansehen −10 %, ab 60 −25 %), Diebe und Mörder mehr
     * (unter −20: +25 %). Ab 50 Ansehen holt der Händler seine Ware „unter der Theke“ hervor.
     */
    @Override
    public void setTradingPlayer(Player p) {
        super.setTradingPlayer(p);
        if (p == null || level().isClientSide) {
            return;
        }
        int ruf = Ansehen.holen(p, sortiment.isEmpty() ? Fraktion.KARAWANE : Fraktion.ZUFLUCHT);
        float faktor = ruf >= 60 ? -0.25F : ruf >= 25 ? -0.10F : ruf <= -20 ? 0.25F : 0F;
        MerchantOffers angebote = getOffers();
        if (ruf >= 50 && angebote.stream().noneMatch(o -> o.getResult().is(de.deadzonemc.beute.Beute.NACHTSICHT.get()))) {
            angebote.add(angebot(ModRegistry.ELEKTRONIK.get(), 10, ModRegistry.BATTERIE.get(), 4,
                    new ItemStack(de.deadzonemc.beute.Beute.NACHTSICHT.get()), 1));
        }
        for (MerchantOffer o : angebote) {
            int basis = o.getBaseCostA().getCount();
            int diff = faktor < 0 ? -Math.max(1, Math.round(basis * -faktor)) : faktor > 0 ? Math.max(1, Math.round(basis * faktor)) : 0;
            if (basis <= 1 && diff < 0) {
                diff = 0;
            }
            o.resetSpecialPriceDiff();
            o.addToSpecialPriceDiff(diff);
        }
        String stufe = ruf >= 60 ? "haendler.deadzonemc.ruf.freund" : ruf >= 25 ? "haendler.deadzonemc.ruf.stamm"
                : ruf <= -20 ? "haendler.deadzonemc.ruf.dieb" : null;
        if (stufe != null) {
            p.displayClientMessage(net.minecraft.network.chat.Component.translatable(stufe)
                    .withStyle(ruf > 0 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED), true);
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer angebot) {
        super.rewardTradeXp(angebot);
        Player p = getTradingPlayer();
        if (p != null && !level().isClientSide) {
            Ansehen.aendern(p, sortiment.isEmpty() ? Fraktion.KARAWANE : Fraktion.ZUFLUCHT, 1);
        }
    }

    @Override
    public void die(net.minecraft.world.damagesource.DamageSource quelle) {
        super.die(quelle);
        if (!level().isClientSide && quelle.getEntity() instanceof Player p) {
            Ansehen.aendern(p, sortiment.isEmpty() ? Fraktion.KARAWANE : Fraktion.ZUFLUCHT, -50);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar c) {
        c.add(new AnimationController<>(this, "bewegung", 5, s -> s.setAndContinue(s.isMoving()
                ? RawAnimation.begin().thenLoop("animation.mensch.walk") : RawAnimation.begin().thenLoop("animation.mensch.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
