package com.lucarita.yonga.modulos.sangrado;

import com.lucarita.yonga.modulos.Modulo;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Lógica compartida para causar sangrado (la usan las armas y el encantamiento Cortante).
 * Otros módulos pueden llamar a esto sin miedo: si el módulo de sangrado está desactivado, no hace nada.
 *
 * Daño: Sangrado I = 1, II = 2, III = 3, cada 4 segundos (ver SangradoEffect).
 * El Sangrado III no se acaba nunca: solo se quita con vendas.
 */
public final class Sangrado {

    /**
     * Probabilidades de cada nivel en un golpe y duración del I y el II (en ticks, 20 = 1 segundo).
     * Las probabilidades son independientes: p. ej. 0.04 / 0.10 / 0.30 = 4% de III, 10% de II, 30% de I.
     */
    public record Tabla(float probNivel3, float probNivel2, float probNivel1, int duracion) {}

    // Duración del Sangrado I y II causado por armas: 30 segundos
    private static final int DURACION_ARMAS = 600;

    // ---- Tablas de cada fuente de sangrado ----
    public static final Tabla PUNAL = new Tabla(0.00f, 0.00f, 0.15f, DURACION_ARMAS);     // débil: solo Sangrado I
    public static final Tabla ALABARDA = new Tabla(0.02f, 0.06f, 0.20f, DURACION_ARMAS);  // medio
    public static final Tabla FALCATA = new Tabla(0.04f, 0.10f, 0.30f, DURACION_ARMAS);   // fuerte: la especialista
    public static final Tabla CORTANTE = new Tabla(0.04f, 0.10f, 0.30f, 400);             // encantamiento, 20 segundos

    private Sangrado() {}

    public static void intentarAplicar(LivingEntity objetivo, RandomSource random, Tabla tabla) {
        if (!Modulo.SANGRADO.activo()) {
            return;
        }

        float tirada = random.nextFloat();
        if (tirada < tabla.probNivel3()) {
            aplicar(objetivo, MobEffectInstance.INFINITE_DURATION, 2);
        } else if (tirada < tabla.probNivel3() + tabla.probNivel2()) {
            aplicar(objetivo, tabla.duracion(), 1);
        } else if (tirada < tabla.probNivel3() + tabla.probNivel2() + tabla.probNivel1()) {
            aplicar(objetivo, tabla.duracion(), 0);
        }
    }

    private static void aplicar(LivingEntity objetivo, int duracion, int nivel) {
        // false, false, true = sin espirales de poción, pero SÍ se muestra el icono
        objetivo.addEffect(new MobEffectInstance(ModuloSangrado.SANGRADO.get(), duracion, nivel, false, false, true));
    }
}
