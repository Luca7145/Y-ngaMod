package com.lucarita.yonga.modulos.habitantes;

import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.TiendaEstadoPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * La tienda de un habitante. Los 9 huecos de venta no son huecos de verdad: el servidor manda su
 * estado (TiendaEstadoPacket) y la pantalla manda clics (TiendaAccionPacket). Abajo va el inventario del jugador,
 * para ver sus monedas. Mientras está abierta el habitante se queda quieto mirando al jugador.
 */
public class TiendaMenu extends AbstractContainerMenu {

    public static final int INVENTARIO_X = 18, INVENTARIO_Y = 82;

    public final int idHabitante;
    @Nullable private final Habitante habitante;     // solo en el servidor
    @Nullable public TiendaEstadoPacket estado;       // solo en el cliente
    private final Player jugador;
    private int contador;

    public TiendaMenu(int id, Inventory inventario, Habitante habitante) {
        this(id, inventario, habitante.getId(), habitante);
    }

    private TiendaMenu(int id, Inventory inventario, int idHabitante, @Nullable Habitante habitante) {
        super(ModuloHabitantes.MENU_TIENDA.get(), id);
        this.idHabitante = idHabitante;
        this.habitante = habitante;
        this.jugador = inventario.player;
        for (int fila = 0; fila < 3; fila++) {
            for (int columna = 0; columna < 9; columna++) {
                addSlot(new Slot(inventario, columna + fila * 9 + 9, INVENTARIO_X + columna * 18, INVENTARIO_Y + fila * 18));
            }
        }
        for (int columna = 0; columna < 9; columna++) {
            addSlot(new Slot(inventario, columna, INVENTARIO_X + columna * 18, INVENTARIO_Y + 58));
        }
        if (habitante != null) {
            habitante.atender(inventario.player);
        }
    }

    public static TiendaMenu desdeRed(int id, Inventory inventario, FriendlyByteBuf datos) {
        return new TiendaMenu(id, inventario, datos.readVarInt(), null);
    }

    @Nullable
    public Habitante habitante() {
        return habitante;
    }

    public void enviarEstado(ServerPlayer jugador) {
        if (habitante != null) {
            HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), habitante.estadoTienda(jugador));
        }
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (habitante != null && contador++ % 20 == 0 && this.jugador instanceof ServerPlayer servidor) {
            enviarEstado(servidor);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player jugador, int hueco) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player jugador) {
        return habitante == null || habitante.isAlive() && habitante.distanceToSqr(jugador) < 64 && habitante.cliente() == jugador;
    }

    @Override
    public void removed(Player jugador) {
        super.removed(jugador);
        if (habitante != null && habitante.cliente() == jugador) {
            habitante.atender(null);
        }
    }
}
