package com.lucarita.yonga.modulos.fe;

/**
 * Copia de la fe del jugador en SU propio cliente (la manda el servidor con SincronizarFePacket).
 * La usan la barra del HUD y el menú de fe. En el servidor no sirve para nada: allí se usa FeDatos.
 */
public final class FeCliente {

    private static double puntos = 0;

    private FeCliente() {}

    public static double puntos() {
        return puntos;
    }

    public static void fijarPuntos(double nuevosPuntos) {
        puntos = nuevosPuntos;
    }
}
