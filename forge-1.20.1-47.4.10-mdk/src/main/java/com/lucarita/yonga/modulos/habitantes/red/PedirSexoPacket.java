package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador que aún no lo ha dicho: que elija si su personaje es hombre o mujer. */
public record PedirSexoPacket() {

    public void codificar(FriendlyByteBuf b) {
    }

    public static PedirSexoPacket decodificar(FriendlyByteBuf b) {
        return new PedirSexoPacket();
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.SexoScreen.pedir());
    }
}
