package com.lucarita.yonga.client;

import com.lucarita.yonga.core.estante.EstanteBlock;
import com.lucarita.yonga.core.estante.EstanteBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Dibuja lo que hay en un estante: los tres objetos en fila delante de la tabla del fondo, entre los dos listones,
 * como en un marco (la 1.20.1 no tiene la transformación "on_shelf" de vanilla: se usa la del marco, más pequeña).
 */
public class EstanteRenderer implements BlockEntityRenderer<EstanteBlockEntity> {

    /** Tamaño de cada objeto (el del marco es 0.5). */
    private static final float ESCALA = 0.35f;
    /** Separación entre ranuras: un tercio del bloque. */
    private static final float SEPARACION = 1.0f / 3.0f;
    /** Lo cerca de la tabla del fondo (en el centro del hueco entre los listones). */
    private static final float FONDO = 0.25f;

    public EstanteRenderer(BlockEntityRendererProvider.Context contexto) {
    }

    @Override
    public void render(EstanteBlockEntity estante, float parcial, PoseStack pose, MultiBufferSource buffers, int luz, int superposicion) {
        Level nivel = estante.getLevel();
        if (nivel == null) return;
        Direction frente = estante.getBlockState().getValue(EstanteBlock.FACING);
        int luzDelante = LevelRenderer.getLightColor(nivel, estante.getBlockPos().relative(frente));
        int semilla = (int) estante.getBlockPos().asLong();
        for (int r = 0; r < EstanteBlockEntity.RANURAS; r++) {
            ItemStack objeto = estante.getItem(r);
            if (objeto.isEmpty()) continue;
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(180.0f - frente.toYRot()));   // como un marco que mira hacia delante
            // ranura 0 a la izquierda (vista de frente): +x en este espacio
            pose.translate((1 - r) * SEPARACION, 0.0, FONDO);
            pose.scale(ESCALA, ESCALA, ESCALA);
            Minecraft.getInstance().getItemRenderer().renderStatic(objeto, ItemDisplayContext.FIXED, luzDelante,
                    OverlayTexture.NO_OVERLAY, pose, buffers, nivel, semilla + r);
            pose.popPose();
        }
    }
}
