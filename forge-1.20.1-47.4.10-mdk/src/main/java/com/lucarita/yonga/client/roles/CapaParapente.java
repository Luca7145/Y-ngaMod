package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.oficios.ParapenteItem;
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
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Dibuja la vela del parapente encima del jugador mientras planea.
 *
 * Es una "capa" que se añade al dibujado de los jugadores (como la capa o las élitras).
 * La vela va enganchada al cuerpo: si el jugador se inclina, la vela también.
 *
 * Medidas en píxeles del modelo (16 = 1 bloque). El eje Y va hacia ABAJO: -22 es por encima de la cabeza.
 */
public class CapaParapente extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "parapente"), "main");
    private static final ResourceLocation TEXTURA = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/parapente.png");

    private final ModelPart vela;

    public CapaParapente(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.vela = modelos.bakeLayer(CAPA);
    }

    // Dónde están las manos (juntas, encima de la cabeza, un poco por delante). Las cuerdas bajan hasta aquí.
    private static final float MANOS_X = 0.0f;
    private static final float MANOS_Y = -9.5f;
    private static final float MANOS_Z = -2.5f;

    // Altura del centro de la vela (más negativo = más alta). La cabeza va de -8 a 0.
    private static final float ALTURA_VELA = -17.0f;

    /**
     * La forma de la vela: un arco de 5 tramos (centro + 2 a cada lado que van cayendo) de unos 2,4 bloques
     * de ancho, y 4 cuerdas que bajan desde las esquinas del tramo central hasta las manos.
     */
    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        // Tramo central, plano, por encima de la cabeza
        PartDefinition centro = raiz.addOrReplaceChild("centro",
                CubeListBuilder.create().texOffs(0, 0).addBox(-7.0f, -1.0f, -6.0f, 14, 1, 12),
                PartPose.offset(0.0f, ALTURA_VELA, 0.0f));

        // Lado derecho: dos tramos que van cayendo (cada uno cuelga del anterior)
        PartDefinition derecha1 = centro.addOrReplaceChild("derecha1",
                CubeListBuilder.create().texOffs(0, 13).addBox(0.0f, -1.0f, -6.0f, 9, 1, 12),
                PartPose.offsetAndRotation(7.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.3f));
        derecha1.addOrReplaceChild("derecha2",
                CubeListBuilder.create().texOffs(0, 26).addBox(0.0f, -1.0f, -6.0f, 7, 1, 12),
                PartPose.offsetAndRotation(9.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.75f));

        // Lado izquierdo, igual pero al revés
        PartDefinition izquierda1 = centro.addOrReplaceChild("izquierda1",
                CubeListBuilder.create().texOffs(0, 13).addBox(-9.0f, -1.0f, -6.0f, 9, 1, 12),
                PartPose.offsetAndRotation(-7.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.3f));
        izquierda1.addOrReplaceChild("izquierda2",
                CubeListBuilder.create().texOffs(0, 26).addBox(-7.0f, -1.0f, -6.0f, 7, 1, 12),
                PartPose.offsetAndRotation(-9.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.75f));

        // Cuerdas: de las 4 esquinas del tramo central hasta las manos (se juntan en el centro)
        cuerda(raiz, "cuerda_der_delante", 6.0f, -4.0f);
        cuerda(raiz, "cuerda_der_detras", 6.0f, 4.0f);
        cuerda(raiz, "cuerda_izq_delante", -6.0f, -4.0f);
        cuerda(raiz, "cuerda_izq_detras", -6.0f, 4.0f);

        return LayerDefinition.create(malla, 64, 64);
    }

    /**
     * Una cuerda recta desde (x, debajo de la vela, z) hasta las manos.
     * Se dibuja como una caja finita de 0,5 píxeles hacia "abajo" y se gira para que apunte a las manos.
     */
    private static void cuerda(PartDefinition raiz, String nombre, float x, float z) {
        float y = ALTURA_VELA + 0.5f;
        float dx = MANOS_X - x;
        float dy = MANOS_Y - y;
        float dz = MANOS_Z - z;
        float largo = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        // Giros para que una caja que apunta hacia abajo (eje Y) apunte hacia las manos
        float giroX = (float) Math.asin(dz / largo);
        float giroZ = (float) Math.atan2(-dx, dy);
        raiz.addOrReplaceChild(nombre,
                CubeListBuilder.create().texOffs(0, 40).addBox(-0.25f, 0.0f, -0.25f, 0.5f, largo, 0.5f),
                PartPose.offsetAndRotation(x, y, z, giroX, 0.0f, giroZ));
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        if (!jugador.isUsingItem() || !(jugador.getUseItem().getItem() instanceof ParapenteItem)) {
            return;
        }
        pose.pushPose();
        // Engancha la vela al cuerpo del jugador
        getParentModel().body.translateAndRotate(pose);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
        vela.render(pose, vertices, luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
