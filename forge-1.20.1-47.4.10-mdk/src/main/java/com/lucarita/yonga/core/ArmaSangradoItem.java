package com.lucarita.yonga.core;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

// Extiende de tu clase ArmaParryItem para no perder tus mecánicas de combate
public class ArmaSangradoItem extends ArmaParryItem {

    public ArmaSangradoItem(Tier tier, int dañoExtra, float velocidadAtaque, Properties properties) {
        super(tier, dañoExtra, velocidadAtaque, properties);
    }

    // Este método se ejecuta automáticamente cada vez que golpeas a una entidad
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // Solo lo calculamos del lado del servidor
        if (!attacker.level().isClientSide()) {

            // Genera un número aleatorio entre 0.0 y 1.0 (0% a 100%)
            float random = attacker.getRandom().nextFloat();

            // Usamos else-if para que las probabilidades se sumen sin solaparse:
            // 0.00 a 0.04 = 4% de probabilidad
            if (random <= 0.04f) {
                // Sangrado III (Nivel 2) - Duración Infinita (999999 ticks)
                target.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 999999, 2, false, false, true));
            }
            // 0.04 a 0.14 = 10% de probabilidad
            else if (random <= 0.14f) {
                // Sangrado II (Nivel 1) - Duración: 20 segundos (400 ticks)
                target.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 1200, 1, false, false, true));
            }
            // 0.14 a 0.44 = 30% de probabilidad
            else if (random <= 0.44f) {
                // Sangrado I (Nivel 0) - Duración: 20 segundos (400 ticks)
                target.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 1200, 0, false, false, true));
            }

            // Fíjate que en 'new MobEffectInstance' ponemos los false, false, true del final
            // Esto es lo que le dice al juego: "Cero espirales de poción, pero SÍ muestra el icono".
        }

        // Llamamos al método original para que el arma pierda durabilidad al golpear de forma normal
        return super.hurtEnemy(stack, target, attacker);
    }
}