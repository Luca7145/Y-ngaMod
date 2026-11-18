package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Un tramo del cuerpo de Zor: escamas carbonizadas con grietas de brasa, el vientre rojizo de placas y una púa en el
 * lomo. Algunos tramos llevan un par de patas cortas con garras (como los dragones orientales) y el último, el
 * penacho de llamas de la cola. Centrado en su origen y mirando hacia -Z (hacia la cabeza). Misma textura que la cabeza.
 */
public class ModeloZorTramo {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "zor"), "tramo");
    /** Ancho del modelo en bloques (el renderer lo escala al ancho de cada tramo). */
    public static final float ANCHO = 1.5f;

    private final ModelPart raiz, pua, pataI, pataD, cola;

    public ModeloZorTramo(ModelPart raiz) {
        this.raiz = raiz;
        this.pua = raiz.getChild("pua");
        this.pataI = raiz.getChild("pata_izquierda");
        this.pataD = raiz.getChild("pata_derecha");
        this.cola = raiz.getChild("cola");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 128).addBox(-12.0f, -10.0f, -16.0f, 24, 20, 32),
                PartPose.ZERO);
        raiz.addOrReplaceChild("pua",
                CubeListBuilder.create().texOffs(120, 128).addBox(-1.0f, -7.0f, -5.0f, 2, 7, 10),
                PartPose.offsetAndRotation(0.0f, -10.0f, 0.0f, -0.35f, 0.0f, 0.0f));
        PartDefinition pataI = raiz.addOrReplaceChild("pata_izquierda",
                CubeListBuilder.create().texOffs(150, 128).addBox(-2.5f, 0.0f, -2.5f, 5, 12, 5),
                PartPose.offsetAndRotation(11.0f, 6.0f, -6.0f, 0.6f, 0.0f, -0.5f));
        pataI.addOrReplaceChild("garra",
                CubeListBuilder.create().texOffs(196, 128).addBox(-3.5f, 0.0f, -5.0f, 7, 2, 7),
                PartPose.offset(0.0f, 12.0f, 0.0f));
        PartDefinition pataD = raiz.addOrReplaceChild("pata_derecha",
                CubeListBuilder.create().texOffs(172, 128).mirror().addBox(-2.5f, 0.0f, -2.5f, 5, 12, 5),
                PartPose.offsetAndRotation(-11.0f, 6.0f, -6.0f, 0.6f, 0.0f, 0.5f));
        pataD.addOrReplaceChild("garra",
                CubeListBuilder.create().texOffs(196, 128).mirror().addBox(-3.5f, 0.0f, -5.0f, 7, 2, 7),
                PartPose.offset(0.0f, 12.0f, 0.0f));
        raiz.addOrReplaceChild("cola",
                CubeListBuilder.create().texOffs(0, 184).addBox(-6.0f, -9.0f, 0.0f, 12, 18, 14),
                PartPose.offset(0.0f, 0.0f, 10.0f));
        return LayerDefinition.create(malla, 256, 256);
    }

    /**
     * Prepara el tramo: patas solo en algunos tramos (dan zarpazos al aire al volar), el penacho solo en la cola.
     */
    public void preparar(int indice, int total, float edad) {
        raiz.getAllParts().forEach(ModelPart::resetPose);
        boolean patas = indice == 1 || indice == 7;
        pataI.visible = pataD.visible = patas;
        boolean ultimo = indice == total - 1;
        cola.visible = ultimo;
        pua.visible = !ultimo;
        if (patas) {
            float s = Mth.sin(edad * 0.18f + indice);
            pataI.xRot += s * 0.4f;
            pataD.xRot -= s * 0.4f;
        }
        if (ultimo) {
            cola.yRot = Mth.sin(edad * 0.3f) * 0.2f;
            cola.xRot = Mth.cos(edad * 0.25f) * 0.1f;
        }
        pua.xRot += Mth.sin(edad * 0.1f + indice * 0.6f) * 0.06f;
    }

    public void render(PoseStack pose, VertexConsumer vertices, int luz, int superposicion) {
        raiz.render(pose, vertices, luz, superposicion);
    }
}
