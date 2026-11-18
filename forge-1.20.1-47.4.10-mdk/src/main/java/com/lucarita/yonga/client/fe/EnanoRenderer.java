package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Enano;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Enano: amarillo (luminoso) o rojo (oscuro). Siempre a plena luz, para que brille aunque esté a oscuras. */
public class EnanoRenderer extends MobRenderer<Enano, ModeloEnano> {

    private final ResourceLocation textura;

    public EnanoRenderer(EntityRendererProvider.Context contexto, String nombreTextura) {
        super(contexto, new ModeloEnano(contexto.bakeLayer(ModeloEnano.CAPA)), 0.45f);
        this.textura = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/" + nombreTextura + ".png");
    }

    public static EnanoRenderer enano(EntityRendererProvider.Context contexto) {
        return new EnanoRenderer(contexto, "enano");
    }

    public static EnanoRenderer enanoOscuro(EntityRendererProvider.Context contexto) {
        return new EnanoRenderer(contexto, "enano_oscuro");
    }

    @Override
    protected int getBlockLightLevel(Enano enano, BlockPos posicion) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(Enano enano) {
        return textura;
    }
}
