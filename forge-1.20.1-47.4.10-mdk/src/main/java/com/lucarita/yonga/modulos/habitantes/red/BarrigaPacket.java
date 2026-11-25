package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> quien ve a una jugadora embarazada: lo avanzado del embarazo (0..1), para dibujarle la barriga. */
public record BarrigaPacket(int idJugador, float barriga) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idJugador);
        b.writeFloat(barriga);
    }

    public static BarrigaPacket decodificar(FriendlyByteBuf b) {
        return new BarrigaPacket(b.readVarInt(), b.readFloat());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.CapaBarriga.recibir(this));
    }
}
