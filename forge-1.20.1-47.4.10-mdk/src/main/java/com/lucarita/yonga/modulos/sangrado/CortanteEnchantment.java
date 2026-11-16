package com.lucarita.yonga.modulos.sangrado;

import com.lucarita.yonga.core.GolpeCargado;
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
        // Solo con golpes cargados, igual que las habilidades de las armas
        if (!atacante.level().isClientSide() && objetivo instanceof LivingEntity objetivoVivo && GolpeCargado.fue(atacante)) {
            // Misma lógica de probabilidad que la falcata, pero con duración más corta
            Sangrado.intentarAplicar(objetivoVivo, atacante.getRandom(), Sangrado.CORTANTE);
        }

        super.doPostAttack(atacante, objetivo, nivel);
    }
}