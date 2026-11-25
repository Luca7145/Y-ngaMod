package com.lucarita.yonga.modulos.habitantes.familia;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.red.EncimaPacket;
import com.lucarita.yonga.modulos.habitantes.red.FundidoPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La noche juntos, como un pequeño ritual en una cama (desde fuera se ve a dos personas acostadas, una encima de la
 * otra; dentro, la pantalla funde a negro y no se ve nada más):
 *   1. Quien va abajo se acuesta: si es un habitante, va él solo a la cama y espera; si es un jugador, hace clic
 *      derecho en la cama.
 *   2. Quien va encima llega: el habitante va andando; el jugador hace clic derecho en la cama.
 *   3. Fundido a negro unos segundos para los jugadores (el habitante, en ropa interior); luego se levantan.
 * Abajo va la mujer; si son del mismo sexo (o no se sabe), a suertes. Entre dos jugadores: uno se lo propone al otro
 * (agachado + clic derecho) y el otro acepta igual.
 * Después: tiempo de espera de un día de juego y, si son de distinto sexo, muy probable embarazo (ver Amor.despues).
 */
public final class Encuentro {

    private Encuentro() {}

    private enum Fase { IR_ABAJO, ESPERA_ABAJO, IR_ARRIBA, ESPERA_ARRIBA, ESCENA }

    private static final class E {
        final ServerLevel nivel;
        final UUID abajo, arriba;
        @Nullable BlockPos cama;
        Fase fase;
        long limite;
        int t;

        E(ServerLevel nivel, UUID abajo, UUID arriba, @Nullable BlockPos cama) {
            this.nivel = nivel;
            this.abajo = abajo;
            this.arriba = arriba;
            this.cama = cama;
            this.limite = nivel.getGameTime() + 20 * 90;
        }

        @Nullable
        Entity ent(UUID u) {
            return nivel.getEntity(u);
        }

        boolean participa(UUID u) {
            return abajo.equals(u) || arriba.equals(u);
        }
    }

    private static final List<E> ACTIVOS = new ArrayList<>();
    /** Propuestas entre jugadores: quién → (a quién, hasta cuándo). */
    private static final Map<UUID, UUID> PROPUESTAS = new HashMap<>();
    private static final Map<UUID, Long> PROPUESTAS_HASTA = new HashMap<>();

    private static final int DURA_ESCENA = 100;
    /** Última noche de cada pareja de jugadores (su tiempo de espera es de la pareja, no de cada uno). */
    private static final Map<String, Long> ULTIMA_PAREJA = new HashMap<>();

    private static String clave(UUID a, UUID b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    static void apuntarPareja(UUID a, UUID b, long ahora) {
        ULTIMA_PAREJA.put(clave(a, b), ahora);
    }

    // =====================================================================================================
    // EMPEZAR
    // =====================================================================================================

    /** La cama más cercana (la cabecera) libre, a 16 bloques como mucho, o null. */
    @Nullable
    public static BlockPos camaCerca(ServerLevel nivel, BlockPos centro) {
        BlockPos mejor = null;
        double d = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(centro.offset(-16, -4, -16), centro.offset(16, 4, 16))) {
            BlockState s = nivel.getBlockState(p);
            if (s.getBlock() instanceof BedBlock && s.getValue(BedBlock.PART) == BedPart.HEAD && !s.getValue(BedBlock.OCCUPIED)) {
                double dd = p.distSqr(centro);
                if (dd < d) {
                    d = dd;
                    mejor = p.immutable();
                }
            }
        }
        return mejor;
    }

    public static boolean ocupado(UUID u) {
        for (E e : ACTIVOS) if (e.participa(u)) return true;
        return false;
    }

    /** ¿Quién va abajo? La mujer; si son del mismo sexo (o no se sabe), a suertes. true = el primero. */
    private static boolean primeroAbajo(ServerLevel nivel, Familias.Persona a, Familias.Persona b) {
        if (a.sexoConocido() && b.sexoConocido() && a.esMujer() != b.esMujer()) return a.esMujer();
        return nivel.random.nextBoolean();
    }

