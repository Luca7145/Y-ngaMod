package com.lucarita.yonga.modulos.roles.cara;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * En el CLIENTE: qué lleva en la cara cada jugador que vemos (incluido el nuestro). Lo manda el servidor.
 * Lo usa CapaAntifaz para dibujarlo.
 */
public final class CaraCliente {

    private static final Map<Integer, ItemStack> CARAS = new HashMap<>();

    private CaraCliente() {}

    public static void fijar(int idEntidad, ItemStack objeto) {
        if (objeto.isEmpty()) {
            CARAS.remove(idEntidad);
        } else {
            CARAS.put(idEntidad, objeto);
        }
    }

    public static ItemStack obtener(int idEntidad) {
        return CARAS.getOrDefault(idEntidad, ItemStack.EMPTY);
    }

    public static void limpiar() {
        CARAS.clear();
    }
}
