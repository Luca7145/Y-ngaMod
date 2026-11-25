package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Por la noche (y un poco antes, para volver a casa) va a su cama y se acuesta; al amanecer se levanta. Si le hacen
 * daño se despierta (lo hace Minecraft). Los guardias no duermen. La cama la reserva como los aldeanos (ver
 * Habitante.revisarCama): nadie más la usa.
 */
public class DormirGoal extends Goal {

    private final Habitante habitante;
    private int espera;

    public DormirGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean toca() {
        return !habitante.esGuardia() && habitante.cama() != null && habitante.enemigo() == null
                && habitante.seguido() == null && habitante.interlocutor() == null && habitante.cliente() == null
                && (Rutina.noche(habitante.level()) || Rutina.vueltaACasa(habitante.level()));
    }

    @Override
    public boolean canUse() {
        return toca();
    }

    @Override
    public boolean canContinueToUse() {
        return toca();
    }

    @Override
    public void start() {
        this.espera = 0;
    }

    @Override
    public void stop() {
        if (habitante.isSleeping()) {
            habitante.stopSleeping();
        }
        habitante.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos cama = habitante.cama();
        if (cama == null || habitante.isSleeping()) return;
        Vec3 centro = Vec3.atBottomCenterOf(cama);
        if (habitante.distanceToSqr(centro) > 1.8 * 1.8) {
            if (--espera <= 0) {
                espera = 20;
                habitante.getNavigation().moveTo(centro.x, centro.y, centro.z, 0.55);
            }
            return;
        }
        // Solo se acuesta de noche (al volver a casa, espera junto a la cama)
        if (Rutina.noche(habitante.level()) && habitante.level().getBlockState(cama).getBlock() instanceof BedBlock
                && !habitante.level().getBlockState(cama).getValue(BedBlock.OCCUPIED)) {
            habitante.getNavigation().stop();
            habitante.startSleeping(cama);
        }
    }
}
