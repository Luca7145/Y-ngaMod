package com.lucarita.yonga.modulos.fe.mobs.rey;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.lucarita.yonga.modulos.fe.mobs.rey.EsqueletoRey.*;

/**
 * Por dónde se escala al Rey de Arenisca: las caras con ASIDEROS (muescas y raíces secas en la textura). Cada
 * superficie es un rectángulo sobre una cara de la caja de un hueso, con coordenadas (u, v):
 *   - cara de normal Z: u = x, v = y;   cara de normal X: u = z, v = y;   cara de normal Y: u = x, v = z.
 * Por sus bordes se pasa a otras superficies (al trepar por la pierna se llega a la cadera, a la espalda, a los
 * hombros...): primero a las vecinas que tiene apuntadas y, si no, a la superficie más cercana que haya (así no te
 * quedas pillado en ningún borde). Si la cara mira hacia arriba (los hombros, la coronilla, la mano plantada en el
 * suelo, la espalda cuando se encorva) se está de pie sobre ella; si es una pared, uno va colgado.
 *
 * El camino: espinillas (desde el suelo; la derecha tiene el primer sello) → muslos → cadera → espalda (segundo
 * sello) → hombros / nuca → coronilla (tercer sello). Y por los brazos: mano → antebrazo → brazo → hombro.
 */
public final class AgarreRey {

    private AgarreRey() {}

    /** Separación del jugador respecto a la pared a la que se agarra. */
    private static final double SEPARACION = 0.75;
    public static final float VELOCIDAD_TREPAR = 0.18f, VELOCIDAD_ANDAR = 0.25f;
    public static final double DISTANCIA_AGARRE = 4.5;
    /** Lo más lejos que puede estar el borde de una superficie de la vecina para pasar de una a otra. */
    private static final float SALTO_MAXIMO = 4.5f;
    /** Lo mismo, para pasar a una superficie que no está apuntada como vecina (solo si está pegada). */
    private static final float SALTO_SIN_VECINA = 1.5f;

    public record Superficie(int hueso, int eje, int signo, float plano, float u0, float u1, float v0, float v1,
                             int[] bajoV, int[] altoV, int[] bajoU, int[] altoU) {}

    private static final int[] NADA = {};

    private static int[] a(int... ids) {
        return ids;
    }

    public static final int ESPINILLA_I_S = 0, ESPINILLA_D_S = 1, MUSLO_I_S = 2, MUSLO_D_S = 3, CADERA_S = 4,
            ESPALDA = 5, HOMBRO_I = 6, HOMBRO_D = 7, NUCA = 8, SIEN_I = 9, SIEN_D = 10, BRAZO_I_S = 11,
            BRAZO_D_S = 12, CORONILLA = 13, ANTEBRAZO_I_S = 14, ANTEBRAZO_D_S = 15, MANO_I_S = 16, MANO_D_S = 17,
            BRAZO_I_ATRAS = 18, BRAZO_D_ATRAS = 19, ANTEBRAZO_I_ATRAS = 20, ANTEBRAZO_D_ATRAS = 21;

