package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.FeDatos;
import com.lucarita.yonga.modulos.roles.oficios.Fantasma;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import com.lucarita.yonga.modulos.roles.red.SincronizarRolPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Dónde se guarda el oficio de cada jugador (en el servidor): su rol y su experiencia de oficio.
 * El rango no se guarda: se calcula a partir de la experiencia (RangoRol.deExperiencia).
 *
 * Igual que la fe, va en los datos "PlayerPersisted" (se conservan al morir).
 * Usa SIEMPRE estos métodos para cambiar el oficio: así el jugador lo ve actualizado en su pantalla.
 * Cuando hagamos las habilidades de cada rol, para dar experiencia solo habrá que llamar a sumarExperiencia.
 */
public final class RolDatos {

    private static final String CLAVE = "yonga_rol";
    private static final String CLAVE_ROL = "rol";
    private static final String CLAVE_EXPERIENCIA = "experiencia";

    private RolDatos() {}

    // ---------- LEER ----------

    /** El rol del jugador, o null si todavía no ha elegido. */
    public static Rol obtenerRol(Player jugador) {
        return Rol.porId(datos(jugador).getString(CLAVE_ROL));
    }

    public static boolean tieneRol(Player jugador) {
        return obtenerRol(jugador) != null;
    }

    public static int obtenerExperiencia(Player jugador) {
        return datos(jugador).getInt(CLAVE_EXPERIENCIA);
    }

    public static RangoRol obtenerRango(Player jugador) {
        return RangoRol.deExperiencia(obtenerExperiencia(jugador));
    }

    // ---------- CAMBIAR ----------

    /**
     * Cambia el oficio del jugador. Pierde el anterior y su rango: empieza como Novato con 0 de experiencia.
     * Si ya tenía ese mismo oficio no hace nada.
     */
    public static void cambiarRol(ServerPlayer jugador, Rol nuevoRol) {
        if (nuevoRol == obtenerRol(jugador)) {
            return;
        }
        CompoundTag datos = datos(jugador);
        datos.putString(CLAVE_ROL, nuevoRol.id());
        datos.putInt(CLAVE_EXPERIENCIA, 0);
        guardar(jugador, datos);
        sincronizar(jugador, false);
        alCambiarOficioORango(jugador);
    }

    /** Deja al jugador sin oficio (y le vuelve a salir el menú para elegir). */
    public static void quitarRol(ServerPlayer jugador) {
        guardar(jugador, new CompoundTag());
        sincronizar(jugador, true);
        alCambiarOficioORango(jugador);
    }

    /** Pone la experiencia de oficio a un valor exacto (nunca negativo). */
    public static void fijarExperiencia(ServerPlayer jugador, int experiencia) {
        if (!tieneRol(jugador)) {
            return;
        }
        RangoRol rangoAntes = obtenerRango(jugador);

        CompoundTag datos = datos(jugador);
        datos.putInt(CLAVE_EXPERIENCIA, Math.max(0, experiencia));
        guardar(jugador, datos);
        sincronizar(jugador, false);

        RangoRol rangoDespues = obtenerRango(jugador);
        if (rangoDespues != rangoAntes) {
            alCambiarOficioORango(jugador);
        }
        if (rangoDespues.ordinal() > rangoAntes.ordinal()) {
            jugador.displayClientMessage(Component.translatable("yonga.rol.sube_rango",
                    rangoDespues.nombre(), obtenerRol(jugador).nombre()), true);
        }
    }

    /** Suma experiencia de oficio (número negativo para restar). */
    public static void sumarExperiencia(ServerPlayer jugador, int cantidad) {
        fijarExperiencia(jugador, obtenerExperiencia(jugador) + cantidad);
    }

    /** Pone al jugador justo al principio de un rango. */
    public static void fijarRango(ServerPlayer jugador, RangoRol rango) {
        fijarExperiencia(jugador, rango.experienciaNecesaria());
    }

    // Cosas de otros módulos que dependen del oficio (p. ej. el nivel 101 del Sacerdote Maestro)
    private static void alCambiarOficioORango(ServerPlayer jugador) {
        if (Modulo.FE.activo()) {
            FeDatos.ajustarAlLimite(jugador);
        }
        if (obtenerRol(jugador) != Rol.FANTASMA) {
            Fantasma.quitarEfectos(jugador);
        }
    }

    // ---------- RED ----------

    /** Manda al cliente su oficio. Si abrirMenu es true, al jugador le sale el menú para elegir oficio. */
    public static void sincronizar(ServerPlayer jugador, boolean abrirMenu) {
        Rol rol = obtenerRol(jugador);
        RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador),
                new SincronizarRolPacket(rol == null ? "" : rol.id(), obtenerExperiencia(jugador), abrirMenu));
    }

    // ---------- INTERNO ----------

    private static CompoundTag datos(Player jugador) {
        return jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(CLAVE);
    }

    private static void guardar(Player jugador, CompoundTag datos) {
        CompoundTag persistentes = jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persistentes.put(CLAVE, datos);
        jugador.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistentes);
    }
}
