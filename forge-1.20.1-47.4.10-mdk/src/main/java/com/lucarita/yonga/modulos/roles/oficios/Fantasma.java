package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * El Fantasma: un oficio para quien quiere estar en el servidor sin meterse en nada.
 * Sube de rango solo, por tiempo conectado (ver ExperienciaOficio).
 *
 *  - Los mobs no lo ven ni van a por él (un esqueleto no le dispara). Pero si le alcanza algo por
 *    accidente (una flecha dirigida a otro, una explosión...) sí le hace daño, igual que un jugador.
 *  - Él tampoco puede hacer daño a nada ni a nadie (ni con golpes, ni flechas, ni hechizos, ni pociones).
 *  - Puede construir (poner bloques), pero no romperlos, ni pisotear cultivos, ni coger o echar líquidos.
 *  - No puede usar bloques: ni cofres, ni hornos, ni yunques, ni botones, ni palancas... Solo puertas,
 *    trampillas, puertas de valla, camas y mesas de crafteo (para poder moverse, dormir y craftear).
 *    Con un objeto en la mano solo puede poner bloques (no labrar, descortezar, encender fuego...).
 *  - No puede interactuar con entidades: ni comerciar, ni montar, ni marcos, ni nada.
 *  - Efectos que ganan con el rango (se acumulan, sin partículas):
 *      Novato: Velocidad I · Aprendiz: + Visión nocturna · Oficial: + Respiración acuática
 *      · Experto: + Resistencia al fuego · Maestro: + Saturación (nunca tiene hambre) y Velocidad II
 */
public final class Fantasma {

    private static final int DURACION_EFECTO = 260;          // >10 s para que la visión nocturna no parpadee
    private static final int TICKS_ENTRE_RENOVACIONES = 20;

    private Fantasma() {}

    public static boolean es(Entity entidad) {
        return entidad instanceof Player jugador && RolDatos.obtenerRol(jugador) == Rol.FANTASMA;
    }

    // ---------- LOS MOBS NO LO VEN, ÉL NO HACE DAÑO ----------

    public static void alCalcularVisibilidad(LivingEvent.LivingVisibilityEvent evento) {
        if (es(evento.getEntity())) {
            evento.modifyVisibility(0.0);
        }
    }

    public static void alCambiarObjetivo(LivingChangeTargetEvent evento) {
        if (es(evento.getNewTarget())) {
            evento.setCanceled(true);
        }
    }

    // Que él haga daño (golpes, flechas, hechizos, TNT que haya encendido...)
    public static void alAtacar(LivingAttackEvent evento) {
        if (es(evento.getSource().getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alGolpear(AttackEntityEvent evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    // ---------- NO ROMPE NI CAMBIA BLOQUES ----------

    public static void alRomper(BlockEvent.BreakEvent evento) {
        if (es(evento.getPlayer())) {
            evento.setCanceled(true);
        }
    }

    // Ni siquiera le sale la grieta al intentarlo
    public static void alCalcularVelocidadRomper(PlayerEvent.BreakSpeed evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alPisotear(BlockEvent.FarmlandTrampleEvent evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alUsarCubo(FillBucketEvent evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    // ---------- NO USA BLOQUES NI ENTIDADES ----------

    public static void alClickDerechoBloque(PlayerInteractEvent.RightClickBlock evento) {
        if (!es(evento.getEntity())) {
            return;
        }
        BlockState bloque = evento.getLevel().getBlockState(evento.getPos());
        if (!sePuedeUsar(bloque)) {
            evento.setUseBlock(Event.Result.DENY);
        }
        // Con un objeto en la mano solo puede poner bloques (nada de TNT)
        ItemStack objeto = evento.getItemStack();
        if (!objeto.isEmpty() && (!(objeto.getItem() instanceof BlockItem bloqueItem) || bloqueItem.getBlock() == Blocks.TNT)) {
            evento.setUseItem(Event.Result.DENY);
        }
    }

    private static boolean sePuedeUsar(BlockState bloque) {
        return bloque.is(BlockTags.DOORS) || bloque.is(BlockTags.TRAPDOORS) || bloque.is(BlockTags.FENCE_GATES)
                || bloque.getBlock() instanceof BedBlock || bloque.getBlock() instanceof CraftingTableBlock;
    }

    // Objetos que se lanzan y afectan a otros (pociones arrojadizas)
    public static void alClickDerechoObjeto(PlayerInteractEvent.RightClickItem evento) {
        if (es(evento.getEntity()) && evento.getItemStack().getItem() instanceof ThrowablePotionItem) {
            evento.setCanceled(true);
        }
    }

    public static void alUsarEntidad(PlayerInteractEvent.EntityInteract evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alUsarEntidadExacto(PlayerInteractEvent.EntityInteractSpecific evento) {
        if (es(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    // ---------- EFECTOS SEGÚN EL RANGO ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)
                || jugador.tickCount % TICKS_ENTRE_RENOVACIONES != 0 || !es(jugador)) {
            return;
        }
        RangoRol rango = RolDatos.obtenerRango(jugador);
        dar(jugador, MobEffects.MOVEMENT_SPEED, rango == RangoRol.MAESTRO ? 1 : 0);
        if (rango.ordinal() >= RangoRol.APRENDIZ.ordinal()) dar(jugador, MobEffects.NIGHT_VISION, 0);
        if (rango.ordinal() >= RangoRol.OFICIAL.ordinal()) dar(jugador, MobEffects.WATER_BREATHING, 0);
        if (rango.ordinal() >= RangoRol.EXPERTO.ordinal()) dar(jugador, MobEffects.FIRE_RESISTANCE, 0);
        if (rango == RangoRol.MAESTRO) dar(jugador, MobEffects.SATURATION, 0);
    }

    private static void dar(ServerPlayer jugador, MobEffect efecto, int nivel) {
        // "ambiental" (como los de un faro), sin partículas, con icono
        jugador.addEffect(new MobEffectInstance(efecto, efecto == MobEffects.SATURATION ? 21 : DURACION_EFECTO, nivel, true, false, true));
    }

    private static final List<MobEffect> EFECTOS = List.of(MobEffects.MOVEMENT_SPEED, MobEffects.NIGHT_VISION,
            MobEffects.WATER_BREATHING, MobEffects.FIRE_RESISTANCE, MobEffects.SATURATION);

    /** Al dejar de ser Fantasma: quitar sus efectos (solo los suyos, no una poción tuya más larga). */
    public static void quitarEfectos(ServerPlayer jugador) {
        for (MobEffect efecto : EFECTOS) {
            MobEffectInstance activo = jugador.getEffect(efecto);
            if (activo != null && activo.isAmbient() && activo.getDuration() <= DURACION_EFECTO) {
                jugador.removeEffect(efecto);
            }
        }
    }
}
