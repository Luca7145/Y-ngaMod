package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.red.BarrigaPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
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
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * La barriga del embarazo: una pieza pegada a la parte baja del tronco que crece hacia delante según lo avanzado que
 * vaya (0..1). Se pinta con la misma textura del cuerpo (la parte baja del tronco), así que lleva la piel o la ropa
 * de cada uno. Para habitantes (piel + ropa) y para jugadoras (su skin, con la segunda capa).
 */
public final class CapaBarriga {

    public static final ModelLayerLocation CAPA =
            new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "barriga"), "main");

    /** Jugadoras embarazadas que se ven: id de entidad → barriga. */
    private static final Map<Integer, Float> JUGADORAS = new HashMap<>();

    private CapaBarriga() {}

    public static void recibir(BarrigaPacket p) {
        if (p.barriga() <= 0.0f) JUGADORAS.remove(p.idJugador());
        else JUGADORAS.put(p.idJugador(), p.barriga());
    }

    public static void vaciar() {
        JUGADORAS.clear();
    }

    /**
     * Tronco: de y 0 (cuello) a 12 (cintura), z de −2 (pecho) a 2 (espalda). La barriga va de y 6 a 12, delante del
     * tronco: así su frente cae exactamente sobre las filas 6-11 de la tripa de la skin (antes iba un píxel
     * desplazada y se metía bajo el pecho), y su cara de arriba toma las filas 3-5, lo que hay justo encima.
     */
    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot().addOrReplaceChild("tronco", CubeListBuilder.create(), PartPose.ZERO);
        raiz.addOrReplaceChild("barriga", CubeListBuilder.create().texOffs(17, 23)
                .addBox(-3.5f, -3.0f, -3.0f, 7, 6, 3, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 9.0f, -1.9f));
        raiz.addOrReplaceChild("capa", CubeListBuilder.create().texOffs(17, 39)
                .addBox(-3.5f, -3.0f, -3.0f, 7, 6, 3, new CubeDeformation(0.25f)), PartPose.offset(0.0f, 9.0f, -1.9f));
        return LayerDefinition.create(malla, 64, 64);
    }

    /** Coloca la barriga según el tronco y lo avanzado del embarazo. false si no se ve aún. */
    private static boolean colocar(ModelPart raiz, ModelPart tronco, float b) {
        if (b < 0.12f) return false;
        raiz.copyFrom(tronco);
        raiz.xScale = tronco.xScale;
        raiz.yScale = tronco.yScale;
        raiz.zScale = tronco.zScale;
        float t = Math.min(1.0f, b);
        for (String n : new String[]{"barriga", "capa"}) {
            ModelPart p = raiz.getChild(n);
            p.xScale = 0.75f + 0.25f * t;
            p.yScale = 0.65f + 0.35f * t;
            p.zScale = 0.15f + 1.05f * t;
        }
        return true;
    }

    private static void pintar(ModelPart pieza, PoseStack pose, MultiBufferSource buffers, int luz, LivingEntity e,
                               ResourceLocation textura, boolean translucida) {
        RenderType tipo = translucida ? RenderType.entityTranslucent(textura) : RenderType.entityCutoutNoCull(textura);
        pieza.render(pose, buffers.getBuffer(tipo), luz, LivingEntityRenderer.getOverlayCoords(e, 0.0f));
    }

    /** En los habitantes: piel y, si va vestida, la ropa por encima. */
    public static class DeHabitante extends RenderLayer<Habitante, ModeloHabitante> {
        private final ModelPart raiz;

        public DeHabitante(RenderLayerParent<Habitante, ModeloHabitante> padre, ModelPart raiz) {
            super(padre);
            this.raiz = raiz.getChild("tronco");
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int luz, Habitante h, float paso, float amplitud,
                           float parcial, float edadTicks, float giroCabeza, float inclinacion) {
            if (h.isInvisible() || !colocar(this.raiz, this.getParentModel().body, h.barriga())) return;
            ModelPart barriga = this.raiz.getChild("barriga");
            this.raiz.getChild("capa").visible = false;
            barriga.visible = true;
            pintar(this.raiz, pose, buffers, luz, h, this.getTextureLocation(h), false);
            ResourceLocation ropa = HabitanteRenderer.ropa(h);
            if (ropa != null) {
                // La ropa, un pelín por fuera en las TRES direcciones: si no, la cara de arriba de la ropa y la de la
                // piel quedaban a la misma altura y parpadeaban (era el fallo de arriba de la barriga)
                barriga.xScale *= 1.06f;
                barriga.yScale *= 1.06f;
                barriga.zScale += 0.08f;
                pintar(this.raiz, pose, buffers, luz, h, ropa, false);
            }
        }
    }

    /** En las jugadoras: su skin (y la segunda capa si la tiene puesta). */
    public static class DeJugadora extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        private final ModelPart raiz;

        public DeJugadora(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> padre, ModelPart raiz) {
            super(padre);
            this.raiz = raiz.getChild("tronco");
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int luz, AbstractClientPlayer j, float paso,
                           float amplitud, float parcial, float edadTicks, float giroCabeza, float inclinacion) {
            Float b = JUGADORAS.get(j.getId());
            if (b == null || j.isInvisible() || Minecraft.getInstance().level == null
                    || !colocar(this.raiz, this.getParentModel().body, b)) return;
            ResourceLocation skin = j.getSkinTextureLocation();
            this.raiz.getChild("barriga").visible = true;
            this.raiz.getChild("capa").visible = false;
            pintar(this.raiz, pose, buffers, luz, j, skin, false);
            if (this.getParentModel().jacket.visible) {
                this.raiz.getChild("barriga").visible = false;
                this.raiz.getChild("capa").visible = true;
                pintar(this.raiz, pose, buffers, luz, j, skin, true);
            }
        }
    }
}
