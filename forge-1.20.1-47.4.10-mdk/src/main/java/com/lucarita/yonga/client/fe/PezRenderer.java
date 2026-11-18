package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.PezDelViento;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El Pez del viento (y sus ilusiones, que se ven exactamente igual). Se inclina según suba o baje (cabeceo),
 * se ladea al girar (alabeo) y, al morir, no se cae de lado: sube girando y se deshace en luz (lo hace la entidad).
 */
public class PezRenderer extends MobRenderer<PezDelViento, ModeloPez> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/pez_del_viento.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/pez_del_viento_ojos.png"));
    private static final float ESCALA = 1.5f;                 // más grande todavía que el modelo: ~10,5 bloques
    private static final float CENTRO = 1.75f * ESCALA;       // altura del centro del cuerpo (para girar sobre él)

    public PezRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloPez(contexto.bakeLayer(ModeloPez.CAPA)), 4.5f);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return OJOS;
            }
        });
    }

    @Override
    protected void setupRotations(PezDelViento pez, PoseStack pose, float edad, float giro, float parcial) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0f - giro));
        pose.translate(0.0f, CENTRO, 0.0f);
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(parcial, pez.xRotO, pez.getXRot())));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(parcial, pez.alabeo0, pez.alabeo)));
        pose.translate(0.0f, -CENTRO, 0.0f);
    }

    @Override
    protected void scale(PezDelViento pez, PoseStack pose, float parcial) {
        pose.scale(ESCALA, ESCALA, ESCALA);
    }

    @Override
    public ResourceLocation getTextureLocation(PezDelViento pez) {
        return TEXTURA;
    }
}
