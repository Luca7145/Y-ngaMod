package com.lucarita.yonga.modulos.habitantes.aldea;

import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.HabitantesConfig;
import com.lucarita.yonga.modulos.habitantes.SustitucionAldeanos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * La aldea como sociedad (fase 4):
 *   · Todos los monstruos hostiles (zombis, esqueletos, arañas, saqueadores de las incursiones…) van a por los
 *     habitantes; los creepers, los endermans y los piglins zombificados, no. Un habitante
 *     muerto por un zombi puede volver como aldeano zombi (50 % en normal, siempre en difícil); curado, vuelve a ser
 *     habitante.
 *   · Los gólems de hierro de las aldeas se cambian por guardias; los que fabrican los jugadores se quedan.
 *   · Fuera el comerciante errante y sus llamas.
 *   · FAMA: pegar a un vecino −5, matarlo −40 (y los guardias cercanos van a por ti); matar monstruos o saqueadores
 *     cerca de la aldea +2 / +3; comprar +1 (ver Habitante.comprar).
 */
public final class Sociedad {

    private Sociedad() {}

    private static final int AVISO_GUARDIAS = 32;

    public static void alEntrar(EntityJoinLevelEvent evento) {
        if (!(evento.getLevel() instanceof ServerLevel nivel)) return;
        var entidad = evento.getEntity();

        if (entidad instanceof net.minecraft.world.entity.monster.Monster monstruo && !(entidad instanceof ZombifiedPiglin)
                && !(entidad instanceof net.minecraft.world.entity.monster.Creeper)
                && !(entidad instanceof net.minecraft.world.entity.monster.EnderMan)
                && !(entidad instanceof net.minecraft.world.entity.monster.piglin.AbstractPiglin)) {
            // Como a los aldeanos (y más): van a por ellos igual que a por los jugadores
            monstruo.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monstruo, Habitante.class,
                    !(entidad instanceof Zombie)));
        } else if (entidad instanceof IronGolem golem && !golem.isPlayerCreated() && HabitantesConfig.GUARDIAS_POR_GOLEMS.get()) {
            evento.setCanceled(true);
            SustitucionAldeanos.guardiaEn(nivel, golem.position(), golem.getYRot());
        } else if ((entidad.getType() == EntityType.WANDERING_TRADER || entidad.getType() == EntityType.TRADER_LLAMA)
                && HabitantesConfig.QUITAR_COMERCIANTE.get()) {
            evento.setCanceled(true);
        }
    }

    public static void alHerir(LivingHurtEvent evento) {
        if (!(evento.getEntity() instanceof Habitante victima) || !(evento.getSource().getEntity() instanceof ServerPlayer jugador)) {
            return;
        }
        if (jugador.isCreative() || !(victima.level() instanceof ServerLevel nivel)) return;
        if (victima.aldea() != null) {
            Aldeas.de(nivel).cambiarFama(victima.aldea(), jugador.getUUID(), -5);
            String quien = com.lucarita.yonga.modulos.habitantes.familia.Amor.nombreDe(nivel, jugador);
            Chismes.contar(nivel, victima, quien, jugador.getUUID(), quien + " le ha pegado a " + victima.nombre(), false);
        }
        llamarGuardias(nivel, victima, jugador);
    }

    public static void alMorir(LivingDeathEvent evento) {
        LivingEntity muerto = evento.getEntity();
        if (!(muerto.level() instanceof ServerLevel nivel)) return;
        var asesino = evento.getSource().getEntity();

        if (muerto instanceof Habitante victima) {
            // Una muerte es noticia: la saben todos los que estén cerca
            if (victima.aldea() != null && victima.etapa() >= 1) {
                if (asesino instanceof ServerPlayer j) {
                    String quien = com.lucarita.yonga.modulos.habitantes.familia.Amor.nombreDe(nivel, j);
                    Chismes.contar(nivel, victima, quien, j.getUUID(), quien + " ha matado a " + victima.nombre(), true);
                } else {
                    Chismes.contar(nivel, victima, victima.nombre(), null, victima.nombre() + " ha muerto"
                            + (asesino instanceof Zombie ? " a manos de los zombis" : ""), true);
                }
            }
            if (asesino instanceof ServerPlayer jugador && !jugador.isCreative() && victima.aldea() != null) {
                Aldeas.de(nivel).cambiarFama(victima.aldea(), jugador.getUUID(), -40);
                llamarGuardias(nivel, victima, jugador);
            }
            if (asesino instanceof Zombie && !(asesino instanceof ZombifiedPiglin) && victima.etapa() >= 1) {
                Difficulty d = nivel.getDifficulty();
                if (d == Difficulty.HARD || d == Difficulty.NORMAL && nivel.random.nextBoolean()) {
                    zombificar(nivel, victima);
                }
            }
            return;
        }
        // Defender la aldea da buena fama
        if (asesino instanceof ServerPlayer jugador && (muerto instanceof Enemy || muerto instanceof Raider)) {
            BlockPos campana = Aldeas.campanaCerca(nivel, muerto.blockPosition());
            if (campana != null) {
                Aldeas.de(nivel).cambiarFama(campana, jugador.getUUID(), muerto instanceof Raider ? 3 : 2);
            }
        }
    }

    /** Los guardias cercanos (32 bloques) van a por quien ha atacado a un vecino. */
    private static void llamarGuardias(ServerLevel nivel, Habitante victima, ServerPlayer jugador) {
        for (Habitante guardia : nivel.getEntitiesOfClass(Habitante.class,
                new AABB(victima.blockPosition()).inflate(AVISO_GUARDIAS), Habitante::esGuardia)) {
            if (guardia != victima) guardia.setTarget(jugador);
        }
    }

    private static void zombificar(ServerLevel nivel, Habitante victima) {
        ZombieVillager zombi = EntityType.ZOMBIE_VILLAGER.create(nivel);
        if (zombi == null) return;
        zombi.moveTo(victima.getX(), victima.getY(), victima.getZ(), victima.getYRot(), victima.getXRot());
        zombi.finalizeSpawn(nivel, nivel.getCurrentDifficultyAt(zombi.blockPosition()), MobSpawnType.CONVERSION, null, null);
        zombi.setBaby(victima.etapa() < 2);
        if (victima.hasCustomName()) zombi.setCustomName(victima.getCustomName());
        zombi.setPersistenceRequired();
        nivel.addFreshEntity(zombi);
    }
}
