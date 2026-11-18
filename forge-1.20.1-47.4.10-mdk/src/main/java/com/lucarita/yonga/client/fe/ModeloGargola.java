package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Gargola;
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
 * Modelo de la gárgola: cuerpo de piedra encorvado, cabeza con hocico, mandíbula y cuernos, alas de murciélago
 * de dos tramos (hueso + membrana), brazos con garras, patas de perro y cola con punta de flecha. Algo más de
 * 1 bloque. Textura de 64x64 (herramientas/dibujar_gargola.py); los ojos brillan.
 *
 * Animaciones: andar torpe (se bambolea, alas plegadas), zarpazo, mordisco, vuelo (aleteo), rodear (planea con
 * las alas abiertas), picado (alas hacia atrás, garras delante), llevar a la presa (aleteo frenético) y colgada
 * (envuelta en sus alas; el renderer la pone boca abajo).
 */
public class ModeloGargola extends HierarchicalModel<Gargola> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "gargola"), "main");

    private final ModelPart raiz, cuerpo, cabeza, mandibula, alaI, puntaAlaI, alaD, puntaAlaD, brazoI, brazoD;
    private final ModelPart cola, cola2, piernaI, piernaD, espinillaI, espinillaD;

    public ModeloGargola(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.cabeza = cuerpo.getChild("cabeza");
        this.mandibula = cabeza.getChild("mandibula");
        this.alaI = cuerpo.getChild("ala_izquierda");
        this.puntaAlaI = alaI.getChild("punta");
        this.alaD = cuerpo.getChild("ala_derecha");
        this.puntaAlaD = alaD.getChild("punta");
        this.brazoI = cuerpo.getChild("brazo_izquierdo");
        this.brazoD = cuerpo.getChild("brazo_derecho");
        this.cola = cuerpo.getChild("cola");
        this.cola2 = cola.getChild("cola2");
        this.piernaI = raiz.getChild("pierna_izquierda");
        this.piernaD = raiz.getChild("pierna_derecha");
        this.espinillaI = piernaI.getChild("espinilla");
        this.espinillaD = piernaD.getChild("espinilla");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -7.0f, -2.5f, 6, 7, 5),
                PartPose.offsetAndRotation(0.0f, 16.0f, 0.0f, 0.4f, 0.0f, 0.0f));

        PartDefinition cabeza = cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(0, 12).addBox(-2.5f, -4.0f, -4.0f, 5, 4, 5)
                        .texOffs(20, 12).addBox(-1.5f, -2.5f, -6.0f, 3, 2, 2)          // hocico
                        .texOffs(30, 12).addBox(-2.5f, -6.0f, -1.0f, 1, 2, 1)          // cuernos
                        .texOffs(30, 12).addBox(1.5f, -6.0f, -1.0f, 1, 2, 1),
                PartPose.offsetAndRotation(0.0f, -7.0f, -1.0f, -0.4f, 0.0f, 0.0f));
        cabeza.addOrReplaceChild("mandibula",
                CubeListBuilder.create().texOffs(34, 12).addBox(-1.5f, 0.0f, -2.0f, 3, 1, 3),
                PartPose.offset(0.0f, -0.5f, -4.0f));

        ala(cuerpo, "ala_izquierda", 1.0f);
        ala(cuerpo, "ala_derecha", -1.0f);

        cuerpo.addOrReplaceChild("brazo_izquierdo",
                CubeListBuilder.create().texOffs(0, 21).addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
                PartPose.offset(3.5f, -6.0f, -1.0f));
        cuerpo.addOrReplaceChild("brazo_derecho",
                CubeListBuilder.create().texOffs(0, 21).mirror().addBox(-1.0f, 0.0f, -1.0f, 2, 6, 2),
                PartPose.offset(-3.5f, -6.0f, -1.0f));

        PartDefinition cola = cuerpo.addOrReplaceChild("cola",
                CubeListBuilder.create().texOffs(8, 21).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 6),
                PartPose.offsetAndRotation(0.0f, -0.5f, 2.5f, -0.3f, 0.0f, 0.0f));
        cola.addOrReplaceChild("cola2",
                CubeListBuilder.create()
                        .texOffs(8, 28).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 5)
                        .texOffs(22, 28).addBox(-1.5f, -0.5f, 4.0f, 3, 1, 2),          // punta de flecha
                PartPose.offsetAndRotation(0.0f, 0.0f, 6.0f, 0.4f, 0.0f, 0.0f));

        pierna(raiz, "pierna_izquierda", 2.0f);
        pierna(raiz, "pierna_derecha", -2.0f);
        return LayerDefinition.create(malla, 64, 64);
    }

    private static void ala(PartDefinition cuerpo, String nombre, float lado) {
        // el hueso va hacia afuera (x) y la membrana cuelga de él (y), en un plano (grosor 0)
        CubeListBuilder brazo = CubeListBuilder.create().texOffs(0, 36);
        if (lado < 0) brazo.mirror();
        brazo.addBox(lado > 0 ? 0.0f : -7.0f, -0.5f, -0.5f, 7, 1, 1)
                .texOffs(0, 40).addBox(lado > 0 ? 0.0f : -7.0f, 0.5f, 0.0f, 7, 6, 0);
        PartDefinition ala = cuerpo.addOrReplaceChild(nombre, brazo,
                PartPose.offsetAndRotation(lado * 2.5f, -6.0f, 2.0f, 0.0f, 0.0f, 0.0f));
        CubeListBuilder punta = CubeListBuilder.create().texOffs(16, 36);
        if (lado < 0) punta.mirror();
        punta.addBox(lado > 0 ? 0.0f : -6.0f, -0.5f, -0.5f, 6, 1, 1)
                .texOffs(16, 40).addBox(lado > 0 ? 0.0f : -6.0f, 0.5f, 0.0f, 6, 5, 0);
        ala.addOrReplaceChild("punta", punta, PartPose.offset(lado * 7.0f, 0.0f, 0.0f));
    }

    private static void pierna(PartDefinition raiz, String nombre, float x) {
        CubeListBuilder muslo = CubeListBuilder.create().texOffs(30, 21);
        if (x < 0) muslo.mirror();
        PartDefinition pierna = raiz.addOrReplaceChild(nombre, muslo.addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2),
                PartPose.offsetAndRotation(x, 16.5f, 0.5f, -0.5f, 0.0f, 0.0f));
        CubeListBuilder baja = CubeListBuilder.create().texOffs(38, 21);
        if (x < 0) baja.mirror();
        baja.addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2).texOffs(46, 21).addBox(-1.0f, 3.5f, -2.5f, 2, 1, 3);   // garra del pie
        pierna.addOrReplaceChild("espinilla", baja, PartPose.offsetAndRotation(0.0f, 4.0f, 0.0f, 0.9f, 0.0f, 0.0f));
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(Gargola g, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        int accion = g.accion();
        float t = edad - g.inicioAccionCliente;
        cantidadPaso = Math.min(cantidadPaso, 1.0f);
        float a = cantidadPaso;

        cabeza.yRot = giroCabeza * Mth.DEG_TO_RAD;
        cabeza.xRot = -0.4f + inclinacionCabeza * Mth.DEG_TO_RAD * 0.6f;
        // alas plegadas en reposo
        plegarAlas(1.0f);
        cola.yRot = Mth.sin(edad * 0.08f) * 0.3f;
        cola2.yRot = Mth.sin(edad * 0.08f - 0.8f) * 0.35f;
        mandibula.xRot = 0.1f + Mth.sin(edad * 0.1f) * 0.05f;

        if (g.colgada()) {
            // colgada (boca abajo): envuelta en sus alas, piernas agarradas arriba, cabeza hacia el suelo
            cuerpo.xRot = 0.0f;
            plegarAlas(1.0f);
            alaI.yRot = -1.2f;
            alaD.yRot = 1.2f;
            alaI.zRot = 0.6f;
            alaD.zRot = -0.6f;
            brazoI.xRot = -0.3f;
            brazoD.xRot = -0.3f;
            cabeza.xRot = Mth.sin(edad * 0.04f) * 0.1f;
            cola.xRot = 0.2f;
            float respira = Mth.sin(edad * 0.07f) * 0.04f;
            cuerpo.xScale = 1.0f + respira;
            return;
        }
        cuerpo.xScale = 1.0f;

        if (!g.volando()) {
            // ---- En el suelo: torpe, se bambolea ----
            float c = paso * 0.6f;
            piernaI.xRot = -0.5f + Mth.cos(c) * 0.8f * a;
            piernaD.xRot = -0.5f + Mth.cos(c + Mth.PI) * 0.8f * a;
            cuerpo.zRot = Mth.sin(c) * 0.15f * a;                  // bamboleo de lado a lado
            cuerpo.y = 16.0f + Math.abs(Mth.sin(c)) * 0.6f * a;
            brazoI.xRot = Mth.cos(c + Mth.PI) * 0.5f * a - 0.2f;
            brazoD.xRot = Mth.cos(c) * 0.5f * a - 0.2f;
            alaI.zRot += Mth.sin(c) * 0.1f * a;
            alaD.zRot -= Mth.sin(c) * 0.1f * a;
            if (accion == Gargola.ZARPAZO) {
                // levanta la garra (0-5), barre hacia abajo (5-8) y vuelve (8-14)
                float brazo;
                if (t < 5) brazo = -2.4f * (t / 5.0f);
                else if (t < 8) brazo = Mth.lerp((t - 5) / 3.0f, -2.4f, 0.3f);
                else brazo = Mth.lerp(Mth.clamp((t - 8) / 6.0f, 0.0f, 1.0f), 0.3f, -0.2f);
                brazoD.xRot = brazo;
                brazoD.zRot = t < 8 ? 0.4f : 0.0f;
                cuerpo.yRot = t < 5 ? -0.3f * (t / 5.0f) : (t < 8 ? 0.4f : 0.0f);
            } else if (accion == Gargola.MORDISCO) {
                float lanza = t < 6 ? t / 6.0f : Math.max(0.0f, 1.0f - (t - 6) / 6.0f);
                cuerpo.xRot += 0.35f * lanza;
                cabeza.xRot += 0.4f * lanza;
                mandibula.xRot = t < 5 ? 0.9f * (t / 5.0f) : 0.05f;  // abre y ¡zas!
            }
            return;
        }

        // ---- Volando ----
        cuerpo.xRot = 0.9f;                                         // casi horizontal
        cabeza.xRot = -0.9f + inclinacionCabeza * Mth.DEG_TO_RAD * 0.5f;
        piernaI.xRot = 0.9f;                                        // patas recogidas hacia atrás
        piernaD.xRot = 0.9f;
        espinillaI.xRot = 0.4f;
        espinillaD.xRot = 0.4f;
        // las patas siguen en la cadera (el cuerpo gira sobre ella): así no se separan al volar
        cola.xRot = -0.9f;
        brazoI.xRot = 0.6f;
        brazoD.xRot = 0.6f;

        switch (accion) {
            case Gargola.RODEAR -> {
                // planea con las alas abiertas, con algún aleteo lento
                abrirAlas(1.0f);
                float aleteo = Mth.sin(edad * 0.25f) * 0.25f;
                alaI.zRot = -0.1f + aleteo;
                alaD.zRot = 0.1f - aleteo;
                cuerpo.zRot = -0.25f;                               // ladeada, girando
            }
            case Gargola.PICADO -> {
                // alas hacia atrás, de cabeza, garras delante
                cuerpo.xRot = 1.25f;
                abrirAlas(0.3f);
                alaI.yRot = -0.9f;                      // barridas hacia atrás
                alaD.yRot = 0.9f;
                brazoI.xRot = -2.2f;
                brazoD.xRot = -2.2f;
                mandibula.xRot = 0.7f;
            }
            case Gargola.LLEVAR -> {
                // aleteo frenético hacia arriba, de pie, con los brazos y las patas sujetando a la presa
                cuerpo.xRot = 0.15f;
                cabeza.xRot = -0.3f;
                abrirAlas(1.0f);
                float aleteo = Mth.sin(edad * 1.3f);
                alaI.zRot = aleteo * 0.9f;
                alaD.zRot = -aleteo * 0.9f;
                puntaAlaI.zRot = aleteo * 0.4f;
                puntaAlaD.zRot = -aleteo * 0.4f;
                brazoI.xRot = 0.1f;
                brazoD.xRot = 0.1f;
                brazoI.zRot = 0.4f;
                brazoD.zRot = -0.4f;
                piernaI.xRot = -0.2f;
                piernaD.xRot = -0.2f;
                espinillaI.xRot = 0.2f;
                espinillaD.xRot = 0.2f;
                cola.xRot = 0.6f;
            }
            default -> {
                // vuelo normal: aleteo
                abrirAlas(1.0f);
                float aleteo = Mth.sin(edad * 0.7f);
                alaI.zRot = aleteo * 0.75f;
                alaD.zRot = -aleteo * 0.75f;
                puntaAlaI.zRot = aleteo * 0.3f;
                puntaAlaD.zRot = -aleteo * 0.3f;
            }
        }
    }

    /** Alas plegadas sobre la espalda (1) o a medias. */
    private void plegarAlas(float f) {
        alaI.yRot = -1.1f * f;                 // hacia atrás, sobre la espalda
        alaD.yRot = 1.1f * f;
        alaI.zRot = 0.5f * f;
        alaD.zRot = -0.5f * f;
        puntaAlaI.yRot = 2.2f * f;
        puntaAlaD.yRot = -2.2f * f;
    }

    /** Alas abiertas en cruz (1) o a medias. */
    private void abrirAlas(float f) {
        alaI.yRot = 0.15f * (1 - f) + 0.0f;
        alaD.yRot = -0.15f * (1 - f);
        alaI.xRot = -0.9f;                     // compensa que el cuerpo vaya tumbado: membrana hacia abajo
        alaD.xRot = -0.9f;
        puntaAlaI.yRot = 0.3f * (1 - f);
        puntaAlaD.yRot = -0.3f * (1 - f);
    }
}
