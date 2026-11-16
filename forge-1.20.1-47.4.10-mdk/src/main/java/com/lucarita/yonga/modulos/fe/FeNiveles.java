package com.lucarita.yonga.modulos.fe;

import net.minecraft.util.Mth;

/**
 * Las cuentas de la fe: cuántos puntos hacen falta para cada nivel.
 *
 * Los niveles van de -100 a 100. Los negativos funcionan igual que los positivos, pero perdiendo puntos.
 * Pasar del nivel 0 al 1 (o al -1) cuesta 10 puntos, y cada nivel cuesta un 10% más que el anterior:
 *   0 -> 1: 10    1 -> 2: 11    2 -> 3: 12.1    3 -> 4: 13.31 ...
 */
public final class FeNiveles {

    public static final int NIVEL_MAXIMO = 100;
    public static final double PUNTOS_PRIMER_NIVEL = 10.0;
    public static final double AUMENTO_POR_NIVEL = 0.1;   // +10% cada nivel

    // Pequeño margen para que los decimales (p. ej. 21.000000001) no den un nivel de menos
    private static final double MARGEN = 1.0E-6;

    /** Puntos totales para llegar al nivel máximo (unos 1.377.961). */
    public static final double PUNTOS_MAXIMOS = puntosParaNivel(NIVEL_MAXIMO);

    private FeNiveles() {}

    /** Puntos que cuesta subir del nivel n al n+1 (n en valor absoluto: vale igual para negativos). */
    public static double costeNivel(int n) {
        return PUNTOS_PRIMER_NIVEL * Math.pow(1 + AUMENTO_POR_NIVEL, n);
    }

    /** Puntos acumulados necesarios para estar en el nivel n (en valor absoluto). Nivel 1 = 10, nivel 2 = 21, nivel 3 = 33.1... */
    public static double puntosParaNivel(int n) {
        return PUNTOS_PRIMER_NIVEL * (Math.pow(1 + AUMENTO_POR_NIVEL, n) - 1) / AUMENTO_POR_NIVEL;
    }

    /** Nivel (de -100 a 100) que corresponde a una cantidad de puntos. */
    public static int nivel(double puntos) {
        double absoluto = Math.abs(puntos);
        int n = 0;
        while (n < NIVEL_MAXIMO && absoluto >= puntosParaNivel(n + 1) - MARGEN) {
            n++;
        }
        return puntos < 0 ? -n : n;
    }

    /** Puntos que ya llevas dentro del nivel actual (hacia el siguiente). */
    public static double puntosEnNivel(double puntos) {
        return Math.abs(puntos) - puntosParaNivel(Math.abs(nivel(puntos)));
    }

    /** De 0.0 a 1.0: cuánto llevas del nivel actual. En el nivel máximo devuelve 1. */
    public static float progreso(double puntos) {
        int n = Math.abs(nivel(puntos));
        if (n >= NIVEL_MAXIMO) {
            return 1.0f;
        }
        return (float) Mth.clamp(puntosEnNivel(puntos) / costeNivel(n), 0.0, 1.0);
    }

    /** Nivel al que vas: el siguiente en la dirección en la que estás (con 0 puntos, hacia el 1). */
    public static int siguienteNivel(double puntos) {
        int n = nivel(puntos);
        if (n > 0 || (n == 0 && puntos >= 0)) {
            return Math.min(n + 1, NIVEL_MAXIMO);
        }
        return Math.max(n - 1, -NIVEL_MAXIMO);
    }

    // ---------- ORACIÓN ----------

    /** Puntos por segundo rezando en el nivel 0: 1 punto por minuto. */
    public static final double VELOCIDAD_BASE_ORACION = 1.0 / 60.0;
    /** Cuánto sube la velocidad por cada nivel alejado del cero (+7.2734%). Menos que el 10% del coste: cada nivel tarda algo más. */
    public static final double AUMENTO_VELOCIDAD_ORACION = 1.072734;

    /**
     * V(L) = d * (1/60) * 1.072734^|L|  -> puntos por segundo.
     * nivel: nivel actual del jugador. direccion: +1 al rezar, -1 al blasfemar.
     * Del 0 al ±1 se tarda 10 minutos; del ±99 al ±100, 2 horas.
     */
    public static double velocidadOracion(int nivel, int direccion) {
        return direccion * VELOCIDAD_BASE_ORACION * Math.pow(AUMENTO_VELOCIDAD_ORACION, Math.abs(nivel));
    }

    /** Recorta los puntos para que no pasen de -100 / 100. */
    public static double limitar(double puntos) {
        return Mth.clamp(puntos, -PUNTOS_MAXIMOS, PUNTOS_MAXIMOS);
    }
}
