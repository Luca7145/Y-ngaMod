package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.LeonDeGuerra;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** El león de guerra (arquero o espadachín, mismo modelo y textura), con los ojos dorados que brillan. */
public class LeonRenderer extends MobRenderer<LeonDeGuerra, ModeloLeon> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/leon_de_guerra.png");
    private static final RenderType OJOS = RenderType.eyes(new ResourceLocation(YongaMain.MOD_ID, "textures/entity/leon_de_guerra_ojos.png"));

    public LeonRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloLeon(contexto.bakeLayer(ModeloLeon.CAPA)), 0.9f);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return OJOS;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LeonDeGuerra leon) {
        return TEXTURA;
    }
}
