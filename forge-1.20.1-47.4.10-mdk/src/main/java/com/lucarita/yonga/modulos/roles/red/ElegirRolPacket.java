package com.lucarita.yonga.modulos.roles.red;

import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "quiero este oficio". */
public record ElegirRolPacket(String idRol) {

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeUtf(idRol);
    }

    public static ElegirRolPacket decodificar(FriendlyByteBuf buffer) {
        return new ElegirRolPacket(buffer.readUtf(64));
    }

    // Se ejecuta en el servidor. Nunca te fíes del cliente: comprobamos que el rol existe.
    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        ServerPlayer jugador = contexto.get().getSender();
        Rol rol = Rol.porId(idRol);
        if (jugador == null || rol == null || rol == RolDatos.obtenerRol(jugador)) {
            return;
        }
        RolDatos.cambiarRol(jugador, rol);
        jugador.displayClientMessage(Component.translatable("yonga.rol.elegido", rol.nombre()), false);
    }
}
