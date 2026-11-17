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
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
 * Dibuja el antifaz en la cara del jugador (lo ven él en tercera persona y los demás).
 *
 * Es una banda negra a la altura de los ojos, con dos agujeros para que se vean los ojos de la skin,
 * y una cinta que rodea la cabeza. Va enganchada a la cabeza: se mueve con ella.
 * Medidas en píxeles del modelo; la cabeza va de x -4 a 4, de y -8 a 0 y de z -4 a 4 (la cara mira a -z).
 */
public class CapaAntifaz extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "antifaz"), "main");
    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/antifaz.png");

    // Un poco hacia fuera (en anchura y profundidad, no en altura) para quedar por encima de la segunda capa
    // de la skin, que sobresale 0,5
    private static final CubeDeformation HOLGURA = new CubeDeformation(0.6f, 0.0f, 0.6f);

    private final ModelPart antifaz;

    public CapaAntifaz(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.antifaz = modelos.bakeLayer(CAPA);
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        // Banda delantera: 8 de ancho y 3 de alto (de y -5 a -2), pegada a la cara.
        // Los ojos de la skin están en la fila del medio (y -4 a -3): ahí la textura tiene agujeros.
        raiz.addOrReplaceChild("delante",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -5.0f, -4.0f, 8, 3, 0, HOLGURA),
                PartPose.ZERO);

        // Cinta alrededor de la cabeza (lados y nuca), a la altura de los ojos
        raiz.addOrReplaceChild("lado_derecho",
                CubeListBuilder.create().texOffs(0, 8).addBox(-4.0f, -4.0f, -4.0f, 0, 1, 8, HOLGURA),
                PartPose.ZERO);
        raiz.addOrReplaceChild("lado_izquierdo",
                CubeListBuilder.create().texOffs(0, 8).addBox(4.0f, -4.0f, -4.0f, 0, 1, 8, HOLGURA),
                PartPose.ZERO);
        raiz.addOrReplaceChild("nuca",
                CubeListBuilder.create().texOffs(0, 20).addBox(-4.0f, -4.0f, 4.0f, 8, 1, 0, HOLGURA),
                PartPose.ZERO);

        return LayerDefinition.create(malla, 32, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        if (!CaraCliente.obtener(jugador.getId()).is(ModuloRoles.ANTIFAZ.get())) {
            return;
        }
        pose.pushPose();
        // Engancha el antifaz a la cabeza del jugador
        getParentModel().head.translateAndRotate(pose);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
        antifaz.render(pose, vertices, luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
