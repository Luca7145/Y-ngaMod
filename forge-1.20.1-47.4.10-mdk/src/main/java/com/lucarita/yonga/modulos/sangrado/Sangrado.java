package com.lucarita.yonga.modulos.sangrado;

import com.lucarita.yonga.modulos.Modulo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lógica compartida para causar sangrado (la usan las armas y el encantamiento Cortante).
 * Otros módulos pueden llamar a esto sin miedo: si el módulo de sangrado está desactivado, no hace nada.
 */
public final class Sangrado {

    // Duración del Sangrado I y II según quién lo cause (20 ticks = 1 segundo)
    public static final int DURACION_ARMAS = 1200;    // 60 segundos
    public static final int DURACION_CORTANTE = 400;  // 20 segundos

    // Probabilidades acumuladas de cada nivel
    private static final float PROB_NIVEL_3 = 0.04f;  // 4%  -> Sangrado III, infinito
    private static final float PROB_NIVEL_2 = 0.14f;  // 10% -> Sangrado II
    private static final float PROB_NIVEL_1 = 0.44f;  // 30% -> Sangrado I

    private Sangrado() {}

    public static void intentarAplicar(LivingEntity objetivo, RandomSource random, int duracion) {
        if (!Modulo.SANGRADO.activo()) {
            return;
        }

        float tirada = random.nextFloat();
        if (tirada < PROB_NIVEL_3) {
            aplicar(objetivo, MobEffectInstance.INFINITE_DURATION, 2);
        } else if (tirada < PROB_NIVEL_2) {
            aplicar(objetivo, duracion, 1);
        } else if (tirada < PROB_NIVEL_1) {
            aplicar(objetivo, duracion, 0);
        }
    }

    private static void aplicar(LivingEntity objetivo, int duracion, int nivel) {
        // false, false, true = sin espirales de poción, pero SÍ se muestra el icono
        objetivo.addEffect(new MobEffectInstance(ModuloSangrado.SANGRADO.get(), duracion, nivel, false, false, true));
    }
}
