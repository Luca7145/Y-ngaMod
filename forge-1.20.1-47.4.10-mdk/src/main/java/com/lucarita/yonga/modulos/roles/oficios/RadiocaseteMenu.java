package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

/**
 * Menú del radiocasete: un hueco para el disco y el inventario debajo. Los tres botones de la pantalla
 * (RadiocaseteScreen) llegan aquí con clickMenuButton: 0 = pausa, 1 = continuar, 2 = volver a empezar.
 * Como la cajetilla, el radiocasete abierto queda bloqueado en su hueco y el disco se guarda al momento en él.
 */
public class RadiocaseteMenu extends AbstractContainerMenu {

    public static final int BOTON_PAUSA = 0;
    public static final int BOTON_CONTINUAR = 1;
    public static final int BOTON_REINICIAR = 2;

    // Dónde está el hueco del disco (la pantalla dibuja el resto alrededor)
    public static final int HUECO_X = 44, HUECO_Y = 18;

    private static final int TECLA_MANO_SECUNDARIA = 40;

    private final SimpleContainer contenido = new SimpleContainer(1);
    private final ItemStack radio;
    private final InteractionHand mano;
    private final int huecoBloqueado;
    private final Player jugador;

    public RadiocaseteMenu(int id, Inventory inventario, InteractionHand mano) {
        super(ModuloRoles.MENU_RADIOCASETE.get(), id);
        this.jugador = inventario.player;
        this.mano = mano;
        this.radio = jugador.getItemInHand(mano);
        this.huecoBloqueado = mano == InteractionHand.MAIN_HAND ? inventario.selected : -1;
        if (!jugador.level().isClientSide()) {
            contenido.setItem(0, RadiocaseteItem.disco(radio));
            contenido.addListener(c -> alCambiarDisco());
        }
        addSlot(new HuecoDisco(contenido, 0, HUECO_X, HUECO_Y));
        int y = 18 + 18 + 13;
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventario, columna + fila * 9 + 9, 8 + columna * 18, y + fila * 18));
            }
        }
        for (int columna = 0; columna < 9; columna++) {
            int x = 8 + columna * 18;
            addSlot(columna == huecoBloqueado ? new Bloqueado(inventario, columna, x, y + 58) : new Slot(inventario, columna, x, y + 58));
        }
    }

    public static RadiocaseteMenu desdeRed(int id, Inventory inventario, FriendlyByteBuf datos) {
        return new RadiocaseteMenu(id, inventario, datos.readEnum(InteractionHand.class));
    }

    /** Al meter un disco empieza a sonar; al sacarlo, se calla. */
    private void alCambiarDisco() {
        ItemStack antes = RadiocaseteItem.disco(radio);
        ItemStack ahora = contenido.getItem(0);
        if (ItemStack.isSameItemSameTags(antes, ahora)) return;
        RadiocaseteItem.ponerDisco(radio, ahora);
        if (jugador instanceof ServerPlayer servidor) {
            if (ahora.isEmpty()) {
                RadiocaseteItem.pararSiEs(servidor, radio);
            } else {
                RadiocaseteItem.tocar(servidor, radio);
            }
        }
    }

    @Override
    public boolean clickMenuButton(Player quien, int boton) {
        if (!(quien instanceof ServerPlayer servidor)) return true;
        switch (boton) {
            case BOTON_PAUSA -> RadiocaseteItem.pausar(servidor, radio);
            case BOTON_CONTINUAR -> RadiocaseteItem.continuar(servidor, radio);
            case BOTON_REINICIAR -> RadiocaseteItem.tocar(servidor, radio);
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player quien) {
        return quien.getItemInHand(mano) == radio;
    }

    @Override
    public void clicked(int indice, int boton, ClickType tipo, Player quien) {
        if (tipo == ClickType.SWAP && (boton == huecoBloqueado || (mano == InteractionHand.OFF_HAND && boton == TECLA_MANO_SECUNDARIA))) {
            return;                // no mover el radiocasete abierto con las teclas 1-9 / F
        }
        super.clicked(indice, boton, tipo, quien);
    }

    @Override
    public ItemStack quickMoveStack(Player quien, int indice) {
        Slot hueco = slots.get(indice);
        if (!hueco.hasItem()) return ItemStack.EMPTY;
        ItemStack objeto = hueco.getItem();
        ItemStack copia = objeto.copy();
        if (indice == 0) {
            if (!moveItemStackTo(objeto, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!slots.get(0).mayPlace(objeto) || slots.get(0).hasItem()) return ItemStack.EMPTY;
            if (!moveItemStackTo(objeto, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (objeto.isEmpty()) hueco.setByPlayer(ItemStack.EMPTY);
        else hueco.setChanged();
        return copia;
    }

    /** El hueco del disco: solo discos de música, de uno en uno. */
    private static class HuecoDisco extends Slot {
        HuecoDisco(SimpleContainer contenedor, int indice, int x, int y) {
            super(contenedor, indice, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack objeto) {
            return objeto.getItem() instanceof RecordItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static class Bloqueado extends Slot {
        Bloqueado(Inventory inventario, int indice, int x, int y) {
            super(inventario, indice, x, y);
        }

        @Override
        public boolean mayPickup(Player quien) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack objeto) {
            return false;
        }
    }
}
