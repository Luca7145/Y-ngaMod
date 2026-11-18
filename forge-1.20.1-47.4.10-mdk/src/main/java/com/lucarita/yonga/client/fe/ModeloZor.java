package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.ZorElQuemado;
import net.minecraft.client.model.HierarchicalModel;
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
 * La cabeza de Zor el quemado: cráneo de dragón oriental carbonizado, hocico largo, mandíbula que se abre, dos cuernos
 * de hueso hacia atrás, barbas largas que ondean y una melena de púas. Comparte la textura (256x256) con los tramos
 * del cuerpo (ModeloZorTramo); las grietas de brasa, los ojos y la garganta brillan (capa de luz).
 */
public class ModeloZor extends HierarchicalModel<ZorElQuemado> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "zor"), "main");

    private final ModelPart raiz, cabeza, mandibula, barbaI, barbaD, cuernoI, cuernoD, melena;

    public ModeloZor(ModelPart raiz) {
        this.raiz = raiz;
        this.cabeza = raiz.getChild("cabeza");
        this.mandibula = cabeza.getChild("mandibula");
        this.barbaI = cabeza.getChild("barba_izquierda");
        this.barbaD = cabeza.getChild("barba_derecha");
        this.cuernoI = cabeza.getChild("cuerno_izquierdo");
        this.cuernoD = cabeza.getChild("cuerno_derecho");
        this.melena = cabeza.getChild("melena");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();
        // el centro de la cabeza, a la mitad de la caja de la entidad (2 bloques)
        PartDefinition cabeza = raiz.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-10.0f, -8.0f, -10.0f, 20, 14, 24)        // cráneo
                        .texOffs(0, 40).addBox(-7.0f, -5.0f, -24.0f, 14, 8, 14),        // hocico (maxilar)
                PartPose.offset(0.0f, 8.0f, 0.0f));
        cabeza.addOrReplaceChild("mandibula",
                CubeListBuilder.create().texOffs(60, 40).addBox(-6.5f, 0.0f, -18.0f, 13, 4, 18),
                PartPose.offset(0.0f, 3.0f, -6.0f));
        cabeza.addOrReplaceChild("cuerno_izquierdo",
                CubeListBuilder.create().texOffs(0, 64).addBox(-1.5f, -1.5f, 0.0f, 3, 3, 22),
                PartPose.offsetAndRotation(6.0f, -7.0f, 4.0f, 0.45f, 0.25f, 0.0f));
        cabeza.addOrReplaceChild("cuerno_derecho",
                CubeListBuilder.create().texOffs(52, 64).mirror().addBox(-1.5f, -1.5f, 0.0f, 3, 3, 22),
                PartPose.offsetAndRotation(-6.0f, -7.0f, 4.0f, 0.45f, -0.25f, 0.0f));
        cabeza.addOrReplaceChild("barba_izquierda",
                CubeListBuilder.create().texOffs(104, 64).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 18),
                PartPose.offsetAndRotation(6.5f, 1.0f, -20.0f, 0.2f, 0.9f, 0.0f));
        cabeza.addOrReplaceChild("barba_derecha",
                CubeListBuilder.create().texOffs(144, 64).mirror().addBox(-0.5f, -0.5f, 0.0f, 1, 1, 18),
                PartPose.offsetAndRotation(-6.5f, 1.0f, -20.0f, 0.2f, -0.9f, 0.0f));
        cabeza.addOrReplaceChild("melena",
                CubeListBuilder.create()
                        .texOffs(0, 92).addBox(-1.0f, -10.0f, 0.0f, 2, 10, 12)
                        .texOffs(28, 92).addBox(-7.0f, -7.0f, 2.0f, 2, 7, 10)
                        .texOffs(28, 92).addBox(5.0f, -7.0f, 2.0f, 2, 7, 10),
                PartPose.offsetAndRotation(0.0f, -7.0f, 2.0f, 0.5f, 0.0f, 0.0f));
        return LayerDefinition.create(malla, 256, 256);
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(ZorElQuemado zor, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        int accion = zor.accion();
        float t = edad - zor.inicioAccionCliente;
        // la boca: entreabierta al volar (echa humo), muy abierta al echar fuego, al rugir, al morder y al salir
        float boca = 0.12f + Mth.sin(edad * 0.08f) * 0.05f;
        switch (accion) {
            case ZorElQuemado.FUEGO -> boca = 0.75f + Mth.sin(edad * 0.6f) * 0.05f;
            case ZorElQuemado.RUGIDO, ZorElQuemado.INTRO -> boca = 0.9f;
            case ZorElQuemado.EMERGER -> boca = Math.min(0.95f, t * 0.08f);
            case ZorElQuemado.BUSCAR -> boca = 0.6f + Mth.sin(edad * 0.5f) * 0.3f;
            case ZorElQuemado.LLEVAR -> boca = 0.25f;           // te tiene cogido
            case ZorElQuemado.IR_A_FUEGO -> boca = 0.35f;
            case ZorElQuemado.BOLAS -> boca = 0.45f + Mth.sin(t * 0.4f) * 0.25f;
            default -> { }
        }
        if (zor.deathTime > 0) boca = 0.8f;
        mandibula.xRot = boca;
        // barbas y melena ondean con el viento
        float o = edad * 0.15f;
        barbaI.yRot += Mth.sin(o) * 0.25f;
        barbaI.xRot += Mth.cos(o * 0.8f) * 0.2f;
        barbaD.yRot -= Mth.sin(o + 1.3f) * 0.25f;
        barbaD.xRot += Mth.cos(o * 0.8f + 1.0f) * 0.2f;
        melena.xRot += Mth.sin(o * 0.7f) * 0.08f;
        cuernoI.zRot = cuernoD.zRot = 0.0f;
        // al rugir sacude la cabeza
        if (accion == ZorElQuemado.RUGIDO) {
            cabeza.zRot = Mth.sin(t * 0.7f) * 0.15f;
        }
    }
}
