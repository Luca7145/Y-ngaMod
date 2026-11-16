package com.lucarita.yonga.modulos.fe;

/**
 * Copia de la fe del jugador en SU propio cliente (la manda el servidor con SincronizarFePacket).
 * La usan las barras del HUD (fe y maná) y el menú de fe. En el servidor no sirve para nada: allí se usa FeDatos.
 */
public final class FeCliente {

    private static double puntos = 0;
    private static double mana = 0;

    private FeCliente() {}

    public static double puntos() {
        return puntos;
    }

    public static double mana() {
        return mana;
    }

    public static RangoFe rango() {
        return RangoFe.dePuntos(puntos);
    }

    public static void actualizar(double nuevosPuntos, double nuevoMana) {
        puntos = nuevosPuntos;
        mana = nuevoMana;
    }
}
