package com.lucarita.yonga.modulos.fe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orbes de fe al matar mobs:
 *  - Monstruos (zombis, esqueletos, slimes, ghasts...): orbes AZULES que suman fe.
 *  - El resto (animales, aldeanos, gólems, peces, murciélagos...): orbes NEGROS que restan fe.
 *
 * Valor total = vida máxima del mob × porcentaje (monstruos 50%, resto 80%) × penalización por repetir.
 *   Ej.: zombi (20 de vida) = +10, vaca (10 de vida) = -8.
 * El valor es fijo: no depende del nivel de quien mata ni de quien recoge.
 *
 * Para que no se pueda abusar con granjas:
 *  - Solo sueltan orbes si los mata un jugador (como la experiencia).
 *  - Los mobs que salen de un generador de monstruos no sueltan orbes.
 *  - Repetir el mismo tipo de mob da cada vez menos: cada uno del mismo tipo que hayas matado en los
 *    últimos 10 minutos quita un 5% al siguiente, hasta un mínimo del 10%. Jugando normal casi no se nota;
 *    en una granja de un solo tipo de mob el valor se queda enseguida al 10%.
 */
public final class OrbesFe {

    // ---- Ajustes ----
    private static final float PORCENTAJE_MONSTRUOS = 0.5f;
    private static final float PORCENTAJE_RESTO = 0.8f;

    private static final long VENTANA_REPETICION = 10 * 60 * 20;   // 10 minutos en ticks
    private static final float PENALIZACION_POR_REPETICION = 0.05f;
    private static final float VALOR_MINIMO = 0.10f;

    // Puntos máximos por orbe: un mob que valga 10 suelta 1 orbe; uno que valga 25, tres
    private static final float VALOR_MAXIMO_POR_ORBE = 10.0f;

    private static final String CLAVE_GENERADOR = "yonga_de_generador";

    // Jugador -> tipo de mob -> momentos (en ticks del mundo) en que mató uno de ese tipo
    private static final Map<UUID, Map<EntityType<?>, Deque<Long>>> MUERTES_RECIENTES = new HashMap<>();

    private OrbesFe() {}

    // ---------- EVENTOS (se registran en ModuloFe) ----------

    /** Marca los mobs que salen de un generador de monstruos para que luego no suelten orbes. */
    public static void alAparecer(MobSpawnEvent.FinalizeSpawn evento) {
        if (evento.getSpawnType() == MobSpawnType.SPAWNER) {
            evento.getEntity().getPersistentData().putBoolean(CLAVE_GENERADOR, true);
        }
    }

    public static void alMorir(LivingDeathEvent evento) {
        LivingEntity muerto = evento.getEntity();
        if (!(muerto.level() instanceof ServerLevel nivel) || muerto instanceof Player || muerto instanceof ArmorStand) {
            return;
        }
        if (muerto.getPersistentData().getBoolean(CLAVE_GENERADOR)) {
            return;
        }

        Player asesino = null;
        if (evento.getSource().getEntity() instanceof Player jugador) {
            asesino = jugador;
        } else if (muerto.getKillCredit() instanceof Player jugador) {
            asesino = jugador;
        }
        if (asesino == null) {
            return;
        }

        boolean esMonstruo = muerto instanceof Enemy;
        float signo = esMonstruo ? 1.0f : -1.0f;
        float porcentaje = esMonstruo ? PORCENTAJE_MONSTRUOS : PORCENTAJE_RESTO;
        float factor = factorRepeticion(asesino, muerto.getType(), nivel.getGameTime());
        float restante = muerto.getMaxHealth() * porcentaje * factor;

        // Lo repartimos en varios orbes, como la experiencia
        while (restante > 0.001f) {
            float trozo = Math.min(restante, VALOR_MAXIMO_POR_ORBE);
            restante -= trozo;
            nivel.addFreshEntity(OrbeFe.crear(nivel, muerto.getX(), muerto.getY() + 0.5, muerto.getZ(), signo * trozo));
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        MUERTES_RECIENTES.remove(evento.getEntity().getUUID());
    }

    // ---------- PENALIZACIÓN POR REPETIR ----------

    /** Cuánto vale esta muerte (de 1.0 a 0.1) según cuántos de ese tipo ha matado el jugador hace poco. La apunta. */
    private static float factorRepeticion(Player asesino, EntityType<?> tipo, long ahora) {
        Deque<Long> muertes = MUERTES_RECIENTES
                .computeIfAbsent(asesino.getUUID(), u -> new HashMap<>())
                .computeIfAbsent(tipo, t -> new ArrayDeque<>());

        // Olvidamos las muertes de hace más de 10 minutos
        while (!muertes.isEmpty() && ahora - muertes.peekFirst() > VENTANA_REPETICION) {
            muertes.pollFirst();
        }

        float factor = Math.max(VALOR_MINIMO, 1.0f - PENALIZACION_POR_REPETICION * muertes.size());
        muertes.addLast(ahora);
        return factor;
    }
}
