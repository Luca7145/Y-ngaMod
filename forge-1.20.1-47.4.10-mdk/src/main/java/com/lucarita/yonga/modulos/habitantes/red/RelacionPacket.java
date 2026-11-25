package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: ha cambiado la relación con el habitante de la conversación (cambian los botones). */
public record RelacionPacket(int idHabitante, int relacion, int seguir) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeByte(relacion);
        b.writeByte(seguir);
    }

    public static RelacionPacket decodificar(FriendlyByteBuf b) {
        return new RelacionPacket(b.readVarInt(), b.readByte(), b.readByte());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.ConversacionScreen.recibir(this));
    }
}
