package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.FeDatos;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * FASE 1 del Astado: el acecho. Todavía no hay ningún mob: solo ruidos.
 *
 * Si un jugador está en un bosque de noche (y el Astado lo atacaría), cada 30 s hay una probabilidad de que
 * empiece el acecho. Durante 1-5 minutos oye cosas a su alrededor (ramas que se parten, pasos a lo lejos,
 * una respiración, gruñidos) cada vez más seguido, más cerca y más agresivas. Si al acabar sigue en el bosque
 * y sigue siendo de noche, aparece el Astado entre los árboles, a su espalda y donde no se le vea (FASE 2).
 * Si sale del bosque o se hace de día, el acecho se acaba sin más.
 *
 * Solo lo oye ese jugador. Nada de esto se guarda: si sale del mundo, se acaba.
 */
public final class AcechoAstado {

    // ---------- AJUSTES ----------
    private static final int TICKS_ENTRE_TIRADAS = 600;       // cada 30 s
    private static final float PROB_EMPEZAR = 0.09f;          // ~73% de que pase en una noche entera en el bosque
    private static final int DURACION_MIN = 20 * 60, DURACION_MAX = 20 * 300;   // 1 a 5 minutos
    private static final int ESPERA_TRAS_CAZA = 20 * 60 * 10;  // 10 min sin otro Astado para el mismo jugador
    // Ruidos: de cada 16 s a lo lejos (28 bloques) al principio, a cada 4 s y a 9 bloques al final
    private static final int INTERVALO_INICIO = 320, INTERVALO_FINAL = 80;
    private static final double DISTANCIA_INICIO = 28.0, DISTANCIA_FINAL = 9.0;
    // Aparición
    private static final double APARECER_MIN = 22.0, APARECER_MAX = 30.0;
    private static final int INTENTOS_APARECER = 40;

    private static final class Acecho {
        final int duracion;
        int ticks;
        int siguienteRuido;
        int pasosPendientes;
        int siguientePaso;
        Vec3 posicionPasos = Vec3.ZERO;
        int intentosAparecer;

        Acecho(int duracion) {
            this.duracion = duracion;
        }

        float progreso() {
            return Mth.clamp(ticks / (float) duracion, 0.0f, 1.0f);
        }
    }

    private static final Map<UUID, Acecho> ACECHOS = new HashMap<>();
    private static final Map<UUID, Long> ESPERA_HASTA = new HashMap<>();

    private AcechoAstado() {}

    /** Se registra en ModuloFe (bus de Forge). */
    public static void alTickJugador(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) {
            return;
        }
        UUID id = jugador.getUUID();
        Acecho acecho = ACECHOS.get(id);

        if (acecho == null) {
            if (jugador.tickCount % TICKS_ENTRE_TIRADAS == 0 && puedeAcechar(jugador)
                    && jugador.level().getGameTime() >= ESPERA_HASTA.getOrDefault(id, 0L)
                    && jugador.getRandom().nextFloat() < PROB_EMPEZAR) {
                empezar(jugador);
            }
            return;
        }

        // Si sale del bosque, se hace de día... se acaba
        if (jugador.tickCount % 20 == 0 && !puedeAcechar(jugador)) {
            ACECHOS.remove(id);
            return;
        }
        acecho.ticks++;
        ruidos(jugador, acecho);

