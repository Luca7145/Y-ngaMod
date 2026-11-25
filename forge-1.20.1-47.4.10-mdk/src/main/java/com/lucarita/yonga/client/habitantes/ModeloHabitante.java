package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * El cuerpo de un habitante: el mismo que el del jugador (texturas en formato de skin de 64x64), con brazos anchos
 * para los hombres y finos para las mujeres, que además tienen pecho. La escala del cuerpo entero la pone
 * HabitanteRenderer; aquí van las proporciones y las animaciones de cada edad.
 *
 * NIÑOS Y JÓVENES: el cuerpo de adulto con la cabeza más grande cuanto más pequeños.
 *
 * BEBÉS: proporciones propias (cabezón, tronco y miembros cortos y gorditos) y dos posturas que se mezclan con suavidad:
 *   · SENTADO, con las piernas estiradas y un poco abiertas, las manos en el regazo, la cabeza que se balancea y mira
 *     a quien tenga cerca, pataditas, y de vez en cuando un rato de "jugar" agitando los brazos;
 *   · A GATAS cuando se mueve: tronco horizontal, brazos y rodillas en el suelo alternándose en cruz (mano derecha con
 *     rodilla izquierda), un leve rebote y la cabeza levantada mirando al frente.
 *
 * PECHO: una cuña que cuelga del frente del torso y se inclina hacia delante por abajo (de perfil, un triángulo).
 * Toma su dibujo del frente del torso, así que la ropa lo cubre sola. Cada mujer tiene su tamaño (8) y su caída (4):
 * los rectos son más cortos, altos y salientes; los caídos, más largos y bajos. Crece desde joven.
 */
public class ModeloHabitante extends PlayerModel<Habitante> {

