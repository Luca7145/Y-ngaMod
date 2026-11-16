package com.lucarita.yonga.modulos.fe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;

/**
 * Maná del jugador (en el servidor). Se guarda junto a la fe, en "PlayerPersisted".
 *
 * El TIPO (maná o maná oscuro) y el MÁXIMO salen del rango de fe (ver RangoFe). Aquí solo se guarda cuánto tiene.
 * Se gasta con los libros de hechizos (ver hechizos/LibroHechizo).
 * Se regenera 1 punto por segundo mientras la barra de comida esté llena.
 *
 * Al cambiar de rango:
 *  - si pasa a no tener maná (Ateo): se queda a 0
 *  - si cambia de tipo (o lo consigue por primera vez): empieza lleno
 *  - si sigue con el mismo tipo y el máximo sube/baja: se suma/resta la diferencia (una barra llena sigue llena)
 */
public final class ManaDatos {

    private static final String CLAVE = "yonga_mana";

    // Regeneración: cuánto y cada cuántos ticks (20 ticks = 1 segundo)
    private static final double REGENERACION = 1.0;
    private static final int TICKS_REGENERACION = 20;
    private static final int COMIDA_LLENA = 20;

    private ManaDatos() {}

    public static double obtener(Player jugador) {
        return persistentes(jugador).getDouble(CLAVE);
    }

    public static RangoFe rango(Player jugador) {
        return RangoFe.dePuntos(FeDatos.obtenerPuntos(jugador));
    }

    public static TipoMana tipo(Player jugador) {
        return rango(jugador).tipoMana();
    }

    public static int maximo(Player jugador) {
        return rango(jugador).manaMaximo();
    }

    /** Pone el maná a un valor (entre 0 y el máximo de su rango) y avisa al cliente. */
    public static void fijar(ServerPlayer jugador, double mana) {
        guardar(jugador, mana);
        FeDatos.sincronizar(jugador);
    }

    public static void sumar(ServerPlayer jugador, double cantidad) {
        fijar(jugador, obtener(jugador) + cantidad);
    }

    /** Intenta gastar maná. Si no hay suficiente no gasta nada y devuelve false. */
    public static boolean gastar(ServerPlayer jugador, double cantidad) {
        double actual = obtener(jugador);
        if (actual < cantidad) {
            return false;
        }
        fijar(jugador, actual - cantidad);
        return true;
    }

    /** Lo llama FeDatos cada vez que cambian los puntos de fe (no sincroniza: ya lo hace FeDatos). */
    static void alCambiarRango(Player jugador, RangoFe antes, RangoFe despues) {
        if (antes == despues) {
            return;
        }
        double nuevo;
        if (despues.tipoMana() == TipoMana.NINGUNO) {
            nuevo = 0;
        } else if (antes.tipoMana() != despues.tipoMana()) {
            nuevo = despues.manaMaximo();
        } else {
            nuevo = obtener(jugador) + (despues.manaMaximo() - antes.manaMaximo());
        }
        guardar(jugador, nuevo);
    }

    /** Para jugadores que ya tenían fe antes de existir el maná: la primera vez que entran, el maná empieza lleno. */
    static void asegurarIniciado(Player jugador) {
        if (!persistentes(jugador).contains(CLAVE)) {
            guardar(jugador, maximo(jugador));
        }
    }

    // ---------- REGENERACIÓN (se registra en ModuloFe) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)
                || jugador.tickCount % TICKS_REGENERACION != 0) {
            return;
        }
        if (jugador.getFoodData().getFoodLevel() < COMIDA_LLENA) {
            return;
        }
        int maximo = maximo(jugador);
        if (maximo > 0 && obtener(jugador) < maximo) {
            sumar(jugador, REGENERACION);
        }
    }

    // ---------- INTERNO ----------

    private static void guardar(Player jugador, double mana) {
        CompoundTag datos = persistentes(jugador);
        datos.putDouble(CLAVE, Mth.clamp(mana, 0, maximo(jugador)));
        jugador.getPersistentData().put(Player.PERSISTED_NBT_TAG, datos);
    }

    private static CompoundTag persistentes(Player jugador) {
        return jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }
}
