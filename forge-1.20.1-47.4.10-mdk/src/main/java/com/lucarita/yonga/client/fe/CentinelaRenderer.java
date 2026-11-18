package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Centinela;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El centinela de piedra, con la cara de la calabaza iluminada por dentro. Al morir no se cae de lado como los
 * demás mobs: se queda de pie, congelado, y parpadea en blanco cada vez más rápido (como un creeper) hasta
 * que explota.
 */
public class CentinelaRenderer extends MobRenderer<Centinela, ModeloCentinela> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/centinela.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/centinela_ojos.png"));

    public CentinelaRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloCentinela(contexto.bakeLayer(ModeloCentinela.CAPA)), 1.0f);
        // La cara brilla, salvo cuando está agotado tras el rayo (se le apagan los ojos)
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return OJOS;
            }

            @Override
            public void render(PoseStack pose, net.minecraft.client.renderer.MultiBufferSource buffers, int luz, Centinela centinela,
                               float paso, float cantidadPaso, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
                if (centinela.accion() != Centinela.AGOTADO) {
                    super.render(pose, buffers, luz, centinela, paso, cantidadPaso, parcial, edad, giroCabeza, inclinacionCabeza);
                }
            }
        });
    }

    @Override
    protected void setupRotations(Centinela centinela, PoseStack pose, float edad, float giro, float parcial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - giro));   // sin la caída de lado al morir
    }

    @Override
    protected float getWhiteOverlayProgress(Centinela centinela, float parcial) {
        if (centinela.deathTime <= 0) {
            return 0.0f;
        }
        float d = (centinela.deathTime + parcial) / Centinela.TICKS_MORIR;
        float velocidad = 2.0f + d * 10.0f;
        return (int) ((centinela.deathTime + parcial) * velocidad / 10.0f) % 2 == 0 ? 0.0f : Mth.clamp(d, 0.3f, 1.0f);
    }

    @Override
    public ResourceLocation getTextureLocation(Centinela centinela) {
        return TEXTURA;
    }
}