    public static final Superficie[] SUPERFICIES = {
            // 0, 1: la parte de atrás de las espinillas (con él de rodillas quedan tumbadas en el suelo)
            new Superficie(ESPINILLA_I, 2, -1, -2, -2, 2, -8, 0, NADA, a(MUSLO_I_S), NADA, NADA),
            new Superficie(ESPINILLA_D, 2, -1, -2, -2, 2, -8, 0, NADA, a(MUSLO_D_S), NADA, NADA),
            // 2, 3: la parte de atrás de los muslos
            new Superficie(MUSLO_I, 2, -1, -2.25f, -2.25f, 2.25f, -9, 0, NADA, a(CADERA_S), NADA, NADA),
            new Superficie(MUSLO_D, 2, -1, -2.25f, -2.25f, 2.25f, -9, 0, NADA, a(CADERA_S), NADA, NADA),
            // 4: la cadera por detrás
            new Superficie(CADERA, 2, -1, -3.5f, -7, 7, 0, 4, a(MUSLO_I_S, MUSLO_D_S), a(ESPALDA), NADA, NADA),
            // 5: la espalda
            new Superficie(TORSO, 2, -1, -4, -7.5f, 7.5f, 0, 10, a(CADERA_S), a(HOMBRO_I, HOMBRO_D, NUCA), a(BRAZO_D_ATRAS), a(BRAZO_I_ATRAS)),
            // 6, 7: los hombros (encima del torso, a los lados de la cabeza): de pie
            new Superficie(TORSO, 1, 1, 10, 4, 7.5f, -4, 4, a(ESPALDA), NADA, a(NUCA, SIEN_I), a(BRAZO_I_S)),
            new Superficie(TORSO, 1, 1, 10, -7.5f, -4, -4, 4, a(ESPALDA), NADA, a(BRAZO_D_S), a(NUCA, SIEN_D)),
            // 8: la nuca
            new Superficie(CABEZA, 2, -1, -4, -4, 4, 0, 8, a(ESPALDA, HOMBRO_I, HOMBRO_D), a(CORONILLA), a(SIEN_D), a(SIEN_I)),
            // 9, 10: los lados de la cabeza
            new Superficie(CABEZA, 0, 1, 4, -4, 4, 0, 8, a(HOMBRO_I), a(CORONILLA), a(NUCA), NADA),
            new Superficie(CABEZA, 0, -1, -4, -4, 4, 0, 8, a(HOMBRO_D), a(CORONILLA), a(NUCA), NADA),
            // 11, 12: los brazos por fuera
            new Superficie(BRAZO_I, 0, 1, 2, -2, 2, -10, 0, a(ANTEBRAZO_I_S), a(HOMBRO_I), NADA, NADA),
            new Superficie(BRAZO_D, 0, -1, -2, -2, 2, -10, 0, a(ANTEBRAZO_D_S), a(HOMBRO_D), NADA, NADA),
            // 13: la coronilla (dentro de la corona): de pie
            new Superficie(CABEZA, 1, 1, 8, -4, 4, -4, 4, a(NUCA), NADA, a(SIEN_D), a(SIEN_I)),
            // 14, 15: los antebrazos por fuera
            new Superficie(ANTEBRAZO_I, 0, 1, 1.75f, -1.75f, 1.75f, -9, 0, a(MANO_I_S), a(BRAZO_I_S), NADA, NADA),
            new Superficie(ANTEBRAZO_D, 0, -1, -1.75f, -1.75f, 1.75f, -9, 0, a(MANO_D_S), a(BRAZO_D_S), NADA, NADA),
            // 16, 17: el dorso de las manos
            new Superficie(MANO_I, 2, 1, 1, -2, 2, -5, 0, NADA, a(ANTEBRAZO_I_S), NADA, NADA),
            new Superficie(MANO_D, 2, 1, 1, -2, 2, -5, 0, NADA, a(ANTEBRAZO_D_S), NADA, NADA),
            // 18-21: la parte de atrás de los brazos (desde los lados de la espalda se pasa a ellos)
            new Superficie(BRAZO_I, 2, -1, -2, -2, 2, -10, 0, a(ANTEBRAZO_I_ATRAS), a(HOMBRO_I), a(ESPALDA), a(BRAZO_I_S)),
            new Superficie(BRAZO_D, 2, -1, -2, -2, 2, -10, 0, a(ANTEBRAZO_D_ATRAS), a(HOMBRO_D), a(BRAZO_D_S), a(ESPALDA)),
            new Superficie(ANTEBRAZO_I, 2, -1, -1.75f, -1.75f, 1.75f, -9, 0, NADA, a(BRAZO_I_ATRAS), NADA, a(ANTEBRAZO_I_S)),
            new Superficie(ANTEBRAZO_D, 2, -1, -1.75f, -1.75f, 1.75f, -9, 0, NADA, a(BRAZO_D_ATRAS), a(ANTEBRAZO_D_S), NADA),
    };

    // ---------- GEOMETRÍA ----------

    private static float[] local(Superficie s, float u, float v) {
        return switch (s.eje()) {
            case 0 -> new float[]{s.plano(), v, u};
            case 1 -> new float[]{u, s.plano(), v};
            default -> new float[]{u, v, s.plano()};
        };
    }

    public static Vec3 punto(EsqueletoRey e, int id, float u, float v) {
        Superficie s = SUPERFICIES[id];
        float[] p = local(s, u, v);
        return e.aMundo(s.hueso(), p[0], p[1], p[2]);
    }

