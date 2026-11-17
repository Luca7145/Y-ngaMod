package com.lucarita.yonga.modulos.roles.oficios.mochila;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Un hueco de la mochila: admite hasta 100 (o 200 en la mejorada) de cada objeto apilable.
 * Los objetos que no se apilan (herramientas, armaduras...) siguen siendo de 1 en 1.
 * No se pueden meter mochilas ni cajas de shulker (para no meter almacenes dentro de almacenes).
 */
public class HuecoMochila extends Slot {

    private final int limite;

    public HuecoMochila(Container contenedor, int indice, int x, int y, int limite) {
        super(contenedor, indice, x, y);
        this.limite = limite;
    }

    @Override
    public int getMaxStackSize() {
        return limite;
    }

    @Override
    public int getMaxStackSize(ItemStack objeto) {
        return objeto.getMaxStackSize() <= 1 ? 1 : limite;
    }

    @Override
    public boolean mayPlace(ItemStack objeto) {
        return !(objeto.getItem() instanceof MochilaItem) && objeto.getItem().canFitInsideContainerItems();
    }

    /**
     * Al sacar con el ratón, como mucho un montón normal (64) de golpe: así en el cursor nunca hay más
     * de lo que admite un hueco normal del inventario.
     */
    @Override
    public ItemStack remove(int cantidad) {
        return super.remove(Math.min(cantidad, getItem().getMaxStackSize()));
    }
}
