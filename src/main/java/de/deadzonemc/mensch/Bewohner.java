package de.deadzonemc.mensch;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Bewohner einer Zuflucht: arbeitet, plaudert, erzählt Gerüchte aus der Deadzone. */
public class Bewohner extends Mensch {

    private static final String[] SAETZE = {
            "Schön, ein neues Gesicht. Die meisten, die hier ankommen, sind schon gebissen.",
            "Nachts nicht in die Städte. Da drin sind Hunderte von denen – und sie hören jeden Schritt.",
            "Wenn du einen Schreier siehst: zuerst den. Sonst kommt die ganze Straße.",
            "Die Wachen schießen auf alles, was nach Sonnenuntergang über die Mauer will. Merk dir das.",
            "Kopf. Immer auf den Kopf. Alles andere ist Munitionsverschwendung.",
            "Militärpatronen sind hier die Währung. Damit bekommst du beim Händler alles, was du brauchst.",
            "Im Nebel bewegt sich etwas Großes. Ich hab's gesehen. Es hatte kein Gesicht.",
            "Hinter der alten Tankstelle soll ein Versteck sein. Keiner traut sich hin.",
            "Wenn dein Geigerzähler rattert: umdrehen. Ohne Filter bist du in Minuten erledigt.",
            "Frag beim Auftraggeber nach Arbeit. Er zahlt gut, und er vergisst niemanden, der hilft.",
            "Die Halden-Werke haben das hier angerichtet. Station Null. Merk dir den Namen.",
            "Schlaf ruhig. Hier drin bist du sicher – solange die Wachen wach bleiben.",
            "Hörst du das Kratzen nachts? Das sind die Wühler. Bleib auf festem Stein.",
            "Die Hunde da draußen jagen im Rudel. Rücken an die Wand, dann kriegen sie dich nicht von hinten.",
    };

    public Bewohner(EntityType<? extends PathfinderMob> typ, Level level) {
        super(typ, level);
    }

    public static AttributeSupplier.Builder attribute() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public Fraktion fraktion() {
        return Fraktion.ZUFLUCHT;
    }

    @Override
    protected void getoetetVon(Player p) {
        Ansehen.aendern(p, Fraktion.ZUFLUCHT, -40);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.3));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10.0F, 1.0, 1.3));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player p, InteractionHand hand) {
        if (!level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            String satz = SAETZE[(int) ((getUUID().getLeastSignificantBits() >>> 1) + tickCount / 400) % SAETZE.length];
            p.sendSystemMessage(Component.literal("<").append(getDisplayName()).append("> ")
                    .withStyle(ChatFormatting.GRAY).append(Component.literal(satz).withStyle(ChatFormatting.WHITE)));
            getLookControl().setLookAt(p);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
