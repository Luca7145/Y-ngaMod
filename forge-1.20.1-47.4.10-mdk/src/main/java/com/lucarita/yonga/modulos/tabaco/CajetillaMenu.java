package com.lucarita.yonga.modulos.tabaco;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * El menú de la cajetilla: un hueco arriba (hasta 10 cigarros sin estrenar) y el inventario abajo. Como la mochila:
 * la cajetilla abierta queda bloqueada en su hueco y todo se guarda al momento en ella.
 */
public class CajetillaMenu extends AbstractContainerMenu {

    private static final int TECLA_MANO_SECUNDARIA = 40;

    private final SimpleContainer contenido;
    private final ItemStack cajetilla;
    private final InteractionHand mano;
    private final int huecoBloqueado;

    public CajetillaMenu(int id, Inventory inventario, InteractionHand mano) {
        super(ModuloTabaco.MENU_CAJETILLA.get(), id);
        Player jugador = inventario.player;
        this.mano = mano;
        this.cajetilla = jugador.getItemInHand(mano);
        this.huecoBloqueado = mano == InteractionHand.MAIN_HAND ? inventario.selected : -1;
        this.contenido = new SimpleContainer(1) {
            @Override
            public int getMaxStackSize() {
                return CajetillaItem.CAPACIDAD;
            }
        };
        if (!jugador.level().isClientSide()) {
            int n = CajetillaItem.cigarros(cajetilla);
            if (n > 0) contenido.setItem(0, new ItemStack(ModuloTabaco.CIGARRO.get(), n));
            contenido.addListener(c -> guardar());
        }
        addSlot(new HuecoCajetilla(contenido, 0, 80, 18));
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

    public static CajetillaMenu desdeRed(int id, Inventory inventario, FriendlyByteBuf datos) {
        return new CajetillaMenu(id, inventario, datos.readEnum(InteractionHand.class));
    }

    private void guardar() {
        ItemStack dentro = contenido.getItem(0);
        CajetillaItem.ponerCigarros(cajetilla, dentro.isEmpty() ? 0 : dentro.getCount());
    }

    @Override
    public boolean stillValid(Player jugador) {
        return jugador.getItemInHand(mano) == cajetilla;
    }

    @Override
    public void removed(Player jugador) {
        super.removed(jugador);
        if (!jugador.level().isClientSide()) guardar();
    }

    @Override
    public void clicked(int indice, int boton, ClickType tipo, Player jugador) {
        if (tipo == ClickType.SWAP && (boton == huecoBloqueado || (mano == InteractionHand.OFF_HAND && boton == TECLA_MANO_SECUNDARIA)
                || indice == 0)) {
            return;                // ni mover la cajetilla abierta, ni sacar el montón de cigarros de golpe con 1-9
        }
        super.clicked(indice, boton, tipo, jugador);
    }

    /** Mayús + clic: de la cajetilla salen de uno en uno a huecos libres; a la cajetilla entran los que quepan. */
    @Override
    public ItemStack quickMoveStack(Player jugador, int indice) {
        Slot hueco = slots.get(indice);
        if (!hueco.hasItem()) return ItemStack.EMPTY;
        ItemStack objeto = hueco.getItem();
        if (indice == 0) {
            while (!objeto.isEmpty()) {
                ItemStack uno = objeto.copyWithCount(1);
                if (!moveItemStackTo(uno, 1, slots.size(), true)) break;
                objeto.shrink(1);
            }
            hueco.setChanged();
            return ItemStack.EMPTY;
        }
        Slot caja = slots.get(0);
        if (!caja.mayPlace(objeto)) return ItemStack.EMPTY;
        ItemStack dentro = caja.getItem();
        if (dentro.isEmpty()) {
            caja.setByPlayer(objeto.split(1));
        } else if (dentro.getCount() < CajetillaItem.CAPACIDAD) {
            dentro.grow(1);
            objeto.shrink(1);
            caja.setChanged();
        } else {
            return ItemStack.EMPTY;
        }
        if (objeto.isEmpty()) hueco.setByPlayer(ItemStack.EMPTY);
        else hueco.setChanged();
        return ItemStack.EMPTY;
    }

    /** El hueco de la cajetilla: solo cigarros sin estrenar, hasta 10, y se sacan de uno en uno. */
    private static class HuecoCajetilla extends Slot {
        HuecoCajetilla(SimpleContainer contenedor, int indice, int x, int y) {
            super(contenedor, indice, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack objeto) {
            return objeto.is(ModuloTabaco.CIGARRO.get()) && FumableItem.nuevo(objeto);
        }

        @Override
        public int getMaxStackSize() {
            return CajetillaItem.CAPACIDAD;
        }

        @Override
        public int getMaxStackSize(ItemStack objeto) {
            return CajetillaItem.CAPACIDAD;
        }

        @Override
        public ItemStack remove(int cantidad) {
            return super.remove(1);
        }
    }

    private static class Bloqueado extends Slot {
        Bloqueado(Inventory inventario, int indice, int x, int y) {
            super(inventario, indice, x, y);
        }

        @Override
        public boolean mayPickup(Player jugador) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack objeto) {
            return false;
        }
    }
}
