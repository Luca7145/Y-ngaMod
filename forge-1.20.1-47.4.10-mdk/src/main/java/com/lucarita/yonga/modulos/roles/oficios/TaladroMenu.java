package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

/**
 * El depósito del taladro: un hueco para el combustible (arriba, con la llama al lado) y el inventario.
 * Lo que se mete se guarda al momento en el propio taladro. El taladro abierto queda bloqueado en su
 * hueco (no se puede mover ni tirar mientras el menú está abierto), como la mochila.
 */
public class TaladroMenu extends AbstractContainerMenu {

    public static final int X_HUECO = 56, Y_HUECO = 53;     // hueco del combustible (donde el del horno)
    private static final int TECLA_MANO_SECUNDARIA = 40;

    private final SimpleContainer contenido = new SimpleContainer(1);
    private final ItemStack taladro;
    private final InteractionHand mano;
    private final int huecoBloqueado;
    private final ContainerData llama;                       // en segundos: 0 lo que queda, 1 lo que duraba (cabe en un short)

    public TaladroMenu(int id, Inventory inventario, InteractionHand mano) {
        super(ModuloRoles.MENU_TALADRO.get(), id);
        Player jugador = inventario.player;
        this.mano = mano;
        this.taladro = jugador.getItemInHand(mano);
        this.huecoBloqueado = mano == InteractionHand.MAIN_HAND ? inventario.selected : -1;

        if (!jugador.level().isClientSide()) {
            contenido.setItem(0, TaladroItem.deposito(taladro));
            contenido.addListener(c -> TaladroItem.fijarDeposito(taladro, contenido.getItem(0)));
            this.llama = new ContainerData() {
                @Override public int get(int i) { return (i == 0 ? TaladroItem.llama(taladro) : TaladroItem.llamaTotal(taladro)) / 20; }
                @Override public void set(int i, int valor) {}
                @Override public int getCount() { return 2; }
            };
        } else {
            this.llama = new SimpleContainerData(2);
        }
        addDataSlots(llama);

        addSlot(new Slot(contenido, 0, X_HUECO, Y_HUECO) {
            @Override
            public boolean mayPlace(ItemStack objeto) {
                return ForgeHooks.getBurnTime(objeto, RecipeType.SMELTING) > 0 && !(objeto.getItem() instanceof TaladroItem);
            }
        });
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventario, columna + fila * 9 + 9, 8 + columna * 18, 84 + fila * 18));
            }
        }
        for (int columna = 0; columna < 9; columna++) {
            int x = 8 + columna * 18;
            addSlot(columna == huecoBloqueado ? new Slot(inventario, columna, x, 142) {
                @Override public boolean mayPickup(Player j) { return false; }
                @Override public boolean mayPlace(ItemStack o) { return false; }
            } : new Slot(inventario, columna, x, 142));
        }
    }

    public static TaladroMenu desdeRed(int id, Inventory inventario, FriendlyByteBuf datos) {
        return new TaladroMenu(id, inventario, datos.readEnum(InteractionHand.class));
    }

    /** De 0 a 1: cuánto le queda a la llama (para dibujarla). */
    public float llama() {
        int total = llama.get(1);
        return total <= 0 ? 0 : Math.min(1f, (float) llama.get(0) / total);
    }

    public int segundosLlama() {
        return llama.get(0);
    }

    @Override
    public boolean stillValid(Player jugador) {
        return jugador.getItemInHand(mano) == taladro;
    }

    @Override
    public void removed(Player jugador) {
        super.removed(jugador);
        if (!jugador.level().isClientSide()) {
            TaladroItem.fijarDeposito(taladro, contenido.getItem(0));
        }
    }

    @Override
    public void clicked(int indice, int boton, ClickType tipo, Player jugador) {
        if (tipo == ClickType.SWAP && (boton == huecoBloqueado || (mano == InteractionHand.OFF_HAND && boton == TECLA_MANO_SECUNDARIA))) {
            return;
        }
        super.clicked(indice, boton, tipo, jugador);
    }

    // Mayúsculas + click: del depósito al inventario y al revés
    @Override
    public ItemStack quickMoveStack(Player jugador, int indice) {
        Slot hueco = slots.get(indice);
        if (!hueco.hasItem() || !hueco.mayPickup(jugador)) {
            return ItemStack.EMPTY;
        }
        ItemStack objeto = hueco.getItem();
        ItemStack copia = objeto.copy();
        if (indice == 0) {
            if (!moveItemStackTo(objeto, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!slots.get(0).mayPlace(objeto) || !moveItemStackTo(objeto, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (objeto.isEmpty()) hueco.setByPlayer(ItemStack.EMPTY); else hueco.setChanged();
        return copia;
    }
}
