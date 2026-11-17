package com.lucarita.yonga.client.roles;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Parte visual del parapente: mientras planeas, los dos brazos van estirados hacia arriba
 * con las manos juntas, agarrando las cuerdas de la vela.
 */
public final class ParapenteCliente {

    // Pose nueva de brazos: "a dos manos", ambos brazos casi verticales y con las manos juntas en el centro
    private static final HumanoidModel.ArmPose POSE_PLANEO = HumanoidModel.ArmPose.create("YONGA_PARAPENTE", true,
            (modelo, entidad, brazo) -> {
                modelo.rightArm.xRot = -2.9f;
                modelo.rightArm.yRot = 0.0f;
                modelo.rightArm.zRot = 0.33f;   // con el brazo levantado, positivo = hacia el centro
                modelo.leftArm.xRot = -2.9f;
                modelo.leftArm.yRot = 0.0f;
                modelo.leftArm.zRot = -0.33f;
            });

    public static final IClientItemExtensions EXTENSIONES = new IClientItemExtensions() {
        @Override
        public @Nullable HumanoidModel.ArmPose getArmPose(LivingEntity entidad, InteractionHand mano, ItemStack stack) {
            return entidad.isUsingItem() && entidad.getUseItem() == stack ? POSE_PLANEO : null;
        }
    };

    private ParapenteCliente() {}
}
