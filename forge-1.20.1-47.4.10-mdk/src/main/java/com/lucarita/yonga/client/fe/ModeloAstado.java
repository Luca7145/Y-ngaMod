package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.Astado;
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
 * Modelo del Astado: humanoide largo y famélico (unos 3 bloques de pie), torso estrecho y encorvado con las
 * costillas y la columna a la vista, cráneo de ciervo con mandíbula, dos grandes astas de cuatro puntas,
 * brazos larguísimos con garras y piernas flacas. Textura de 128x64 (herramientas/dibujar_astado.py).
 *
 * Animaciones (todas por código, mezcladas):
 *  - De pie <-> a cuatro patas, pasando poco a poco (Astado.caza va de 0 a 1).
 *  - Andar de pie (encorvado, brazos colgando) y galopar a cuatro patas.
 *  - Respirar (el pecho se hincha y la mandíbula se mueve) y tics bruscos de la cabeza.
 *  - Rugido (se alza, echa la cabeza atrás, abre los brazos y tiembla), preparar la embestida (cabeza abajo
 *    con las astas por delante y escarbando), zarpazo, agarre (brazos delante y mordiscos) y lanzar.
 */
public class ModeloAstado extends HierarchicalModel<Astado> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "astado"), "main");

    // Inclinación del torso: de pie (encorvado) y a cuatro patas
    private static final float TORSO_DE_PIE = 0.32f, TORSO_CAZA = 1.45f;
    private static final float CADERA_DE_PIE = 0.0f, CADERA_CAZA = 6.0f;

    private final ModelPart raiz, cadera, torso, cuello, cabeza, mandibula;
    private final ModelPart brazoI, antebrazoI, manoI, brazoD, antebrazoD, manoD;
    private final ModelPart piernaI, espinillaI, pieI, piernaD, espinillaD, pieD;

    public ModeloAstado(ModelPart raiz) {
        this.raiz = raiz;
        this.cadera = raiz.getChild("cadera");
        this.torso = cadera.getChild("torso");
        this.cuello = torso.getChild("cuello");
        this.cabeza = cuello.getChild("cabeza");
        this.mandibula = cabeza.getChild("mandibula");
        this.brazoI = torso.getChild("brazo_izquierdo");
        this.antebrazoI = brazoI.getChild("antebrazo");
        this.manoI = antebrazoI.getChild("mano");
        this.brazoD = torso.getChild("brazo_derecho");
        this.antebrazoD = brazoD.getChild("antebrazo");
        this.manoD = antebrazoD.getChild("mano");
        this.piernaI = raiz.getChild("pierna_izquierda");
        this.espinillaI = piernaI.getChild("espinilla");
        this.pieI = espinillaI.getChild("pie");
        this.piernaD = raiz.getChild("pierna_derecha");
        this.espinillaD = piernaD.getChild("espinilla");
        this.pieD = espinillaD.getChild("pie");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        // Pelvis; de ella sale el torso (las piernas van aparte para poder agacharse sin que giren con él)
        PartDefinition cadera = raiz.addOrReplaceChild("cadera",
                CubeListBuilder.create().texOffs(56, 0).addBox(-3.5f, -2.0f, -2.0f, 7, 4, 4),
                PartPose.offset(0.0f, 0.0f, 0.0f));

        // Torso estrecho, con las vértebras saliendo por la espalda
        CubeListBuilder torsoCubos = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -13.0f, -2.5f, 8, 13, 5);
        for (int i = 0; i < 5; i++) {
            torsoCubos.texOffs(70, 8).addBox(-0.5f, -12.5f + i * 2.6f, 2.5f, 1, 1, 2);
        }
        PartDefinition torso = cadera.addOrReplaceChild("torso", torsoCubos,
                PartPose.offsetAndRotation(0.0f, -2.0f, 0.0f, TORSO_DE_PIE, 0.0f, 0.0f));

        // Cuello flaco y cráneo de ciervo (cráneo + hocico), con la mandíbula aparte para abrirla
        PartDefinition cuello = torso.addOrReplaceChild("cuello",
                CubeListBuilder.create().texOffs(56, 8).addBox(-1.5f, -4.0f, -1.5f, 3, 4, 3),
                PartPose.offsetAndRotation(0.0f, -13.0f, -0.5f, -0.25f, 0.0f, 0.0f));
        PartDefinition cabeza = cuello.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-3.0f, -6.0f, -4.0f, 6, 6, 7)
                        .texOffs(80, 0).addBox(-2.0f, -3.0f, -8.0f, 4, 3, 4),
                PartPose.offset(0.0f, -4.0f, -0.5f));
        cabeza.addOrReplaceChild("mandibula",
                CubeListBuilder.create().texOffs(98, 0).addBox(-2.0f, 0.0f, -5.0f, 4, 1, 5),
                PartPose.offset(0.0f, 0.0f, -3.0f));

        // Las astas: una rama principal hacia arriba y afuera, con tres puntas
        astas(cabeza, "asta_izquierda", 1.0f);
        astas(cabeza, "asta_derecha", -1.0f);

        // Brazos larguísimos: brazo, antebrazo (casi hueso) y mano con tres garras
        brazo(torso, "brazo_izquierdo", 1.0f);
        brazo(torso, "brazo_derecho", -1.0f);

        // Piernas flacas: muslo, espinilla y pie (el suelo está en y = 24)
        pierna(raiz, "pierna_izquierda", 1.0f);
        pierna(raiz, "pierna_derecha", -1.0f);

        return LayerDefinition.create(malla, 128, 64);
    }

    private static void astas(PartDefinition cabeza, String nombre, float lado) {
        PartDefinition asta = cabeza.addOrReplaceChild(nombre,
                CubeListBuilder.create()
                        .texOffs(102, 8).addBox(-1.0f, -4.0f, -1.0f, 2, 4, 2)      // base gruesa
                        .texOffs(78, 8).addBox(-0.5f, -14.0f, -0.5f, 1, 10, 1),   // rama principal
                PartPose.offsetAndRotation(lado * 2.0f, -5.5f, -1.0f, -0.3f, 0.0f, lado * 0.6f));
        asta.addOrReplaceChild("punta_afuera",
                CubeListBuilder.create().texOffs(84, 8).addBox(-0.5f, -7.0f, -0.5f, 1, 7, 1),
                PartPose.offsetAndRotation(0.0f, -5.0f, 0.0f, 0.0f, 0.0f, lado * 0.85f));
        asta.addOrReplaceChild("punta_arriba",
                CubeListBuilder.create().texOffs(90, 8).addBox(-0.5f, -8.0f, -0.5f, 1, 8, 1),
                PartPose.offsetAndRotation(0.0f, -13.5f, 0.0f, -0.35f, 0.0f, -lado * 0.5f));
        asta.addOrReplaceChild("punta_media",
                CubeListBuilder.create().texOffs(84, 8).addBox(-0.5f, -7.0f, -0.5f, 1, 7, 1),
                PartPose.offsetAndRotation(0.0f, -10.0f, 0.0f, 0.2f, 0.0f, lado * 0.9f));
        asta.addOrReplaceChild("punta_delante",
                CubeListBuilder.create().texOffs(96, 8).addBox(-0.5f, -5.0f, -0.5f, 1, 5, 1),
                PartPose.offsetAndRotation(0.0f, -3.0f, 0.0f, -1.0f, 0.0f, -lado * 0.3f));
    }

    private static void brazo(PartDefinition torso, String nombre, float lado) {
        CubeListBuilder cubo = CubeListBuilder.create().texOffs(0, 20);
        if (lado < 0) cubo.mirror();
        PartDefinition brazo = torso.addOrReplaceChild(nombre, cubo.addBox(-1.5f, -1.0f, -1.5f, 3, 14, 3),
                PartPose.offsetAndRotation(lado * 5.0f, -12.0f, 0.0f, 0.0f, 0.0f, lado * -0.1f));
        CubeListBuilder ante = CubeListBuilder.create().texOffs(14, 20);
        if (lado < 0) ante.mirror();
        PartDefinition antebrazo = brazo.addOrReplaceChild("antebrazo", ante.addBox(-1.0f, 0.0f, -1.0f, 2, 12, 2),
                PartPose.offset(0.0f, 12.5f, 0.0f));
        CubeListBuilder mano = CubeListBuilder.create().texOffs(24, 20);
        if (lado < 0) mano.mirror();
        mano.addBox(-1.5f, 0.0f, -1.5f, 3, 2, 3)
                .texOffs(38, 20).addBox(-1.5f, 2.0f, -1.5f, 1, 4, 1)
                .texOffs(38, 20).addBox(0.5f, 2.0f, -1.5f, 1, 4, 1)
                .texOffs(38, 20).addBox(-0.5f, 2.0f, 0.5f, 1, 4, 1);
        antebrazo.addOrReplaceChild("mano", mano, PartPose.offset(0.0f, 12.0f, 0.0f));
    }

    private static void pierna(PartDefinition raiz, String nombre, float lado) {
        CubeListBuilder muslo = CubeListBuilder.create().texOffs(44, 20);
        if (lado < 0) muslo.mirror();
        PartDefinition pierna = raiz.addOrReplaceChild(nombre, muslo.addBox(-1.5f, 0.0f, -1.5f, 3, 12, 3),
                PartPose.offset(lado * 2.5f, 0.0f, 0.0f));
        CubeListBuilder espinilla = CubeListBuilder.create().texOffs(58, 20);
        if (lado < 0) espinilla.mirror();
        PartDefinition parteBaja = pierna.addOrReplaceChild("espinilla", espinilla.addBox(-1.0f, 0.0f, -1.0f, 2, 11, 2),
                PartPose.offset(0.0f, 12.0f, 0.0f));
        CubeListBuilder pie = CubeListBuilder.create().texOffs(68, 20);
        if (lado < 0) pie.mirror();
        parteBaja.addOrReplaceChild("pie", pie.addBox(-1.5f, 0.0f, -3.0f, 3, 1, 4), PartPose.offset(0.0f, 11.0f, 0.0f));
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(Astado astado, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float parcial = edad - astado.tickCount;
        float f = Mth.lerp(parcial, astado.caza0, astado.caza);       // 0 de pie, 1 a cuatro patas
        f = f * f * (3 - 2 * f);                                       // suavizado
        float g = 1.0f - f;
        cantidadPaso = Math.min(cantidadPaso, 1.0f);

        // ---- Postura base (mezcla de pie / a cuatro patas) ----
        cadera.y = Mth.lerp(f, CADERA_DE_PIE, CADERA_CAZA);
        cadera.z = 5.0f * f;                     // a cuatro patas el torso va hacia delante: todo un poco atrás
        piernaI.y = piernaD.y = cadera.y + 1.0f;
        piernaI.z = piernaD.z = cadera.z;
        float inclinacion = Mth.lerp(f, TORSO_DE_PIE, TORSO_CAZA);
        torso.xRot = inclinacion;

        // Cabeza: compensa la inclinación del torso para mirar hacia delante, y sigue al objetivo
        float giro = giroCabeza * Mth.DEG_TO_RAD;
        float cabeceo = inclinacionCabeza * Mth.DEG_TO_RAD;
        cuello.xRot = Mth.lerp(f, -0.25f, -0.75f);
        cabeza.xRot = -inclinacion - cuello.xRot + cabeceo * 0.6f;
        cuello.yRot = giro * 0.5f;
        cabeza.yRot = giro * 0.5f;

        // Brazos: de pie cuelgan; a cuatro patas apoyan en el suelo (contrarrestan el torso), algo doblados
        float brazoBase = Mth.lerp(f, -inclinacion + 0.12f, -inclinacion - 0.05f);
        brazoI.xRot = brazoD.xRot = brazoBase;
        antebrazoI.xRot = antebrazoD.xRot = Mth.lerp(f, -0.15f, -1.0f);
        manoI.xRot = manoD.xRot = Mth.lerp(f, 0.1f, 1.0f);
        brazoI.zRot = -0.1f - 0.12f * g;
        brazoD.zRot = 0.1f + 0.12f * g;

        // Piernas: de pie rectas; a cuatro patas dobladas (rodilla adelante)
        piernaI.xRot = piernaD.xRot = Mth.lerp(f, 0.0f, -0.85f);
        espinillaI.xRot = espinillaD.xRot = Mth.lerp(f, 0.05f, 1.45f);
        pieI.xRot = pieD.xRot = -piernaI.xRot - espinillaI.xRot;

        // ---- Andar de pie ----
        float andar = paso * 0.55f;
        float am = cantidadPaso * g;
        piernaI.xRot += Mth.cos(andar) * 0.75f * am;
        piernaD.xRot += Mth.cos(andar + Mth.PI) * 0.75f * am;
        espinillaI.xRot += Math.max(0.0f, Mth.sin(andar)) * 0.9f * am;
        espinillaD.xRot += Math.max(0.0f, Mth.sin(andar + Mth.PI)) * 0.9f * am;
        brazoI.xRot += Mth.cos(andar + Mth.PI) * 0.35f * am;
        brazoD.xRot += Mth.cos(andar) * 0.35f * am;
        antebrazoI.xRot -= Math.max(0.0f, Mth.cos(andar)) * 0.3f * am;
        antebrazoD.xRot -= Math.max(0.0f, Mth.cos(andar + Mth.PI)) * 0.3f * am;
        torso.yRot += Mth.cos(andar) * 0.08f * am;
        cadera.y += Math.abs(Mth.sin(andar)) * -0.8f * am;

        // ---- Galope a cuatro patas ----
        int accion = astado.accion();
        float t = edad - astado.inicioAccionCliente;
        float galope = (accion == Astado.EMBESTIDA ? edad * 1.5f : paso * 0.45f);
        float ag = (accion == Astado.EMBESTIDA ? 1.0f : cantidadPaso) * f;
        brazoI.xRot += Mth.sin(galope) * 0.85f * ag;
        brazoD.xRot += Mth.sin(galope + 0.5f) * 0.85f * ag;
        antebrazoI.xRot += Math.min(0.0f, Mth.cos(galope)) * 0.6f * ag;
        antebrazoD.xRot += Math.min(0.0f, Mth.cos(galope + 0.5f)) * 0.6f * ag;
        piernaI.xRot += Mth.sin(galope + Mth.PI) * 0.7f * ag;
        piernaD.xRot += Mth.sin(galope + Mth.PI + 0.5f) * 0.7f * ag;
        espinillaI.xRot += Math.max(0.0f, Mth.cos(galope + Mth.PI)) * 0.5f * ag;
        espinillaD.xRot += Math.max(0.0f, Mth.cos(galope + Mth.PI + 0.5f)) * 0.5f * ag;
        torso.xRot += Mth.sin(galope * 2.0f) * 0.07f * ag;
        cadera.y += Mth.sin(galope * 2.0f) * -1.2f * ag;
        cabeza.xRot -= Mth.sin(galope * 2.0f) * 0.07f * ag;

        // ---- Respirar y tics ----
        float respira = Mth.sin(edad * (0.10f + 0.08f * f));
        torso.zScale = 1.0f + 0.05f * respira;
        torso.xScale = 1.0f + 0.03f * respira;
        mandibula.xRot = 0.12f + 0.08f * respira + 0.15f * f;
        float ciclo = (edad + astado.getId() * 13) % 70.0f;
        if (ciclo < 5.0f) {
            // giro de cabeza brusco, como un animal que escucha
            float tic = Mth.sin(ciclo / 5.0f * Mth.PI);
            cabeza.zRot += tic * 0.45f * (astado.getId() % 2 == 0 ? 1 : -1);
            cabeza.yRot += tic * 0.25f;
        }

        // ---- Acciones ----
        switch (accion) {
            case Astado.RUGIDO -> rugido(t);
            case Astado.PREPARA_EMBESTIDA -> prepararEmbestida(t);
            case Astado.EMBESTIDA -> {
                cuello.xRot += 0.55f;
                cabeza.xRot += 0.65f;
                mandibula.xRot = 0.5f;
            }
            case Astado.ZARPAZO -> zarpazo(t, astado.getId() % 2 == 0);
            case Astado.AGARRE -> agarre(t);
            case Astado.LANZAR -> lanzar(t);
            default -> {}
        }
    }

    /** 0 -> 1 al empezar (en "entrada" ticks) y 1 -> 0 al acabar (en "salida" ticks). */
    private static float envolvente(float t, float duracion, float entrada, float salida) {
        if (t < 0) return 0.0f;
        float a = Mth.clamp(t / entrada, 0.0f, 1.0f);
        float b = Mth.clamp((duracion - t) / salida, 0.0f, 1.0f);
        return Math.min(a, b);
    }

    private void rugido(float t) {
        float e = envolvente(t, 40, 8, 10);
        float temblor = Mth.sin(t * 3.1f) * 0.08f * e;
        torso.xRot -= 0.7f * e;                         // se alza
        cuello.xRot -= 0.5f * e;
        cabeza.xRot -= 0.6f * e;                         // cabeza atrás
        cabeza.zRot += temblor;
        cabeza.yRot += temblor * 0.6f;
        mandibula.xRot = Mth.lerp(e, mandibula.xRot, 1.0f + Mth.sin(t * 2.3f) * 0.1f);
        brazoI.xRot -= 0.6f * e;
        brazoD.xRot -= 0.6f * e;
        brazoI.zRot -= 1.0f * e;                         // brazos abiertos
        brazoD.zRot += 1.0f * e;
        antebrazoI.xRot -= 0.5f * e;
        antebrazoD.xRot -= 0.5f * e;
        manoI.xRot += temblor * 3;
        manoD.xRot -= temblor * 3;
    }

    private void prepararEmbestida(float t) {
        float e = envolvente(t, 16, 4, 2);
        cadera.y += 2.0f * e;                            // se agacha
        cuello.xRot += 0.6f * e;                          // cabeza abajo, astas por delante
        cabeza.xRot += 0.7f * e;
        cabeza.zRot += Mth.sin(t * 1.7f) * 0.12f * e;      // sacude las astas
        mandibula.xRot = 0.35f;
        // escarba el suelo con una garra y luego con la otra
        brazoI.xRot += Mth.sin(t * 1.3f) * 0.55f * e;
        brazoD.xRot += Mth.sin(t * 1.3f + Mth.PI) * 0.55f * e;
    }

    private void zarpazo(float t, boolean izquierdo) {
        ModelPart brazo = izquierdo ? brazoI : brazoD;
        ModelPart antebrazo = izquierdo ? antebrazoI : antebrazoD;
        float lado = izquierdo ? 1 : -1;
        float levantar, golpe;
        if (t < 6) {                       // carga: levanta el brazo
            levantar = t / 6.0f;
            golpe = 0;
        } else if (t < 9) {                // golpe rápido hacia abajo y delante
            levantar = 1.0f - (t - 6) / 3.0f;
            golpe = (t - 6) / 3.0f;
        } else {                           // vuelve
            levantar = 0;
            golpe = Math.max(0.0f, 1.0f - (t - 9) / 5.0f);
        }
        brazo.xRot += -2.4f * levantar - 0.9f * golpe;
        brazo.zRot += lado * (-0.5f * levantar + 0.3f * golpe);
        antebrazo.xRot += -0.6f * levantar;
        torso.yRot += lado * (0.35f * levantar - 0.45f * golpe);
        cabeza.zRot += lado * 0.2f * golpe;
        mandibula.xRot = 0.3f + 0.5f * golpe;
    }

    private void agarre(float t) {
        float e = envolvente(t, 58, 4, 4);
        // Brazos por delante sujetando
        brazoI.xRot += -1.05f * e;
        brazoD.xRot += -1.05f * e;
        brazoI.zRot += 0.45f * e;
        brazoD.zRot -= 0.45f * e;
        antebrazoI.xRot += -0.5f * e;
        antebrazoD.xRot += -0.5f * e;
        torso.xRot -= 0.35f * e;
        // Mordiscos: abre y cierra de golpe cada 16 ticks, lanzando el cuello hacia delante
        float ciclo = (t % 16.0f) / 16.0f;
        float abre = ciclo < 0.7f ? ciclo / 0.7f : 1.0f - (ciclo - 0.7f) / 0.3f;
        mandibula.xRot = 0.1f + 1.0f * abre * e;
        cuello.xRot += (0.35f * (1 - abre) - 0.25f * abre) * e;
        cabeza.xRot += (-0.2f * abre) * e;
        cabeza.zRot += Mth.sin(t * 0.9f) * 0.15f * e;
    }

    private void lanzar(float t) {
        float e = envolvente(t, 12, 2, 6);
        brazoI.xRot += -2.6f * e;
        brazoD.xRot += -2.6f * e;
        brazoI.zRot -= 0.3f * e;
        brazoD.zRot += 0.3f * e;
        torso.xRot -= 0.5f * e;
        cuello.xRot -= 0.4f * e;
        mandibula.xRot = 0.9f * e;
    }
}
