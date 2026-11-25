package com.lucarita.yonga.modulos.habitantes.red;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Jugador -> servidor: los cambios del libro del creador. Solo vale en creativo y con el habitante cerca. */
public record GuardarEditorPacket(int idHabitante, CompoundTag datos) {

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeNbt(datos);
    }

    public static GuardarEditorPacket decodificar(FriendlyByteBuf b) {
        int id = b.readVarInt();
        CompoundTag t = b.readNbt();
        return new GuardarEditorPacket(id, t == null ? new CompoundTag() : t);
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        var jugador = contexto.get().getSender();
        if (jugador == null || !jugador.isCreative()) return;
        if (jugador.level().getEntity(idHabitante) instanceof Habitante h && h.isAlive() && h.distanceToSqr(jugador) < 32 * 32) {
            h.aplicarEditor(jugador, datos);
        }
    }
}
