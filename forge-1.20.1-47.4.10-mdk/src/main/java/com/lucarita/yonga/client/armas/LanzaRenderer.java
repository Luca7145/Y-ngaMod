package com.lucarita.yonga.client.armas;

import com.lucarita.yonga.modulos.armas.LanzaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Dibuja la lanza en el aire (o clavada): su propio dibujo de objeto, girado para que la punta mire
 * hacia donde va (el dibujo del objeto tiene la punta arriba a la derecha, por eso el giro de -45º).
 */
public class LanzaRenderer extends EntityRenderer<LanzaEntity> {

    private final ItemRenderer objetos;

    public LanzaRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.objetos = contexto.getItemRenderer();
    }

    @Override
    public void render(LanzaEntity lanza, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(parcial, lanza.yRotO, lanza.getYRot()) - 90.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(parcial, lanza.xRotO, lanza.getXRot()) - 45.0f));
        pose.scale(1.1f, 1.1f, 1.1f);
        objetos.renderStatic(lanza.lanza(), ItemDisplayContext.NONE, luz, OverlayTexture.NO_OVERLAY, pose, buffers, lanza.level(), lanza.getId());
        pose.popPose();
        super.render(lanza, giro, parcial, pose, buffers, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(LanzaEntity lanza) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
