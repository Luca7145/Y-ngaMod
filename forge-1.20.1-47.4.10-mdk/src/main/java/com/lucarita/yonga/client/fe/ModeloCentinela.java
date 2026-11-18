package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Centinela;
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
 * Modelo del centinela de piedra: un gólem macizo de arenisca (casi 3 bloques) con una calabaza tallada por
 * cabeza, brazos de dos piezas con puños grandes y piernas gruesas. Textura de 128x128
 * (herramientas/dibujar_centinela.py); la cara de la calabaza brilla (capa de ojos).
 *
 * Animaciones: despertar (se sacude la arena, estira los brazos), andar pesado (el cuerpo se balancea a cada
 * paso), apuntar el rayo (adelanta la cabeza, los brazos atrás), pisotón (alza los brazos y cae con todo) y la
 * muerte (congelado, tiembla cada vez más hasta explotar).
 */
public class ModeloCentinela extends HierarchicalModel<Centinela> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "centinela"), "main");

    private final ModelPart raiz, cuerpo, cabeza, brazoI, antebrazoI, brazoD, antebrazoD, piernaI, piernaD;

    public ModeloCentinela(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.cabeza = cuerpo.getChild("cabeza");
        this.brazoI = cuerpo.getChild("brazo_izquierdo");
        this.antebrazoI = brazoI.getChild("antebrazo");
        this.brazoD = cuerpo.getChild("brazo_derecho");
        this.antebrazoD = brazoD.getChild("antebrazo");
        this.piernaI = raiz.getChild("pierna_izquierda");
        this.piernaD = raiz.getChild("pierna_derecha");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0f, -18.0f, -6.0f, 18, 18, 12),
                PartPose.offset(0.0f, 10.0f, 0.0f));
        cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create().texOffs(60, 0).addBox(-7.0f, -14.0f, -7.0f, 14, 14, 14),
                PartPose.offset(0.0f, -18.0f, 0.0f));
        brazo(cuerpo, "brazo_izquierdo", 1.0f);
        brazo(cuerpo, "brazo_derecho", -1.0f);

        CubeListBuilder pierna = CubeListBuilder.create().texOffs(52, 30).addBox(-3.5f, 0.0f, -3.5f, 7, 14, 7);
        raiz.addOrReplaceChild("pierna_izquierda", pierna, PartPose.offset(4.5f, 10.0f, 0.0f));
        raiz.addOrReplaceChild("pierna_derecha", CubeListBuilder.create().texOffs(52, 30).mirror()
                .addBox(-3.5f, 0.0f, -3.5f, 7, 14, 7), PartPose.offset(-4.5f, 10.0f, 0.0f));
        return LayerDefinition.create(malla, 128, 128);
    }

    private static void brazo(PartDefinition cuerpo, String nombre, float lado) {
        CubeListBuilder alto = CubeListBuilder.create().texOffs(0, 30);
        if (lado < 0) alto.mirror();
        PartDefinition brazo = cuerpo.addOrReplaceChild(nombre, alto.addBox(-3.0f, -2.0f, -3.0f, 6, 12, 6),
                PartPose.offsetAndRotation(lado * 12.0f, -15.0f, 0.0f, 0.0f, 0.0f, -lado * 0.08f));
        CubeListBuilder bajo = CubeListBuilder.create().texOffs(24, 30);
        if (lado < 0) bajo.mirror();
        brazo.addOrReplaceChild("antebrazo", bajo.addBox(-3.5f, 0.0f, -3.5f, 7, 13, 7), PartPose.offset(0.0f, 10.0f, 0.0f));
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(Centinela centinela, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float t = edad - centinela.inicioAccionCliente;
        cantidadPaso = Math.min(cantidadPaso, 1.0f);
        float a = cantidadPaso;
        float g = paso * 0.45f;

        // ---- Andar pesado ----
        piernaI.xRot = Mth.cos(g) * 0.7f * a;
        piernaD.xRot = Mth.cos(g + Mth.PI) * 0.7f * a;
        brazoI.xRot = Mth.cos(g + Mth.PI) * 0.45f * a;
        brazoD.xRot = Mth.cos(g) * 0.45f * a;
        antebrazoI.xRot = -0.15f - Math.max(0.0f, Mth.cos(g)) * 0.3f * a;
        antebrazoD.xRot = -0.15f - Math.max(0.0f, Mth.cos(g + Mth.PI)) * 0.3f * a;
        cuerpo.zRot = Mth.sin(g) * 0.06f * a;                 // carga el peso en cada pierna
        cuerpo.yRot = Mth.sin(g) * 0.08f * a;
        cuerpo.y = 10.0f + Math.abs(Mth.sin(g)) * 0.8f * a;   // cae a cada paso
        cabeza.yRot = giroCabeza * Mth.DEG_TO_RAD;
        cabeza.xRot = inclinacionCabeza * Mth.DEG_TO_RAD;
        cabeza.zRot = -cuerpo.zRot * 0.5f;

        // ---- Muriendo: congelado, tiembla cada vez más ----
        if (centinela.deathTime > 0) {
            float d = (centinela.deathTime + (edad - (int) edad)) / Centinela.TICKS_MORIR;
            float temblor = Mth.sin(edad * 3.1f) * 0.06f * d;
            cuerpo.zRot = temblor;
            cuerpo.x = Mth.sin(edad * 2.7f) * 0.6f * d;
            cabeza.zRot = -temblor * 2;
            cabeza.xRot = 0.3f * d;                           // la cabeza se le cae
            brazoI.zRot = -0.08f - 0.25f * d;                 // los brazos se le abren
            brazoD.zRot = 0.08f + 0.25f * d;
            piernaI.xRot = 0;
            piernaD.xRot = 0;
            return;
        }

        switch (centinela.accion()) {
            case Centinela.DESPERTAR -> {
                // se sacude la arena y estira los brazos
                float e = Mth.clamp(t / 40.0f, 0.0f, 1.0f);
                float sacudida = (1 - e) * Mth.sin(t * 2.4f) * 0.12f;
                cuerpo.zRot = sacudida;
                cabeza.zRot = -sacudida * 2;
                float estira = Mth.sin(e * Mth.PI);
                brazoI.zRot = -0.08f - 1.3f * estira;
                brazoD.zRot = 0.08f + 1.3f * estira;
                antebrazoI.xRot = -0.8f * estira;
                antebrazoD.xRot = -0.8f * estira;
                cabeza.xRot = -0.5f * estira;                 // mira al cielo
                cuerpo.xRot = -0.12f * estira;
            }
            case Centinela.APUNTAR -> {
                // adelanta la cabeza para apuntar, brazos hacia atrás; retroceso al disparar
                float carga = Mth.clamp(t / 10.0f, 0.0f, 1.0f);
                float retroceso = t >= 30 && t < 38 ? 1.0f - (t - 30) / 8.0f : 0.0f;
                cuerpo.xRot = 0.18f * carga - 0.3f * retroceso;
                cabeza.xRot += 0.15f * carga - 0.4f * retroceso;
                brazoI.xRot = 0.5f * carga;
                brazoD.xRot = 0.5f * carga;
                brazoI.zRot = -0.3f * carga;
                brazoD.zRot = 0.3f * carga;
                if (t < 30) {
                    cabeza.zRot += Mth.sin(t * 1.9f) * 0.03f * carga;   // vibra mientras carga
                }
            }
            case Centinela.PISOTON -> {
                float alza = t < 16 ? Mth.clamp(t / 12.0f, 0.0f, 1.0f) : Math.max(0.0f, 1.0f - (t - 16) / 2.0f);
                float golpe = t >= 16 ? Math.max(0.0f, 1.0f - (t - 18) / 8.0f) : 0.0f;
                brazoI.xRot = -2.9f * alza + 0.4f * golpe;    // los dos puños arriba...
                brazoD.xRot = -2.9f * alza + 0.4f * golpe;    // ...y abajo con todo
                antebrazoI.xRot = -0.4f * alza;
                antebrazoD.xRot = -0.4f * alza;
                cuerpo.xRot = -0.15f * alza + 0.35f * golpe;
                cuerpo.y -= 1.5f * alza;
                piernaI.xRot = -0.6f * alza;                  // levanta una pierna
                cabeza.xRot += 0.2f * golpe;
            }
            case Centinela.AGOTADO -> {
                // agotado tras el rayo: cabeza y brazos caídos, el cuerpo vencido hacia delante, respira hondo
                float e = Math.min(Mth.clamp(t / 6.0f, 0.0f, 1.0f), Mth.clamp((70 - t) / 10.0f, 0.0f, 1.0f));
                float respira = Mth.sin(t * 0.25f) * 0.05f;
                cuerpo.xRot = (0.3f + respira) * e;
                cabeza.xRot += 0.45f * e;
                brazoI.xRot = 0.15f * e;
                brazoD.xRot = 0.15f * e;
                brazoI.zRot = -0.08f + 0.1f * e;
                brazoD.zRot = 0.08f - 0.1f * e;
                antebrazoI.xRot = 0.0f;
                antebrazoD.xRot = 0.0f;
                cuerpo.y += 1.0f * e;
            }
            default -> {}
        }
    }
}
