package com.lucarita.yonga.mixin;

import com.lucarita.yonga.client.fe.PoseOracion;
import com.lucarita.yonga.client.roles.PoseParapente;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin: un "enganche" en el código de Minecraft.
 * Al FINAL de PlayerModel.setupAnim (donde Minecraft coloca brazos y piernas del jugador cada frame)
 * añadimos nuestras poses: rezar/blasfemar y planear con el parapente. Si no toca, no hacen nada.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void yonga$poseOracion(LivingEntity entidad, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, CallbackInfo ci) {
        PlayerModel<?> modelo = (PlayerModel<?>) (Object) this;
        PoseOracion.aplicar(modelo, entidad);
        PoseParapente.aplicar(modelo, entidad);
    }
}
