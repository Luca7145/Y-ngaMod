package com.lucarita.yonga.modulos.fe.red;

import com.lucarita.yonga.modulos.fe.OracionCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes: "el jugador con este id empieza a rezar (1), blasfemar (-1) o para (0)". Sirve para la animación. */
public record EstadoOracionPacket(int idEntidad, int direccion) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(idEntidad);
        buffer.writeByte(direccion);
    }

    public static EstadoOracionPacket decodificar(FriendlyByteBuf buffer) {
        return new EstadoOracionPacket(buffer.readVarInt(), buffer.readByte());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        OracionCliente.fijar(idEntidad, direccion);
    }
}
