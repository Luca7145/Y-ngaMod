package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: quiere ver su árbol familiar (tecla J). */
public record PedirFamiliaPacket() {

    public void codificar(FriendlyByteBuf b) {
    }

    public static PedirFamiliaPacket decodificar(FriendlyByteBuf b) {
        return new PedirFamiliaPacket();
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        var jugador = contexto.get().getSender();
        if (jugador != null) com.lucarita.yonga.modulos.habitantes.familia.Arbol.enviar(jugador);
    }
}
