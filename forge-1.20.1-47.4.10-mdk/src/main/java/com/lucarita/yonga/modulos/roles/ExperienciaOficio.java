package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.modulos.fe.Oracion;
import com.lucarita.yonga.core.GolpeCargado;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cómo gana experiencia de oficio cada rol. Todas las cifras están aquí arriba para equilibrarlas fácil.
 *
 * Los rangos piden (ver RangoRol): Aprendiz 100, Oficial 400, Experto 1200, Maestro 3200.
 * Cada salto cuesta unas 2,5-3 veces el anterior. Con un ritmo de ~100 de experiencia por hora de juego
 * dedicada al oficio: Aprendiz ~1 h, Oficial ~4 h, Experto ~12 h, Maestro ~32 h (algo menos rezando).
 *
 *  - Explorador: 1 por cada 100 bloques recorridos (a pie, nadando, a caballo, en barca, volando...).
 *    Se mide en línea recta cada 5 s, así que dar vueltas en el sitio o ir y volver en un pasillo apenas
 *    cuenta, y como mucho cuentan 50 bloques cada 5 s (los teletransportes no cuentan).
 *  - Sacerdote: 1 por cada 20 s rezando o blasfemando (necesita el módulo de fe). Maestro: ~18 h rezando.
 *  - Melómano: golpes a tiempo. 1 por cada 10 golpes cuerpo a cuerpo a mobs con el ataque cargado del
 *    todo (con cualquier arma o herramienta, no hacen falta instrumentos). Aporrear no cuenta.
 *  - Fantasma: 1 por cada minuto conectado, sin hacer nada.
 *  - Herrero e Ingeniero: todavía no ganan experiencia (pendiente de decidir cómo).
 */
public final class ExperienciaOficio {

    // ---- Ajustes ----
    private static final long TICKS_ENTRE_MUESTRAS = 100;      // Explorador: cada 5 s
    private static final double DISTANCIA_MAXIMA_MUESTRA = 50;  // más que esto en 5 s = teletransporte
    private static final double BLOQUES_POR_PUNTO = 100;

    private static final int TICKS_REZANDO_POR_PUNTO = 20 * 20;

    private static final int GOLPES_POR_PUNTO = 10;

    private static final int TICKS_FANTASMA_POR_PUNTO = 20 * 60;

    // ---- Lo que va acumulando cada jugador (se pierde al desconectarse, no pasa nada) ----
    private record Muestra(Vec3 posicion, String dimension) {}
    private static final Map<UUID, Muestra> ULTIMA_MUESTRA = new HashMap<>();
    private static final Map<UUID, Double> BLOQUES = new HashMap<>();
    private static final Map<UUID, Integer> TICKS_REZANDO = new HashMap<>();
    private static final Map<UUID, Integer> GOLPES = new HashMap<>();

    private ExperienciaOficio() {}

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) {
            return;
        }
        Rol rol = RolDatos.obtenerRol(jugador);
        if (rol == null) {
            return;
        }
        switch (rol) {
            case EXPLORADOR -> explorador(jugador);
            case SACERDOTE -> sacerdote(jugador);
            case FANTASMA -> {
                if (jugador.tickCount % TICKS_FANTASMA_POR_PUNTO == 0) {
                    RolDatos.sumarExperiencia(jugador, 1);
                }
            }
            default -> {}
        }
    }

    private static void explorador(ServerPlayer jugador) {
        if (jugador.tickCount % TICKS_ENTRE_MUESTRAS != 0) {
            return;
        }
        UUID id = jugador.getUUID();
        Muestra ahora = new Muestra(jugador.position(), jugador.level().dimension().location().toString());
        Muestra antes = ULTIMA_MUESTRA.put(id, ahora);
        if (antes == null || !antes.dimension().equals(ahora.dimension())) {
            return;
        }
        double dx = ahora.posicion().x - antes.posicion().x;
        double dz = ahora.posicion().z - antes.posicion().z;
        double distancia = Math.sqrt(dx * dx + dz * dz);
        if (distancia > DISTANCIA_MAXIMA_MUESTRA) {
            return;   // teletransporte, perla de ender...
        }
        double total = BLOQUES.getOrDefault(id, 0.0) + distancia;
        int puntos = (int) (total / BLOQUES_POR_PUNTO);
        if (puntos > 0) {
            RolDatos.sumarExperiencia(jugador, puntos);
            total -= puntos * BLOQUES_POR_PUNTO;
        }
        BLOQUES.put(id, total);
    }

    private static void sacerdote(ServerPlayer jugador) {
        if (Oracion.direccion(jugador) == Oracion.NADA) {
            return;
        }
        int ticks = TICKS_REZANDO.getOrDefault(jugador.getUUID(), 0) + 1;
        if (ticks >= TICKS_REZANDO_POR_PUNTO) {
            RolDatos.sumarExperiencia(jugador, 1);
            ticks = 0;
        }
        TICKS_REZANDO.put(jugador.getUUID(), ticks);
    }

    // ---------- MELÓMANO: golpes a tiempo ----------

    /**
     * Cada golpe cuerpo a cuerpo a un mob con el ataque cargado del todo (pegar a tiempo, no aporrear),
     * con cualquier arma o herramienta, y que le haga daño de verdad.
     */
    public static void alHacerDano(LivingDamageEvent evento) {
        if (!(evento.getSource().getDirectEntity() instanceof ServerPlayer jugador)
                || RolDatos.obtenerRol(jugador) != Rol.MELOMANO
                || !(evento.getEntity() instanceof Mob) || evento.getAmount() <= 0
                || !GolpeCargado.fue(jugador)) {
            return;
        }
        int golpes = GOLPES.getOrDefault(jugador.getUUID(), 0) + 1;
        if (golpes >= GOLPES_POR_PUNTO) {
            RolDatos.sumarExperiencia(jugador, 1);
            golpes = 0;
        }
        GOLPES.put(jugador.getUUID(), golpes);
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        UUID id = evento.getEntity().getUUID();
        ULTIMA_MUESTRA.remove(id);
        BLOQUES.remove(id);
        TICKS_REZANDO.remove(id);
        GOLPES.remove(id);
    }
}
