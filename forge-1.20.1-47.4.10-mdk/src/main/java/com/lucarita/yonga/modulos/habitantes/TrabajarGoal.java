package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * De día, el habitante con oficio va a su puesto y trabaja allí: mira el bloque, saca su herramienta y da golpes
 * de vez en cuando. Mientras está junto al puesto su barra de trabajo se llena (ver Habitante.tick).
 * No trabaja si está atendiendo a alguien o si su tienda está llena.
 */
public class TrabajarGoal extends Goal {

    private static final double CERCA = 2.5 * 2.5;

    private final Habitante habitante;
    private int espera;

    public TrabajarGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return habitante.puedeTrabajar();
    }

    @Override
    public boolean canContinueToUse() {
        return habitante.puedeTrabajar();
    }

    @Override
    public void start() {
        this.espera = 0;
        habitante.setItemSlot(EquipmentSlot.MAINHAND, habitante.oficio() == null ? ItemStack.EMPTY : habitante.oficio().herramienta());
    }

    @Override
    public void stop() {
        habitante.getNavigation().stop();
        if (!habitante.esGuardia()) habitante.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        BlockPos puesto = habitante.puesto();
        if (puesto == null) {
            return;
        }
        Vec3 centro = Vec3.atCenterOf(puesto);
        if (habitante.distanceToSqr(centro) > CERCA) {
            if (--espera <= 0) {
                habitante.getNavigation().moveTo(centro.x, puesto.getY(), centro.z, 0.6);
                espera = 40;
            }
            return;
        }
        habitante.getNavigation().stop();
        habitante.getLookControl().setLookAt(centro.x, centro.y, centro.z);
        habitante.marcarTrabajando();
        if (habitante.tickCount % 30 == 0) {
            habitante.swing(InteractionHand.MAIN_HAND);
        }
    }
}
