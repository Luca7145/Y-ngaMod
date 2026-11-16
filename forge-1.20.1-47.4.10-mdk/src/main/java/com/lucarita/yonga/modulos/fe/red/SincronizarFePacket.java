package com.lucarita.yonga.modulos.fe.red;

import com.lucarita.yonga.modulos.fe.FeCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "tienes estos puntos de fe". */
public record SincronizarFePacket(double puntos) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeDouble(puntos);
    }

    public static SincronizarFePacket decodificar(FriendlyByteBuf buffer) {
        return new SincronizarFePacket(buffer.readDouble());
    }

    // Se ejecuta en el cliente, en el hilo principal del juego
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        FeCliente.fijarPuntos(puntos);
    }
}
