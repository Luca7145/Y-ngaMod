package com.lucarita.yonga.core;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class CortanteEnchantment extends Enchantment {

    public CortanteEnchantment(Rarity rareza, EnchantmentCategory categoria, EquipmentSlot... ranuras) {
        super(rareza, categoria, ranuras);
    }

    // Nivel máximo del encantamiento (solo habrá "Cortante I")
    @Override
    public int getMaxLevel() {
        return 1;
    }

    // Forzamos a que solo se pueda aplicar a Espadas y Hachas
    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || super.canEnchant(stack);
    }

    // Este método se dispara automáticamente cuando el jugador golpea a algo con el encantamiento
    @Override
    public void doPostAttack(LivingEntity atacante, Entity objetivo, int nivel) {
        // Verificamos que estamos en el servidor y que el objetivo está vivo (no es un bote o un cuadro)
        if (!atacante.level().isClientSide() && objetivo instanceof LivingEntity objetivoVivo) {

            float random = atacante.getRandom().nextFloat();

            // Misma lógica de probabilidad que hicimos para las armas especiales
            if (random <= 0.04f) {
                objetivoVivo.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 999999, 2, false, false, true));
            } else if (random <= 0.14f) {
                objetivoVivo.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 400, 1, false, false, true));
            } else if (random <= 0.44f) {
                objetivoVivo.addEffect(new MobEffectInstance(ModEffects.SANGRADO.get(), 400, 0, false, false, true));
            }
        }

        super.doPostAttack(atacante, objetivo, nivel);
    }
}