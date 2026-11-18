package com.lucarita.yonga.modulos.fe.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador agarrado por el Astado: "¡susto!". El cliente enseña la cara a pantalla completa. */
public record JumpscarePacket() {

    public void codificar(FriendlyByteBuf buffer) {
    }

    public static JumpscarePacket decodificar(FriendlyByteBuf buffer) {
        return new JumpscarePacket();
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.fe.SustoAstado.lanzar());
    }
}
