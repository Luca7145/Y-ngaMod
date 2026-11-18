package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Dardo;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** El dardo: como una flecha pero más pequeño, con las plumas del color de su efecto. */
public class DardoRenderer extends ArrowRenderer<Dardo> {

    private static final ResourceLocation[] TEXTURAS = {
            new ResourceLocation(YongaMain.MOD_ID, "textures/entity/dardo_veneno.png"),
            new ResourceLocation(YongaMain.MOD_ID, "textures/entity/dardo_nausea.png"),
            new ResourceLocation(YongaMain.MOD_ID, "textures/entity/dardo_lentitud.png")};

    public DardoRenderer(EntityRendererProvider.Context contexto) {
        super(contexto);
    }

    @Override
    public void render(Dardo dardo, float yaw, float parcial, PoseStack pose, MultiBufferSource buffers, int luz) {
        pose.pushPose();
        pose.scale(0.6f, 0.6f, 0.6f);
        super.render(dardo, yaw, parcial, pose, buffers, luz);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Dardo dardo) {
        return TEXTURAS[Math.floorMod(dardo.tipo(), 3)];
    }
}
