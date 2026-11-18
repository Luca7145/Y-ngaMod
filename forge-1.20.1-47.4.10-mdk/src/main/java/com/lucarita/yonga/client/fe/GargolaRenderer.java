package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Gargola;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** La gárgola, con los ojos rojos que brillan. Colgada de un saliente se dibuja boca abajo. */
public class GargolaRenderer extends MobRenderer<Gargola, ModeloGargola> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/gargola.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/gargola_ojos.png"));

    public GargolaRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloGargola(contexto.bakeLayer(ModeloGargola.CAPA)), 0.5f);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return OJOS;
            }
        });
    }

    @Override
    protected void setupRotations(Gargola gargola, PoseStack pose, float edad, float giro, float parcial) {
        if (gargola.colgada()) {
            // boca abajo, con las patas agarradas al saliente
            pose.translate(0.0f, gargola.getBbHeight(), 0.0f);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0f));
        }
        super.setupRotations(gargola, pose, edad, giro, parcial);
    }

    @Override
    public ResourceLocation getTextureLocation(Gargola gargola) {
        return TEXTURA;
    }
}
