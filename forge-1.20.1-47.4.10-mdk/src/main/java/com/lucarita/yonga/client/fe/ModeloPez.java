package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.PezDelViento;
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
 * Modelo del Pez del viento: un cachalote blanco celestial (7 bloques de modelo, el renderer lo dibuja x1.5: ~10,5).
 * La gran cabeza cuadrada con un halo de oro que gira encima, la mandíbula estrecha y dentada (se abre), dos tramos de
 * cuerpo que se van estrechando, la cola, la aleta caudal y dos grandes alas de plumas en lugar de las aletas
 * pectorales. Textura de 256x128 (herramientas/dibujar_pez.py); las constelaciones, las runas, el halo y los ojos
 * brillan (capa de luz).
 *
 * Nado: el cuerpo ondula de arriba abajo, cada tramo con un poco de retraso y más amplitud hacia la cola (como las
 * ballenas de verdad); más fuerte cuanto más rápido va. El renderer lo inclina según suba o baje y lo ladea al girar.
 * Acciones: abrir la boca para el rayo, embestir (cola a toda prisa), picado (rígido) y aturdido (tumbado de lado,
 * dando coletazos).
 */
public class ModeloPez extends HierarchicalModel<PezDelViento> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "pez_del_viento"), "main");

    private final ModelPart raiz, cuerpo, cabeza, mandibula, aletaI, aletaD, puntaI, puntaD, cuerpo2, cola, aletaCola, halo;

    public ModeloPez(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.cabeza = cuerpo.getChild("cabeza");
        this.mandibula = cabeza.getChild("mandibula");
        this.aletaI = cuerpo.getChild("aleta_izquierda");
        this.aletaD = cuerpo.getChild("aleta_derecha");
        this.puntaI = aletaI.getChild("punta");
        this.puntaD = aletaD.getChild("punta");
        this.halo = cabeza.getChild("halo");
        this.cuerpo2 = cuerpo.getChild("cuerpo2");
        this.cola = cuerpo2.getChild("cola");
        this.aletaCola = cola.getChild("aleta_cola");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(112, 0).addBox(-11.0f, -10.0f, -13.0f, 22, 20, 26),
                PartPose.offset(0.0f, -4.0f, 0.0f));
        PartDefinition cabeza = cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create().texOffs(0, 0).addBox(-10.0f, -12.0f, -36.0f, 20, 22, 36),
                PartPose.offset(0.0f, 0.0f, -13.0f));
        // El halo: ocho piezas de oro en corro sobre la cabeza (gira despacio)
        PartDefinition halo = cabeza.addOrReplaceChild("halo", CubeListBuilder.create(),
                PartPose.offset(0.0f, -22.0f, -16.0f));
        for (int k = 0; k < 8; k++) {
            halo.addOrReplaceChild("pieza" + k,
                    CubeListBuilder.create().texOffs(196, 96).addBox(-2.5f, -0.5f, 9.0f, 5, 1, 2),
                    PartPose.rotation(0.0f, k * Mth.PI / 4.0f, 0.0f));
        }
        cabeza.addOrReplaceChild("mandibula",
                CubeListBuilder.create().texOffs(0, 58).addBox(-4.0f, 0.0f, -30.0f, 8, 4, 30),
                PartPose.offset(0.0f, 10.0f, -6.0f));
        // Alas de plumas (en lugar de las aletas pectorales), de dos tramos
        PartDefinition alaI = cuerpo.addOrReplaceChild("aleta_izquierda",
                CubeListBuilder.create().texOffs(96, 96).addBox(0.0f, -0.5f, -8.0f, 34, 1, 16),
                PartPose.offsetAndRotation(11.0f, 4.0f, -8.0f, 0.0f, 0.3f, 0.5f));
        alaI.addOrReplaceChild("punta",
                CubeListBuilder.create().texOffs(0, 112).addBox(0.0f, -0.5f, -6.0f, 20, 1, 12),
                PartPose.offset(34.0f, 0.0f, 2.0f));
        PartDefinition alaD = cuerpo.addOrReplaceChild("aleta_derecha",
                CubeListBuilder.create().texOffs(96, 96).mirror().addBox(-34.0f, -0.5f, -8.0f, 34, 1, 16),
                PartPose.offsetAndRotation(-11.0f, 4.0f, -8.0f, 0.0f, -0.3f, -0.5f));
        alaD.addOrReplaceChild("punta",
                CubeListBuilder.create().texOffs(0, 112).mirror().addBox(-20.0f, -0.5f, -6.0f, 20, 1, 12),
                PartPose.offset(-34.0f, 0.0f, 2.0f));
        PartDefinition cuerpo2 = cuerpo.addOrReplaceChild("cuerpo2",
                CubeListBuilder.create().texOffs(76, 58).addBox(-9.0f, -8.0f, 0.0f, 18, 16, 22),
                PartPose.offset(0.0f, 0.0f, 13.0f));
        PartDefinition cola = cuerpo2.addOrReplaceChild("cola",
                CubeListBuilder.create().texOffs(156, 58).addBox(-5.0f, -5.0f, 0.0f, 10, 10, 18),
                PartPose.offset(0.0f, -1.0f, 22.0f));
        cola.addOrReplaceChild("aleta_cola",
                CubeListBuilder.create().texOffs(0, 96).addBox(-18.0f, -1.0f, 0.0f, 36, 2, 12),
                PartPose.offset(0.0f, 0.0f, 18.0f));
        return LayerDefinition.create(malla, 256, 128);
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(PezDelViento pez, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        int accion = pez.accion();
        float t = edad - pez.inicioAccionCliente;
        float rapidez = (float) Math.min(1.0, pez.getDeltaMovement().length() / 1.0);

        // ---- Nado: ola de la cabeza a la cola ----
        float frecuencia = 0.09f + 0.12f * rapidez;
        float fuerza = 0.6f + 0.8f * rapidez;
        if (accion == PezDelViento.BARRIDO) {
            frecuencia = 0.3f;                         // embiste a coletazos
            fuerza = 1.6f;
        } else if (accion == PezDelViento.PICADO) {
            frecuencia = 0.05f;                        // rígido, como un proyectil
            fuerza = 0.25f;
        }
        float f = edad * frecuencia;
        cuerpo.xRot = Mth.sin(f) * 0.03f * fuerza;
        cabeza.xRot = Mth.sin(f + 0.7f) * -0.025f * fuerza;
        cuerpo2.xRot = Mth.sin(f - 0.9f) * 0.09f * fuerza;
        cola.xRot = Mth.sin(f - 1.8f) * 0.17f * fuerza;
        aletaCola.xRot = Mth.sin(f - 2.7f) * 0.32f * fuerza;
        // las alas: aletean despacio, como un ave enorme (la punta va con retraso)
        float aleteo = Mth.sin(edad * 0.07f + 1.0f);
        aletaI.zRot = 0.15f + aleteo * 0.35f;
        aletaD.zRot = -0.15f - aleteo * 0.35f;
        puntaI.zRot = Mth.sin(edad * 0.07f + 0.3f) * 0.25f;
        puntaD.zRot = -Mth.sin(edad * 0.07f + 0.3f) * 0.25f;
        aletaI.yRot = 0.3f + Mth.sin(f) * 0.05f;
        aletaD.yRot = -0.3f - Mth.sin(f) * 0.05f;
        // el halo gira y sube y baja un poco
        halo.yRot = edad * 0.02f;
        halo.y = -22.0f + Mth.sin(edad * 0.05f) * 0.8f;
        // la boca entreabierta, respirando
        mandibula.xRot = 0.03f + Mth.sin(edad * 0.05f) * 0.02f;

        switch (accion) {
            case PezDelViento.APUNTAR -> {
                float abre = Mth.clamp(t / 25.0f, 0.0f, 1.0f);
                mandibula.xRot = 0.55f * abre;
                cabeza.xRot -= 0.06f * abre;
                aletaI.zRot += 0.3f * abre;                   // se planta en el aire
                aletaD.zRot -= 0.3f * abre;
            }
            case PezDelViento.RAYO -> {
                mandibula.xRot = 0.65f + Mth.sin(edad * 1.7f) * 0.03f;   // tiembla con el rayo
                cabeza.xRot -= 0.06f;
                aletaI.zRot = 0.9f;
                aletaD.zRot = -0.9f;
            }
            case PezDelViento.ELEVARSE -> {
                aletaI.zRot = 0.2f;
                aletaD.zRot = -0.2f;
            }
            case PezDelViento.PICADO -> {
                aletaI.zRot = -0.2f;                          // aletas pegadas
                aletaD.zRot = 0.2f;
                aletaI.yRot = -0.9f;                          // alas plegadas hacia atrás
                aletaD.yRot = 0.9f;
                mandibula.xRot = 0.0f;
            }
            case PezDelViento.ATURDIDO -> {
                // tumbado de lado en el suelo, dando coletazos y boqueando
                float coletazo = Mth.sin(edad * 0.25f);
                cuerpo.zRot = 0.55f;
                cola.yRot = coletazo * 0.3f;
                aletaCola.yRot = coletazo * 0.4f;
                cuerpo2.yRot = coletazo * 0.1f;
                mandibula.xRot = 0.2f + Math.abs(Mth.sin(edad * 0.18f)) * 0.25f;
                aletaI.zRot = 0.3f + Math.abs(coletazo) * 0.3f;
                aletaD.zRot = -0.9f;
            }
            case PezDelViento.DIVIDIRSE -> {
                // se estira y se arquea mientras brilla
                float e = Mth.sin(Mth.clamp(t / 40.0f, 0.0f, 1.0f) * Mth.PI);
                cuerpo.xRot -= 0.15f * e;
                cuerpo2.xRot += 0.2f * e;
                cola.xRot += 0.3f * e;
                mandibula.xRot = 0.4f * e;
                aletaI.zRot = 1.0f * e + 0.5f;
                aletaD.zRot = -1.0f * e - 0.5f;
            }
            default -> {}
        }
    }
}
