package de.deadzonemc.client.kino;

import de.deadzonemc.kino.KinoFigur;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Spielermodell mit den Haltungen aus dem Intro: sitzen (angeschnallt) und schweben (Arme treiben auseinander). */
public class KinoModell extends PlayerModel<KinoFigur> {

    public KinoModell(ModelPart wurzel, boolean schmal) {
        super(wurzel, schmal);
    }

    @Override
    public void setupAnim(KinoFigur f, float lauf, float laufMenge, float alter, float kopfGier, float kopfNicken) {
        super.setupAnim(f, 0, 0, alter, kopfGier, kopfNicken);
        float arme = Mth.lerp(Mth.frac(alter), f.armeO, f.arme);
        if (f.haltung == KinoFigur.SITZEN) {
            rightArm.xRot = -0.62831855F + Mth.sin(alter * 0.07F) * 0.03F;
            leftArm.xRot = -0.62831855F - Mth.sin(alter * 0.07F) * 0.03F;
            rightArm.zRot = 0.08F;
            leftArm.zRot = -0.08F;
            rightLeg.xRot = -1.4137167F;
            rightLeg.yRot = 0.31415927F;
            rightLeg.zRot = 0.07853982F;
            leftLeg.xRot = -1.4137167F;
            leftLeg.yRot = -0.31415927F;
            leftLeg.zRot = -0.07853982F;
        } else if (f.haltung == KinoFigur.SCHWEBEN) {
            float w = Mth.sin(alter * 0.05F + f.getId());
            rightArm.xRot = -0.3F + w * 0.2F;
            leftArm.xRot = -0.25F - w * 0.2F;
            rightArm.zRot = 0.2F + 1.1F * arme + w * 0.1F;
            leftArm.zRot = -0.2F - 1.1F * arme - w * 0.1F;
            rightLeg.xRot = 0.2F * w - 0.2F;
            leftLeg.xRot = -0.2F * w + 0.1F;
            rightLeg.zRot = 0.25F * arme;
            leftLeg.zRot = -0.25F * arme;
        }
        hat.copyFrom(head);
        jacket.copyFrom(body);
        rightSleeve.copyFrom(rightArm);
        leftSleeve.copyFrom(leftArm);
        rightPants.copyFrom(rightLeg);
        leftPants.copyFrom(leftLeg);
    }
}