    public static Vec3 normal(EsqueletoRey e, int id) {
        Superficie s = SUPERFICIES[id];
        float n = s.signo();
        return switch (s.eje()) {
            case 0 -> e.direccion(s.hueso(), n, 0, 0);
            case 1 -> e.direccion(s.hueso(), 0, n, 0);
            default -> e.direccion(s.hueso(), 0, 0, n);
        };
    }

    private static Vec3 ejeU(EsqueletoRey e, Superficie s) {
        return switch (s.eje()) {
            case 0 -> e.direccion(s.hueso(), 0, 0, 1);
            default -> e.direccion(s.hueso(), 1, 0, 0);
        };
    }

    private static Vec3 ejeV(EsqueletoRey e, Superficie s) {
        return switch (s.eje()) {
            case 1 -> e.direccion(s.hueso(), 0, 0, 1);
            default -> e.direccion(s.hueso(), 0, 1, 0);
        };
    }

    /** ¿Se está de pie en ella (mira hacia arriba)? */
    public static boolean dePie(EsqueletoRey e, int id) {
        return normal(e, id).y > 0.7;
    }

    /** ¿Está boca abajo? (ahí no hay quien se sujete) */
    public static boolean bocaAbajo(EsqueletoRey e, int id) {
        return normal(e, id).y < -0.6;
    }

    /** Dónde van los pies del jugador agarrado. */
    public static Vec3 pies(EsqueletoRey e, int id, float u, float v, double altoJugador) {
        Vec3 p = punto(e, id, u, v);
        Vec3 n = normal(e, id);
        if (n.y > 0.7) {
            return p.add(0, 0.02, 0);                                   // de pie encima
        }
        // colgado: las manos en el asidero, el cuerpo separado de la pared
        Vec3 fuera = new Vec3(n.x, 0, n.z);
        fuera = fuera.lengthSqr() < 1.0E-4 ? Vec3.ZERO : fuera.normalize().scale(SEPARACION);
        return p.add(fuera).add(0, -altoJugador * 0.8, 0);
    }

    /** Proyecta un punto del mundo sobre una superficie: {u, v, distancia}. */
    public static float[] proyectar(EsqueletoRey e, int id, Vec3 mundo) {
        Superficie s = SUPERFICIES[id];
        Vector3f l = e.aLocal(s.hueso(), mundo);
        float u, v;
        switch (s.eje()) {
            case 0 -> { u = l.z; v = l.y; }
            case 1 -> { u = l.x; v = l.z; }
            default -> { u = l.x; v = l.y; }
        }
        u = Mth.clamp(u, s.u0(), s.u1());
        v = Mth.clamp(v, s.v0(), s.v1());
        float d = (float) punto(e, id, u, v).distanceTo(mundo);
        return new float[]{u, v, d};
    }

    /** La superficie (y el punto) más cercana a donde está el jugador. Null si no hay ninguna a mano. */
    public static float[] masCercana(EsqueletoRey e, Vec3 ojos) {
        float[] mejor = null;
        for (int id = 0; id < SUPERFICIES.length; id++) {
            if (bocaAbajo(e, id)) continue;
            float[] p = proyectar(e, id, ojos);
            if (p[2] < DISTANCIA_AGARRE && (mejor == null || p[2] < mejor[3])) {
                mejor = new float[]{id, p[0], p[1], p[2]};
            }
        }
        return mejor;
    }

    // ---------- TREPAR ----------

    /** El agarre de un jugador. */
    public static final class Agarre {
        public int superficie;
        public float u, v;
        public int ticks;
        /** Ha pasado de una plataforma a una pared andando hacia delante: mientras siga pulsando W, W baja. */
        public boolean bajandoConW;
        /** Le han echado (sacudida, estallido...): al soltarse sale despedido. */
        public boolean lanzado;
        /** De dónde viene y cuánto hace (para no rebotar entre dos superficies en una esquina). */
        public int anterior = -1, ticksCambio = 99;

        public Agarre(int superficie, float u, float v) {
            this.superficie = superficie;
            this.u = u;
            this.v = v;
        }
    }

