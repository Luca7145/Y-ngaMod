package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador en creativo: abre el libro del creador con todo lo de ese habitante. */
public record EditorPacket(int idHabitante, CompoundTag datos) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeNbt(datos);
    }

    public static EditorPacket decodificar(FriendlyByteBuf b) {
        int id = b.readVarInt();
        CompoundTag t = b.readNbt();
        return new EditorPacket(id, t == null ? new CompoundTag() : t);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.EditorScreen.abrir(this));
    }
}
