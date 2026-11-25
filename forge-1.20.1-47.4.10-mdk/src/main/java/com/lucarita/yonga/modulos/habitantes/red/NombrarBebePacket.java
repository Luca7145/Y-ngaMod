package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> el padre o la madre que le pone nombre: ha nacido tu bebé, ¿cómo se va a llamar? */
public record NombrarBebePacket(int idBebe, boolean mujer, String nombre) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idBebe);
        b.writeBoolean(mujer);
        b.writeUtf(nombre, 64);
    }

    public static NombrarBebePacket decodificar(FriendlyByteBuf b) {
        return new NombrarBebePacket(b.readVarInt(), b.readBoolean(), b.readUtf(64));
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lucarita.yonga.client.habitantes.NombrarBebeScreen.pedir(idBebe, mujer, nombre));
    }
}
