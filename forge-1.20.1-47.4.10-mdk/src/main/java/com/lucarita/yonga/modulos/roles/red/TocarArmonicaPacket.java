package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.modulos.roles.oficios.Armonica;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "he pulsado la tecla de la armónica". El servidor comprueba que la lleva puesta. */
public record TocarArmonicaPacket() {

    public void codificar(FriendlyByteBuf buffer) {
    }

    public static TocarArmonicaPacket decodificar(FriendlyByteBuf buffer) {
        return new TocarArmonicaPacket();
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        if (jugador != null) {
            Armonica.tocar(jugador);
        }
    }
}
