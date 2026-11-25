package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** Le han pedido que se quede quieto: vuelve a su sitio si se ha movido y de ahí no se mueve (solo mira). */
public class QuietoGoal extends Goal {

    private final Habitante habitante;

    public QuietoGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return habitante.quieto() != null && habitante.enemigo() == null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos sitio = habitante.quieto();
        if (sitio == null) return;
        Vec3 c = Vec3.atBottomCenterOf(sitio);
        if (habitante.distanceToSqr(c) > 1.2 * 1.2) {
            if (habitante.getNavigation().isDone()) habitante.getNavigation().moveTo(c.x, c.y, c.z, 0.5);
        } else {
            habitante.getNavigation().stop();
        }
    }
}
