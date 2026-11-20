package com.lucarita.yonga.core.moderacion;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.cara.AccesoCara;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * El inventario de otro jugador abierto con el guante del moderador, con la forma de un cofre de 5 filas (en el
 * cliente se ve con la pantalla normal de cofre, por eso el tipo es GENERIC_9x5 y los huecos están donde los pone el
 * cofre):
 *   filas 1-3: el inventario     fila 4: la barra rápida
 *   fila 5:    casco, peto, pantalones, botas, mano secundaria, la cara (si el módulo de roles está activo) y
 *              huecos de cristal que no se usan.
 * Los huecos leen y escriben directamente en el inventario del otro jugador, así que todo se ve al momento en los
 * dos lados. Se cierra si el otro se desconecta o muere.
 */
public class InventarioAjenoMenu extends AbstractContainerMenu {

    private static final int FILAS = 5;
    private static final int HUECOS = FILAS * 9;
    private static final int CARA = -2;                       // el hueco de la cara (no es del inventario normal)
    private static final EquipmentSlot[] ARMADURA = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final ServerPlayer otro;

    public InventarioAjenoMenu(int id, Inventory inventarioModerador, ServerPlayer otro) {
        super(MenuType.GENERIC_9x5, id);
        this.otro = otro;
        Container vista = new Vista(otro);
        int arriba = (FILAS - 4) * 18;
        for (int fila = 0; fila < FILAS; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                int indice = columna + fila * 9;
                addSlot(new Hueco(vista, indice, 8 + columna * 18, 18 + fila * 18));
            }
        }
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventarioModerador, columna + fila * 9 + 9, 8 + columna * 18, 103 + fila * 18 + arriba));
            }
        }
        for (int columna = 0; columna < 9; columna++) {
            addSlot(new Slot(inventarioModerador, columna, 8 + columna * 18, 161 + arriba));
        }
    }

    /** Hueco del menú -> hueco del inventario del otro (-1 = cristal, no se usa). */
    private static int destino(int indice) {
        if (indice < 27) return indice + 9;                  // inventario
        if (indice < 36) return indice - 27;                 // barra rápida
        if (indice < 40) return 39 - (indice - 36);          // armadura: casco (39), peto (38), pantalones (37), botas (36)
        if (indice == 40) return 40;                         // mano secundaria
        if (indice == 41 && Modulo.ROLES.activo()) return CARA;
        return -1;
    }

    @Override
    public boolean stillValid(Player moderador) {
        return otro.isAlive() && !otro.hasDisconnected() && GuanteModeradorItem.puedeUsarlo(moderador);
    }

    @Override
    public ItemStack quickMoveStack(Player moderador, int indice) {
        Slot hueco = this.slots.get(indice);
        if (!hueco.hasItem() || !hueco.mayPickup(moderador)) return ItemStack.EMPTY;
        ItemStack objeto = hueco.getItem();
        ItemStack copia = objeto.copy();
        if (indice < HUECOS) {
            if (!moveItemStackTo(objeto, HUECOS, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(objeto, 0, 36, false)) {
            return ItemStack.EMPTY;
        }
        if (objeto.isEmpty()) hueco.setByPlayer(ItemStack.EMPTY);
        else hueco.setChanged();
        return copia;
    }

    /** Un hueco del inventario del otro: la armadura solo admite lo que va en ese sitio, y el cristal no admite nada. */
    private static class Hueco extends Slot {
        private final int destino;

        Hueco(Container vista, int indice, int x, int y) {
            super(vista, indice, x, y);
            this.destino = destino(indice);
        }

        @Override
        public boolean mayPlace(ItemStack objeto) {
            if (destino == CARA) return objeto.is(Cara.OBJETOS_DE_CARA);
            if (destino < 0) return false;
            if (destino >= 36 && destino <= 39) {
                Player otro = ((Vista) this.container).otro;
                return objeto.canEquip(ARMADURA[39 - destino], otro);
            }
            return true;
        }

        @Override
        public boolean mayPickup(Player quien) {
            return destino >= 0 || destino == CARA;
        }

        @Override
        public int getMaxStackSize() {
            return (destino >= 36 && destino <= 39) || destino == CARA ? 1 : super.getMaxStackSize();
        }
    }

    /** El inventario del otro jugador visto como un contenedor de 45 huecos. */
    private static class Vista implements Container {
        private static final ItemStack CRISTAL = new ItemStack(Items.GRAY_STAINED_GLASS_PANE)
                .setHoverName(Component.literal(" "));
        final ServerPlayer otro;

        Vista(ServerPlayer otro) {
            this.otro = otro;
        }

        private Inventory inventario() {
            return otro.getInventory();
        }

        private Container cara() {
            return ((AccesoCara) otro).yonga$cara();
        }

        @Override
        public int getContainerSize() {
            return HUECOS;
        }

        @Override
        public boolean isEmpty() {
            return inventario().isEmpty();
        }

        @Override
        public ItemStack getItem(int indice) {
            int d = destino(indice);
            if (d == CARA) return cara().getItem(0);
            return d < 0 ? CRISTAL : inventario().getItem(d);
        }

        @Override
        public ItemStack removeItem(int indice, int cantidad) {
            int d = destino(indice);
            if (d == CARA) return cara().removeItem(0, cantidad);
            return d < 0 ? ItemStack.EMPTY : inventario().removeItem(d, cantidad);
        }

        @Override
        public ItemStack removeItemNoUpdate(int indice) {
            int d = destino(indice);
            if (d == CARA) return cara().removeItemNoUpdate(0);
            return d < 0 ? ItemStack.EMPTY : inventario().removeItemNoUpdate(d);
        }

        @Override
        public void setItem(int indice, ItemStack objeto) {
            int d = destino(indice);
            if (d == CARA) cara().setItem(0, objeto);
            else if (d >= 0) inventario().setItem(d, objeto);
        }

        @Override
        public void setChanged() {
            inventario().setChanged();
            if (Modulo.ROLES.activo()) cara().setChanged();
            otro.inventoryMenu.broadcastChanges();           // que el otro lo vea al momento
        }

        @Override
        public boolean stillValid(Player quien) {
            return true;
        }

        @Override
        public void clearContent() {
        }
    }
}
