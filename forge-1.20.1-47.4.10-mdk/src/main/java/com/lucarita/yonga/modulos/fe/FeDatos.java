package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.roles.oficios.Sacerdote;
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
 * Para fe que se gana o pierde por una ACCIÓN del jugador (orbes, rezar...) usa ganarFe: aplica bonificaciones
 * como la del Sacerdote. sumarPuntos/fijarPuntos cambian la fe tal cual (los usan los comandos).
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

    /** Nivel máximo que puede alcanzar el jugador: 100, o 101 si es Sacerdote Maestro. */
    public static int nivelMaximo(Player jugador) {
        return Sacerdote.nivelMaximoFe(jugador);
    }

    /**
     * Vuelve a recortar la fe al máximo del jugador. Se llama cuando cambia su oficio o su rango:
     * p. ej. un Sacerdote Maestro en el nivel 101 que deja de ser sacerdote vuelve al 100.
     */
    public static void ajustarAlLimite(ServerPlayer jugador) {
        fijarPuntos(jugador, obtenerPuntos(jugador));
    }

    /** Pone la fe a un valor exacto (se recorta al nivel máximo del jugador: ±100, o ±101 el Sacerdote Maestro). */
    public static void fijarPuntos(ServerPlayer jugador, double puntos) {
        fijarPuntos(jugador, puntos, true);
    }

    /**
     * Igual que el anterior, pero puedes elegir no avisar al cliente todavía
     * (lo usa la oración, que cambia los puntos cada tick y solo sincroniza de vez en cuando).
     */
    public static void fijarPuntos(ServerPlayer jugador, double puntos, boolean sincronizarAhora) {
        RangoFe rangoAntes = obtenerRango(jugador);

        CompoundTag persistentes = datosPersistentes(jugador);
        persistentes.putDouble(CLAVE, FeNiveles.limitar(puntos, nivelMaximo(jugador)));
        jugador.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistentes);

        // Si ha cambiado de rango, el maná se ajusta (tipo y máximo dependen del rango)
        ManaDatos.alCambiarRango(jugador, rangoAntes, obtenerRango(jugador));

        if (sincronizarAhora) {
            sincronizar(jugador);
        }
    }

    /**
     * Fe ganada (o perdida, si es negativa) por una acción del jugador. Aplica el multiplicador de su oficio
     * (p. ej. un Sacerdote Experto gana y pierde el doble). Usa esto para cualquier forma nueva de ganar fe.
     */
    public static void ganarFe(ServerPlayer jugador, double cantidad) {
        sumarPuntos(jugador, cantidad * multiplicador(jugador));
    }

    /** Multiplicador de fe del jugador por su oficio (1.0 si no tiene ninguno que lo cambie). */
    public static double multiplicador(Player jugador) {
        return Sacerdote.multiplicadorFe(jugador);
    }

    /** Suma puntos tal cual, sin bonificaciones (usa un número negativo para restar). */
    public static void sumarPuntos(ServerPlayer jugador, double cantidad) {
        fijarPuntos(jugador, obtenerPuntos(jugador) + cantidad);
    }

    /** Envía la fe y el maná del jugador a su cliente (para las barras y el menú). */
    public static void sincronizar(ServerPlayer jugador) {
        FeRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                new SincronizarFePacket(obtenerPuntos(jugador), ManaDatos.obtener(jugador)));
    }

    private static CompoundTag datosPersistentes(Player jugador) {
        // getCompound devuelve una etiqueta vacía (sin guardar) si aún no existe; por eso en fijarPuntos la volvemos a poner
        return jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }
}
