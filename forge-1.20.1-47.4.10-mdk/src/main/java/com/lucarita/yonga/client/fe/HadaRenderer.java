package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Hada;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Hada: una bolita de luz que siempre mira a la cámara (como los orbes de experiencia), brilla aunque esté
 * a oscuras, sube y baja un poco y está animada: la textura es una tira vertical de fotogramas (la dibuja
 * herramientas/dibujar_hadas.py) con el halo que respira y destellos que giran. Al recibir un golpe se pone
 * roja, como cualquier mob.
 */
public class HadaRenderer extends EntityRenderer<Hada> {

    private static final int FOTOGRAMAS = 8;
    private static final int TICKS_POR_FOTOGRAMA = 3;

    private final ResourceLocation textura;
    private final RenderType tipoRender;

    public HadaRenderer(EntityRendererProvider.Context contexto, String nombreTextura) {
        super(contexto);
        this.textura = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/" + nombreTextura + ".png");
        this.tipoRender = RenderType.entityTranslucentEmissive(textura);
        this.shadowRadius = 0.0f;
    }

    public static HadaRenderer hada(EntityRendererProvider.Context contexto) {
        return new HadaRenderer(contexto, "hada");
    }

    public static HadaRenderer hadaOscura(EntityRendererProvider.Context contexto) {
        return new HadaRenderer(contexto, "hada_oscura");
    }

    @Override
    public void render(Hada hada, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        pose.pushPose();
        float t = hada.tickCount + parcial + hada.getId() * 7;
        pose.translate(0.0f, hada.getBbHeight() * 0.5f + Mth.sin(t * 0.12f) * 0.06f, 0.0f);
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
        pose.scale(0.7f, 0.7f, 0.7f);

        // Fotograma de la animación (cada hada va desfasada para que un grupo no parpadee a la vez)
        int fotograma = (int) (t / TICKS_POR_FOTOGRAMA) % FOTOGRAMAS;
        float v0 = fotograma / (float) FOTOGRAMAS;
        float v1 = (fotograma + 1) / (float) FOTOGRAMAS;

        int superposicion = OverlayTexture.pack(OverlayTexture.u(0.0f), OverlayTexture.v(hada.hurtTime > 0 || hada.deathTime > 0));
        VertexConsumer vertices = buffers.getBuffer(tipoRender);
        PoseStack.Pose ultima = pose.last();
        Matrix4f matriz = ultima.pose();
        Matrix3f normal = ultima.normal();
        int brillo = LightTexture.FULL_BRIGHT;
        vertice(vertices, matriz, normal, -0.5f, -0.5f, 0.0f, v1, superposicion, brillo);
        vertice(vertices, matriz, normal, 0.5f, -0.5f, 1.0f, v1, superposicion, brillo);
        vertice(vertices, matriz, normal, 0.5f, 0.5f, 1.0f, v0, superposicion, brillo);
        vertice(vertices, matriz, normal, -0.5f, 0.5f, 0.0f, v0, superposicion, brillo);

        pose.popPose();
        super.render(hada, yaw, parcial, pose, buffers, luz);
    }

    private static void vertice(VertexConsumer c, Matrix4f matriz, Matrix3f normal, float x, float y,
                                float u, float v, int superposicion, int luz) {
        c.vertex(matriz, x, y, 0.0f).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(superposicion).uv2(luz).normal(normal, 0.0f, 1.0f, 0.0f).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(Hada hada) {
        return textura;
    }
}
