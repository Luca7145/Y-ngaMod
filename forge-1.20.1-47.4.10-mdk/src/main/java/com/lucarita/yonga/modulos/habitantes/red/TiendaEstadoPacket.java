package com.lucarita.yonga.modulos.habitantes.red;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Servidor -> jugador con la tienda abierta: todo lo que enseña la pantalla. Se manda al abrir, tras cada compra
 * y cada segundo (para la barra de trabajo y los precios pactados hablando).
 *
 * @param ofertas   9 líneas (lote con la cantidad y el precio para ESTE jugador, trato 1 mejor / −1 peor / 0 normal,
 *                  lotes que quedan); vacías = ItemStack.EMPTY
 */
public record TiendaEstadoPacket(int idHabitante, int oficio, int nivel, int xp, int xpDesde, int xpHasta,
                                 boolean puedeSubir, float barra, List<Linea> ofertas) {

    public record Linea(ItemStack stack, int precio, int trato, int cantidad) {}

    public void codificar(FriendlyByteBuf b) {
        b.writeVarInt(idHabitante);
        b.writeVarInt(oficio);
        b.writeVarInt(nivel);
        b.writeVarInt(xp);
        b.writeVarInt(xpDesde);
        b.writeVarInt(xpHasta);
        b.writeBoolean(puedeSubir);
        b.writeFloat(barra);
        escribir(b, ofertas);
    }

    private static void escribir(FriendlyByteBuf b, List<Linea> lineas) {
        b.writeVarInt(lineas.size());
        for (Linea l : lineas) {
            b.writeItem(l.stack());
            b.writeVarInt(l.precio());
            b.writeVarInt(l.trato());
            b.writeVarInt(l.cantidad());
        }
    }

    private static List<Linea> leer(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 16);
        List<Linea> lineas = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            lineas.add(new Linea(b.readItem(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
        }
        return lineas;
    }

    public static TiendaEstadoPacket decodificar(FriendlyByteBuf b) {
        return new TiendaEstadoPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(),
                b.readVarInt(), b.readBoolean(), b.readFloat(), leer(b));
    }

    public void manejar(Supplier<NetworkEvent.Context> contexto) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lucarita.yonga.client.habitantes.TiendaScreen.recibir(this));
    }
}
