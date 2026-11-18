package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.modulos.roles.oficios.Musica;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "silencia / vuelve a activar las notas de mis instrumentos". */
public record SilenciarNotasPacket() {

    public void codificar(FriendlyByteBuf buffer) {
    }

    public static SilenciarNotasPacket decodificar(FriendlyByteBuf buffer) {
        return new SilenciarNotasPacket();
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        if (jugador != null) {
            Musica.alternarSilencio(jugador);
        }
    }
}
