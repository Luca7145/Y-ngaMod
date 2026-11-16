package com.lucarita.yonga.modulos.fe;

import net.minecraft.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * En el CLIENTE: qué jugadores que vemos están rezando o blasfemando, y desde cuándo
 * (lo de "desde cuándo" sirve para que la animación de arrodillarse sea gradual).
 */
public final class OracionCliente {

    public record Estado(int direccion, long inicioMilis) {}

    private static final Map<Integer, Estado> ESTADOS = new HashMap<>();

    private OracionCliente() {}

    public static void fijar(int idEntidad, int direccion) {
        if (direccion == Oracion.NADA) {
            ESTADOS.remove(idEntidad);
        } else {
            ESTADOS.put(idEntidad, new Estado(direccion, Util.getMillis()));
        }
    }

    /** null si no está rezando. */
    public static Estado estado(int idEntidad) {
        return ESTADOS.get(idEntidad);
    }

    public static void limpiar() {
        ESTADOS.clear();
    }
}
