package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Gnomo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** El gnomo espadachín. */
public class GnomoRenderer extends MobRenderer<Gnomo, ModeloGnomo> {

    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/gnomo.png");

    public GnomoRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ModeloGnomo(contexto.bakeLayer(ModeloGnomo.CAPA)), 0.3f);
    }

    @Override
    public ResourceLocation getTextureLocation(Gnomo gnomo) {
        return TEXTURA;
    }
}
