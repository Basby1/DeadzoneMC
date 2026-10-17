package de.deadzonemc.mensch;

import de.deadzonemc.quest.Auftraege;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Auftraggeber einer Zuflucht: vergibt Aufträge und nimmt erledigte entgegen (Rechtsklick). */
public class Auftraggeber extends Mensch {

    public Auftraggeber(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.22);
    }

    @Override
    public Fraktion fraktion() {
        return Fraktion.ZUFLUCHT;
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, Fraktion.ZUFLUCHT, -60);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, 0.7));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F, 0.6F));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.4));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (p instanceof ServerPlayer sp) {
            if (Ansehen.krieg(sp, Fraktion.ZUFLUCHT)) {
                sp.sendSystemMessage(net.minecraft.network.chat.Component.literal("„Verschwinde. Für Leute wie dich haben wir nichts.“")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            } else {
                getLookControl().setLookAt(sp);
                Auftraege.sprechen(sp, this);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
