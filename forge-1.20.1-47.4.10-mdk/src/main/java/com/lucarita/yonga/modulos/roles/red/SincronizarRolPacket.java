package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolCliente;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "tu oficio es este (id vacío = ninguno), con esta experiencia". Puede pedir abrir el menú. */
public record SincronizarRolPacket(String idRol, int experiencia, boolean abrirMenu) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeUtf(idRol);
        buffer.writeVarInt(experiencia);
        buffer.writeBoolean(abrirMenu);
    }

    public static SincronizarRolPacket decodificar(FriendlyByteBuf buffer) {
        return new SincronizarRolPacket(buffer.readUtf(64), buffer.readVarInt(), buffer.readBoolean());
    }

    // Se ejecuta en el cliente
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        RolCliente.actualizar(Rol.porId(idRol), experiencia, abrirMenu);
    }
}
