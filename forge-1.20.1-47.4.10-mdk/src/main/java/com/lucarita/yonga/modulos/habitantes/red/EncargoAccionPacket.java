package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.taberna.Taberna;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: aceptar (caza) o entregar (traer) un encargo del tablón de una aldea. */
public record EncargoAccionPacket(BlockPos campana, int id, int accion) {

    public void codificar(FriendlyByteBuf b) {
        b.writeBlockPos(campana);
        b.writeVarInt(id);
        b.writeByte(accion);
    }

    public static EncargoAccionPacket decodificar(FriendlyByteBuf b) {
        return new EncargoAccionPacket(b.readBlockPos(), b.readVarInt(), b.readByte());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        var jugador = contexto.get().getSender();
        if (jugador != null) Taberna.accion(jugador, campana, id, accion);
    }
}
