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
 * Dibuja el imán colgado del cuello: un cordón fino en V desde los hombros y, en medio del pecho, la herradura roja
 * con las puntas plateadas hacia abajo. Medidas en píxeles del modelo: el pecho va de x -4 a 4, de y 0 (hombros) a
 * 12, y su frente está en z = -2. La textura: arriba el cordón, en medio el rojo, abajo la plata.
 */
public class CapaIman extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "iman"), "main");
    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/iman.png");

    private final ModelPart iman;

    public CapaIman(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.iman = modelos.bakeLayer(CAPA);
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        // El cordón: trocitos finos de cada hombro al centro
        CubeListBuilder cordon = CubeListBuilder.create().texOffs(0, 0);
        int trozos = 6;
        for (int i = 0; i < trozos; i++) {
            float t = (float) i / trozos;
            float y = 0.2f + t * 4.2f;
            float x = 3.4f * (1 - t);
            cordon.addBox(-x - 0.3f, y - 0.3f, -0.3f, 0.6f, 0.9f, 0.6f);
            cordon.addBox(x - 0.3f, y - 0.3f, -0.3f, 0.6f, 0.9f, 0.6f);
        }
        raiz.addOrReplaceChild("cordon", cordon, PartPose.offset(0, 0, -2.4f));
        // La herradura: el arco arriba, dos brazos rojos y las puntas plateadas
        raiz.addOrReplaceChild("iman", CubeListBuilder.create()
                        .texOffs(0, 4).addBox(-1.5f, 4.5f, -0.5f, 3, 1, 1)          // arco
                        .texOffs(0, 4).addBox(-1.5f, 5.5f, -0.5f, 1, 2, 1)          // brazo izquierdo
                        .texOffs(0, 4).addBox(0.5f, 5.5f, -0.5f, 1, 2, 1)           // brazo derecho
                        .texOffs(0, 12).addBox(-1.5f, 7.5f, -0.5f, 1, 1, 1)         // punta izquierda
                        .texOffs(0, 12).addBox(0.5f, 7.5f, -0.5f, 1, 1, 1),         // punta derecha
                PartPose.offset(0, 0, -2.6f));
        return LayerDefinition.create(malla, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        if (!CaraCliente.obtener(jugador.getId()).is(ModuloRoles.IMAN.get())) {
            return;
        }
        pose.pushPose();
        getParentModel().body.translateAndRotate(pose);
        iman.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA)), luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
