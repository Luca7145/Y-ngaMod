package com.lucarita.yonga.modulos.fe;

import net.minecraft.util.Mth;

/**
 * Las cuentas de la fe: cuántos puntos hacen falta para cada nivel.
 *
 * Los niveles van de -100 a 100. Los negativos funcionan igual que los positivos, pero perdiendo puntos.
 * Solo el Sacerdote Maestro puede llegar un nivel más allá: 101 (Alfa) o -101 (Omega).
 * Pasar del nivel 0 al 1 (o al -1) cuesta 10 puntos, y cada nivel cuesta un 5% más que el anterior:
 *   0 -> 1: 10    1 -> 2: 10.5    2 -> 3: 11.025    3 -> 4: 11.58 ...
 * Puntos para estar en el nivel L: P(L) = sgn(L) * 200 * (1.05^|L| - 1)  ->  nivel 100 = 26.100 puntos.
 */
public final class FeNiveles {

    /** Nivel máximo normal (Iluminado / Siervo de las sombras). */
    public static final int NIVEL_MAXIMO = 100;
    /** Nivel máximo del Sacerdote Maestro (Alfa / Omega). */
    public static final int NIVEL_SUPREMO = 101;
    public static final double PUNTOS_PRIMER_NIVEL = 10.0;
    public static final double AUMENTO_POR_NIVEL = 0.05;   // +5% cada nivel

    // Pequeño margen para que los decimales (p. ej. 21.000000001) no den un nivel de menos
    private static final double MARGEN = 1.0E-6;

    private FeNiveles() {}

    /** Puntos que cuesta subir del nivel n al n+1 (n en valor absoluto: vale igual para negativos). */
    public static double costeNivel(int n) {
        return PUNTOS_PRIMER_NIVEL * Math.pow(1 + AUMENTO_POR_NIVEL, n);
    }

    /** Puntos acumulados necesarios para estar en el nivel n (en valor absoluto). Nivel 1 = 10, nivel 2 = 20.5, nivel 3 = 31.525... */
    public static double puntosParaNivel(int n) {
        return PUNTOS_PRIMER_NIVEL * (Math.pow(1 + AUMENTO_POR_NIVEL, n) - 1) / AUMENTO_POR_NIVEL;
    }

    /** Nivel (de -101 a 101) que corresponde a una cantidad de puntos. */
    public static int nivel(double puntos) {
        double absoluto = Math.abs(puntos);
        int n = 0;
        while (n < NIVEL_SUPREMO && absoluto >= puntosParaNivel(n + 1) - MARGEN) {
            n++;
        }
        return puntos < 0 ? -n : n;
    }

    /** Puntos que ya llevas dentro del nivel actual (hacia el siguiente). */
    public static double puntosEnNivel(double puntos) {
        return Math.abs(puntos) - puntosParaNivel(Math.abs(nivel(puntos)));
    }

    /** De 0.0 a 1.0: cuánto llevas del nivel actual. En tu nivel máximo (100, o 101 el Sacerdote Maestro) devuelve 1. */
    public static float progreso(double puntos, int nivelMaximo) {
        int n = Math.abs(nivel(puntos));
        if (n >= nivelMaximo) {
            return 1.0f;
        }
        return (float) Mth.clamp(puntosEnNivel(puntos) / costeNivel(n), 0.0, 1.0);
    }

    /** Nivel al que vas: el siguiente en la dirección en la que estás (con 0 puntos, hacia el 1). */
    public static int siguienteNivel(double puntos, int nivelMaximo) {
        int n = nivel(puntos);
        if (n > 0 || (n == 0 && puntos >= 0)) {
            return Math.min(n + 1, nivelMaximo);
        }
        return Math.max(n - 1, -nivelMaximo);
    }

    // ---------- ORACIÓN ----------

    // Lo que se tarda rezando en el primer nivel (0 -> ±1) y en el último (±99 -> ±100), en segundos
    public static final double SEGUNDOS_PRIMER_NIVEL = 10 * 60;    // 10 minutos
    public static final double SEGUNDOS_ULTIMO_NIVEL = 2 * 60 * 60; // 2 horas

    /** Puntos por segundo rezando en el nivel 0: 10 puntos en 10 minutos = 1 punto por minuto. */
    public static final double VELOCIDAD_BASE_ORACION = PUNTOS_PRIMER_NIVEL / SEGUNDOS_PRIMER_NIVEL;

    /**
     * Cuánto sube la velocidad por cada nivel alejado del cero. Se calcula solo para que se cumplan
     * los 10 minutos y las 2 horas de arriba aunque cambies AUMENTO_POR_NIVEL.
     * Con el coste al +5% sale 1.05 / 12^(1/99) = 1.023973 (+2.3973% por nivel).
     */
    public static final double AUMENTO_VELOCIDAD_ORACION = (1 + AUMENTO_POR_NIVEL)
            / Math.pow(SEGUNDOS_ULTIMO_NIVEL / SEGUNDOS_PRIMER_NIVEL, 1.0 / (NIVEL_MAXIMO - 1));

    /**
     * V(L) = d * (1/60) * 1.023973^|L|  -> puntos por segundo.
     * nivel: nivel actual del jugador. direccion: +1 al rezar, -1 al blasfemar.
     * Del 0 al ±1 se tarda 10 minutos; del ±99 al ±100, 2 horas.
     */
    public static double velocidadOracion(int nivel, int direccion) {
        return direccion * VELOCIDAD_BASE_ORACION * Math.pow(AUMENTO_VELOCIDAD_ORACION, Math.abs(nivel));
    }

    /** Recorta los puntos para que no pasen del nivel máximo del jugador (±100, o ±101 el Sacerdote Maestro). */
    public static double limitar(double puntos, int nivelMaximo) {
        double maximo = puntosParaNivel(nivelMaximo);
        return Mth.clamp(puntos, -maximo, maximo);
    }
}
