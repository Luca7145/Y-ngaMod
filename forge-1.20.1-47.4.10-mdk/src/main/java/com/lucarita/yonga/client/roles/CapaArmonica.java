package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja la armónica en la boca del jugador (lo ven él en tercera persona y los demás).
 *
 * Es una barrita de 6 de ancho, 2 de alto y 2 de fondo, a la altura de la boca, que sobresale de la cara
 * (por delante de la segunda capa de la skin). Va enganchada a la cabeza: se mueve con ella.
 * Medidas en píxeles del modelo; la cabeza va de x -4 a 4, de y -8 a 0 y de z -4 a 4 (la cara mira a -z).
 *
 * Textura: textures/entity/armonica.png (16x16). Partes (x, y, ancho, alto):
 *   arriba (2,0,6,2) · abajo (8,0,6,2) · lado derecho (0,2,2,2) · DELANTE con los agujeros (2,2,6,2)
 *   · lado izquierdo (8,2,2,2) · detrás (10,2,6,2)
 */
public class CapaArmonica extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "armonica"), "main");
    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/armonica.png");

    private final ModelPart armonica;

    public CapaArmonica(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.armonica = modelos.bakeLayer(CAPA);
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        // Boca: filas y -2 a -1 de la cara. Del fondo: de z -5,5 (fuera) a -3,5 (metida en la cabeza)
        malla.getRoot().addOrReplaceChild("armonica",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -2.5f, -5.5f, 6, 2, 2),
                PartPose.ZERO);
        return LayerDefinition.create(malla, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        if (!CaraCliente.obtener(jugador.getId()).is(ModuloRoles.ARMONICA.get())) {
            return;
        }
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
        armonica.render(pose, vertices, luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