        if (acecho.ticks >= acecho.duracion) {
            if (aparecer(jugador, true)) {
                ACECHOS.remove(id);
                ESPERA_HASTA.put(id, jugador.level().getGameTime() + ESPERA_TRAS_CAZA);
            } else if (++acecho.intentosAparecer > 10) {
                ACECHOS.remove(id);    // no hay sitio (p. ej. está en un claro enorme o bajo tierra)
            } else {
                acecho.ticks = acecho.duracion - 100;   // lo vuelve a intentar en 5 s, sin dejar de sonar
            }
        }
    }

    /** Empezar el acecho ya (comando /fe astado acecho). */
    public static void empezar(ServerPlayer jugador) {
        RandomSource azar = jugador.getRandom();
        Acecho acecho = new Acecho(DURACION_MIN + azar.nextInt(DURACION_MAX - DURACION_MIN + 1));
        acecho.siguienteRuido = 100 + azar.nextInt(100);
        ACECHOS.put(jugador.getUUID(), acecho);
    }

    public static void alDesconectarse(PlayerEvent.PlayerLoggedOutEvent evento) {
        ACECHOS.remove(evento.getEntity().getUUID());
    }

    /** En el mundo normal, de noche, en un bosque, no en creativo y siendo alguien a quien el Astado atacaría. */
    public static boolean puedeAcechar(ServerPlayer jugador) {
        Level nivel = jugador.level();
        return nivel.dimension() == Level.OVERWORLD
                && nivel.getDifficulty() != Difficulty.PEACEFUL
                && nivel.isNight()
                && !jugador.isCreative() && !jugador.isSpectator()
                && nivel.getBiome(jugador.blockPosition()).is(BiomeTags.IS_FOREST)
                && MobFe.ataca(true, true, FeDatos.obtenerNivel(jugador))
                && !hayAstadoCerca(jugador);
    }

    private static boolean hayAstadoCerca(ServerPlayer jugador) {
        return !jugador.level().getEntitiesOfClass(Astado.class, jugador.getBoundingBox().inflate(96)).isEmpty();
    }

    // ---------- LOS RUIDOS ----------

    private static void ruidos(ServerPlayer jugador, Acecho acecho) {
        RandomSource azar = jugador.getRandom();
        float p = acecho.progreso();

        // Pasos en racha (uno tras otro, acercándose un poco)
        if (acecho.pasosPendientes > 0 && --acecho.siguientePaso <= 0) {
            acecho.pasosPendientes--;
            acecho.siguientePaso = 7 + azar.nextInt(4);
            Vec3 hacia = jugador.position().subtract(acecho.posicionPasos).normalize().scale(0.8);
            acecho.posicionPasos = acecho.posicionPasos.add(hacia);
            sonar(jugador, ModuloFe.SONIDO_ASTADO_PASO.get(), acecho.posicionPasos, 0.6f + p * 0.8f, 0.85f + azar.nextFloat() * 0.1f);
        }

        if (--acecho.siguienteRuido > 0) {
            return;
        }
        int intervalo = Mth.floor(Mth.lerp(p, INTERVALO_INICIO, INTERVALO_FINAL));
        acecho.siguienteRuido = intervalo / 2 + azar.nextInt(intervalo);

        double distancia = Mth.lerp(p, DISTANCIA_INICIO, DISTANCIA_FINAL) * (0.85 + azar.nextDouble() * 0.3);
        Vec3 donde = alrededor(jugador, distancia, azar);
        float volumen = 0.7f + p * 1.3f;

        // Qué suena: al principio ramas y pasos lejanos; luego respiraciones; al final gruñidos
        float tirada = azar.nextFloat();
        if (tirada < 0.35f - p * 0.2f) {
            sonar(jugador, ModuloFe.SONIDO_ASTADO_RAMA.get(), donde, volumen, 0.9f + azar.nextFloat() * 0.2f);
        } else if (tirada < 0.65f - p * 0.15f) {
            acecho.pasosPendientes = 3 + azar.nextInt(3) + Mth.floor(p * 4);
            acecho.siguientePaso = 0;
            acecho.posicionPasos = donde;
        } else if (tirada < 0.9f - p * 0.25f || p < 0.35f) {
            sonar(jugador, ModuloFe.SONIDO_ASTADO_RESPIRACION.get(), donde, volumen, 0.8f + azar.nextFloat() * 0.25f);
        } else {
            sonar(jugador, ModuloFe.SONIDO_ASTADO_GRUNIDO.get(), donde, volumen, 0.75f + azar.nextFloat() * 0.2f);
        }
    }

    /** Un punto a esa distancia alrededor del jugador, a la altura del suelo. */
    private static Vec3 alrededor(ServerPlayer jugador, double distancia, RandomSource azar) {
        double angulo = azar.nextDouble() * Math.PI * 2;
        double x = jugador.getX() + Math.cos(angulo) * distancia;
        double z = jugador.getZ() + Math.sin(angulo) * distancia;
        double y = jugador.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)) + 0.5;
        if (Math.abs(y - jugador.getY()) > 12) {
            y = jugador.getY();
        }
        return new Vec3(x, y, z);
    }

    /** Un sonido que solo oye este jugador. */
    private static void sonar(ServerPlayer jugador, SoundEvent sonido, Vec3 donde, float volumen, float tono) {
        jugador.connection.send(new ClientboundSoundPacket(Holder.direct(sonido), SoundSource.HOSTILE,
                donde.x, donde.y, donde.z, volumen, tono, jugador.getRandom().nextLong()));
    }

    // ---------- LA APARICIÓN ----------

    /**
     * Aparece a su espalda (entre 22 y 30 bloques), sobre suelo, con sitio para estar de pie y, si puede ser,
     * con algo en medio (árboles) para que no se vea cómo aparece.
     */
    public static boolean aparecer(ServerPlayer jugador, boolean deAcecho) {
        ServerLevel nivel = jugador.serverLevel();
        RandomSource azar = jugador.getRandom();
        Vec3 mejor = null;
        for (int i = 0; i < INTENTOS_APARECER; i++) {
            double angulo = Math.toRadians(jugador.getYRot() + 90 + 180 + (azar.nextDouble() - 0.5) * 140);
            double distancia = APARECER_MIN + azar.nextDouble() * (APARECER_MAX - APARECER_MIN);
            double x = jugador.getX() + Math.cos(angulo) * distancia;
            double z = jugador.getZ() + Math.sin(angulo) * distancia;
            BlockPos suelo = BlockPos.containing(x, 0, z);
            int y = nivel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, suelo.getX(), suelo.getZ());
            if (Math.abs(y - jugador.getY()) > 10 || !nivel.isLoaded(suelo)) {
                continue;
            }
            Vec3 sitio = new Vec3(suelo.getX() + 0.5, y, suelo.getZ() + 0.5);
            if (!nivel.noCollision(ModuloFe.ASTADO.get().getAABB(sitio.x, sitio.y, sitio.z))
                    || !nivel.getBlockState(BlockPos.containing(sitio).below()).isSolidRender(nivel, BlockPos.containing(sitio).below())) {
                continue;
            }
            if (mejor == null) {
                mejor = sitio;
            }
            if (oculto(nivel, jugador, sitio)) {
                mejor = sitio;
                break;
            }
        }
        if (mejor == null) {
            return false;
        }
        Astado astado = ModuloFe.ASTADO.get().create(nivel);
        if (astado == null) {
            return false;
        }
        float mirando = (float) (Mth.atan2(jugador.getZ() - mejor.z, jugador.getX() - mejor.x) * Mth.RAD_TO_DEG) - 90.0f;
        astado.moveTo(mejor.x, mejor.y, mejor.z, mirando, 0.0f);
        astado.setYHeadRot(mirando);
        astado.fijarPresa(jugador, deAcecho);
        nivel.addFreshEntity(astado);
        return true;
    }

    /** ¿Hay algo (árboles, rocas...) entre los ojos del jugador y el sitio? */
    private static boolean oculto(ServerLevel nivel, ServerPlayer jugador, Vec3 sitio) {
        for (double altura : new double[]{1.0, 2.2}) {
            Vec3 punto = sitio.add(0, altura, 0);
            if (nivel.clip(new ClipContext(jugador.getEyePosition(), punto, ClipContext.Block.VISUAL,
                    ClipContext.Fluid.NONE, jugador)).getType() == HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }
}
