package com.lucarita.yonga.modulos.fe.mobs.rey;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * El esqueleto del Rey de Arenisca. Lo usan a la vez el servidor (para colocar las cajas de golpe y los asideros
 * por donde se le escala) y el cliente (para dibujarlo), así que lo que ves es exactamente donde le das y por donde
 * trepas. Es el mismo que herramientas/esqueleto_rey.py (con el que se comprueban las poses).
 *
 * Todo en bloques. Espacio del rey: +y arriba, +z hacia delante (adonde mira), +x a su izquierda.
 * Cada hueso: M = padre · T(sitio) · Ry · Rx · Rz. En Rx, negativo = el miembro que cuelga va hacia delante.
 * Las matrices van relativas a la raíz (sus pies); la posición de la raíz se guarda aparte, en doble precisión.
 */
public final class EsqueletoRey {

    public static final int CADERA = 0, TORSO = 1, CABEZA = 2, BRAZO_I = 3, ANTEBRAZO_I = 4, MANO_I = 5,
            BRAZO_D = 6, ANTEBRAZO_D = 7, MANO_D = 8, MUSLO_I = 9, ESPINILLA_I = 10, PIE_I = 11,
            MUSLO_D = 12, ESPINILLA_D = 13, PIE_D = 14, N = 15;
    public static final int[] PADRE = {-1, 0, 1, 1, 3, 4, 1, 6, 7, 0, 9, 10, 0, 12, 13};
    private static final float[][] SITIO = {{0, 0, 0}, {0, 4, 0}, {0, 10, 0}, {9.5f, 9, 0}, {0, -10, 0}, {0, -9, 0},
            {-9.5f, 9, 0}, {0, -10, 0}, {0, -9, 0}, {4, 0, 0}, {0, -9, 0}, {0, -8, 0}, {-4, 0, 0}, {0, -9, 0}, {0, -8, 0}};
    /** Caja de cada hueso en su espacio: x0, y0, z0, x1, y1, z1. */
    public static final float[][] CAJA = {
            {-7, 0, -3.5f, 7, 4, 3.5f}, {-7.5f, 0, -4, 7.5f, 10, 4}, {-4, 0, -4, 4, 8, 4},
            {-2, -10, -2, 2, 0, 2}, {-1.75f, -9, -1.75f, 1.75f, 0, 1.75f}, {-2, -5, -1, 2, 0, 1},
            {-2, -10, -2, 2, 0, 2}, {-1.75f, -9, -1.75f, 1.75f, 0, 1.75f}, {-2, -5, -1, 2, 0, 1},
            {-2.25f, -9, -2.25f, 2.25f, 0, 2.25f}, {-2, -8, -2, 2, 0, 2}, {-2, -2, -1.5f, 2, 0, 4.5f},
            {-2.25f, -9, -2.25f, 2.25f, 0, 2.25f}, {-2, -8, -2, 2, 0, 2}, {-2, -2, -1.5f, 2, 0, 4.5f}};
    /** Los tres sellos (puntos débiles): hueso y centro. 0: espinilla derecha, 1: espalda, 2: coronilla. */
    public static final int[] SELLO_HUESO = {ESPINILLA_D, TORSO, CABEZA};
    public static final float[][] SELLO_SITIO = {{0, -4, -2.2f}, {0, 6, -4.2f}, {0, 8.2f, 0}};
    public static final float MEDIO_SELLO = 1.25f;

    public static final float ALTURA_CADERA = 19.0f;
    private static final float BAJADA_RODILLAS = 8.5f;
    public static final float ALTURA = 44.0f;

    public final Matrix4f[] huesos = new Matrix4f[N];
    private final Matrix4f[] inversas = new Matrix4f[N];
    private final boolean[] inversaLista = new boolean[N];
    private final float[][] rot = new float[N][3];
    public double ox, oy, oz;            // la raíz (los pies) en el mundo

    public EsqueletoRey() {
        for (int i = 0; i < N; i++) {
            huesos[i] = new Matrix4f();
            inversas[i] = new Matrix4f();
        }
    }

