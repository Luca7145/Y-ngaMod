package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Servidor -> jugador: las líneas de la crónica de la aldea (libro de la aldea). */
public record CronicaPacket(List<Component> lineas) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(lineas.size());
        for (Component c : lineas) b.writeComponent(c);
    }

    public static CronicaPacket decodificar(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 4000);
        List<Component> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) lista.add(b.readComponent());
        return new CronicaPacket(lista);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.CronicaScreen.abrir(this));
    }
}
