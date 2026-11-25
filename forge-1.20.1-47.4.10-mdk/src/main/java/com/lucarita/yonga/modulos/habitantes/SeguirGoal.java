package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Seguir a un jugador amigo (lo acepta hablando, si le aprecia lo bastante), o un bebé a su madre. Va tras él andando,
 * y casi corriendo si se aleja; cerca, se queda a su lado mirándole. Si un jugador se queda a más de 48 bloques (o en
 * otra dimensión), lo pierde y deja de seguirle; un bebé que se queda atrás aparece junto a su madre (la lleva en
 * brazos, se supone). Mientras sigue a alguien no trabaja ni pasea.
 */
public class SeguirGoal extends Goal {

    private final Habitante habitante;
    private int espera;

    public SeguirGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity seguido = habitante.seguido();
        if (seguido == null || habitante.enemigo() != null || habitante.isSleeping()) return false;
        if (seguido.level() != habitante.level() || seguido.distanceToSqr(habitante) > 48 * 48) {
            if (habitante.sigueAFamilia()) {
                if (seguido.level() == habitante.level()) habitante.teleportTo(seguido.getX(), seguido.getY(), seguido.getZ());
                return false;
            }
            habitante.dejarDeSeguir();
            if (seguido instanceof Player jugador) {
                jugador.displayClientMessage(net.minecraft.network.chat.Component.translatable("yonga.habla.perdido",
                        habitante.nombrePara(jugador)), true);
            }
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.espera = 0;
    }

    @Override
    public void stop() {
        habitante.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity seguido = habitante.seguido();
        if (seguido == null) return;
        habitante.getLookControl().setLookAt(seguido, 20.0f, 20.0f);
        double distancia = habitante.distanceToSqr(seguido);
        if (habitante.sigueAFamilia() && distancia > 20 * 20) {           // el bebé se ha quedado muy atrás
            habitante.teleportTo(seguido.getX(), seguido.getY(), seguido.getZ());
            return;
        }
        if (distancia < (habitante.sigueAFamilia() ? 2.0 * 2.0 : 3.0 * 3.0)) {
            habitante.getNavigation().stop();
            return;
        }
        if (--espera <= 0) {
            espera = 10;
            habitante.getNavigation().moveTo(seguido, distancia > 12 * 12 ? 0.9 : 0.65);
        }
    }
}
