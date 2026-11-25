package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> jugador que conversa: una línea nueva para su pantalla de conversación (respuesta o aviso).
 * Con cerrar = true, además, la conversación se acaba (le ha echado o le han atacado) y la pantalla se cierra.
 */
public record LineaConversacionPacket(int idHabitante, Component linea, boolean cerrar) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeComponent(linea);
        b.writeBoolean(cerrar);
    }

    public static LineaConversacionPacket decodificar(FriendlyByteBuf b) {
        return new LineaConversacionPacket(b.readVarInt(), b.readComponent(), b.readBoolean());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.ConversacionScreen.recibir(this));
    }
}
