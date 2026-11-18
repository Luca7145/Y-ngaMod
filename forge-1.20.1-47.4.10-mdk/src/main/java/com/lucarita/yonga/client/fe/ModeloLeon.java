package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.mobs.LeonDeGuerra;
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
 * Modelo del león de guerra: cuerpo de león (con cola y borla) y, donde iría el cuello, el torso de un
 * guerrero con coraza, hombreras y una gran melena. Unos 2,6 bloques de alto. Textura de 128x128
 * (herramientas/dibujar_leon.py). Las dos variantes comparten modelo: el arquero enseña el arco (y la flecha
 * al tensar) y el espadachín el sable curvo y el escudo.
 *
 * Animaciones: galope (patas por parejas, el cuerpo cabecea, la cola se estira, el torso se inclina),
 * respiración y cola que se mueve en reposo, rugido, guardia con el escudo, espadazo (carga detrás y barrido
 * con giro del torso), zarpazo (se alza sobre las patas de atrás), escudazo, tambaleo y tensar el arco.
 */
public class ModeloLeon extends HierarchicalModel<LeonDeGuerra> {

    public static final ModelLayerLocation CAPA = new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "leon_de_guerra"), "main");

    private final ModelPart raiz, cuerpo, torso, cabeza;
    private final ModelPart pataDI, pataDD, pataTI, pataTD, bajaDI, bajaDD, bajaTI, bajaTD;
    private final ModelPart cola, cola2, borla;
    private final ModelPart brazoD, antebrazoD, brazoI, antebrazoI;
    private final ModelPart sable, flecha, escudo, arco;

    public ModeloLeon(ModelPart raiz) {
        this.raiz = raiz;
        this.cuerpo = raiz.getChild("cuerpo");
        this.pataDI = cuerpo.getChild("pata_delantera_izquierda");
        this.pataDD = cuerpo.getChild("pata_delantera_derecha");
        this.pataTI = cuerpo.getChild("pata_trasera_izquierda");
        this.pataTD = cuerpo.getChild("pata_trasera_derecha");
        this.bajaDI = pataDI.getChild("baja");
        this.bajaDD = pataDD.getChild("baja");
        this.bajaTI = pataTI.getChild("baja");
        this.bajaTD = pataTD.getChild("baja");
        this.cola = cuerpo.getChild("cola");
        this.cola2 = cola.getChild("cola2");
        this.borla = cola2.getChild("borla");
        this.torso = cuerpo.getChild("torso");
        this.cabeza = torso.getChild("cabeza");
        this.brazoD = torso.getChild("brazo_derecho");
        this.antebrazoD = brazoD.getChild("antebrazo");
        this.brazoI = torso.getChild("brazo_izquierdo");
        this.antebrazoI = brazoI.getChild("antebrazo");
        this.sable = antebrazoD.getChild("sable");
        this.flecha = antebrazoD.getChild("flecha");
        this.escudo = antebrazoI.getChild("escudo");
        this.arco = antebrazoI.getChild("arco");
    }

    public static LayerDefinition crearCapa() {
        MeshDefinition malla = new MeshDefinition();
        PartDefinition raiz = malla.getRoot();

        // ---- El león ----
        PartDefinition cuerpo = raiz.addOrReplaceChild("cuerpo",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0f, -4.0f, -10.0f, 10, 8, 20),
                PartPose.offset(0.0f, 9.0f, 0.0f));
        pata(cuerpo, "pata_delantera_izquierda", 3.0f, -7.5f, false);
        pata(cuerpo, "pata_delantera_derecha", -3.0f, -7.5f, true);
        pata(cuerpo, "pata_trasera_izquierda", 3.0f, 7.5f, false);
        pata(cuerpo, "pata_trasera_derecha", -3.0f, 7.5f, true);

        PartDefinition cola = cuerpo.addOrReplaceChild("cola",
                CubeListBuilder.create().texOffs(60, 12).addBox(-1.0f, -1.0f, 0.0f, 2, 2, 7),
                PartPose.offsetAndRotation(0.0f, -3.0f, 10.0f, -0.9f, 0.0f, 0.0f));
        PartDefinition cola2 = cola.addOrReplaceChild("cola2",
                CubeListBuilder.create().texOffs(78, 12).addBox(-1.0f, -1.0f, 0.0f, 2, 2, 6),
                PartPose.offsetAndRotation(0.0f, 0.0f, 7.0f, 0.6f, 0.0f, 0.0f));
        cola2.addOrReplaceChild("borla",
                CubeListBuilder.create().texOffs(94, 12).addBox(-1.5f, -1.5f, 0.0f, 3, 3, 3),
                PartPose.offset(0.0f, 0.0f, 6.0f));

        // ---- El guerrero ----
        PartDefinition torso = cuerpo.addOrReplaceChild("torso",
                CubeListBuilder.create().texOffs(0, 30).addBox(-4.5f, -13.0f, -2.5f, 9, 13, 5),
                PartPose.offset(0.0f, -4.0f, -7.5f));
        torso.addOrReplaceChild("cabeza",
                CubeListBuilder.create()
                        .texOffs(28, 30).addBox(-3.5f, -8.0f, -3.5f, 7, 8, 7)
                        .texOffs(56, 30).addBox(-5.5f, -10.0f, -2.5f, 11, 12, 6)      // la melena
                        .texOffs(0, 92).addBox(-5.0f, -1.0f, -3.5f, 10, 3, 7),        // y su collarín
                PartPose.offset(0.0f, -13.0f, 0.0f));

        PartDefinition brazoD = brazo(torso, "brazo_derecho", -1.0f);
        PartDefinition antebrazoD = brazoD.getChild("antebrazo");
        PartDefinition sable = antebrazoD.addOrReplaceChild("sable",
                CubeListBuilder.create()
                        .texOffs(0, 50).addBox(-0.5f, -0.5f, 0.0f, 1, 1, 3)           // empuñadura
                        .texOffs(8, 50).addBox(-0.5f, -1.5f, -1.0f, 1, 3, 1)          // guarda
                        .texOffs(0, 54).addBox(-0.5f, -1.0f, -15.0f, 1, 2, 14),       // hoja
                PartPose.offset(0.0f, 5.0f, -0.5f));
        sable.addOrReplaceChild("punta",
                CubeListBuilder.create().texOffs(30, 54).addBox(-0.5f, -1.0f, -4.0f, 1, 2, 4),
                PartPose.offsetAndRotation(0.0f, 0.0f, -15.0f, 0.3f, 0.0f, 0.0f));   // curva del sable
        antebrazoD.addOrReplaceChild("flecha",
                CubeListBuilder.create().texOffs(100, 60).addBox(-0.5f, 0.0f, -0.5f, 1, 13, 1),
                PartPose.offset(0.0f, 5.0f, 0.0f));

        PartDefinition brazoI = brazo(torso, "brazo_izquierdo", 1.0f);
        PartDefinition antebrazoI = brazoI.getChild("antebrazo");
        // El escudo gira sobre su centro: en guardia (antebrazo cruzado) va de frente; si no, a lo largo del brazo
        antebrazoI.addOrReplaceChild("escudo",
                CubeListBuilder.create()
                        .texOffs(40, 50).addBox(-5.5f, -4.5f, -3.5f, 11, 9, 1)
                        .texOffs(64, 50).addBox(-1.5f, -1.5f, -4.5f, 3, 3, 1),        // umbo
                PartPose.offset(0.0f, 3.0f, 0.0f));
        antebrazoI.addOrReplaceChild("arco",
                CubeListBuilder.create()
                        .texOffs(70, 60).addBox(-0.5f, 0.0f, -1.5f, 1, 2, 3)          // empuñadura
                        .texOffs(78, 60).addBox(-0.5f, 0.5f, -7.5f, 1, 1, 6)          // pala de arriba
                        .texOffs(78, 60).addBox(-0.5f, 0.5f, 1.5f, 1, 1, 6)           // pala de abajo
                        .texOffs(92, 60).addBox(-0.5f, -0.5f, -10.0f, 1, 1, 3)        // puntas curvadas
                        .texOffs(92, 60).addBox(-0.5f, -0.5f, 7.0f, 1, 1, 3)
                        .texOffs(0, 72).addBox(-0.5f, -1.0f, -9.5f, 1, 0, 19),        // cuerda
                PartPose.offset(0.0f, 5.0f, 0.0f));

        return LayerDefinition.create(malla, 128, 128);
    }

    private static void pata(PartDefinition cuerpo, String nombre, float x, float z, boolean espejo) {
        CubeListBuilder alta = CubeListBuilder.create().texOffs(60, 0);
        if (espejo) alta.mirror();
        PartDefinition pata = cuerpo.addOrReplaceChild(nombre, alta.addBox(-2.0f, 0.0f, -2.0f, 4, 7, 4),
                PartPose.offset(x, 3.0f, z));
        CubeListBuilder baja = CubeListBuilder.create().texOffs(76, 0);
        if (espejo) baja.mirror();
        baja.addBox(-1.5f, 0.0f, -1.5f, 3, 5, 3).texOffs(88, 0).addBox(-2.0f, 4.0f, -3.0f, 4, 1, 4);   // zarpa
        pata.addOrReplaceChild("baja", baja, PartPose.offset(0.0f, 7.0f, 0.0f));
    }

    private static PartDefinition brazo(PartDefinition torso, String nombre, float lado) {
        CubeListBuilder alto = CubeListBuilder.create().texOffs(90, 30);
        if (lado < 0) alto.mirror();
        alto.addBox(-2.0f, -1.0f, -2.0f, 4, 7, 4).texOffs(106, 0).addBox(-2.5f, -2.0f, -2.5f, 5, 3, 5);   // hombrera
        PartDefinition brazo = torso.addOrReplaceChild(nombre, alto, PartPose.offset(lado * 6.5f, -11.0f, 0.0f));
        CubeListBuilder bajo = CubeListBuilder.create().texOffs(90, 42);
        if (lado < 0) bajo.mirror();
        brazo.addOrReplaceChild("antebrazo", bajo.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4), PartPose.offset(0.0f, 6.0f, 0.0f));
        return brazo;
    }

    @Override
    public ModelPart root() {
        return raiz;
    }

    @Override
    public void setupAnim(LeonDeGuerra leon, float paso, float cantidadPaso, float edad, float giroCabeza, float inclinacionCabeza) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        boolean arquero = leon.esArquero();
        sable.visible = !arquero;
        escudo.visible = !arquero;
        arco.visible = arquero;
        int accion = leon.accion();
        float t = edad - leon.inicioAccionCliente;
        flecha.visible = arquero && accion == LeonDeGuerra.DISPARAR && t < 21;
        escudo.zRot = leon.enGuardia() ? 0.0f : Mth.HALF_PI;

        cantidadPaso = Math.min(cantidadPaso, 1.0f);
        float g = paso * 0.55f;                 // fase del galope
        float a = cantidadPaso;

        // ---- Galope: delanteras juntas, traseras al revés ----
        pataDI.xRot = Mth.sin(g) * 1.0f * a;
        pataDD.xRot = Mth.sin(g + 0.4f) * 1.0f * a;
        pataTI.xRot = Mth.sin(g + Mth.PI) * 1.0f * a;
        pataTD.xRot = Mth.sin(g + Mth.PI + 0.4f) * 1.0f * a;
        bajaDI.xRot = Math.max(0.0f, -Mth.cos(g)) * 0.9f * a;
        bajaDD.xRot = Math.max(0.0f, -Mth.cos(g + 0.4f)) * 0.9f * a;
        bajaTI.xRot = -Math.max(0.0f, Mth.cos(g)) * 0.7f * a;
        bajaTD.xRot = -Math.max(0.0f, Mth.cos(g + 0.4f)) * 0.7f * a;
        cuerpo.xRot = Mth.sin(g * 2.0f) * 0.07f * a;
        cuerpo.y = 9.0f - Math.abs(Mth.sin(g)) * 1.3f * a;
        // el guerrero compensa el cabeceo y se echa hacia delante al correr
        torso.xRot = -cuerpo.xRot + 0.25f * a;
        // la cola se estira al correr y se mueve sola en reposo
        float reposo = 1.0f - a;
        cola.xRot = -0.9f + 0.65f * a + Mth.sin(edad * 0.09f) * 0.08f;
        cola.yRot = Mth.sin(edad * 0.07f) * 0.45f * reposo + Mth.sin(g) * 0.15f * a;
        cola2.xRot = 0.6f - 0.45f * a + Mth.sin(edad * 0.11f + 1) * 0.12f;
        borla.yRot = Mth.sin(edad * 0.13f) * 0.3f;

        // ---- Respirar, mirar ----
        float respira = Mth.sin(edad * 0.08f);
        torso.xScale = 1.0f + 0.025f * respira;
        torso.zScale = 1.0f + 0.035f * respira;
        cuerpo.xScale = 1.0f + 0.02f * respira;
        cabeza.yRot = giroCabeza * Mth.DEG_TO_RAD;
        cabeza.xRot = inclinacionCabeza * Mth.DEG_TO_RAD - torso.xRot * 0.6f;

        // ---- Brazos en reposo: balanceo al correr ----
        brazoD.xRot = Mth.cos(g) * 0.4f * a;
        brazoI.xRot = Mth.cos(g + Mth.PI) * 0.4f * a;
        brazoD.zRot = 0.08f + Mth.sin(edad * 0.06f) * 0.03f;
        brazoI.zRot = -0.08f - Mth.sin(edad * 0.06f) * 0.03f;
        antebrazoD.xRot = -0.25f;
        antebrazoI.xRot = -0.25f;
        if (arquero) {
            brazoI.xRot -= 0.35f;              // lleva el arco un poco adelantado
        }

        // ---- Guardia del espadachín: escudo cruzado delante del pecho, sable preparado ----
        if (leon.enGuardia()) {
            brazoI.xRot = -0.55f + Mth.cos(g) * 0.1f * a;
            brazoI.yRot = -0.25f;
            brazoI.zRot = -0.15f;
            antebrazoI.xRot = 0.0f;
            antebrazoI.zRot = 1.45f;           // antebrazo horizontal cruzando el pecho: el escudo mira de frente
            brazoD.xRot = -0.6f + Mth.cos(g) * 0.1f * a;
            brazoD.zRot = 0.35f;
            antebrazoD.xRot = -0.9f;
            torso.yRot = 0.18f;
        }

        // ---- Acciones ----
        switch (accion) {
            case LeonDeGuerra.RUGIDO -> rugido(t);
            case LeonDeGuerra.DISPARAR -> disparar(t);
            case LeonDeGuerra.ESPADAZO -> espadazo(t);
            case LeonDeGuerra.ZARPAZO -> zarpazo(t, arquero);
            case LeonDeGuerra.ESCUDAZO -> escudazo(t);
            case LeonDeGuerra.TAMBALEO_ACCION -> tambaleo(t, edad);
            default -> {}
        }
    }

    private static float envolvente(float t, float duracion, float entrada, float salida) {
        if (t < 0) return 0.0f;
        return Math.min(Mth.clamp(t / entrada, 0.0f, 1.0f), Mth.clamp((duracion - t) / salida, 0.0f, 1.0f));
    }

    private void rugido(float t) {
        float e = envolvente(t, 30, 6, 8);
        float temblor = Mth.sin(t * 2.8f) * 0.08f * e;
        cuerpo.xRot -= 0.3f * e;                   // se alza un poco de delante
        pataDI.xRot -= 0.7f * e;
        pataDD.xRot -= 0.5f * e;
        torso.xRot -= 0.45f * e;                   // pecho fuera
        cabeza.xRot -= 0.55f * e;                  // cabeza atrás
        cabeza.zRot += temblor;                    // sacude la melena
        brazoD.zRot += 0.9f * e;                   // brazos abiertos
        brazoI.zRot -= 0.9f * e;
        brazoD.xRot -= 0.5f * e;
        brazoI.xRot -= 0.5f * e;
        cola.xRot += 0.8f * e;
    }

    private void disparar(float t) {
        float tensar = Mth.clamp(t / 20.0f, 0.0f, 1.0f);
        boolean soltado = t > 20;
        torso.yRot = 0.55f;                        // de perfil, como un arquero
        cabeza.yRot -= 0.5f;
        brazoI.xRot = -1.55f;                      // brazo del arco estirado al frente
        brazoI.yRot = -0.45f;
        brazoI.zRot = 0.0f;
        antebrazoI.xRot = 0.0f;
        brazoD.xRot = -1.55f;                      // el otro tira de la cuerda hacia la cara
        brazoD.yRot = -0.45f + (soltado ? -0.6f : -0.5f * tensar);
        brazoD.zRot = 0.0f;
        antebrazoD.xRot = soltado ? 0.0f : -1.1f * tensar;
    }

    private void espadazo(float t) {
        float carga, barrido;
        if (t <= 16) {                             // aviso: sable atrás y arriba, el torso se retuerce
            carga = Mth.clamp(t / 10.0f, 0.0f, 1.0f);
            barrido = 0;
        } else if (t <= 20) {                      // el barrido
            carga = 1.0f - (t - 16) / 4.0f;
            barrido = (t - 16) / 4.0f;
        } else {                                   // vuelve a la guardia
            carga = 0;
            barrido = Math.max(0.0f, 1.0f - (t - 20) / 8.0f);
        }
        float temblor = t <= 16 ? Mth.sin(t * 3.0f) * 0.03f * carga : 0;
        brazoD.xRot = Mth.lerp(carga, brazoD.xRot, -2.7f) - 0.9f * barrido + temblor;
        brazoD.zRot = Mth.lerp(carga, brazoD.zRot, 0.9f) - 1.6f * barrido;
        brazoD.yRot = 0.4f * barrido;
        antebrazoD.xRot = Mth.lerp(carga, antebrazoD.xRot, -0.4f);
        torso.yRot = -0.65f * carga + 0.75f * barrido;
        torso.xRot += -0.15f * carga + 0.2f * barrido;
        cabeza.yRot += 0.4f * carga - 0.3f * barrido;
        brazoI.xRot = Mth.lerp(Math.max(carga, barrido), brazoI.xRot, -0.2f);   // baja el escudo para golpear
        cuerpo.yRot = 0.15f * barrido;
    }

    private void zarpazo(float t, boolean arquero) {
        float aviso = arquero ? 8 : 10;
        float alza, golpe;
        if (t <= aviso) {                          // se alza sobre las patas de atrás
            alza = Mth.clamp(t / (aviso * 0.7f), 0.0f, 1.0f);
            golpe = 0;
        } else if (t <= aviso + 3) {               // cae golpeando con la pata
            alza = 1.0f - (t - aviso) / 3.0f;
            golpe = (t - aviso) / 3.0f;
        } else {
            alza = 0;
            golpe = Math.max(0.0f, 1.0f - (t - aviso - 3) / 6.0f);
        }
        cuerpo.xRot -= 0.55f * alza - 0.12f * golpe;
        cuerpo.y -= 3.0f * alza;
        pataTI.xRot += 0.55f * alza;               // las de atrás aguantan
        pataTD.xRot += 0.55f * alza;
        pataDD.xRot = -1.7f * alza + 0.4f * golpe; // la garra
        bajaDD.xRot = -0.6f * alza;
        pataDI.xRot = -0.6f * alza;
        torso.xRot += 0.35f * alza - 0.2f * golpe; // el guerrero se echa adelante para no caerse
        cabeza.xRot -= 0.3f * alza;
        cola.xRot += 0.6f * alza;
    }

    private void escudazo(float t) {
        float e = envolvente(t, 12, 2, 6);
        float golpe = t < 5 ? t / 5.0f : Math.max(0.0f, 1.0f - (t - 5) / 7.0f);
        brazoI.xRot = -0.55f - 0.9f * golpe;
        antebrazoI.zRot = 1.45f * (1 - golpe * 0.3f);
        torso.xRot += 0.35f * golpe * e;
        torso.yRot = 0.18f - 0.4f * golpe;
        cuerpo.xRot += 0.1f * golpe;
    }

    private void tambaleo(float t, float edad) {
        float e = envolvente(t, 50, 4, 10);
        float vaiven = Mth.sin(edad * 0.35f);
        torso.xRot += 0.4f * e;                    // encorvado, agotado
        torso.zRot = vaiven * 0.12f * e;
        cabeza.xRot += 0.5f * e;
        cabeza.zRot += vaiven * 0.2f * e;
        brazoD.xRot = Mth.lerp(e, brazoD.xRot, -0.25f);
        brazoD.zRot = Mth.lerp(e, brazoD.zRot, 0.35f);
        antebrazoD.xRot = Mth.lerp(e, antebrazoD.xRot, -0.1f);
        brazoI.xRot = Mth.lerp(e, brazoI.xRot, 0.1f);           // guardia baja
        brazoI.zRot = Mth.lerp(e, brazoI.zRot, -0.35f);
        antebrazoI.zRot = Mth.lerp(e, antebrazoI.zRot, 0.0f);
        cuerpo.zRot = vaiven * 0.06f * e;
        pataDI.xRot += 0.15f * e;
        pataDD.xRot -= 0.1f * e;
        cola.xRot -= 0.3f * e;
    }
}
