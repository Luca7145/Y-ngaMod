package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Enano;
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
 * Modelo del enano: un bloque (cuerpo y cabeza a la vez, con la cara delante), dos bracitos que salen de
 * los lados caídos hacia abajo y cuatro patitas. Textura de 64x32 (herramientas/dibujar_enanos.py).
 *
 * Animaciones: andar (patas), preparar la embestida (tiembla y se inclina) y embestir (inclinado hacia
 * delante, brazos hacia atrás y patas a toda prisa).
 */
public class ModeloEnano extends HierarchicalModel<Enano> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "enano"), "main");

    private static final float BRAZO_CAIDO = 0.55f;

    private final ModelPart raiz;
    private final ModelPart cuerpo;
    private final ModelPart brazoIzquierdo;
    private final ModelPart brazoDerecho;
    private final ModelPart pataDelanteraIzquierda;
    private final ModelPart pataDelanteraDerecha;
    private final ModelPart pataTraseraIzquierda;
    private final ModelPart pataTraseraDerecha;

    public ModeloEnano(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.brazoIzquierdo = cuerpo.getChild("brazo_izquierdo");
        this.brazoDerecho = cuerpo.getChild("brazo_derecho");
        this.pataDelanteraIzquierda = raiz.getChild("pata_delantera_izquierda");
        this.pataDelanteraDerecha = raiz.getChild("pata_delantera_derecha");
        this.pataTraseraIzquierda = raiz.getChild("pata_trasera_izquierda");
        this.pataTraseraDerecha = raiz.getChild("pata_trasera_derecha");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        // Cuerpo: 12x8x12, apoyado sobre las patas (el suelo está en y = 24)
        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-6.0f, -8.0f, -6.0f, 12, 8, 12),
                PartPose.offset(0.0f, 21.0f, 0.0f));

        // Bracitos: salen de los lados, un poco hacia delante, caídos
        cuerpo.addOrReplaceChild("brazo_izquierdo",
                CubeListBuilder.create().texOffs(16, 20).addBox(0.0f, -1.0f, -1.5f, 4, 2, 3),
                PartPose.offsetAndRotation(6.0f, -4.0f, -1.0f, 0.0f, 0.0f, BRAZO_CAIDO));
        cuerpo.addOrReplaceChild("brazo_derecho",
                CubeListBuilder.create().texOffs(16, 20).mirror().addBox(-4.0f, -1.0f, -1.5f, 4, 2, 3),
                PartPose.offsetAndRotation(-6.0f, -4.0f, -1.0f, 0.0f, 0.0f, -BRAZO_CAIDO));

        // Patitas: 4x3x4 en las esquinas
        CubeListBuilder pata = CubeListBuilder.create().texOffs(0, 20).addBox(-2.0f, 0.0f, -2.0f, 4, 3, 4);
        raiz.addOrReplaceChild("pata_delantera_izquierda", pata, PartPose.offset(3.5f, 21.0f, -3.5f));
        raiz.addOrReplaceChild("pata_delantera_derecha", pata, PartPose.offset(-3.5f, 21.0f, -3.5f));
        raiz.addOrReplaceChild("pata_trasera_izquierda", pata, PartPose.offset(3.5f, 21.0f, 3.5f));
        raiz.addOrReplaceChild("pata_trasera_derecha", pata, PartPose.offset(-3.5f, 21.0f, 3.5f));

        return LayerDefinition.create(malla, 64, 32);
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(Enano enano, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // Andar: patas en diagonal, como un animal de cuatro patas
        float andar = paso * 0.6662f;
        float amplitud = 1.4f * cantidadPaso;
        // Balanceo suave de los bracitos
        float balanceo = Mth.sin(edad * 0.08f) * 0.08f;
        brazoIzquierdo.zRot = BRAZO_CAIDO + balanceo;
        brazoDerecho.zRot = -BRAZO_CAIDO - balanceo;

        int estado = enano.estado();
        if (estado == Enano.PREPARANDO) {
            // Tiembla, se inclina hacia delante y abre los brazos
            cuerpo.x = Mth.sin(edad * 2.5f) * 0.4f;
            cuerpo.xRot = 0.12f;
            brazoIzquierdo.zRot = 0.15f;
            brazoDerecho.zRot = -0.15f;
        } else if (estado == Enano.EMBISTIENDO) {
            // A la carga: inclinado, brazos hacia atrás y patas a toda prisa
            cuerpo.xRot = 0.22f;
            brazoIzquierdo.yRot = -0.9f;
            brazoDerecho.yRot = 0.9f;
            andar = edad * 1.6f;
            amplitud = 1.2f;
        }
        pataDelanteraIzquierda.xRot = Mth.cos(andar) * amplitud;
        pataTraseraDerecha.xRot = Mth.cos(andar) * amplitud;
        pataDelanteraDerecha.xRot = Mth.cos(andar + Mth.PI) * amplitud;
        pataTraseraIzquierda.xRot = Mth.cos(andar + Mth.PI) * amplitud;
    }
}
