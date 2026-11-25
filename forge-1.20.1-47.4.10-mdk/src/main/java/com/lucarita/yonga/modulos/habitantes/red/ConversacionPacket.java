package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Jugador -> servidor: abrir la conversación con un habitante (tecla de hablar), decirle algo, pasar a su tienda o
 * cerrar la conversación.
 */
public record ConversacionPacket(int idHabitante, int tipo, String texto) {

    public static final int ABRIR = 0, HABLAR = 1, TIENDA = 2, CERRAR = 3, REGALAR = 4, DECLARARSE = 5, NOCHE = 6,
            ENCARGOS = 7, SEGUIR = 8, DEJAR = 9, QUEDARSE = 10, LIBRE = 11, ROMPER = 12;

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeByte(tipo);
        b.writeUtf(texto, 1024);
    }

    public static ConversacionPacket decodificar(FriendlyByteBuf b) {
        return new ConversacionPacket(b.readVarInt(), b.readByte(), b.readUtf(1024));
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        if (jugador != null) {
            Conversaciones.alPaquete(jugador, this);
        }
        contexto.get().setPacketHandled(true);
    }
}
