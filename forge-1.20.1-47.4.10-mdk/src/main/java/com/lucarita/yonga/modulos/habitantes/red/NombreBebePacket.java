package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.familia.Amor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: el nombre que le ha puesto a su bebé. */
public record NombreBebePacket(int idBebe, String nombre) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idBebe);
        b.writeUtf(nombre, 64);
    }

    public static NombreBebePacket decodificar(FriendlyByteBuf b) {
        return new NombreBebePacket(b.readVarInt(), b.readUtf(64));
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        var jugador = contexto.get().getSender();
        if (jugador != null) Amor.nombrarBebe(jugador, idBebe, nombre);
    }
}
