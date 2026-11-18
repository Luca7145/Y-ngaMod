package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Duende;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * El duende, con la textura de su máscara (veneno, náusea o lentitud) y los ojos de la máscara brillando.
 * Cuando está invisible no se ve nada (ni los ojos).
 */
public class DuendeRenderer extends MobRenderer<Duende, ModeloDuende> {

    private static final String[] NOMBRES = {"veneno", "nausea", "lentitud"};
    private static final ResourceLocation[] TEXTURAS = new ResourceLocation[3];
    private static final RenderType[] OJOS = new RenderType[3];

    static {
        for (int i = 0; i < 3; i++) {
            TEXTURAS[i] = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/duende_" + NOMBRES[i] + ".png");
            OJOS[i] = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/duende_" + NOMBRES[i] + "_ojos.png"));
        }
    }

    public DuendeRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloDuende(contexto.bakeLayer(ModeloDuende.CAPA)), 0.35f);
        this.addLayer(new Ojos(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Duende duende) {
        return TEXTURAS[Math.floorMod(duende.tipo(), 3)];
    }

    private static class Ojos extends RenderLayer<Duende, ModeloDuende> {
        Ojos(RenderLayerParent<Duende, ModeloDuende> padre) {
            super(padre);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int luz, Duende duende, float paso, float cantidadPaso,
                           float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
            if (duende.isInvisible()) {
                return;
            }
            this.getParentModel().renderToBuffer(pose, buffers.getBuffer(OJOS[Math.floorMod(duende.tipo(), 3)]),
                    15728640, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
