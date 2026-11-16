package com.lucarita.yonga.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

/**
 * Las recetas de Minecraft no saben quién está fabricando. Esto lo averigua buscando qué jugador
 * tiene abierto el menú (mesa de trabajo o inventario) al que pertenece la cuadrícula de fabricación.
 * Lo usan las recetas con requisitos (oficio de herrero, rango de fe...).
 */
public final class JugadorFabricando {

    private JugadorFabricando() {}

    /** El jugador que está usando esa cuadrícula, o null si no hay ninguno (p. ej. una máquina de otro mod). */
    public static Player buscar(CraftingContainer cuadricula, Level nivel) {
        for (Player jugador : nivel.players()) {
            AbstractContainerMenu menu = jugador.containerMenu;
            if (menu == null) {
                continue;
            }
            for (Slot hueco : menu.slots) {
                if (hueco.container == cuadricula) {
                    return jugador;
                }
            }
        }
        return null;
    }
}
