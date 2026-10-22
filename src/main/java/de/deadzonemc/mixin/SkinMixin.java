package de.deadzonemc.mixin;

import de.deadzonemc.client.charakter.ClientCharakter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Spieler erscheinen so, wie sie sich im Charakter-Editor gebaut haben. */
@Mixin(AbstractClientPlayer.class)
public abstract class SkinMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void deadzonemc$charakter(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin eigen = ClientCharakter.skin(((AbstractClientPlayer) (Object) this).getUUID(), cir.getReturnValue());
        if (eigen != null) {
            cir.setReturnValue(eigen);
        }
    }
}
