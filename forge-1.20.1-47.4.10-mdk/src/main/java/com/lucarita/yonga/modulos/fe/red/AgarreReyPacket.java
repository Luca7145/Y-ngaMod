package com.lucarita.yonga.modulos.fe.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Servidor -> clientes que ven al Rey de Arenisca: dónde está agarrado un jugador (superficie y punto).
 * Superficie -1 = ya no está agarrado. Con esto el cliente coloca al jugador sobre el rey sin tirones.
 */
public record AgarreReyPacket(int rey, int jugador, int superficie, float u, float v) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(rey);
        buffer.writeVarInt(jugador);
        buffer.writeVarInt(superficie + 1);
        buffer.writeFloat(u);
        buffer.writeFloat(v);
    }

    public static AgarreReyPacket decodificar(FriendlyByteBuf buffer) {
        return new AgarreReyPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt() - 1,
                buffer.readFloat(), buffer.readFloat());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.fe.ClienteRey.recibir(this));
    }
}
