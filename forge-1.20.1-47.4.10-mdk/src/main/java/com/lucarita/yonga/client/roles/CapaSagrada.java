package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Dibuja el HALO (un aro dorado que brilla, flotando sobre la cabeza y girando despacio) o los CUERNOS (dos cuernos
 * que salen de la frente y se curvan hacia atrás) del Sacerdote. Van enganchados a la cabeza.
 * Medidas en píxeles del modelo: la cabeza va de y -8 a 0 y la cara mira a -z.
 */
public class CapaSagrada extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "sagrado"), "main");
    private static final ResourceLocation TEXTURA_HALO = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/halo.png");
    private static final ResourceLocation TEXTURA_CUERNOS = new ResourceLocation(YongaMain.MOD_ID, "textures/entity/cuernos.png");
    private static final int TRAMOS_HALO = 10;
    private static final float RADIO_HALO = 4.6f;

    private final ModelPart halo, cuernos;

    public CapaSagrada(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        ModelPart raiz = modelos.bakeLayer(CAPA);
        this.halo = raiz.getChild("halo");
        this.cuernos = raiz.getChild("cuernos");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        // El halo: un aro de 10 tramos alrededor del eje vertical
        PartDefinition halo = raiz.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.ZERO);
        float largo = 2 * RADIO_HALO * Mth.sin(Mth.PI / TRAMOS_HALO) + 0.3f;
        for (int i = 0; i < TRAMOS_HALO; i++) {
            float a = i * Mth.TWO_PI / TRAMOS_HALO;
            halo.addOrReplaceChild("tramo" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-largo / 2, -0.5f, -0.5f, largo, 1, 1),
                    PartPose.offsetAndRotation(Mth.sin(a) * RADIO_HALO, 0, Mth.cos(a) * RADIO_HALO, 0, a + Mth.HALF_PI, 0));
        }
        // Los cuernos: tres tramos cada uno, cada vez más finos y más inclinados hacia atrás
        PartDefinition cuernos = raiz.addOrReplaceChild("cuernos", CubeListBuilder.create(), PartPose.ZERO);
        for (int lado = -1; lado <= 1; lado += 2) {
            String n = lado < 0 ? "derecho" : "izquierdo";
            PartDefinition t1 = cuernos.addOrReplaceChild(n + "_1",
                    CubeListBuilder.create().texOffs(0, 2).addBox(-1.0f, -3.0f, -1.0f, 2, 3, 2),
                    PartPose.offsetAndRotation(lado * 2.6f, -7.5f, -2.5f, -0.25f, 0, lado * 0.3f));
            PartDefinition t2 = t1.addOrReplaceChild("tramo_2",
                    CubeListBuilder.create().texOffs(8, 2).addBox(-0.75f, -3.0f, -0.75f, 1.5f, 3, 1.5f),
                    PartPose.offsetAndRotation(0, -2.7f, 0, -0.55f, 0, lado * 0.15f));
            t2.addOrReplaceChild("punta",
                    CubeListBuilder.create().texOffs(0, 8).addBox(-0.5f, -2.5f, -0.5f, 1, 2.5f, 1),
                    PartPose.offsetAndRotation(0, -2.8f, 0, -0.6f, 0, 0));
        }
        return LayerDefinition.create(malla, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        ItemStack cara = CaraCliente.obtener(jugador.getId());
        boolean conHalo = cara.is(ModuloRoles.HALO.get()), conCuernos = cara.is(ModuloRoles.CUERNOS.get());
        if (!conHalo && !conCuernos) return;
        pose.pushPose();
        getParentModel().head.translateAndRotate(pose);
        if (conHalo) {
            pose.translate(0, (-11.5f + Mth.sin(edad * 0.08f) * 0.4f) / 16.0f, 0);
            pose.mulPose(Axis.YP.rotationDegrees(edad * 1.5f));
            halo.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA_HALO)), luz, OverlayTexture.NO_OVERLAY);
            halo.render(pose, buffers.getBuffer(RenderType.eyes(TEXTURA_HALO)), 0xF000F0, OverlayTexture.NO_OVERLAY);   // brilla
        } else {
            cuernos.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA_CUERNOS)), luz, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }
}
