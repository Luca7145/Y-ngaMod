package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Duende;
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
 * Modelo del duende: cabezón con máscara, orejas puntiagudas, capucha con pico, cuerpecillo, brazos largos,
 * piernas cortas y la cerbatana. Algo más de 1 bloque. Textura de 64x64 (herramientas/dibujar_duende.py),
 * una por máscara.
 *
 * Animaciones (nerviosas): corre a saltitos con los brazos sueltos y la cabeza dando bandazos, orejas y pico de
 * la capucha que se bambolean, tics de cabeza en reposo, soplar (se agacha, se lleva la cerbatana a la boca,
 * hincha los carrillos y da un respingo al soplar) y floritura al reaparecer (gira sobre sí mismo con los
 * brazos abiertos).
 */
public class ModeloDuende extends HierarchicalModel<Duende> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "duende"), "main");

    private final ModelPart raiz, cuerpo, cabeza, orejaI, orejaD, pico, brazoI, brazoD, piernaI, piernaD;
    private final ModelPart cerbatanaMano, cerbatanaBoca;

    public ModeloDuende(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.cabeza = cuerpo.getChild("cabeza");
        this.orejaI = cabeza.getChild("oreja_izquierda");
        this.orejaD = cabeza.getChild("oreja_derecha");
        this.pico = cabeza.getChild("pico");
        this.cerbatanaBoca = cabeza.getChild("cerbatana_boca");
        this.brazoI = cuerpo.getChild("brazo_izquierdo");
        this.brazoD = cuerpo.getChild("brazo_derecho");
        this.cerbatanaMano = brazoD.getChild("cerbatana");
        this.piernaI = raiz.getChild("pierna_izquierda");
        this.piernaD = raiz.getChild("pierna_derecha");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.5f, -4.0f, -1.5f, 5, 4, 3),
                PartPose.offset(0.0f, 20.0f, 0.0f));

        PartDefinition cabeza = cuerpo.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-3.5f, -6.0f, -3.5f, 7, 6, 7)
                        .texOffs(28, 8).addBox(-3.5f, -6.0f, -4.5f, 7, 6, 1)          // la máscara
                        .texOffs(0, 22).addBox(-2.5f, -8.0f, -2.5f, 5, 2, 5),         // capucha
                PartPose.offset(0.0f, -4.0f, 0.0f));
        cabeza.addOrReplaceChild("pico",
                CubeListBuilder.create().texOffs(20, 22).addBox(-1.0f, -2.0f, -1.0f, 2, 2, 2),
                PartPose.offsetAndRotation(0.0f, -8.0f, 0.5f, -0.3f, 0.0f, 0.0f));
        cabeza.addOrReplaceChild("oreja_izquierda",
                CubeListBuilder.create().texOffs(28, 16).addBox(0.0f, -1.0f, -0.5f, 4, 2, 1),
                PartPose.offsetAndRotation(3.5f, -3.5f, 0.5f, 0.0f, -0.3f, -0.35f));
        cabeza.addOrReplaceChild("oreja_derecha",
                CubeListBuilder.create().texOffs(28, 16).mirror().addBox(-4.0f, -1.0f, -0.5f, 4, 2, 1),
                PartPose.offsetAndRotation(-3.5f, -3.5f, 0.5f, 0.0f, 0.3f, 0.35f));
        cabeza.addOrReplaceChild("cerbatana_boca",
                CubeListBuilder.create().texOffs(0, 32).addBox(-0.5f, -0.5f, -10.0f, 1, 1, 10),
                PartPose.offset(0.0f, -1.5f, -4.5f));

        cuerpo.addOrReplaceChild("brazo_izquierdo",
                CubeListBuilder.create().texOffs(28, 22).addBox(-1.0f, -0.5f, -1.0f, 2, 6, 2),
                PartPose.offsetAndRotation(3.0f, -3.5f, 0.0f, 0.0f, 0.0f, -0.15f));
        PartDefinition brazoD = cuerpo.addOrReplaceChild("brazo_derecho",
                CubeListBuilder.create().texOffs(28, 22).mirror().addBox(-1.0f, -0.5f, -1.0f, 2, 6, 2),
                PartPose.offsetAndRotation(-3.0f, -3.5f, 0.0f, 0.0f, 0.0f, 0.15f));
        brazoD.addOrReplaceChild("cerbatana",
                CubeListBuilder.create().texOffs(0, 32).addBox(-0.5f, -0.5f, -5.0f, 1, 1, 10),
                PartPose.offset(0.0f, 5.0f, 0.0f));

        raiz.addOrReplaceChild("pierna_izquierda",
                CubeListBuilder.create().texOffs(36, 22).addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2),
                PartPose.offset(1.3f, 20.0f, 0.0f));
        raiz.addOrReplaceChild("pierna_derecha",
                CubeListBuilder.create().texOffs(36, 22).mirror().addBox(-1.0f, 0.0f, -1.0f, 2, 4, 2),
                PartPose.offset(-1.3f, 20.0f, 0.0f));

        return LayerDefinition.create(malla, 64, 64);
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(Duende duende, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        cabeza.xScale = 1.0f;
        int accion = duende.accion();
        float t = edad - duende.inicioAccionCliente;
        cantidadPaso = Math.min(cantidadPaso, 1.0f);
        float a = cantidadPaso;
        float g = paso * 0.9f;              // pasitos rápidos

        boolean soplando = accion == Duende.SOPLAR && t < 18;
        cerbatanaBoca.visible = soplando;
        cerbatanaMano.visible = !soplando;

        // ---- Correr a saltitos ----
        piernaI.xRot = Mth.cos(g) * 1.3f * a;
        piernaD.xRot = Mth.cos(g + Mth.PI) * 1.3f * a;
        float salto = Math.abs(Mth.sin(g)) * 1.6f * a;
        cuerpo.y = 20.0f - salto;
        piernaI.y = piernaD.y = 20.0f - salto * 0.6f;
        cuerpo.xRot = 0.3f * a;                       // se echa adelante al correr
        brazoI.xRot = Mth.cos(g + Mth.PI) * 1.2f * a;
        brazoD.xRot = Mth.cos(g) * 1.2f * a;
        brazoI.zRot = -0.15f - 0.5f * a * Math.abs(Mth.sin(g * 0.5f));   // brazos sueltos, al aire
        brazoD.zRot = 0.15f + 0.5f * a * Math.abs(Mth.cos(g * 0.5f));
        cabeza.zRot = Mth.sin(g) * 0.18f * a;          // bandazos de cabeza
        cabeza.xRot = inclinacionCabeza * Mth.DEG_TO_RAD - cuerpo.xRot;
        cabeza.yRot = giroCabeza * Mth.DEG_TO_RAD;

        // ---- Orejas y pico: se bambolean (con retraso) ----
        float bamboleo = Mth.sin(g - 0.8f) * 0.35f * a + Mth.sin(edad * 0.21f) * 0.06f;
        orejaI.zRot = -0.35f + bamboleo;
        orejaD.zRot = 0.35f - bamboleo;
        orejaI.yRot = -0.3f + Mth.sin(edad * 0.13f) * 0.05f;
        orejaD.yRot = 0.3f - Mth.sin(edad * 0.13f) * 0.05f;
        pico.xRot = -0.3f - Mth.sin(g - 1.2f) * 0.4f * a + Mth.sin(edad * 0.1f) * 0.08f;
        pico.zRot = Mth.sin(g * 0.5f - 1.0f) * 0.3f * a;

        // ---- Tics en reposo: cabeza que gira de golpe y orejas que se mueven ----
        float reposo = 1.0f - a;
        float ciclo = (edad + duende.getId() * 17) % 45.0f;
        if (ciclo < 4.0f) {
            float tic = Mth.sin(ciclo / 4.0f * Mth.PI) * reposo;
            cabeza.zRot += tic * 0.5f * (duende.getId() % 2 == 0 ? 1 : -1);
            orejaI.zRot -= tic * 0.4f;
            orejaD.zRot += tic * 0.4f;
        }
        float respira = Mth.sin(edad * 0.15f);
        cuerpo.xScale = 1.0f + 0.04f * respira;
        cuerpo.zScale = 1.0f + 0.04f * respira;

        // ---- Acciones ----
        if (soplando) {
            float subir = Mth.clamp(t / 5.0f, 0.0f, 1.0f);
            float sopla = t >= 12 ? Math.max(0.0f, 1.0f - (t - 12) / 6.0f) : 0.0f;
            cuerpo.y += 1.0f * subir;                 // se agacha
            cuerpo.xRot += 0.2f * subir;
            cabeza.xRot -= 0.2f * subir + 0.25f * sopla;   // respingo hacia atrás al soplar
            brazoI.xRot = -1.35f * subir;              // sujeta la cerbatana con las dos manos
            brazoD.xRot = -1.35f * subir;
            brazoI.yRot = -0.55f * subir;
            brazoD.yRot = 0.55f * subir;
            brazoI.zRot = 0.0f;
            brazoD.zRot = 0.0f;
            float carrillos = t > 8 && t < 13 ? 0.12f : 0.0f;   // hincha los carrillos justo antes
            cabeza.xScale = 1.0f + carrillos;
        } else if (accion == Duende.FLORITURA) {
            float p = Mth.clamp(t / 12.0f, 0.0f, 1.0f);
            float e = Mth.sin(p * Mth.PI);
            cuerpo.yRot = p * Mth.TWO_PI;              // una vuelta entera
            brazoI.zRot = -1.6f * e;                   // brazos abiertos
            brazoD.zRot = 1.6f * e;
            cuerpo.y -= 2.0f * e;                      // saltito
            cabeza.zRot += Mth.sin(t * 1.3f) * 0.2f * e;
        }
    }
}
