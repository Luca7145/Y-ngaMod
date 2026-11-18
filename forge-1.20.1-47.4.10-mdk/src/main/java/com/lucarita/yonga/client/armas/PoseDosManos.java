package com.lucarita.yonga.client.armas;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.BooleanSupplier;

/**
 * Pose de las armas a dos manos (mandoble): el personaje lo coge con las dos manos, por delante y un poco
 * hacia abajo, como se lleva una espada grande. La otra mano agarra la empuñadura justo debajo de la
 * primera. El golpe (el balanceo del brazo al atacar) se suma encima, como con cualquier arma.
 */
public final class PoseDosManos {

    public static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("YONGA_DOS_MANOS", true,
            (modelo, entidad, brazo) -> {
                boolean derecho = brazo == HumanoidArm.RIGHT;
                ModelPart principal = derecho ? modelo.rightArm : modelo.leftArm;
                ModelPart otro = derecho ? modelo.leftArm : modelo.rightArm;
                float lado = derecho ? 1.0f : -1.0f;
                // Mira un poco hacia donde mira la cabeza (como al apuntar), pero sin exagerar
                float cabeza = Mth.clamp(modelo.head.xRot, -0.6f, 0.6f) * 0.5f;
                principal.xRot = -0.75f + cabeza;
                principal.yRot = -0.25f * lado;
                principal.zRot = 0.0f;
                otro.xRot = -0.95f + cabeza;
                otro.yRot = 0.65f * lado;
                otro.zRot = 0.0f;
            });

    /** Lo que se le da a Minecraft: usa la pose si el arma es a dos manos y va en la mano principal. */
    public static IClientItemExtensions para(BooleanSupplier esDosManos) {
        return new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entidad, InteractionHand mano, ItemStack objeto) {
                return mano == InteractionHand.MAIN_HAND && esDosManos.getAsBoolean() ? POSE : null;
            }
        };
    }

    private PoseDosManos() {}
}
