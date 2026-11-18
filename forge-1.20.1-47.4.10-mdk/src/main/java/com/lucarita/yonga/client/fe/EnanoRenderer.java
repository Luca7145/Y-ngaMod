package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Enano;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Enano: amarillo (luminoso) o rojo (oscuro). Siempre a plena luz, para que brille aunque esté a oscuras. */
public class EnanoRenderer extends MobRenderer<Enano, ModeloEnano> {

    private final ResourceLocation textura;
    private final float escala;

    public EnanoRenderer(EntityRendererProvider.Context contexto, String nombreTextura) {
        this(contexto, nombreTextura, 1.0f);
    }

    /** escala: 1 para los enanos, 3 para la enana madre (el mismo modelo, mucho más grande). */
    public EnanoRenderer(EntityRendererProvider.Context contexto, String nombreTextura, float escala) {
        super(contexto, new ModeloEnano(contexto.bakeLayer(ModeloEnano.CAPA)), 0.45f * escala);
        this.textura = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/" + nombreTextura + ".png");
        this.escala = escala;
    }

    public static EnanoRenderer madre(EntityRendererProvider.Context contexto) {
        return new EnanoRenderer(contexto, "enano_madre", 3.0f);
    }

    public static EnanoRenderer madreOscura(EntityRendererProvider.Context contexto) {
        return new EnanoRenderer(contexto, "enano_madre_oscuro", 3.0f);
    }

    @Override
    protected void scale(Enano enano, PoseStack pose, float parcial) {
        pose.scale(escala, escala, escala);
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
