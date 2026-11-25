package com.lucarita.yonga.modulos.habitantes;

import net.minecraft.world.level.Level;

/**
 * El horario de un habitante (hora del día de Minecraft: 0 = 6:00, 6000 = 12:00, 12000 = 18:00, 18000 = 0:00):
 *   6:00 - 8:00    se levanta y pasea
 *   8:00 - 15:00   trabaja en su puesto (TrabajarGoal)
 *   15:00 - 17:30  charla con los vecinos en la plaza, junto a la campana (CharlaGoal)
 *   17:30 - 18:00  vuelve a casa
 *   18:00 - 6:20   duerme en su cama (DormirGoal); los guardias no: hacen la guardia de noche
 */
public final class Rutina {

    private Rutina() {}

    public static long hora(Level nivel) {
        return nivel.getDayTime() % 24000L;
    }

    public static boolean trabajo(Level nivel) {
        long h = hora(nivel);
        return h >= 2000 && h < 9000;
    }

    public static boolean charla(Level nivel) {
        long h = hora(nivel);
        return h >= 9000 && h < 11500;
    }

    public static boolean vueltaACasa(Level nivel) {
        long h = hora(nivel);
        return h >= 11500 && h < 12000;
    }

    public static boolean noche(Level nivel) {
        long h = hora(nivel);
        return h >= 12000 || h < 330;
    }
}
