package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.oficios.ParapenteItem;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Piernas quietas mientras se planea (si no, Minecraft las mueve como si corriera por el aire).
 * La llama PlayerModelMixin después de que Minecraft coloque el modelo. Los brazos van en ParapenteCliente.
 */
public final class PoseParapente {

    private PoseParapente() {}

    public static void aplicar(PlayerModel<?> modelo, LivingEntity entidad) {
        if (!entidad.isUsingItem() || !(entidad.getUseItem().getItem() instanceof ParapenteItem)) {
            return;
        }
        // Piernas juntas y casi rectas, un poco hacia atrás, colgando
        modelo.rightLeg.xRot = 0.15f;
        modelo.rightLeg.yRot = 0.0f;
        modelo.rightLeg.zRot = 0.03f;
        modelo.leftLeg.xRot = 0.15f;
        modelo.leftLeg.yRot = 0.0f;
        modelo.leftLeg.zRot = -0.03f;

        // Los pantalones de la skin copian la pose
        modelo.rightPants.copyFrom(modelo.rightLeg);
        modelo.leftPants.copyFrom(modelo.leftLeg);
    }
}