    /** Un jugador y un habitante (ya ha dicho que sí y hay cama). */
    public static void conHabitante(ServerLevel nivel, ServerPlayer j, Habitante h, BlockPos cama) {
        Familias f = Familias.de(nivel.getServer());
        boolean habAbajo = primeroAbajo(nivel, f.persona(h.getUUID()), f.persona(j.getUUID()));
        h.terminarConversacion();
        cerrarConversacion(j, h);
        E e = habAbajo ? new E(nivel, h.getUUID(), j.getUUID(), cama) : new E(nivel, j.getUUID(), h.getUUID(), cama);
        e.fase = habAbajo ? Fase.IR_ABAJO : Fase.ESPERA_ABAJO;
        ACTIVOS.add(e);
        j.displayClientMessage(Component.translatable(habAbajo ? "yonga.encuentro.va_a_la_cama" : "yonga.encuentro.tumbate"), true);
    }

    private static void cerrarConversacion(ServerPlayer j, Habitante h) {
        HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> j),
                new com.lucarita.yonga.modulos.habitantes.red.RelacionPacket(h.getId(), -2, 0));
    }

    /** Agachado + clic derecho sobre otro jugador: se lo propones; si él ya te lo había propuesto, aceptas. */
    public static void alInteractuarJugador(PlayerInteractEvent.EntityInteract evento) {
        // Clic derecho sobre el habitante que te espera acostado: como hacerlo en la cama
        if (evento.getEntity() instanceof ServerPlayer yo && evento.getTarget() instanceof Habitante h && h.isSleeping()) {
            for (E e : ACTIVOS) {
                if (e.fase == Fase.ESPERA_ARRIBA && e.arriba.equals(yo.getUUID()) && e.abajo.equals(h.getUUID()) && e.cama != null) {
                    evento.setCanceled(true);
                    evento.setCancellationResult(InteractionResult.SUCCESS);
                    acostar(yo, e.cama, true);
                    empezarEscena(e);
                    return;
                }
            }
        }
        if (!(evento.getEntity() instanceof ServerPlayer j) || !(evento.getTarget() instanceof ServerPlayer otro)
                || !j.isShiftKeyDown() || evento.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !Amor.activo() || !j.getMainHandItem().isEmpty()) return;
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);
        ServerLevel nivel = j.serverLevel();
        Familias f = Familias.de(nivel.getServer());
        long ahora = nivel.getGameTime();
        if (f.parientes(j.getUUID(), otro.getUUID())) return;
        if (ocupado(j.getUUID()) || ocupado(otro.getUUID())) return;
        if (ahora - ULTIMA_PAREJA.getOrDefault(clave(j.getUUID(), otro.getUUID()), -1_000_000L) < Amor.ESPERA_NOCHE) {
            j.displayClientMessage(Component.translatable("yonga.encuentro.cansados"), true);
            return;
        }
        Long hasta = PROPUESTAS_HASTA.get(otro.getUUID());
        if (j.getUUID().equals(PROPUESTAS.get(otro.getUUID())) && hasta != null && hasta >= ahora) {
            PROPUESTAS.remove(otro.getUUID());
            PROPUESTAS_HASTA.remove(otro.getUUID());
            f.apuntar(j.getUUID(), j.getGameProfile().getName(), f.persona(j.getUUID()).mujer, true);
            f.apuntar(otro.getUUID(), otro.getGameProfile().getName(), f.persona(otro.getUUID()).mujer, true);
            boolean yoAbajo = primeroAbajo(nivel, f.persona(j.getUUID()), f.persona(otro.getUUID()));
            ServerPlayer abajo = yoAbajo ? j : otro, arriba = yoAbajo ? otro : j;
            E e = new E(nivel, abajo.getUUID(), arriba.getUUID(), null);
            e.fase = Fase.ESPERA_ABAJO;
            ACTIVOS.add(e);
            abajo.displayClientMessage(Component.translatable("yonga.encuentro.tumbate"), true);
            arriba.displayClientMessage(Component.translatable("yonga.encuentro.espera_que_se_tumbe"), true);
            return;
        }
        PROPUESTAS.put(j.getUUID(), otro.getUUID());
        PROPUESTAS_HASTA.put(j.getUUID(), ahora + 20 * 30);
        otro.sendSystemMessage(Component.translatable("yonga.encuentro.propone", j.getDisplayName()));
        j.displayClientMessage(Component.translatable("yonga.encuentro.propuesto", otro.getDisplayName()), true);
    }

    // =====================================================================================================
    // CAMA
    // =====================================================================================================

    private static BlockPos cabecera(BlockPos p, BlockState s) {
        return s.getValue(BedBlock.PART) == BedPart.HEAD ? p : p.relative(s.getValue(BedBlock.FACING));
    }

    /** Clic derecho en una cama de un jugador que tiene que acostarse para su encuentro. */
    public static void alClicCama(PlayerInteractEvent.RightClickBlock evento) {
        if (!(evento.getEntity() instanceof ServerPlayer j)) return;
        BlockState s = j.level().getBlockState(evento.getPos());
        if (!(s.getBlock() instanceof BedBlock)) return;
        for (E e : ACTIVOS) {
            boolean toca = e.fase == Fase.ESPERA_ABAJO && e.abajo.equals(j.getUUID())
                    || e.fase == Fase.ESPERA_ARRIBA && e.arriba.equals(j.getUUID());
            if (!toca) continue;
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.SUCCESS);
            BlockPos cama = cabecera(evento.getPos(), s);
            if (e.cama != null && !e.cama.equals(cama)) {
                j.displayClientMessage(Component.translatable("yonga.encuentro.otra_cama"), true);
                return;
            }
            if (e.cama == null) {
                if (s.getValue(BedBlock.OCCUPIED)) return;
                e.cama = cama;
            }
            acostar(j, e.cama, e.fase == Fase.ESPERA_ARRIBA);
            if (e.fase == Fase.ESPERA_ABAJO) {
                Entity arriba = e.ent(e.arriba);
                if (arriba instanceof Habitante) {
                    e.fase = Fase.IR_ARRIBA;
                } else {
                    e.fase = Fase.ESPERA_ARRIBA;
                    if (arriba instanceof ServerPlayer pa) pa.displayClientMessage(Component.translatable("yonga.encuentro.te_espera"), true);
                }
                e.limite = e.nivel.getGameTime() + 20 * 90;
            } else {
                empezarEscena(e);
            }
            return;
        }
    }

    private static void acostar(LivingEntity quien, BlockPos cama, boolean encima) {
        // En cuanto se tumba, en ropa interior (hasta que se levante; como mucho, la espera más la escena)
        if (quien instanceof Habitante h) h.ropaInteriorDurante(20 * 90 + DURA_ESCENA + 40);
        quien.startSleeping(cama);
        if (encima) {
            HabitantesRed.CANAL.send(quien instanceof ServerPlayer
                            ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> quien)
                            : PacketDistributor.TRACKING_ENTITY.with(() -> quien),
                    new EncimaPacket(quien.getId(), true));
        }
    }

    private static void levantar(@Nullable Entity quien) {
        if (!(quien instanceof LivingEntity v)) return;
        if (v instanceof Habitante h) h.vestirse();
        HabitantesRed.CANAL.send(v instanceof ServerPlayer
                        ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> v)
                        : PacketDistributor.TRACKING_ENTITY.with(() -> v),
                new EncimaPacket(v.getId(), false));
        if (!v.isSleeping()) return;
        if (v instanceof ServerPlayer p) p.stopSleepInBed(true, true);
        else v.stopSleeping();
    }

    // =====================================================================================================
    // HABITANTES (EscenaGoal)
    // =====================================================================================================

    /** ¿Está este habitante metido en un encuentro? (su goal manda sobre todo lo demás) */
    public static boolean participa(Habitante h) {
        return ocupado(h.getUUID());
    }

    /** La cama a la que tiene que ir andando ahora, o null (esperando o ya acostado). */
    @Nullable
    public static BlockPos irA(Habitante h) {
        for (E e : ACTIVOS) {
            if (e.cama == null || h.isSleeping()) continue;
            if (e.fase == Fase.IR_ABAJO && e.abajo.equals(h.getUUID()) || e.fase == Fase.IR_ARRIBA && e.arriba.equals(h.getUUID())) {
                return e.cama;
            }
        }
        return null;
    }

    /** El habitante ha llegado a la cama: se acuesta (abajo, a esperar; encima, empieza). */
    public static void llego(Habitante h) {
        for (E e : ACTIVOS) {
            if (e.cama == null) continue;
            if (e.fase == Fase.IR_ABAJO && e.abajo.equals(h.getUUID())) {
                acostar(h, e.cama, false);
                e.fase = Fase.ESPERA_ARRIBA;
                e.limite = e.nivel.getGameTime() + 20 * 90;
                if (e.ent(e.arriba) instanceof ServerPlayer j) j.displayClientMessage(Component.translatable("yonga.encuentro.te_espera"), true);
                return;
            }
            if (e.fase == Fase.IR_ARRIBA && e.arriba.equals(h.getUUID())) {
                acostar(h, e.cama, true);
                empezarEscena(e);
                return;
            }
        }
    }

    // =====================================================================================================
    // ESCENA
    // =====================================================================================================

    private static void empezarEscena(E e) {
        e.fase = Fase.ESCENA;
        e.t = 0;
        for (UUID u : new UUID[]{e.abajo, e.arriba}) {
            Entity x = e.ent(u);
            if (x instanceof ServerPlayer p) HabitantesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> p), new FundidoPacket(DURA_ESCENA + 20));
            if (x instanceof Habitante h) h.ropaInteriorDurante(DURA_ESCENA + 10);
        }
    }

    /** Los jugadores de un encuentro pueden estar acostados aunque sea de día. */
    public static void alComprobarHora(net.minecraftforge.event.entity.player.SleepingTimeCheckEvent evento) {
        if (ocupado(evento.getEntity().getUUID())) evento.setResult(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
    }

    public static void alTick(TickEvent.ServerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || ACTIVOS.isEmpty()) return;
        Iterator<E> it = ACTIVOS.iterator();
        while (it.hasNext()) {
            E e = it.next();
            Entity abajo = e.ent(e.abajo), arriba = e.ent(e.arriba);
            boolean perdido = abajo == null || arriba == null || !abajo.isAlive() || !arriba.isAlive()
                    || abajo.distanceToSqr(arriba) > 40 * 40;
            // Un jugador que ya estaba acostado se ha levantado (botón de salir de la cama)
            boolean abajoAcostado = e.fase != Fase.IR_ABAJO && e.fase != Fase.ESPERA_ABAJO;
            if (!perdido && abajoAcostado && abajo instanceof LivingEntity la && !la.isSleeping()) perdido = true;
            if (e.fase == Fase.ESCENA) {
                if (++e.t >= DURA_ESCENA || perdido) {
                    it.remove();
                    levantar(abajo);
                    levantar(arriba);
                    Amor.despues(e.nivel, e.abajo, e.arriba);
                }
                continue;
            }
            if (perdido || e.nivel.getGameTime() > e.limite) {
                it.remove();
                levantar(abajo);
                levantar(arriba);
                for (Entity x : new Entity[]{abajo, arriba}) {
                    if (x instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("yonga.encuentro.cancelado"), true);
                }
            }
        }
        long ahora = ACTIVOS.isEmpty() ? 0 : ACTIVOS.get(0).nivel.getGameTime();
        PROPUESTAS_HASTA.entrySet().removeIf(x -> x.getValue() < ahora - 20 * 60);
    }
}
