package com.lucarita.yonga.modulos.fe;

import com.lucarita.yonga.modulos.roles.oficios.Sacerdote;

import com.lucarita.yonga.modulos.fe.red.EstadoOracionPacket;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rezar (+1) y blasfemar (-1). Lado SERVIDOR: aquí se suman/restan los puntos.
 *
 * Mientras el jugador reza o blasfema gana V(L) puntos por segundo (ver FeNiveles.velocidadOracion).
 * Se deja de rezar al: moverse, recibir daño, cambiar de dimensión, desconectarse, volver a pulsar
 * el botón, o llegar al nivel máximo (100 / -100, o 101 / -101 el Sacerdote Maestro).
 */
public final class Oracion {

    public static final int REZAR = 1;
    public static final int BLASFEMAR = -1;
    public static final int NADA = 0;

    // Cada cuántos ticks se manda la fe al cliente mientras reza (10 ticks = medio segundo)
    private static final int TICKS_ENTRE_SINCRONIZACIONES = 10;
    // Cada cuántos ticks salen partículas
    private static final int TICKS_ENTRE_PARTICULAS = 15;
    // Distancia (en bloques) que puede desplazarse sin que se interrumpa (empujones mínimos)
    private static final double MOVIMIENTO_MAXIMO = 0.15;

    private record Estado(int direccion, Vec3 posicionInicial) {}

    private static final Map<UUID, Estado> REZANDO = new HashMap<>();

    private Oracion() {}

    public static int direccion(Player jugador) {
        Estado estado = REZANDO.get(jugador.getUUID());
        return estado == null ? NADA : estado.direccion();
    }

    /**
     * Lo que pasa al pulsar el botón de rezar o blasfemar:
     * si ya estaba haciendo eso, para; si no, empieza (o cambia de rezar a blasfemar).
     */
    public static void alternar(ServerPlayer jugador, int direccion) {
        if (direccion == NADA || direccion(jugador) == direccion) {
            parar(jugador, true);
        } else {
            empezar(jugador, direccion);
        }
    }

    public static void empezar(ServerPlayer jugador, int direccion) {
        // Hay que estar quieto en el suelo, no montado, ni nadando, ni durmiendo
        if (!jugador.onGround() || jugador.isPassenger() || jugador.isSwimming() || jugador.isSleeping()) {
            jugador.displayClientMessage(Component.translatable("yonga.fe.oracion.no_puedes"), true);
            return;
        }

        int nivel = FeDatos.obtenerNivel(jugador);
        if (nivel * direccion >= FeDatos.nivelMaximo(jugador)) {
            jugador.displayClientMessage(Component.translatable("yonga.fe.oracion.maximo"), true);
            return;
        }

        REZANDO.put(jugador.getUUID(), new Estado(direccion, jugador.position()));
        avisarATodos(jugador, direccion);
        jugador.displayClientMessage(Component.translatable(
                direccion == REZAR ? "yonga.fe.oracion.rezando" : "yonga.fe.oracion.blasfemando"), true);
    }

    public static void parar(ServerPlayer jugador, boolean conMensaje) {
        if (REZANDO.remove(jugador.getUUID()) == null) {
            return;
        }
        avisarATodos(jugador, NADA);
        FeDatos.sincronizar(jugador);
        if (conMensaje) {
            jugador.displayClientMessage(Component.translatable("yonga.fe.oracion.parar"), true);
        }
    }

    // Cuenta a este jugador y a todos los que lo ven que empieza/deja de rezar (para la animación)
    private static void avisarATodos(ServerPlayer jugador, int direccion) {
        FeRed.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> jugador),
                new EstadoOracionPacket(jugador.getId(), direccion));
    }

    // ---------- EVENTOS (se registran en ModuloFe) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) {
            return;
        }
        Estado estado = REZANDO.get(jugador.getUUID());
        if (estado == null) {
            return;
        }

        // ¿Se ha movido?
        if (jugador.position().distanceToSqr(estado.posicionInicial()) > MOVIMIENTO_MAXIMO * MOVIMIENTO_MAXIMO
                || !jugador.onGround() || jugador.isPassenger()) {
            parar(jugador, true);
            return;
        }

        // Sumamos la parte de V(L) que toca a este tick (20 ticks = 1 segundo)
        double puntos = FeDatos.obtenerPuntos(jugador);
        int nivelAntes = FeNiveles.nivel(puntos);
        // (multiplicado por la bonificación de su oficio, p. ej. Sacerdote)
        double nuevosPuntos = puntos
                + FeNiveles.velocidadOracion(nivelAntes, estado.direccion()) / 20.0 * FeDatos.multiplicador(jugador)
                * Sacerdote.multiplicadorOracion(jugador);
        int nivelDespues = FeNiveles.nivel(nuevosPuntos);

        // Si cambia de nivel lo mandamos ya; si no, cada medio segundo
        boolean sincronizar = nivelDespues != nivelAntes || jugador.tickCount % TICKS_ENTRE_SINCRONIZACIONES == 0;
        FeDatos.fijarPuntos(jugador, nuevosPuntos, sincronizar);

        if (nivelDespues != nivelAntes) {
            jugador.displayClientMessage(Component.translatable("yonga.fe.oracion.nuevo_nivel",
                    nivelDespues, RangoFe.deNivel(nivelDespues).nombre()), true);
        }

        if (nivelDespues * estado.direccion() >= FeDatos.nivelMaximo(jugador)) {
            parar(jugador, false);
            jugador.displayClientMessage(Component.translatable("yonga.fe.oracion.maximo"), true);
            return;
        }

        if (jugador.tickCount % TICKS_ENTRE_PARTICULAS == 0) {
            soltarParticulas(jugador, estado.direccion());
        }
    }

    private static void soltarParticulas(ServerPlayer jugador, int direccion) {
        ServerLevel nivel = jugador.serverLevel();
        ParticleOptions particula = direccion == REZAR ? ParticleTypes.END_ROD : ParticleTypes.SMOKE;
        nivel.sendParticles(particula, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(),
                2, 0.3, 0.3, 0.3, 0.01);
    }

    public static void alRecibirDano(LivingHurtEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            parar(jugador, true);
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            parar(jugador, false);
        }
    }

    public static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            parar(jugador, false);
        }
    }

    // Cuando alguien empieza a ver a un jugador (se acerca, entra al mundo...), le contamos si está rezando
    public static void alEmpezarAVer(PlayerEvent.StartTracking evento) {
        if (evento.getTarget() instanceof ServerPlayer rezador
                && evento.getEntity() instanceof ServerPlayer observador) {
            int direccion = direccion(rezador);
            if (direccion != NADA) {
                FeRed.CANAL.send(PacketDistributor.PLAYER.with(() -> observador),
                        new EstadoOracionPacket(rezador.getId(), direccion));
            }
        }
    }
}
