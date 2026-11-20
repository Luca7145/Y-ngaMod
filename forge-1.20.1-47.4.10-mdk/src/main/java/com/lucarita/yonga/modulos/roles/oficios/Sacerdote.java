package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.FeNiveles;
import com.lucarita.yonga.modulos.fe.hechizos.Hechizo;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Habilidades del oficio Sacerdote.
 *
 *  - Multiplicador de fe: gana Y pierde más fe con sus acciones (orbes, rezar, blasfemar...).
 *    Los comandos (/fe sumar, /fe fijar...) no se ven afectados.
 *  - Hechizos mejorados (II): a partir de cierto rango sus hechizos son más fuertes.
 *  - Nivel supremo: el Sacerdote Maestro puede llegar al nivel 101 (Alfa) o -101 (Omega).
 *  - Rosario (lo fabrica desde Novato, va en la cara): quien lo lleve reza y blasfema más rápido.
 *
 * Todo vale tanto en el servidor (con RolDatos) como en el cliente (con RolCliente), por eso
 * cada cosa tiene una versión que recibe el rol y el rango directamente.
 *
 * (Cada oficio tendrá su clase en este paquete: Herrero, Explorador...)
 */
public final class Sacerdote {

    // Multiplicador de fe según el rango del sacerdote
    private static final Map<RangoRol, Double> MULTIPLICADOR_FE = new EnumMap<>(Map.of(
            RangoRol.NOVATO, 1.0,
            RangoRol.APRENDIZ, 1.5,
            RangoRol.OFICIAL, 1.5,
            RangoRol.EXPERTO, 2.0,
            RangoRol.MAESTRO, 2.0
    ));

    // Rango de sacerdote desde el que cada hechizo pasa a ser su versión II
    private static final Map<Hechizo, RangoRol> RANGO_HECHIZO_MEJORADO = new EnumMap<>(Map.of(
            Hechizo.GRAN_SALTO, RangoRol.OFICIAL,
            Hechizo.EXPULSION, RangoRol.OFICIAL,
            Hechizo.CASTIGO_DEL_CIELO, RangoRol.MAESTRO,
            Hechizo.PROYECTIL_OSCURO, RangoRol.MAESTRO
    ));

    // Rango de sacerdote que desbloquea el nivel 101 / -101
    private static final RangoRol RANGO_NIVEL_SUPREMO = RangoRol.MAESTRO;

    // Cuánto más rápido se reza (o blasfema) con el rosario puesto
    private static final double ORACION_ROSARIO = 1.5;

    private Sacerdote() {}

    // ---------- ROSARIO ----------

    /** Multiplicador de lo que se gana al rezar o blasfemar: más con el rosario en la cara (lo lleve quien lo lleve). */
    public static double multiplicadorOracion(Player jugador) {
        return Modulo.ROLES.activo() && Cara.obtener(jugador).is(ModuloRoles.ROSARIO.get()) ? ORACION_ROSARIO : 1.0;
    }

    // ---------- MULTIPLICADOR DE FE ----------

    public static double multiplicadorFe(Rol rol, RangoRol rango) {
        if (!esSacerdote(rol, rango)) {
            return 1.0;
        }
        return MULTIPLICADOR_FE.getOrDefault(rango, 1.0);
    }

    public static double multiplicadorFe(Player jugador) {
        return Modulo.ROLES.activo() ? multiplicadorFe(RolDatos.obtenerRol(jugador), RolDatos.obtenerRango(jugador)) : 1.0;
    }

    // ---------- HECHIZOS MEJORADOS ----------

    public static boolean hechizoMejorado(Rol rol, RangoRol rango, Hechizo hechizo) {
        RangoRol necesario = RANGO_HECHIZO_MEJORADO.get(hechizo);
        return esSacerdote(rol, rango) && necesario != null && rango.ordinal() >= necesario.ordinal();
    }

    public static boolean hechizoMejorado(Player jugador, Hechizo hechizo) {
        return Modulo.ROLES.activo()
                && hechizoMejorado(RolDatos.obtenerRol(jugador), RolDatos.obtenerRango(jugador), hechizo);
    }

    // ---------- NIVEL SUPREMO (101 / -101) ----------

    /** Nivel de fe máximo para alguien con ese oficio y rango: 101 el Sacerdote Maestro, 100 el resto. */
    public static int nivelMaximoFe(Rol rol, RangoRol rango) {
        if (esSacerdote(rol, rango) && rango.ordinal() >= RANGO_NIVEL_SUPREMO.ordinal()) {
            return FeNiveles.NIVEL_SUPREMO;
        }
        return FeNiveles.NIVEL_MAXIMO;
    }

    public static int nivelMaximoFe(Player jugador) {
        return Modulo.ROLES.activo()
                ? nivelMaximoFe(RolDatos.obtenerRol(jugador), RolDatos.obtenerRango(jugador))
                : FeNiveles.NIVEL_MAXIMO;
    }

    private static boolean esSacerdote(Rol rol, RangoRol rango) {
        return rol == Rol.SACERDOTE && rango != null;
    }
}
