package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.fe.red.SincronizarFePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Dónde se guarda la fe de cada jugador (en el servidor).
 *
 * Se guarda en los datos "PlayerPersisted" del jugador, que Forge conserva al morir y al volver del End.
 * Usa SIEMPRE estos métodos para cambiar la fe: así el jugador recibe el valor nuevo en su pantalla.
 * (Más adelante, cuando hagamos las formas de ganar o perder fe, solo habrá que llamar a sumarPuntos.)
 */
public final class FeDatos {

    private static final String CLAVE = "yonga_fe";

    private FeDatos() {}

    public static double obtenerPuntos(Player jugador) {
        return datosPersistentes(jugador).getDouble(CLAVE);
    }

    public static int obtenerNivel(Player jugador) {
        return FeNiveles.nivel(obtenerPuntos(jugador));
    }

    public static RangoFe obtenerRango(Player jugador) {
        return RangoFe.deNivel(obtenerNivel(jugador));
    }

    /** Pone la fe a un valor exacto (se recorta entre los puntos del nivel -100 y el 100). */
    public static void fijarPuntos(ServerPlayer jugador, double puntos) {
        fijarPuntos(jugador, puntos, true);
    }

    /**
     * Igual que el anterior, pero puedes elegir no avisar al cliente todavía
     * (lo usa la oración, que cambia los puntos cada tick y solo sincroniza de vez en cuando).
     */
    public static void fijarPuntos(ServerPlayer jugador, double puntos, boolean sincronizarAhora) {
        CompoundTag persistentes = datosPersistentes(jugador);
        persistentes.putDouble(CLAVE, FeNiveles.limitar(puntos));
        jugador.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistentes);
        if (sincronizarAhora) {
            sincronizar(jugador);
        }
    }

    /** Suma puntos (usa un número negativo para restar). */
    public static void sumarPuntos(ServerPlayer jugador, double cantidad) {
        fijarPuntos(jugador, obtenerPuntos(jugador) + cantidad);
    }

    /** Envía la fe del jugador a su cliente (para la barra y el menú). */
    public static void sincronizar(ServerPlayer jugador) {
        FeRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), new SincronizarFePacket(obtenerPuntos(jugador)));
    }

    private static CompoundTag datosPersistentes(Player jugador) {
        // getCompound devuelve una etiqueta vacía (sin guardar) si aún no existe; por eso en fijarPuntos la volvemos a poner
        return jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }
}
