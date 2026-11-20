package com.lucarita.yonga.modulos.roles.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador: lo que ha encontrado su escáner en el último barrido (posiciones aproximadas, ya con el
 * error puesto, y cuántos bloques hay en cada zona). Una lista vacía apaga el sonar. Lo maneja
 * client/roles/EscanerCliente.
 */
public record EscanerPacket(List<Mancha> manchas) {

    public record Mancha(int x, int y, int z, int cuantos) {}

    public void codificar(FriendlyByteBuf buffer) {
        buffer.writeVarInt(manchas.size());
        for (Mancha m : manchas) {
            buffer.writeVarInt(m.x());
            buffer.writeVarInt(m.y());
            buffer.writeVarInt(m.z());
            buffer.writeByte(m.cuantos());
        }
    }

    public static EscanerPacket decodificar(FriendlyByteBuf buffer) {
        int n = Math.min(buffer.readVarInt(), 64);
        List<Mancha> manchas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            manchas.add(new Mancha(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readUnsignedByte()));
        }
        return new EscanerPacket(manchas);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.roles.EscanerCliente.recibir(manchas));
    }
}
