package com.lucarita.yonga.modulos.roles;

/**
 * Copia del oficio del jugador en SU propio cliente (la manda el servidor con SincronizarRolPacket).
 * La usa el menú de oficios. En el servidor se usa RolDatos.
 */
public final class RolCliente {

    private static Rol rol = null;
    private static int experiencia = 0;
    private static boolean pedirMenu = false;

    private RolCliente() {}

    public static Rol rol() {
        return rol;
    }

    public static int experiencia() {
        return experiencia;
    }

    public static RangoRol rango() {
        return RangoRol.deExperiencia(experiencia);
    }

    public static void actualizar(Rol nuevoRol, int nuevaExperiencia, boolean abrirMenu) {
        rol = nuevoRol;
        experiencia = nuevaExperiencia;
        if (abrirMenu) {
            pedirMenu = true;
        }
    }

    /** ¿Hay que abrir el menú de elegir oficio? Devuelve true una sola vez. */
    public static boolean consumirPeticionMenu() {
        boolean valor = pedirMenu;
        pedirMenu = false;
        return valor;
    }

    public static void limpiar() {
        rol = null;
        experiencia = 0;
        pedirMenu = false;
    }
}
