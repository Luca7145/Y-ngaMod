package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.OrbeFe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dibuja el orbe de fe: la misma textura que el orbe de experiencia, pero teñida de azul o de negro.
 * (Copiado del dibujado del orbe de experiencia de Minecraft, cambiando los colores.)
 */
public class OrbeFeRenderer extends EntityRenderer<OrbeFe> {

    private static final ResourceLocation TEXTURA = new ResourceLocation("minecraft", "textures/entity/experience_orb.png");
    private static final RenderType TIPO_RENDER = RenderType.itemEntityTranslucentCull(TEXTURA);

    public OrbeFeRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.shadowRadius = 0.15f;
        this.shadowStrength = 0.75f;
    }

    // Brilla un poco aunque esté a oscuras, como la experiencia
    @Override
    protected int getBlockLightLevel(OrbeFe orbe, BlockPos posicion) {
        return Mth.clamp(super.getBlockLightLevel(orbe, posicion) + 7, 0, 15);
    }

    @Override
    public void render(OrbeFe orbe, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        pose.pushPose();

        // Qué dibujo del orbe usar (la textura tiene 16, de pequeño a grande)
        int icono = icono(Math.abs(orbe.valor()));
        float u0 = (icono % 4 * 16) / 64.0f;
        float u1 = (icono % 4 * 16 + 16) / 64.0f;
        float v0 = (icono / 4 * 16) / 64.0f;
        float v1 = (icono / 4 * 16 + 16) / 64.0f;

        // Color que "late" suavemente
        float t = (orbe.tickCount + parcial) / 2.0f;
        float pulso = (Mth.sin(t) + 1.0f) * 0.5f;
        int r, g, b, alfa;
        if (orbe.esOscuro()) {
            r = (int) (20 + pulso * 35);   // negro con un reflejo morado
            g = 5;
            b = (int) (25 + pulso * 45);
            alfa = 220;
        } else {
            r = (int) (40 + pulso * 60);   // azul
            g = (int) (120 + pulso * 60);
            b = 255;
            alfa = 128;
        }

        pose.translate(0.0f, 0.1f, 0.0f);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(0.3f, 0.3f, 0.3f);

        VertexConsumer vertices = buffers.getBuffer(TIPO_RENDER);
        PoseStack.Pose ultima = pose.last();
        Matrix4f matriz = ultima.pose();
        Matrix3f normal = ultima.normal();
        vertice(vertices, matriz, normal, -0.5f, -0.25f, r, g, b, alfa, u0, v1, luz);
        vertice(vertices, matriz, normal, 0.5f, -0.25f, r, g, b, alfa, u1, v1, luz);
        vertice(vertices, matriz, normal, 0.5f, 0.75f, r, g, b, alfa, u1, v0, luz);
        vertice(vertices, matriz, normal, -0.5f, 0.75f, r, g, b, alfa, u0, v0, luz);

        pose.popPose();
        super.render(orbe, yaw, parcial, pose, buffers, luz);
    }

    private static void vertice(VertexConsumer c, Matrix4f matriz, Matrix3f normal, float x, float y,
                                int r, int g, int b, int alfa, float u, float v, int luz) {
        c.vertex(matriz, x, y, 0.0f).color(r, g, b, alfa).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }

    // Tamaño del orbe según su valor
    private static int icono(float valor) {
        if (valor >= 10) return 3;
        if (valor >= 7) return 2;
        if (valor >= 3) return 1;
        return 0;
    }

    @Override
    public ResourceLocation getTextureLocation(OrbeFe orbe) {
        return TEXTURA;
    }
}