    /** Lo que hace falta saber de cómo se mueve el rey en este instante. */
    public record Estado(double x, double y, double z, float giro, int accion, float t, int lado, float rodillas,
                         float andar, float paso, float muerte, float miraGiro, float miraCabeceo, float bajada) {}

    public void calcular(Estado e) {
        this.ox = e.x();
        this.oy = e.y();
        this.oz = e.z();
        for (float[] r : rot) {
            r[0] = r[1] = r[2] = 0;
        }
        float altura = posar(e);
        Matrix4f raiz = new Matrix4f().translate(0, -e.bajada(), 0).rotateY(-e.giro() * Mth.DEG_TO_RAD);
        for (int b = 0; b < N; b++) {
            float sx = SITIO[b][0], sy = b == CADERA ? altura : SITIO[b][1], sz = SITIO[b][2];
            Matrix4f padre = PADRE[b] < 0 ? raiz : huesos[PADRE[b]];
            huesos[b].set(padre).translate(sx, sy, sz).rotateY(rot[b][1]).rotateX(rot[b][0]).rotateZ(rot[b][2]);
            inversaLista[b] = false;
        }
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static float tramo(float t, float a, float b) {
        return suave((t - a) / (b - a));
    }

    /** Las poses (igual que posar() de esqueleto_rey.py). Devuelve la altura de la cadera. */
    private float posar(Estado e) {
        float[][] r = rot;
        float t = e.t();
        int s = e.lado();
        r[BRAZO_I][2] += 0.08f;
        r[BRAZO_D][2] -= 0.08f;
        r[ANTEBRAZO_I][0] -= 0.15f;
        r[ANTEBRAZO_D][0] -= 0.15f;
        // andar
        float a = e.andar() * (1 - e.rodillas());
        float sp = Mth.sin(e.paso());
        r[MUSLO_I][0] += -sp * 0.3f * a;
        r[MUSLO_D][0] += sp * 0.3f * a;
        r[ESPINILLA_I][0] += Math.max(0, sp) * 0.45f * a;
        r[ESPINILLA_D][0] += Math.max(0, -sp) * 0.45f * a;
        r[BRAZO_I][0] += sp * 0.2f * a;
        r[BRAZO_D][0] -= sp * 0.2f * a;
        float bajada = Math.abs(Mth.cos(e.paso())) * 0.4f * a;

        float agacharse = 0, inclinar = 0;
        switch (e.accion()) {
            case ReyDeArenisca.PISOTON -> {
                float l = t < 42 ? tramo(t, 0, 30) : 1 - tramo(t, 42, 48);
                int mi = s > 0 ? MUSLO_I : MUSLO_D, es = s > 0 ? ESPINILLA_I : ESPINILLA_D;
                r[mi][0] += -1.35f * l;
                r[es][0] += 1.55f * l;
                r[TORSO][0] -= 0.12f * l;
                r[BRAZO_I][2] += 0.3f * l;
                r[BRAZO_D][2] -= 0.3f * l;
            }
            case ReyDeArenisca.BARRIDO -> {
                float en = t < 48 ? tramo(t, 0, 30) : 1 - tramo(t, 48, 80);
                agacharse = 0.7f * en;
                inclinar = 0.9f * en;
                float giro = t < 30 ? 0.75f * s : s * (0.75f - 2.0f * tramo(t, 30, 48));
                r[TORSO][1] += giro * en;
                int br = s > 0 ? BRAZO_I : BRAZO_D;
                r[br][0] += -1.44f * en;
                r[br][2] += 0.15f * s * en;
                r[br + 1][0] += 0.15f * en;
                r[CABEZA][0] -= 0.5f * en;
            }
            case ReyDeArenisca.PALMADA -> {
                float en = t < 120 ? tramo(t, 0, 40) : 1 - tramo(t, 120, 150);
                agacharse = 0.78f * en;
                inclinar = en;
                int br = s > 0 ? BRAZO_I : BRAZO_D;
                float brazo;
                if (t < 25) brazo = -2.4f * tramo(t, 0, 25);
                else if (t < 40) brazo = -2.4f + 1.4f * ((t - 25) / 15.0f) * ((t - 25) / 15.0f);
                else if (t < 120) brazo = -1.0f;
                else brazo = -1.0f * (1 - tramo(t, 120, 150));
                r[br][0] += brazo;
                r[br + 1][0] += 0.15f;
                r[br + 2][0] += -1.5708f * (t < 120 ? tramo(t, 25, 40) : 1 - tramo(t, 120, 150));
                r[CABEZA][0] -= 0.6f * en;
            }
            case ReyDeArenisca.COZ -> {
                // coz hacia atrás: dobla la pierna, la echa atrás de golpe (recta) y se inclina hacia delante
                float pierna = t < 20 ? -0.35f * tramo(t, 0, 20) : t < 28 ? -0.35f + 1.45f * tramo(t, 20, 28)
                        : t < 34 ? 1.1f : 1.1f * (1 - tramo(t, 34, 50));
                float rodilla = t < 20 ? 0.7f * tramo(t, 0, 20) : t < 28 ? 0.7f * (1 - tramo(t, 20, 26)) : 0;
                int mi = s > 0 ? MUSLO_I : MUSLO_D, es = s > 0 ? ESPINILLA_I : ESPINILLA_D;
                float atras = Math.max(0, pierna);
                r[mi][0] += pierna;
                r[es][0] += rodilla;
                r[TORSO][0] += 0.25f * atras;
                r[CABEZA][0] -= 0.2f * atras;
                r[BRAZO_I][0] -= 0.3f * atras;
                r[BRAZO_D][0] -= 0.3f * atras;
            }
            case ReyDeArenisca.TEMBLOR -> {
                // se agacha, salta con los brazos en alto y cae a plomo (el salto lo pone ReyDeArenisca, en la bajada)
                agacharse = t < 20 ? 0.45f * tramo(t, 0, 20) : t < 28 ? 0.45f * (1 - tramo(t, 20, 28)) : t < 40 ? 0
                        : t < 44 ? 0.5f * tramo(t, 40, 44) : 0.5f * (1 - tramo(t, 44, 64));
                float brazos = t < 20 ? -0.8f * tramo(t, 0, 20) : t < 40 ? -0.8f - 1.4f * tramo(t, 20, 28) : -2.2f * (1 - tramo(t, 40, 50));
                r[BRAZO_I][0] += brazos;
                r[BRAZO_D][0] += brazos;
                r[BRAZO_I][2] += 0.2f;
                r[BRAZO_D][2] -= 0.2f;
                r[CABEZA][0] -= 0.2f * (t < 40 ? tramo(t, 0, 20) : 1 - tramo(t, 40, 50));
            }
            case ReyDeArenisca.SACUDIDA, ReyDeArenisca.SACUDIDA_FUERTE -> {
                // primero un temblor de aviso, luego la sacudida de verdad (la fuerte: aviso más largo y más bestia)
                boolean fuerte = e.accion() == ReyDeArenisca.SACUDIDA_FUERTE;
                float aviso = fuerte ? ReyDeArenisca.AVISO_FUERTE : ReyDeArenisca.AVISO_SACUDIDA;
                float en = t < aviso ? (fuerte ? 0.2f : 0.12f) * tramo(t, 0, aviso)
                        : Mth.sin((float) Math.PI * Mth.clamp((t - aviso) / (ReyDeArenisca.TICKS_SACUDIDA - aviso), 0, 1));
                if (fuerte) en *= 1.7f;
                r[TORSO][2] += Mth.sin(t * 1.1f) * 0.16f * en;
                r[TORSO][1] += Mth.sin(t * 0.7f) * 0.12f * en;
                r[CADERA][2] += Mth.sin(t * 1.1f + 1) * 0.05f * en;
                r[CABEZA][0] += Mth.sin(t * 1.7f) * 0.15f * en;
                r[BRAZO_I][0] += Mth.sin(t * 0.9f + 1) * 0.4f * en;
                r[BRAZO_D][0] += Mth.sin(t * 0.9f - 1) * 0.4f * en;
                r[BRAZO_I][2] += (0.3f + Mth.sin(t * 1.3f) * 0.2f) * en;
                r[BRAZO_D][2] -= (0.3f + Mth.sin(t * 1.3f + 2) * 0.2f) * en;
            }
            case ReyDeArenisca.RUGIDO -> {
                float en = t < 45 ? tramo(t, 0, 15) : 1 - tramo(t, 45, 60);
                r[CABEZA][0] -= 0.5f * en;
                r[BRAZO_I][2] += 0.8f * en;
                r[BRAZO_D][2] -= 0.8f * en;
                r[TORSO][0] -= 0.15f * en;
            }
            case ReyDeArenisca.RAYO -> {
                r[TORSO][0] += 0.1f;
                r[BRAZO_I][0] -= 0.2f;
                r[BRAZO_D][0] -= 0.2f;
            }
            default -> { }
        }
        r[CABEZA][1] += Mth.clamp(e.miraGiro(), -1.0f, 1.0f);
        r[CABEZA][0] += Mth.clamp(e.miraCabeceo(), -0.6f, 0.7f);

        float m = e.muerte();
        if (m > 0) {
            float mm = tramo(m, 40, 110);
            agacharse = Math.max(agacharse, mm);
            inclinar = Math.max(inclinar, 1.2f * tramo(m, 70, 140));
            r[CABEZA][0] += 0.5f * mm;
            r[BRAZO_I][2] -= 0.05f * mm;
            r[BRAZO_D][2] += 0.05f * mm;
        }

        float k = Math.max(e.rodillas(), agacharse);
        float altura = ALTURA_CADERA - BAJADA_RODILLAS * k - bajada;
        int[][] piernas = {{MUSLO_I, ESPINILLA_I, PIE_I}, {MUSLO_D, ESPINILLA_D, PIE_D}};
        for (int[] p : piernas) {
            r[p[0]][0] *= (1 - k);
            r[p[1]][0] = r[p[1]][0] * (1 - k) + 1.5708f * k;
            r[p[2]][0] += -1.4f * k;
        }
        r[TORSO][0] += 0.2f * k + inclinar;
        r[CABEZA][0] -= 0.15f * k;
        return altura;
    }

    // ---------- DE HUESO A MUNDO Y AL REVÉS ----------

    public Vec3 aMundo(int hueso, float x, float y, float z) {
        Vector3f p = huesos[hueso].transformPosition(x, y, z, new Vector3f());
        return new Vec3(ox + p.x, oy + p.y, oz + p.z);
    }

    public Vec3 direccion(int hueso, float x, float y, float z) {
        Vector3f p = huesos[hueso].transformDirection(x, y, z, new Vector3f());
        return new Vec3(p.x, p.y, p.z);
    }

    public Vector3f aLocal(int hueso, Vec3 mundo) {
        if (!inversaLista[hueso]) {
            huesos[hueso].invert(inversas[hueso]);
            inversaLista[hueso] = true;
        }
        return inversas[hueso].transformPosition((float) (mundo.x - ox), (float) (mundo.y - oy), (float) (mundo.z - oz), new Vector3f());
    }

    /** La caja del mundo que envuelve la caja (girada) de un hueso. */
    public AABB caja(int hueso) {
        float[] c = CAJA[hueso];
        return envolver(hueso, c[0], c[1], c[2], c[3], c[4], c[5]);
    }

    public AABB cajaSello(int sello) {
        float[] p = SELLO_SITIO[sello];
        float m = MEDIO_SELLO;
        return envolver(SELLO_HUESO[sello], p[0] - m, p[1] - m, p[2] - m, p[0] + m, p[1] + m, p[2] + m);
    }

    public Vec3 centroSello(int sello) {
        float[] p = SELLO_SITIO[sello];
        return aMundo(SELLO_HUESO[sello], p[0], p[1], p[2]);
    }

    private AABB envolver(int hueso, float x0, float y0, float z0, float x1, float y1, float z1) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vector3f p = new Vector3f();
        for (int i = 0; i < 8; i++) {
            huesos[hueso].transformPosition((i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, p);
            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);
        }
        return new AABB(ox + minX, oy + minY, oz + minZ, ox + maxX, oy + maxY, oz + maxZ);
    }
}
