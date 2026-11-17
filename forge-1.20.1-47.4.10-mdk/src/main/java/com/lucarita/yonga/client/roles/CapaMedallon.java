package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import com.lucarita.yonga.modulos.roles.cara.Medallon;
import com.lucarita.yonga.modulos.roles.cara.MedallonItem;
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
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/**
 * Dibuja el medallón como un pin en el pecho del jugador, a un lado (a su izquierda), no en el centro.
 * Medidas en píxeles del modelo; el pecho va de x -4 a 4, de y 0 (hombros) a 12 y su frente está en z = -2.
 */
public class CapaMedallon extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "medallon"), "main");

    // Una textura por medallón: textures/entity/medallon_<id>.png
    private static final Map<Medallon, ResourceLocation> TEXTURAS = new EnumMap<>(Medallon.class);
    static {
        for (Medallon medallon : Medallon.values()) {
            TEXTURAS.put(medallon, new ResourceLocation(YongaMain.MOD_ID, "textures/entity/medallon_" + medallon.id() + ".png"));
        }
    }

    private final ModelPart pin;

    public CapaMedallon(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, EntityModelSet modelos) {
        super(padre);
        this.pin = modelos.bakeLayer(CAPA);
    }

    /** Una chapa de 3x3 píxeles y 1 de grosor, en la parte alta del pecho, a la izquierda del jugador. */
    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        raiz.addOrReplaceChild("pin",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -1.5f, -0.5f, 3, 3, 1),
                // x = 2: a la izquierda del jugador; y = 3.5: parte alta del pecho; z = -2.7: por delante de la chaqueta de la skin
                PartPose.offset(2.0f, 3.5f, -2.7f));
        return LayerDefinition.create(malla, 16, 16);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer jugador,
                       float limbSwing, float limbSwingAmount, float parcial, float edad, float giroCabeza, float inclinacionCabeza) {
        ItemStack enLaCara = CaraCliente.obtener(jugador.getId());
        if (!(enLaCara.getItem() instanceof MedallonItem medallon)) {
            return;
        }
        pose.pushPose();
        // Enganchado al cuerpo: se mueve con él (al agacharse, etc.)
        getParentModel().body.translateAndRotate(pose);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURAS.get(medallon.medallon())));
        pin.render(pose, vertices, luz, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
