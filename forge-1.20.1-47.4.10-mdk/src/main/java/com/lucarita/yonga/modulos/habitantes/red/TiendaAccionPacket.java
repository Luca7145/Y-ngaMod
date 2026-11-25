package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.TiendaMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Jugador -> servidor desde la tienda: comprar el lote de un hueco (el regateo se hace hablando).
 * Con "todo" (mayúsculas pulsadas) compra todos los lotes que se puedan pagar de ese hueco.
 */
public record TiendaAccionPacket(int accion, int indice, boolean todo) {

    public static final int COMPRAR = 0;

    public void codificar(FriendlyByteBuf b) {
        b.writeByte(accion);
        b.writeByte(indice);
        b.writeBoolean(todo);
    }

    public static TiendaAccionPacket decodificar(FriendlyByteBuf b) {
        return new TiendaAccionPacket(b.readByte(), b.readByte(), b.readBoolean());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        if (jugador != null && jugador.containerMenu instanceof TiendaMenu menu) {
            Habitante habitante = menu.habitante();
            if (habitante != null && menu.stillValid(jugador)) {
                habitante.accionTienda(jugador, accion, indice, todo);
                menu.enviarEstado(jugador);
            }
        }
        contexto.get().setPacketHandled(true);
    }
}
