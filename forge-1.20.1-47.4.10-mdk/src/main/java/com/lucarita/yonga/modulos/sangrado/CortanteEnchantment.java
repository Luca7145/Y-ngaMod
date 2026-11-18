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

    // Cortante I, II y III (cada nivel, más probabilidad y sangrados más fuertes: ver Sangrado.cortante)
    @Override
    public int getMaxLevel() {
        return 3;
    }

    // Lo que cuesta en la mesa de encantamientos (como los encantamientos raros de Minecraft)
    @Override
    public int getMinCost(int nivel) {
        return 15 + (nivel - 1) * 9;
    }

    @Override
    public int getMaxCost(int nivel) {
        return getMinCost(nivel) + 20;
    }

    // Forzamos a que solo se pueda aplicar a Espadas y Hachas
    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || super.canEnchant(stack);
    }

    // Este método se dispara automáticamente cuando el jugador golpea a algo con el encantamiento
    // No con Conmoción (o sangra o aturde)
    @Override
    protected boolean checkCompatibility(net.minecraft.world.item.enchantment.Enchantment otro) {
        var id = net.minecraftforge.registries.ForgeRegistries.ENCHANTMENTS.getKey(otro);
        return super.checkCompatibility(otro) && (id == null || !id.toString().equals("yonga:conmocion"));
    }

    @Override
    public void doPostAttack(LivingEntity atacante, Entity objetivo, int nivel) {
        // Verificamos que estamos en el servidor y que el objetivo está vivo (no es un bote o un cuadro)
        // Solo con golpes cargados, igual que las habilidades de las armas
        if (!atacante.level().isClientSide() && objetivo instanceof LivingEntity objetivoVivo && GolpeCargado.fue(atacante)) {
            // Misma lógica que las armas con sangrado, con la tabla de su nivel
            Sangrado.intentarAplicar(objetivoVivo, atacante.getRandom(), Sangrado.cortante(nivel));
        }

        super.doPostAttack(atacante, objetivo, nivel);
    }
}