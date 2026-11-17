package com.lucarita.yonga.modulos.roles.cara;

import net.minecraft.world.SimpleContainer;

/**
 * Todo jugador tiene un "hueco de cara" (lo añade PlayerMixin). Para llegar a él:
 *   ((AccesoCara) jugador).yonga$cara()
 * Mejor usa Cara.obtener(jugador), que es más cómodo.
 */
public interface AccesoCara {
    SimpleContainer yonga$cara();
}
