package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> quien lo ve: esa entidad está acostada encima de otra (se dibuja un poco más arriba). */
public record EncimaPacket(int idEntidad, boolean encima) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idEntidad);
        b.writeBoolean(encima);
    }

    public static EncimaPacket decodificar(FriendlyByteBuf b) {
        return new EncimaPacket(b.readVarInt(), b.readBoolean());
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.Encima.recibir(this));
    }
}
