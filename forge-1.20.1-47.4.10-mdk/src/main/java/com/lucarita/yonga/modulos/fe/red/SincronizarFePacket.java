package com.lucarita.yonga.modulos.fe.red;

import com.lucarita.yonga.modulos.fe.FeCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "tienes estos puntos de fe y este maná". */
public record SincronizarFePacket(double puntos, double mana) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeDouble(puntos);
        buffer.writeDouble(mana);
    }

    public static SincronizarFePacket decodificar(FriendlyByteBuf buffer) {
        return new SincronizarFePacket(buffer.readDouble(), buffer.readDouble());
    }

    // Se ejecuta en el cliente, en el hilo principal del juego
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        FeCliente.actualizar(puntos, mana);
    }
}
