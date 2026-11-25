package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> quien ve al habitante: un bocadillo sobre su cabeza durante unos ticks ("…" mientras piensa). */
public record BurbujaPacket(int idHabitante, Component texto, int ticks) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeComponent(texto);
        b.writeVarInt(ticks);
    }

    public static BurbujaPacket decodificar(FriendlyByteBuf b) {
        return new BurbujaPacket(b.readVarInt(), b.readComponent(), b.readVarInt());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.Burbujas.recibir(this));
    }
}
