package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Gnomo;
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
 * Modelo del gnomo: cabezón con nariz y barba, gorro puntiagudo de tres piezas (se queda atrás con la velocidad),
 * cuerpecillo y una espada corta. 1,25 bloques con el gorro. Textura de 64x64 (herramientas/dibujar_gnomo.py).
 *
 * Animaciones (rápidas y fluidas): carrera con zancadas muy rápidas e inclinado, saltitos recogiendo las piernas,
 * agacharse para saltar, subida con la espada en alto, picado con la espada hacia abajo y el gorro al viento,
 * atascado tirando de la espada clavada (echado hacia atrás, a tirones), aviso del tajo (agachado, espada atrás),
 * el tajo relámpago (estirado como una flecha) y la recuperación.
 */
public class ModeloGnomo extends HierarchicalModel<Gnomo> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "gnomo"), "main");

    private final ModelPart raiz, cuerpo, cabeza, barba, gorro, gorro2, gorro3, brazoI, brazoD, espada, piernaI, piernaD;

    public ModeloGnomo(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.cabeza = cuerpo.getChild("cabeza");
        this.barba = cabeza.getChild("barba");
        this.gorro = cabeza.getChild("gorro");
        this.gorro2 = gorro.getChild("gorro2");
        this.gorro3 = gorro2.getChild("gorro3");
        this.brazoI = cuerpo.getChild("brazo_izquierdo");
        this.brazoD = cuerpo.getChild("brazo_derecho");
        this.espada = brazoD.getChild("espada");
        this.piernaI = raiz.getChild("pierna_izquierda");
        this.piernaD = raiz.getChild("pierna_derecha");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -4.0f, -1.5f, 4, 4, 3),
                PartPose.offset(0.0f, 21.0f, 0.0f));
        PartDefinition cabeza = cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-2.5f, -5.0f, -2.5f, 5, 5, 5)
                        .texOffs(20, 8).addBox(-0.5f, -3.0f, -3.5f, 1, 2, 1),         // nariz
                PartPose.offset(0.0f, -4.0f, 0.0f));
        cabeza.addOrReplaceChild("barba",
                CubeListBuilder.create().texOffs(24, 8).addBox(-2.0f, 0.0f, -0.5f, 4, 3, 1),
                PartPose.offset(0.0f, -1.0f, -2.6f));
        PartDefinition gorro = cabeza.addOrReplaceChild("gorro",
                CubeListBuilder.create().texOffs(0, 18).addBox(-3.0f, -2.0f, -3.0f, 6, 2, 6),
                PartPose.offset(0.0f, -4.5f, 0.0f));
        PartDefinition gorro2 = gorro.addOrReplaceChild("gorro2",
                CubeListBuilder.create().texOffs(24, 18).addBox(-2.0f, -4.0f, -2.0f, 4, 4, 4),
                PartPose.offsetAndRotation(0.0f, -2.0f, 0.0f, -0.2f, 0.0f, 0.0f));
        gorro2.addOrReplaceChild("gorro3",
                CubeListBuilder.create().texOffs(40, 18).addBox(-1.0f, -4.0f, -1.0f, 2, 4, 2),
                PartPose.offsetAndRotation(0.0f, -4.0f, 0.3f, -0.35f, 0.0f, 0.0f));

        cuerpo.addOrReplaceChild("brazo_izquierdo",
                CubeListBuilder.create().texOffs(0, 28).addBox(-1.0f, -0.5f, -1.0f, 2, 4, 2),
                PartPose.offset(3.0f, -3.5f, 0.0f));
        PartDefinition brazoD = cuerpo.addOrReplaceChild("brazo_derecho",
                CubeListBuilder.create().texOffs(0, 28).mirror().addBox(-1.0f, -0.5f, -1.0f, 2, 4, 2),
                PartPose.offset(-3.0f, -3.5f, 0.0f));
        brazoD.addOrReplaceChild("espada",
                CubeListBuilder.create()
                        .texOffs(16, 28).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 2)          // empuñadura
                        .texOffs(22, 28).addBox(-1.5f, -0.5f, -1.0f, 3, 1, 1)         // guarda
                        .texOffs(0, 36).addBox(-0.5f, -0.5f, -8.0f, 1, 1, 7),         // hoja
                PartPose.offset(0.0f, 3.0f, 0.0f));

        raiz.addOrReplaceChild("pierna_izquierda",
                CubeListBuilder.create().texOffs(8, 28).addBox(-1.0f, 0.0f, -1.0f, 2, 3, 2),
                PartPose.offset(1.0f, 21.0f, 0.0f));
        raiz.addOrReplaceChild("pierna_derecha",
                CubeListBuilder.create().texOffs(8, 28).mirror().addBox(-1.0f, 0.0f, -1.0f, 2, 3, 2),
                PartPose.offset(-1.0f, 21.0f, 0.0f));
        return LayerDefinition.create(malla, 64, 64);
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    /** Pone la espada apuntando hacia abajo (sea cual sea el ángulo del brazo). */
    private void espadaAbajo() {
        espada.xRot = Mth.HALF_PI - brazoD.xRot;
    }

    @Override
    public void setupAnim(Gnomo gnomo, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        int accion = gnomo.accion();
        float t = edad - gnomo.inicioAccionCliente;
        cantidadPaso = Math.min(cantidadPaso * 1.2f, 1.0f);
        float a = cantidadPaso;
        float g = paso * 1.15f;                         // zancadas muy rápidas
        boolean enElAire = !gnomo.onGround();

        // ---- Carrera ----
        piernaI.xRot = Mth.cos(g) * 1.4f * a;
        piernaD.xRot = Mth.cos(g + Mth.PI) * 1.4f * a;
        cuerpo.xRot = 0.35f * a;                         // se lanza hacia delante
        cuerpo.y = 21.0f - Math.abs(Mth.sin(g)) * 1.0f * a;
        piernaI.y = piernaD.y = 21.0f - Math.abs(Mth.sin(g)) * 0.5f * a;
        brazoI.xRot = Mth.cos(g + Mth.PI) * 1.1f * a;
        brazoD.xRot = -0.35f * a + Mth.cos(g) * 0.3f * a;   // la espada, lista, por delante
        brazoD.zRot = 0.2f * a;
        cabeza.xRot = inclinacionCabeza * Mth.DEG_TO_RAD - cuerpo.xRot * 0.8f;
        cabeza.yRot = giroCabeza * Mth.DEG_TO_RAD;
        cabeza.zRot = Mth.sin(g) * 0.08f * a;
        // gorro y barba con retraso: se van hacia atrás con la velocidad
        gorro2.xRot = -0.2f - 0.25f * a + Mth.sin(g - 1.0f) * 0.12f * a;
        gorro3.xRot = -0.35f - 0.35f * a + Mth.sin(g - 1.6f) * 0.18f * a;
        gorro2.zRot = Mth.sin(edad * 0.1f) * 0.05f;
        barba.xRot = -0.15f * a + Mth.sin(g - 0.8f) * 0.15f * a;

        // ---- Reposo: respira, mira, balancea la espada ----
        float reposo = 1.0f - a;
        cuerpo.xScale = 1.0f + 0.04f * Mth.sin(edad * 0.15f) * reposo;
        espada.xRot = Mth.sin(edad * 0.07f) * 0.08f * reposo;

        // ---- Saltitos: recoge las piernas ----
        if (enElAire && accion == Gnomo.NADA) {
            piernaI.xRot = -0.9f;
            piernaD.xRot = 0.5f;
            brazoI.xRot = -1.2f;
            brazoI.zRot = -0.5f;
            gorro2.xRot = -0.9f;
            gorro3.xRot = -0.8f;
        }

        switch (accion) {
            case Gnomo.AGACHARSE -> {
                float e = Mth.clamp(t / 4.0f, 0.0f, 1.0f);
                cuerpo.y += 1.6f * e;
                cuerpo.xRot = 0.55f * e;
                piernaI.xRot = -0.7f * e;
                piernaD.xRot = 0.6f * e;
                brazoD.xRot = 0.9f * e;                 // espada atrás
                brazoI.xRot = -0.8f * e;
                cabeza.xRot = -0.4f * e;                 // mira arriba, adonde va a saltar
            }
            case Gnomo.SUBIDA -> {
                // espada en alto con las dos manos; al llegar arriba la va girando hacia abajo
                float giro = Mth.clamp((t - 6) / 6.0f, 0.0f, 1.0f);
                brazoD.xRot = Mth.lerp(giro, -2.9f, -0.4f);
                brazoI.xRot = Mth.lerp(giro, -2.7f, -0.4f);
                brazoI.yRot = 0.4f;
                brazoD.yRot = -0.4f;
                espada.xRot = Mth.lerp(giro, 0.0f, Mth.HALF_PI - brazoD.xRot);
                piernaI.xRot = -0.8f;
                piernaD.xRot = -0.4f;
                cuerpo.xRot = -0.15f + 0.3f * giro;
                gorro2.xRot = 0.4f;                      // el aire lo empuja hacia arriba... al subir, abajo
                gorro3.xRot = 0.5f;
            }
            case Gnomo.PICADO -> {
                // en picado: recto, espada hacia abajo con las dos manos, gorro al viento hacia arriba
                brazoD.xRot = -0.35f;
                brazoI.xRot = -0.35f;
                brazoI.yRot = 0.5f;
                brazoD.yRot = -0.5f;
                espadaAbajo();
                piernaI.xRot = 0.25f;
                piernaD.xRot = 0.25f;
                cuerpo.xRot = 0.1f;
                cabeza.xRot = 0.5f;                      // mira abajo
                gorro.xRot = -0.3f;
                gorro2.xRot = -1.0f + Mth.sin(edad * 2.0f) * 0.15f;
                gorro3.xRot = -0.9f + Mth.sin(edad * 2.3f) * 0.2f;
                barba.xRot = -0.9f;                      // la barba hacia arriba
            }
            case Gnomo.ATASCADO -> {
                // tira de la espada clavada delante: echado atrás, a tirones, con las piernas abiertas
                float ciclo = (t % 12.0f) / 12.0f;
                float tiron = ciclo < 0.3f ? ciclo / 0.3f : 1.0f - (ciclo - 0.3f) / 0.7f;
                brazoD.xRot = -0.95f + 0.15f * tiron;
                brazoI.xRot = -0.95f + 0.15f * tiron;
                brazoI.yRot = 0.55f;
                brazoD.yRot = -0.55f;
                espadaAbajo();
                cuerpo.xRot = -0.25f - 0.25f * tiron;
                cuerpo.y = 21.0f + 0.6f;
                piernaI.zRot = -0.35f;
                piernaD.zRot = 0.35f;
                piernaI.xRot = -0.3f - 0.2f * tiron;
                piernaD.xRot = -0.3f - 0.2f * tiron;
                cabeza.xRot = -0.3f * tiron + 0.2f;
                cabeza.zRot = Mth.sin(t * 0.9f) * 0.12f;  // resopla y sacude la cabeza
                gorro2.zRot = Mth.sin(t * 0.8f) * 0.25f;
                gorro3.zRot = Mth.sin(t * 0.8f - 0.6f) * 0.3f;
            }
            case Gnomo.AVISO_TAJO -> {
                float e = Mth.clamp(t / 4.0f, 0.0f, 1.0f);
                cuerpo.y += 1.2f * e;
                cuerpo.xRot = 0.5f * e;
                cuerpo.yRot = 0.6f * e;                  // se gira para coger impulso
                brazoD.xRot = 0.5f * e;
                brazoD.yRot = 0.9f * e;                  // espada atrás, cruzada
                brazoD.zRot = 0.6f * e;
                brazoI.xRot = -0.9f * e;
                piernaI.xRot = -0.8f * e;
                piernaD.xRot = 0.7f * e;
                cabeza.yRot -= 0.6f * e;                 // sin dejar de mirarte
            }
            case Gnomo.DASH -> {
                // estirado como una flecha: espada al frente, el otro brazo atrás, piernas abiertas
                cuerpo.xRot = 0.75f;
                cuerpo.yRot = -0.2f;
                brazoD.xRot = -Mth.HALF_PI - 0.75f;
                brazoD.yRot = 0.0f;
                brazoD.zRot = 0.0f;
                espada.xRot = Mth.HALF_PI;
                brazoI.xRot = 1.2f;
                piernaI.xRot = -1.1f;
                piernaD.xRot = 1.0f;
                cabeza.xRot = -0.6f;
                gorro2.xRot = -1.2f;
                gorro3.xRot = -1.0f;
                barba.xRot = -0.8f;
            }
            case Gnomo.RECUPERAR -> {
                // el tajo sigue de largo y vuelve a la guardia
                float e = 1.0f - Mth.clamp(t / 8.0f, 0.0f, 1.0f);
                brazoD.xRot = -1.2f * e;
                brazoD.yRot = -0.9f * e;
                cuerpo.yRot = -0.5f * e;
                cuerpo.xRot = 0.3f * e;
                espada.xRot = 0.6f * e;
            }
            default -> {}
        }
    }
}
