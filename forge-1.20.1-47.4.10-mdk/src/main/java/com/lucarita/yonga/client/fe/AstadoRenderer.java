package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Astado;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** El Astado, con los ojos que brillan en la oscuridad (como los de las arañas o el enderman). */
public class AstadoRenderer extends MobRenderer<Astado, ModeloAstado> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/astado.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/astado_ojos.png"));

    public AstadoRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloAstado(contexto.bakeLayer(ModeloAstado.CAPA)), 0.8f);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return OJOS;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(Astado astado) {
        return TEXTURA;
    }
}