    public static final ModelLayerLocation CAPA_HOMBRE =
            new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "habitante"), "hombre");
    public static final ModelLayerLocation CAPA_MUJER =
            new ModelLayerLocation(new ResourceLocation(YongaMain.MOD_ID, "habitante"), "mujer");

    private final ModelPart pecho;
    private final boolean mujer;
    private final ModelPart[] todas;

    public ModeloHabitante(ModelPart raiz, boolean mujer) {
        super(raiz, mujer);
        this.mujer = mujer;
        this.pecho = this.body.getChild("pecho");
        this.todas = new ModelPart[]{this.head, this.hat, this.body, this.jacket, this.pecho, this.rightArm, this.leftArm,
                this.rightSleeve, this.leftSleeve, this.rightLeg, this.leftLeg, this.rightPants, this.leftPants};
    }

    public static LayerDefinition crearHombre() {
        return crear(false);
    }

    public static LayerDefinition crearMujer() {
        return crear(true);
    }

    private static LayerDefinition crear(boolean mujer) {
        MeshDefinition malla = PlayerModel.createMesh(CubeDeformation.NONE, mujer);
        PartDefinition raiz = malla.getRoot();
        // Caja de 8x4x3 colgada del frente del torso a la altura de la fila 2; su cara delantera cae justo en las
        // filas 2-5 del frente del torso. Sin inclinar queda dentro del torso y no se ve.
        raiz.getChild("body").addOrReplaceChild("pecho",
                CubeListBuilder.create().texOffs(17, 19).addBox(-4.0f, 0.0f, 0.0f, 8, 4, 3),
                PartPose.offset(0.0f, 2.0f, -2.0f));
        return LayerDefinition.create(malla, 64, 64);
    }

    @Override
    public void setupAnim(Habitante habitante, float paso, float amplitud, float edadTicks, float giroCabeza, float inclinacion) {
        this.young = false;        // la edad la llevamos nosotros, no el "bebé" de Minecraft
        for (ModelPart parte : this.todas) {
            parte.resetPose();
        }
        float crecimiento = habitante.crecimiento();
        if (crecimiento < 1.0f) {
            this.pecho.visible = false;
            animarBebe(habitante, edadTicks, giroCabeza, inclinacion);
            return;
        }

        super.setupAnim(habitante, paso, amplitud, edadTicks, giroCabeza, inclinacion);

        float cabeza = Habitante.escalaCabeza(crecimiento);
        escalar(this.head, cabeza, cabeza, cabeza);
        escalar(this.hat, cabeza, cabeza, cabeza);

        // Complexión: más o menos ancho de hombros y caderas
        float ancho = habitante.complexion();
        escalar(this.body, ancho, 1.0f, ancho);
        escalar(this.jacket, ancho, 1.0f, ancho);
        // Los brazos, pegados al tronco: si el tronco es más estrecho (o más ancho), los hombros se mueven con él
        float hombro = 4.0f * (ancho - 1.0f);
        this.rightArm.x -= hombro;
        this.rightSleeve.x -= hombro;
        this.leftArm.x += hombro;
        this.leftSleeve.x += hombro;

        animarPecho(habitante, crecimiento);
    }

    // =====================================================================================================
    // PECHO
    // =====================================================================================================

    private void animarPecho(Habitante habitante, float crecimiento) {
        float desarrollo = this.mujer ? Mth.clamp((crecimiento - 2.0f) * 1.25f, 0.0f, 1.0f) : 0.0f;
        this.pecho.visible = desarrollo > 0.05f;
        if (!this.pecho.visible) {
            return;
        }
        float tamano = habitante.pecho() / 7.0f;          // 0 muy pequeño .. 1 muy grande
        float caida = habitante.caidaPecho() / 3.0f;      // 0 recto .. 1 caído
        // Cuánto sale hacia delante: de ~0,7 píxeles (muy pequeño) a ~3 (muy grande). Los caídos salen algo menos.
        float angulo = desarrollo * (0.18f + tamano * 0.52f) * (1.0f - caida * 0.15f);
        // Largo: cortos y altos por defecto; los caídos cuelgan un poco más y los grandes son algo más largos.
        float largo = 1.0f + desarrollo * (-0.22f + tamano * 0.18f + caida * 0.2f);
        float anchoPecho = 0.92f + tamano * 0.1f;
        this.pecho.y = 2.0f + caida * 0.35f * desarrollo;
        this.pecho.xRot = -angulo;
        escalar(this.pecho, anchoPecho, largo, 1.0f);
    }

    // =====================================================================================================
    // BEBÉ
    // =====================================================================================================

    // Proporciones del bebé (respecto al cuerpo de adulto del modelo)
    private static final float TRONCO_X = 0.85f, TRONCO_Y = 0.5f, TRONCO_Z = 0.8f;
    private static final float MIEMBRO_XZ = 0.8f, MIEMBRO_Y = 0.45f;
    private static final float LARGO_TRONCO = 12 * TRONCO_Y;              // 6 píxeles
    private static final float MEDIO_GROSOR = 2 * TRONCO_Z;               // 1,6 píxeles
    private static final float HOMBRO_X = 4 * TRONCO_X + MIEMBRO_XZ;      // los brazos pegados al tronco
    private static final float CADERA_X = 1.9f * TRONCO_X;
    private static final float SUELO = 24.0f;

    /** Una postura: posición y giro de cada parte (cabeza, tronco, brazo D, brazo I, pierna D, pierna I). */
    private static final class Postura {
        final float[][] v = new float[6][6];    // x, y, z, giroX, giroY, giroZ

        void poner(int parte, float x, float y, float z, float gx, float gy, float gz) {
            float[] p = v[parte];
            p[0] = x; p[1] = y; p[2] = z; p[3] = gx; p[4] = gy; p[5] = gz;
        }
    }

    private static final int CABEZA = 0, TRONCO = 1, BRAZO_D = 2, BRAZO_I = 3, PIERNA_D = 4, PIERNA_I = 5;

    private void animarBebe(Habitante bebe, float t, float giroCabeza, float inclinacion) {
        float parcial = t - bebe.tickCount;
        float gateo = Mth.lerp(Mth.clamp(parcial, 0.0f, 1.0f), bebe.gateoAntes, bebe.gateo);
        gateo = gateo * gateo * (3.0f - 2.0f * gateo);       // suavizado
        float yaw = giroCabeza * Mth.DEG_TO_RAD, pitch = inclinacion * Mth.DEG_TO_RAD;

        Postura sentado = sentado(bebe, t, yaw, pitch);
        Postura gatas = aGatas(t, yaw, pitch);
        ModelPart[] partes = {this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg};
        for (int i = 0; i < 6; i++) {
            float[] a = sentado.v[i], b = gatas.v[i];
            ModelPart parte = partes[i];
            parte.x = Mth.lerp(gateo, a[0], b[0]);
            parte.y = Mth.lerp(gateo, a[1], b[1]);
            parte.z = Mth.lerp(gateo, a[2], b[2]);
            parte.xRot = Mth.lerp(gateo, a[3], b[3]);
            parte.yRot = Mth.lerp(gateo, a[4], b[4]);
            parte.zRot = Mth.lerp(gateo, a[5], b[5]);
        }

        escalar(this.head, 1.0f, 1.0f, 1.0f);
        escalar(this.body, TRONCO_X, TRONCO_Y, TRONCO_Z);
        for (ModelPart miembro : new ModelPart[]{this.rightArm, this.leftArm, this.rightLeg, this.leftLeg}) {
            escalar(miembro, MIEMBRO_XZ, MIEMBRO_Y, MIEMBRO_XZ);
        }
        copiar(this.head, this.hat);
        copiar(this.body, this.jacket);
        copiar(this.rightArm, this.rightSleeve);
        copiar(this.leftArm, this.leftSleeve);
        copiar(this.rightLeg, this.rightPants);
        copiar(this.leftLeg, this.leftPants);
    }

    /** Sentado con las piernas estiradas; juega de vez en cuando. */
    private Postura sentado(Habitante bebe, float t, float yaw, float pitch) {
        Postura p = new Postura();
        float cadera = SUELO - MEDIO_GROSOR;
        float hombros = cadera - LARGO_TRONCO;
        float balanceo = Mth.sin(t * 0.07f) * 0.12f;

        // Rato de jugar: cada ~13 s, unos 2,5 s agitando los brazos arriba y abajo (cada bebé a su ritmo)
        float fase = (t + bebe.getId() * 53) % 260.0f;
        float juego = fase < 50.0f ? Mth.sin(fase / 50.0f * Mth.PI) : 0.0f;
        float agitar = Mth.sin(t * 0.9f) * 0.35f * juego;

        p.poner(TRONCO, 0, hombros, 0, -0.08f + Mth.sin(t * 0.05f) * 0.03f, 0, balanceo * 0.3f);
        p.poner(CABEZA, 0, hombros, 0, pitch * 0.6f + juego * -0.15f, yaw, balanceo);
        float brazo = Mth.lerp(juego, -0.55f, -2.5f) + Mth.sin(t * 0.09f) * 0.05f;
        p.poner(BRAZO_D, -HOMBRO_X, hombros + 1.0f, 0, brazo + agitar, 0, 0.15f + juego * 0.35f);
        p.poner(BRAZO_I, HOMBRO_X, hombros + 1.0f, 0, brazo - agitar, 0, -0.15f - juego * 0.35f);
        float patada = Mth.sin(t * 0.15f) * 0.06f;
        p.poner(PIERNA_D, -CADERA_X, cadera, 0, -Mth.HALF_PI + patada, 0.3f, 0);
        p.poner(PIERNA_I, CADERA_X, cadera, 0, -Mth.HALF_PI - patada, -0.3f, 0);
        return p;
    }

    /** A gatas: tronco horizontal, manos y rodillas en el suelo, avanzando en cruz. */
    private Postura aGatas(float t, float yaw, float pitch) {
        Postura p = new Postura();
        float c = t * 0.35f;
        float s = Mth.sin(c);
        float brazoLargo = 10 * MIEMBRO_Y;                 // del hombro a la mano
        float hombros = SUELO - brazoLargo - Math.abs(s) * 0.3f;
        float delante = -LARGO_TRONCO / 2;                 // el tronco queda centrado sobre la caja

        p.poner(TRONCO, 0, hombros, delante, Mth.HALF_PI, 0, s * 0.04f);
        p.poner(CABEZA, 0, hombros - 0.4f, delante - 0.6f, -0.35f + pitch * 0.4f, yaw * 0.6f, s * 0.05f);
        p.poner(BRAZO_D, -HOMBRO_X, hombros, delante + 0.8f, -s * 0.5f, 0, 0.05f);
        p.poner(BRAZO_I, HOMBRO_X, hombros, delante + 0.8f, s * 0.5f, 0, -0.05f);
        float caderas = delante + LARGO_TRONCO - 0.4f;
        p.poner(PIERNA_D, -CADERA_X, hombros, caderas, 0.55f + s * 0.35f, 0, 0);
        p.poner(PIERNA_I, CADERA_X, hombros, caderas, 0.55f - s * 0.35f, 0, 0);
        return p;
    }

    // =====================================================================================================

    private static void copiar(ModelPart de, ModelPart a) {
        a.copyFrom(de);
        escalar(a, de.xScale, de.yScale, de.zScale);
    }

    private static void escalar(ModelPart parte, float x, float y, float z) {
        parte.xScale = x;
        parte.yScale = y;
        parte.zScale = z;
    }
}