    /**
     * Mueve el agarre con las teclas (adelante = W/S, lado = A/D, como las manda el juego al ir "montado") y el giro de
     * la cámara. En una pared: W sube, S baja y A/D van a los lados (como si estuvieras de cara a ella). De pie: se
     * anda adonde se mira.
     * Al pasarse de un borde, salta a la superficie vecina que quede más cerca (si la hay).
     */
    public static void mover(EsqueletoRey e, Agarre g, float adelante, float lado, float giroCamara) {
        if (adelante <= 0.01f) g.bajandoConW = false;
        if (Math.abs(adelante) < 0.01f && Math.abs(lado) < 0.01f) return;
        g.ticksCambio++;
        boolean estabaDePie = dePie(e, g.superficie);
        Superficie s = SUPERFICIES[g.superficie];
        Vec3 ue = ejeU(e, s), ve = ejeV(e, s);
        Vec3 frente = Vec3.directionFromRotation(0, giroCamara);
        Vec3 izquierda = new Vec3(frente.z, 0, -frente.x);
        float du, dv;
        if (dePie(e, g.superficie)) {
            Vec3 deseo = frente.scale(adelante).add(izquierda.scale(lado));
            if (deseo.lengthSqr() > 1) deseo = deseo.normalize();
            deseo = deseo.scale(VELOCIDAD_ANDAR);
            du = (float) deseo.dot(ue);
            dv = (float) deseo.dot(ve);
        } else {
            // colgado: W sube y S baja (salvo si acabas de bajar de una plataforma andando: sigues bajando)
            dv = adelante * VELOCIDAD_TREPAR * (ve.y >= 0 ? 1 : -1) * (g.bajandoConW ? -1 : 1);
            // A/D: izquierda y derecha tal como las ves de cara a la pared (no dependen de adónde mire la cámara)
            Vec3 n = normal(e, g.superficie);
            Vec3 deCara = new Vec3(-n.x, 0, -n.z);
            Vec3 izquierdaPared = deCara.lengthSqr() < 1.0E-4 ? izquierda : new Vec3(deCara.z, 0, -deCara.x);
            du = lado * VELOCIDAD_TREPAR * (izquierdaPared.dot(ue) >= 0 ? 1 : -1);
        }
        float nu = g.u + du, nv = g.v + dv;
        int[] vecinas = nv > s.v1() ? s.altoV() : nv < s.v0() ? s.bajoV() : nu > s.u1() ? s.altoU() : nu < s.u0() ? s.bajoU() : null;
        g.u = Mth.clamp(nu, s.u0(), s.u1());
        g.v = Mth.clamp(nv, s.v0(), s.v1());
        if (vecinas != null) {
            Vec3 borde = punto(e, g.superficie, g.u, g.v);
            // primero, las vecinas apuntadas; si no hay ninguna a mano, cualquier superficie cercana
            int evitar = g.ticksCambio < 20 ? g.anterior : -1;   // no volver enseguida a la de antes
            float[] mejor = vecinaMasCercana(e, g.superficie, evitar, borde, vecinas, SALTO_MAXIMO);
            if (mejor == null && vecinas != s.bajoV()) {       // (hacia abajo, sin vecinas, simplemente no se baja más)
                mejor = vecinaMasCercana(e, g.superficie, evitar, borde, null, SALTO_SIN_VECINA);
            }
            if (mejor != null) {
                g.anterior = g.superficie;
                g.ticksCambio = 0;
                g.superficie = (int) mejor[3];
                g.u = mejor[0];
                g.v = mejor[1];
                Superficie nueva = SUPERFICIES[g.superficie];
                float arriba = ejeV(e, nueva).y >= 0 ? nueva.v1() : nueva.v0();
                // de una plataforma a una pared por su parte de ARRIBA, andando hacia delante: W sigue bajando
                g.bajandoConW = estabaDePie && !dePie(e, g.superficie) && adelante > 0.01f && Math.abs(g.v - arriba) < 0.6f;
            }
        }
    }

    /** {u, v, distancia, id} de la superficie (de la lista, o de todas si es null) más cercana a un punto. */
    private static float[] vecinaMasCercana(EsqueletoRey e, int actual, int evitar, Vec3 borde, int[] lista, float maximo) {
        float[] mejor = null;
        int n = lista != null ? lista.length : SUPERFICIES.length;
        for (int k = 0; k < n; k++) {
            int id = lista != null ? lista[k] : k;
            if (id == actual || id == evitar || bocaAbajo(e, id)) continue;
            float[] p = proyectar(e, id, borde);
            if (p[2] < maximo && (mejor == null || p[2] < mejor[2])) {
                mejor = new float[]{p[0], p[1], p[2], id};
            }
        }
        return mejor;
    }
}
