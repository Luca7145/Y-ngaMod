package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> clientes: "el jugador con este id lleva esto en la cara" (para dibujarlo). */
public record CaraPacket(int idEntidad, ItemStack objeto) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(idEntidad);
        buffer.writeItem(objeto);
    }

    public static CaraPacket decodificar(FriendlyByteBuf buffer) {
        return new CaraPacket(buffer.readVarInt(), buffer.readItem());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        CaraCliente.fijar(idEntidad, objeto);
    }
}
