package com.lucarita.yonga.modulos.roles.oficios.mochila;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Guarda y lee lo que hay dentro de una mochila (en los datos del propio objeto).
 *
 * Ojo: Minecraft guarda la cantidad de un montón en un número que solo llega a 127.
 * Como la mochila mejorada admite 200 por hueco, guardamos la cantidad aparte ("Cantidad").
 */
public final class MochilaContenido {

    private static final String CLAVE = "yonga_mochila";

    private MochilaContenido() {}

    public static void cargar(ItemStack mochila, Container destino) {
        CompoundTag datos = mochila.getTag();
        if (datos == null || !datos.contains(CLAVE)) {
            return;
        }
        ListTag lista = datos.getList(CLAVE, Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag entrada = lista.getCompound(i);
            int hueco = entrada.getInt("Hueco");
            if (hueco < 0 || hueco >= destino.getContainerSize()) {
                continue;
            }
            ItemStack objeto = ItemStack.of(entrada.getCompound("Objeto"));
            objeto.setCount(entrada.getInt("Cantidad"));
            destino.setItem(hueco, objeto);
        }
    }

    public static void guardar(ItemStack mochila, Container origen) {
        ListTag lista = new ListTag();
        for (int i = 0; i < origen.getContainerSize(); i++) {
            ItemStack objeto = origen.getItem(i);
            if (objeto.isEmpty()) {
                continue;
            }
            CompoundTag entrada = new CompoundTag();
            entrada.putInt("Hueco", i);
            entrada.put("Objeto", objeto.copyWithCount(1).save(new CompoundTag()));
            entrada.putInt("Cantidad", objeto.getCount());
            lista.add(entrada);
        }
        mochila.getOrCreateTag().put(CLAVE, lista);
    }

    /** Cuántos huecos tiene ocupados (para la descripción del objeto). */
    public static int huecosOcupados(ItemStack mochila) {
        CompoundTag datos = mochila.getTag();
        return datos == null ? 0 : datos.getList(CLAVE, Tag.TAG_COMPOUND).size();
    }
}
