package com.lucarita.yonga.modulos.roles.cara;

import com.mojang.datafixers.util.Pair;
import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.BooleanSupplier;

/**
 * El hueco de cara del inventario del jugador. Solo admite objetos de cara (etiqueta "yonga:cara"), de uno en uno.
 */
public class HuecoCara extends Slot {

    /** Posición en el inventario: justo encima del hueco del escudo (que está en 77, 62). */
    public static final int X = 77;
    public static final int Y = 44;

    // Círculo gris que se ve cuando el hueco está vacío (como los dibujos de los huecos de armadura)
    private static final ResourceLocation ICONO_VACIO = new ResourceLocation(YongaMain.MOD_ID, "item/hueco_cara");

    /**
     * En el inventario del modo creativo Minecraft recoloca los huecos a su manera y el nuestro acabaría
     * encima de la barra rápida, así que allí lo ocultamos. El cliente pone aquí la comprobación
     * "¿está abierto el inventario creativo?" (ver ClientSetup); en el servidor nunca se oculta.
     */
    public static BooleanSupplier ocultar = () -> false;

    public HuecoCara(Container contenedor) {
        super(contenedor, 0, X, Y);
    }

    @Override
    public boolean mayPlace(ItemStack objeto) {
        return objeto.is(Cara.OBJETOS_DE_CARA);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isActive() {
        return !ocultar.getAsBoolean();
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        return Pair.of(InventoryMenu.BLOCK_ATLAS, ICONO_VACIO);
    }
}
