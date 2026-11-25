package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador: abre la pantalla de conversación con un habitante, con cómo se llama para ese jugador (su
 * nombre o "La herrera"), si tiene tienda, lo último que se dijeron y la relación para los botones de amor
 * (−1 sin botones: niño, familia o romance apagado; 0 aún nada; 1 novios; 2 casados).
 */
public record AbrirConversacionPacket(int idHabitante, Component titulo, boolean tienda, List<Component> lineas, int relacion,
                                      boolean encargos, int seguir) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeComponent(titulo);
        b.writeBoolean(tienda);
        b.writeVarInt(lineas.size());
        for (Component l : lineas) b.writeComponent(l);
        b.writeByte(relacion);
        b.writeBoolean(encargos);
        b.writeByte(seguir);
    }

    public static AbrirConversacionPacket decodificar(FriendlyByteBuf b) {
        int id = b.readVarInt();
        Component titulo = b.readComponent();
        boolean tienda = b.readBoolean();
        int n = Math.min(b.readVarInt(), 64);
        List<Component> lineas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) lineas.add(b.readComponent());
        int relacion = b.readByte();
        boolean encargos = b.readBoolean();
        int seguir = b.readByte();
        return new AbrirConversacionPacket(id, titulo, tienda, lineas, relacion, encargos, seguir);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.ConversacionScreen.abrir(this));
    }
}
