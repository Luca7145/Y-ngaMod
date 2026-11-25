package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.familia.Amor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: el sexo que ha elegido para su personaje (1 hombre, 2 mujer). */
public record SexoPacket(int sexo) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(sexo);
    }

    public static SexoPacket decodificar(FriendlyByteBuf b) {
        return new SexoPacket(b.readVarInt());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        var jugador = contexto.get().getSender();
        if (jugador != null) Amor.elegirSexo(jugador, sexo);
    }
}
