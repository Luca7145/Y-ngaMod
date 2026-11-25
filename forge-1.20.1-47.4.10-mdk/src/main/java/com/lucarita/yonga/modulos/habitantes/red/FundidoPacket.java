package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: la pantalla funde a negro unos ticks (intimidad: nunca se ve nada más). */
public record FundidoPacket(int ticks) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(ticks);
    }

    public static FundidoPacket decodificar(FriendlyByteBuf b) {
        return new FundidoPacket(b.readVarInt());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.Fundido.empezar(this.ticks));
    }
}
