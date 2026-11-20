package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.rey.EsqueletoRey;
import com.lucarita.yonga.modulos.fe.mobs.rey.ReyDeArenisca;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Rey de Arenisca. Calcula su esqueleto (el mismo que usa el servidor para las cajas de golpe y los asideros) y
 * dibuja cada pieza de ModeloRey con la matriz de su hueso. Encima, la capa que brilla: la cara, las runas y los
 * sellos abiertos (el de la coronilla no brilla hasta que se abre; los rotos quedan apagados).
 */
public class ReyRenderer extends EntityRenderer<ReyDeArenisca> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/rey_arenisca.png");
    private static final ResourceLocation BRILLO = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/rey_arenisca_brillo.png");
    private static final float ESCALA = 8.0f;

    private final ModeloRey modelo;
    private final EsqueletoRey esqueleto = new EsqueletoRey();

    public ReyRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
        this.modelo = new ModeloRey(contexto.bakeLayer(ModeloRey.CAPA));
        this.shadowRadius = 6.0f;
    }

    @Override
    public void render(ReyDeArenisca rey, float giro, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        esqueleto.calcular(rey.estado(parcial));
        double ex = Mth.lerp(parcial, rey.xo, rey.getX());
        double ey = Mth.lerp(parcial, rey.yo, rey.getY());
        double ez = Mth.lerp(parcial, rey.zo, rey.getZ());
        // la luz del pecho (es tan alto que la de los pies no sirve)
        int luzRey = Math.max(luz, LevelRenderer.getLightColor(rey.level(), BlockPos.containing(ex, ey + 25, ez)));
        int superposicion = LivingEntityRenderer.getOverlayCoords(rey, 0.0f);
        float pulso = 0.75f + 0.25f * Mth.sin((rey.tickCount + parcial) * 0.15f);

        for (int capa = 0; capa < 2; capa++) {
            VertexConsumer vertices = buffers.getBuffer(capa == 0 ? RenderType.entityCutoutNoCull(TEXTURA) : RenderType.eyes(BRILLO));
            int l = capa == 0 ? luzRey : 0xF000F0;
            int o = capa == 0 ? superposicion : OverlayTexture.NO_OVERLAY;
            for (int b = 0; b < EsqueletoRey.N; b++) {
                dibujar(pose, modelo.huesos[b], b, 0, 0, 0, esqueleto.ox - ex, esqueleto.oy - ey, esqueleto.oz - ez, vertices, l, o, 1, 1, 1);
            }
            for (int s = 0; s < 3; s++) {
                boolean roto = rey.selloRoto(s), abierto = rey.selloAbierto(s);
                if (capa == 1 && !abierto) continue;
                float[] p = EsqueletoRey.SELLO_SITIO[s];
                float c = capa == 1 ? pulso : 1;
                dibujar(pose, roto ? modelo.selloRoto : modelo.selloVivo, EsqueletoRey.SELLO_HUESO[s], p[0], p[1], p[2],
                        esqueleto.ox - ex, esqueleto.oy - ey, esqueleto.oz - ez, vertices, l, o, c, c, c);
            }
        }
        super.render(rey, giro, parcial, pose, buffers, luz);
    }

    private void dibujar(PoseStack pose, ModelPart pieza, int hueso, float x, float y, float z, double dx, double dy, double dz,
                         VertexConsumer vertices, int luz, int superposicion, float r, float g, float b) {
        pose.pushPose();
        pose.translate(dx, dy, dz);
        pose.mulPoseMatrix(esqueleto.huesos[hueso]);
        pose.translate(x, y, z);
        pose.scale(-ESCALA, -ESCALA, ESCALA);
        pieza.render(pose, vertices, luz, superposicion, r, g, b, 1.0f);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ReyDeArenisca rey) {
        return TEXTURA;
    }
}
