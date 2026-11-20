package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja el rosario colgado del cuello: dos hileras de cuentas en V desde los hombros hasta el centro del pecho,
 * y una cruz de oro colgando debajo. Medidas en píxeles del modelo: el pecho va de x -4 a 4, de y 0 (hombros) a 12,
 * y su frente está en z = -2.
 */
public class CapaRosario extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "rosario"), "main");
    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/rosario.png");

    private final ModelPart rosario;

    public CapaRosario(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.rosario = modelos.bakeLayer(CAPA);
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        // Cuentas (madera, arriba de la textura): de cada hombro al centro del pecho
        CubeListBuilder cuentas = CubeListBuilder.create().texOffs(0, 0);
        int numero = 5;
        for (int i = 0; i < numero; i++) {
            float t = (float) i / numero;
            float y = 0.3f + t * 5.0f;
            float x = 3.2f * (1 - t);
            cuentas.addBox(-x - 0.5f, y - 0.5f, -0.5f, 1, 1, 1);
            cuentas.addBox(x - 0.5f, y - 0.5f, -0.5f, 1, 1, 1);
        }
        cuentas.addBox(-0.5f, 4.8f, -0.5f, 1, 1, 1);          // la del centro, donde se junta con la cruz
        raiz.addOrReplaceChild("cuentas", cuentas, PartPose.offset(0, 0, -2.6f));
        // Cruz (oro, abajo de la textura)
        raiz.addOrReplaceChild("cruz", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-0.5f, 5.5f, -0.5f, 1, 4, 1)
                        .texOffs(8, 8).addBox(-1.5f, 6.5f, -0.5f, 3, 1, 1),
                PartPose.offset(0, 0, -2.6f));
        return LayerDefinition.create(malla, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        if (!CaraCliente.obtener(jugador.getId()).is(ModuloRoles.ROSARIO.get())) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        rosario.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA)), luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
