package com.lucarita.yonga.modulos.roles.oficios.mochila;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.red.HuecoMochilaPacket;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * El menú de la mochila: sus huecos arriba y el inventario del jugador abajo (como un cofre).
 *
 * Cosas importantes para que no se puedan duplicar objetos:
 *  - La mochila abierta queda bloqueada en su hueco: no se puede mover, tirar ni cambiar con las teclas 1-9.
 *  - Si la mochila deja de estar en la mano (p. ej. muere el jugador), el menú se cierra.
 *  - Todo cambio se guarda al momento en la propia mochila.
 *
 * Y para los montones de más de 127 (mochila mejorada): Minecraft envía las cantidades en un número que
 * solo llega a 127, así que esos huecos los mandamos nosotros con HuecoMochilaPacket.
 */
public class MochilaMenu extends AbstractContainerMenu {

    private static final int LIMITE_PAQUETE_VANILLA = 127;
    private static final int TECLA_MANO_SECUNDARIA = 40;

    private final SimpleContainer contenido;
    private final ItemStack mochila;
    private final InteractionHand mano;
    private final Player jugador;
    private final int huecos;
    private final int filas;
    private final int huecoBloqueado;   // índice en la barra rápida de la mochila abierta (-1 si va en la mano secundaria)

    public MochilaMenu(int id, Inventory inventario, InteractionHand mano, int huecos, int limite) {
        super(ModuloRoles.MENU_MOCHILA.get(), id);
        this.jugador = inventario.player;
        this.mano = mano;
        this.mochila = jugador.getItemInHand(mano);
        this.huecos = huecos;
        this.filas = (huecos + 8) / 9;
        this.huecoBloqueado = mano == InteractionHand.MAIN_HAND ? inventario.selected : -1;

        this.contenido = new SimpleContainer(huecos);
        if (!jugador.level().isClientSide()) {
            MochilaContenido.cargar(mochila, contenido);
            contenido.addListener(c -> MochilaContenido.guardar(mochila, contenido));
        }

        // Huecos de la mochila (9 por fila, como un cofre)
        for (int i = 0; i < huecos; i++) {
            addSlot(new HuecoMochila(contenido, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18, limite));
        }

        // Inventario del jugador
        int yInventario = 18 + filas * 18 + 13;
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventario, columna + fila * 9 + 9, 8 + columna * 18, yInventario + fila * 18));
            }
        }
        // Barra rápida (con la mochila abierta bloqueada)
        for (int columna = 0; columna < 9; columna++) {
            int x = 8 + columna * 18;
            int y = yInventario + 58;
            addSlot(columna == huecoBloqueado ? new HuecoBloqueado(inventario, columna, x, y) : new Slot(inventario, columna, x, y));
        }
    }

    /** El cliente monta el menú con lo que le manda el servidor al abrir. */
    public static MochilaMenu desdeRed(int id, Inventory inventario, FriendlyByteBuf datos) {
        return new MochilaMenu(id, inventario, datos.readEnum(InteractionHand.class), datos.readVarInt(), datos.readVarInt());
    }

    public int huecos() {
        return huecos;
    }

    public int filas() {
        return filas;
    }

    // Si la mochila ya no está en la mano (se ha tirado, ha muerto...), se cierra
    @Override
    public boolean stillValid(Player jugador) {
        return jugador.getItemInHand(mano) == mochila;
    }

    @Override
    public void removed(Player jugador) {
        super.removed(jugador);
        if (!jugador.level().isClientSide()) {
            MochilaContenido.guardar(mochila, contenido);
        }
    }

    // Bloquea los cambios con las teclas 1-9 / F que moverían la mochila abierta o sacarían montones gigantes
    @Override
    public void clicked(int indice, int boton, ClickType tipo, Player jugador) {
        if (tipo == ClickType.SWAP) {
            if (boton == huecoBloqueado || (mano == InteractionHand.OFF_HAND && boton == TECLA_MANO_SECUNDARIA)) {
                return;
            }
            if (indice >= 0 && indice < huecos) {
                ItemStack enHueco = slots.get(indice).getItem();
                if (enHueco.getCount() > enHueco.getMaxStackSize()) {
                    return;
                }
            }
        }
        super.clicked(indice, boton, tipo, jugador);
    }

    // ---------- MAYÚS + CLICK ----------

    @Override
    public ItemStack quickMoveStack(Player jugador, int indice) {
        Slot hueco = slots.get(indice);
        if (!hueco.hasItem() || !hueco.mayPickup(jugador)) {
            return ItemStack.EMPTY;
        }
        ItemStack objeto = hueco.getItem();
        ItemStack copia = objeto.copy();

        if (indice < huecos) {
            // Mochila -> inventario: de montón normal en montón normal (se repite solo mientras quepa)
            ItemStack parte = objeto.copyWithCount(Math.min(objeto.getCount(), objeto.getMaxStackSize()));
            int antes = parte.getCount();
            if (!moveItemStackTo(parte, huecos, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            objeto.shrink(antes - parte.getCount());
        } else {
            // Inventario -> mochila
            if (!meterEnMochila(objeto)) {
                return ItemStack.EMPTY;
            }
        }

        if (objeto.isEmpty()) {
            hueco.setByPlayer(ItemStack.EMPTY);
        } else {
            hueco.setChanged();
        }
        return copia;
    }

    /** Mete un montón en la mochila respetando su límite de 100/200 (Minecraft solo llenaría hasta 64). */
    private boolean meterEnMochila(ItemStack objeto) {
        boolean movido = false;
        // Primero, completar huecos con el mismo objeto
        for (int i = 0; i < huecos && !objeto.isEmpty(); i++) {
            Slot hueco = slots.get(i);
            ItemStack dentro = hueco.getItem();
            if (!dentro.isEmpty() && hueco.mayPlace(objeto) && ItemStack.isSameItemSameTags(dentro, objeto)) {
                int cabe = hueco.getMaxStackSize(dentro) - dentro.getCount();
                if (cabe > 0) {
                    int cantidad = Math.min(cabe, objeto.getCount());
                    dentro.grow(cantidad);
                    objeto.shrink(cantidad);
                    hueco.setChanged();
                    movido = true;
                }
            }
        }
        // Luego, huecos vacíos
        for (int i = 0; i < huecos && !objeto.isEmpty(); i++) {
            Slot hueco = slots.get(i);
            if (!hueco.hasItem() && hueco.mayPlace(objeto)) {
                hueco.setByPlayer(objeto.split(Math.min(hueco.getMaxStackSize(objeto), objeto.getCount())));
                movido = true;
            }
        }
        return movido;
    }

    // ---------- MONTONES DE MÁS DE 127 ----------

    @Override
    public void setSynchronizer(ContainerSynchronizer original) {
        if (!(jugador instanceof ServerPlayer servidor)) {
            super.setSynchronizer(original);
            return;
        }
        super.setSynchronizer(new ContainerSynchronizer() {
            @Override
            public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> objetos, ItemStack cursor, int[] datos) {
                NonNullList<ItemStack> seguros = NonNullList.withSize(objetos.size(), ItemStack.EMPTY);
                for (int i = 0; i < objetos.size(); i++) {
                    ItemStack objeto = objetos.get(i);
                    seguros.set(i, objeto.getCount() > LIMITE_PAQUETE_VANILLA ? objeto.copyWithCount(1) : objeto);
                }
                original.sendInitialData(menu, seguros, cursor, datos);
                for (int i = 0; i < objetos.size(); i++) {
                    if (objetos.get(i).getCount() > LIMITE_PAQUETE_VANILLA) {
                        enviarHueco(servidor, i, objetos.get(i));
                    }
                }
            }

            @Override
            public void sendSlotChange(AbstractContainerMenu menu, int indice, ItemStack objeto) {
                if (objeto.getCount() > LIMITE_PAQUETE_VANILLA) {
                    enviarHueco(servidor, indice, objeto);
                } else {
                    original.sendSlotChange(menu, indice, objeto);
                }
            }

            @Override
            public void sendCarriedChange(AbstractContainerMenu menu, ItemStack objeto) {
                original.sendCarriedChange(menu, objeto);
            }

            @Override
            public void sendDataChange(AbstractContainerMenu menu, int indice, int valor) {
                original.sendDataChange(menu, indice, valor);
            }
        });
    }

    private void enviarHueco(ServerPlayer servidor, int indice, ItemStack objeto) {
        RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> servidor),
                new HuecoMochilaPacket(this.containerId, indice, objeto.copyWithCount(1), objeto.getCount()));
    }

    /** El hueco de la barra rápida donde está la mochila abierta: no se puede tocar. */
    private static class HuecoBloqueado extends Slot {
        HuecoBloqueado(Inventory inventario, int indice, int x, int y) {
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
