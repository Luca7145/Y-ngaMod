package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * Por la tarde los vecinos se juntan en la plaza, junto a la campana: se acercan a otro vecino, se miran y de vez en
 * cuando se dicen algo (frases sencillas, sin IA, en un bocadillo). Los niños corretean por la plaza.
 */
public class CharlaGoal extends Goal {

    private final Habitante habitante;
    private Habitante con;
    private int espera;
    private int cambio;

    public CharlaGoal(Habitante habitante) {
        this.habitante = habitante;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean toca() {
        return habitante.etapa() >= 1 && !habitante.esGuardia() && habitante.aldea() != null
                && habitante.enemigo() == null && habitante.seguido() == null && habitante.interlocutor() == null
                && habitante.cliente() == null && Rutina.charla(habitante.level());
    }

    @Override
    public boolean canUse() {
        return toca() && habitante.getRandom().nextInt(10) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return toca();
    }

    @Override
    public void start() {
        this.con = null;
        this.espera = 0;
        this.cambio = 0;
    }

    @Override
    public void stop() {
        this.con = null;
        habitante.getNavigation().stop();
    }

    @Override
    public void tick() {
        BlockPos campana = habitante.aldea();
        if (campana == null) return;
        if (--cambio <= 0 || con == null || !con.isAlive() || con.distanceToSqr(Vec3.atCenterOf(campana)) > 20 * 20) {
            cambio = 400 + habitante.getRandom().nextInt(400);
            List<Habitante> cerca = habitante.level().getEntitiesOfClass(Habitante.class,
                    new AABB(campana).inflate(16), v -> v != habitante && v.etapa() >= 1);
            Habitante pareja = com.lucarita.yonga.modulos.habitantes.familia.Amor.parejaEntre(habitante, cerca);
            con = pareja != null ? pareja : cerca.isEmpty() ? null : cerca.get(habitante.getRandom().nextInt(cerca.size()));
        }
        Vec3 destino = con != null ? con.position() : Vec3.atBottomCenterOf(campana);
        double d = habitante.distanceToSqr(destino);
        if (d > 2.5 * 2.5) {
            if (--espera <= 0) {
                espera = 30;
                habitante.getNavigation().moveTo(destino.x, destino.y, destino.z, habitante.etapa() == 1 ? 0.7 : 0.5);
            }
        } else {
            habitante.getNavigation().stop();
            if (con != null) {
                habitante.getLookControl().setLookAt(con, 30.0f, 30.0f);
                // De vez en cuando le dice algo (cada ~15 s de media)
                if (habitante.getRandom().nextInt(300) == 0 && habitante.etapa() >= 2) {
                    com.lucarita.yonga.modulos.habitantes.familia.Amor.charlan(habitante, con);
                }
                if (habitante.getRandom().nextInt(300) == 0 && habitante.etapa() >= 2 && !habitante.cotillearCon(con)) {
                    String tipo = habitante.getRandom().nextInt(3) == 0 && habitante.oficio() != null ? "oficio" : "plaza";
                    habitante.decirEnAlto(Component.translatable("yonga.charla." + tipo + "." + habitante.getRandom().nextInt(6)));
                }
            }
        }
    }
}
