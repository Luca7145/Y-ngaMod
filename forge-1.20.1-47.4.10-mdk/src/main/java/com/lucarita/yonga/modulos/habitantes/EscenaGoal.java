package com.lucarita.yonga.modulos.habitantes;

import com.lucarita.yonga.modulos.habitantes.familia.Encuentro;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Mientras está en una noche juntos (ver familia/Encuentro) no hace nada más: va andando a la cama cuando le toca,
 * se acuesta y se queda quieto hasta que acaba.
 */
public class EscenaGoal extends Goal {

    private final Habitante habitante;
    private int espera;

    public EscenaGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return Encuentro.participa(habitante);
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
        BlockPos cama = Encuentro.irA(habitante);
        if (cama == null) {
            habitante.getNavigation().stop();
            return;
        }
        Vec3 centro = Vec3.atBottomCenterOf(cama);
        if (habitante.distanceToSqr(centro) < 1.8 * 1.8) {
            habitante.getNavigation().stop();
            Encuentro.llego(habitante);
            return;
        }
        if (--espera <= 0) {
            espera = 20;
            habitante.getNavigation().moveTo(centro.x, centro.y, centro.z, 0.6);
        }
    }
}
