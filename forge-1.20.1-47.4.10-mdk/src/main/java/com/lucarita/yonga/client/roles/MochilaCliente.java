package com.lucarita.yonga.client.roles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Recibe del servidor los huecos de la mochila con más de 127 objetos y los pone en el menú abierto. */
public final class MochilaCliente {

    private MochilaCliente() {}

    public static void actualizarHueco(int idMenu, int hueco, ItemStack objeto) {
        LocalPlayer jugador = Minecraft.getInstance().player;
        if (jugador == null) {
            return;
        }
        AbstractContainerMenu menu = jugador.containerMenu;
        if (menu.containerId == idMenu && hueco >= 0 && hueco < menu.slots.size()) {
            menu.getSlot(hueco).set(objeto);
        }
    }
}
