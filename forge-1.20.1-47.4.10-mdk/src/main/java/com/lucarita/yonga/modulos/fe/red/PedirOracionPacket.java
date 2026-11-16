package com.lucarita.yonga.modulos.fe.red;

import com.lucarita.yonga.modulos.fe.Oracion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "he pulsado rezar (1) / blasfemar (-1) / parar (0)". */
public record PedirOracionPacket(int direccion) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeByte(direccion);
    }

    public static PedirOracionPacket decodificar(FriendlyByteBuf buffer) {
        // Nunca te fíes de lo que manda un cliente: lo dejamos entre -1 y 1
        return new PedirOracionPacket(Mth.clamp(buffer.readByte(), -1, 1));
    }

    // Se ejecuta en el servidor
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        if (jugador != null) {
            Oracion.alternar(jugador, direccion);
        }
    }
}
