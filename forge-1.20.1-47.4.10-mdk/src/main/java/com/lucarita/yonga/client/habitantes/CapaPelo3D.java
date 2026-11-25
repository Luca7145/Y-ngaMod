package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Pelo con volumen: piezas en 3D pegadas a la cabeza (se mueven y crecen con ella) para algunos peinados.
 *   coleta · trenza atrás · moño · dos trenzas por delante de los hombros · flequillo que sobresale de la frente ·
 *   melena que cae por detrás del cuello.
 * Textura en gris (herramientas/dibujar_habitantes.py → pelo3d.png), teñida con el color de pelo del habitante.
 */
public class CapaPelo3D extends RenderLayer<Habitante, ModeloHabitante> {

    public static final ModelLayerLocation CAPA =
            new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "habitante_pelo"), "main");
    private static final ResourceLocation TEXTURA =
            new ResourceLocation(YongaMain.MOD_ID, "textures/entity/habitante/pelo3d.png");

    private static final int COLETA = 1, MONO = 2, TRENZAS = 4, FLEQUILLO = 8, MELENA = 16, TRENZA_ATRAS = 32;


    private final ModelPart cabeza, coleta, trenzaAtras, mono, trenzaI, trenzaD, flequillo, melena;
    private final int[] colores;

    public CapaPelo3D(RenderLayerParent<Habitante, ModeloHabitante> padre, ModelPart raiz, int[] coloresPelo) {
        super(padre);
        this.cabeza = raiz.getChild("cabeza");
        this.coleta = cabeza.getChild("coleta");
        this.trenzaAtras = cabeza.getChild("trenza_atras");
        this.mono = cabeza.getChild("mono");
        this.trenzaI = cabeza.getChild("trenza_i");
        this.trenzaD = cabeza.getChild("trenza_d");
        this.flequillo = cabeza.getChild("flequillo");
        this.melena = cabeza.getChild("melena");
        this.colores = coloresPelo;
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition cabeza = malla.getRoot().addOrReplaceChild("cabeza", CubeListBuilder.create(), PartPose.ZERO);
        CubeDeformation poco = new CubeDeformation(0.05f);
        // La cabeza va de y −8 (arriba) a 0 (cuello) y de z −4 (cara) a 4 (nuca)
        cabeza.addOrReplaceChild("coleta", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0f, 0.0f, 0.0f, 2, 8, 2, poco),
                PartPose.offsetAndRotation(0.0f, -6.0f, 3.6f, 0.35f, 0.0f, 0.0f));
        cabeza.addOrReplaceChild("trenza_atras", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, 0.0f, 0.0f, 2, 8, 2, poco),
                PartPose.offsetAndRotation(0.0f, -3.0f, 3.6f, 0.15f, 0.0f, 0.0f));
        cabeza.addOrReplaceChild("mono", CubeListBuilder.create().texOffs(8, 0).addBox(-2.0f, -3.0f, -2.0f, 4, 3, 4, poco),
                PartPose.offset(0.0f, -7.6f, 1.5f));
        cabeza.addOrReplaceChild("trenza_i", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0f, 0.0f, -1.0f, 2, 8, 2, poco),
                PartPose.offsetAndRotation(3.2f, -2.0f, -2.6f, 0.0f, 0.0f, -0.06f));
        cabeza.addOrReplaceChild("trenza_d", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(-1.0f, 0.0f, -1.0f, 2, 8, 2, poco),
                PartPose.offsetAndRotation(-3.2f, -2.0f, -2.6f, 0.0f, 0.0f, 0.06f));
        cabeza.addOrReplaceChild("flequillo", CubeListBuilder.create().texOffs(0, 12).addBox(-4.0f, -8.4f, -5.0f, 8, 3, 2, poco),
                PartPose.ZERO);
        cabeza.addOrReplaceChild("melena", CubeListBuilder.create().texOffs(0, 18).addBox(-4.0f, -4.0f, 3.0f, 8, 9, 2, poco),
                PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.08f, 0.0f, 0.0f));
        return LayerDefinition.create(malla, 64, 32);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int luz, Habitante h, float paso, float amplitud,
                       float parcial, float edadTicks, float giroCabeza, float inclinacion) {
        if (h.isInvisible() || h.etapa() == 0) return;
        // Las piezas de cada peinado de cada época vienen en el Catalogo (lo genera el dibujante)
        int piezas = com.lucarita.yonga.modulos.habitantes.Catalogo.PIEZAS[
                com.lucarita.yonga.modulos.habitantes.Catalogo.dibujo(h.epoca()).ordinal()][h.esMujer() ? 1 : 0][
                HabitanteRenderer.peinadoVisible(h)];
        if (piezas == 0) return;

        ModelPart head = this.getParentModel().head;
        this.cabeza.copyFrom(head);
        this.cabeza.xScale = head.xScale;
        this.cabeza.yScale = head.yScale;
        this.cabeza.zScale = head.zScale;
        this.coleta.visible = (piezas & COLETA) != 0;
        this.trenzaAtras.visible = (piezas & TRENZA_ATRAS) != 0;
        this.mono.visible = (piezas & MONO) != 0;
        this.trenzaI.visible = (piezas & TRENZAS) != 0;
        this.trenzaD.visible = (piezas & TRENZAS) != 0;
        this.flequillo.visible = (piezas & FLEQUILLO) != 0;
        this.melena.visible = (piezas & MELENA) != 0;

        int c = this.colores[h.colorPelo()];
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURA));
        this.cabeza.render(pose, vc, luz, LivingEntityRenderer.getOverlayCoords(h, 0.0f),
                ((c >> 16) & 0xFF) / 255.0f, ((c >> 8) & 0xFF) / 255.0f, (c & 0xFF) / 255.0f, 1.0f);
    }
}
