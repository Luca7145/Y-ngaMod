package com.lucarita.yonga.mixin;

import com.lucarita.yonga.client.roles.EscanerCliente;
import com.lucarita.yonga.modulos.roles.oficios.EscanerItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El escáner se lleva en la mano como el mapa de Minecraft: se usan las mismas funciones del mapa (con las dos manos
 * si la otra está libre, en una si no; al mirar hacia abajo se acerca), pero en vez del papel del mapa se dibuja la
 * tablet con el sonar (EscanerCliente.dibujarTablet).
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private void renderTwoHandedMap(PoseStack pose, MultiBufferSource buffers, int luz, float cabeceo, float equipar, float golpe) {
    }

    @Shadow
    private void renderOneHandedMap(PoseStack pose, MultiBufferSource buffers, int luz, float equipar, HumanoidArm brazo,
                                    float golpe, ItemStack objeto) {
    }

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void yonga$escanerComoMapa(AbstractClientPlayer jugador, float parcial, float cabeceo, InteractionHand mano,
                                       float golpe, ItemStack objeto, float equipar, PoseStack pose,
                                       MultiBufferSource buffers, int luz, CallbackInfo ci) {
        if (!(objeto.getItem() instanceof EscanerItem) || jugador.isScoping()) {
            return;
        }
        boolean principal = mano == InteractionHand.MAIN_HAND;
        HumanoidArm brazo = principal ? jugador.getMainArm() : jugador.getMainArm().getOpposite();
        pose.pushPose();
        if (principal && this.offHandItem.isEmpty()) {
            renderTwoHandedMap(pose, buffers, luz, cabeceo, equipar, golpe);
        } else {
            renderOneHandedMap(pose, buffers, luz, equipar, brazo, golpe, objeto);
        }
        pose.popPose();
        ci.cancel();
    }

    /** Donde el mapa dibuja su papel, el escáner dibuja la tablet (con las mismas medidas: de -7 a 135 "píxeles"). */
    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void yonga$dibujarTablet(PoseStack pose, MultiBufferSource buffers, int luz, ItemStack objeto, CallbackInfo ci) {
        if (!(objeto.getItem() instanceof EscanerItem)) {
            return;
        }
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        pose.scale(0.38f, 0.38f, 0.38f);
        pose.translate(-0.5f, -0.5f, 0.0f);
        pose.scale(0.0078125f, 0.0078125f, 0.0078125f);
        EscanerCliente.dibujarTablet(pose, buffers, luz, objeto);
        ci.cancel();
    }
}
