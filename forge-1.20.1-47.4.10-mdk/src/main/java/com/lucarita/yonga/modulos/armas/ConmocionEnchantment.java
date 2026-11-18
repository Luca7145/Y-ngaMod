package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.core.GolpeCargado;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Encantamiento Conmoción (I, II, III): probabilidad de ATURDIR al golpear, como la maza.
 * El aturdimiento es el mismo de la maza (1,5 s sin moverse, saltar ni atacar, y luego 4 s sin que se le
 * pueda volver a aturdir). Solo con golpes cargados.
 *
 *   Conmoción I: 4%    II: 7%    III: 10%      (la maza: 20%)
 *
 * No se puede juntar con Cortante en la misma arma (o sangra o aturde).
 */
public class ConmocionEnchantment extends Enchantment {

    private static final float[] PROBABILIDAD = {0.04f, 0.07f, 0.10f};

    public ConmocionEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinCost(int nivel) {
        return 15 + (nivel - 1) * 9;
    }

    @Override
    public int getMaxCost(int nivel) {
        return getMinCost(nivel) + 20;
    }

    // Espadas y hachas (y las armas del mod, que son espadas)
    @Override
    public boolean canEnchant(ItemStack objeto) {
        return objeto.getItem() instanceof SwordItem || objeto.getItem() instanceof AxeItem || super.canEnchant(objeto);
    }

    // No con Cortante (si el módulo de sangrado no está, no hay Cortante y no pasa nada)
    @Override
    protected boolean checkCompatibility(Enchantment otro) {
        var id = ForgeRegistries.ENCHANTMENTS.getKey(otro);
        return super.checkCompatibility(otro) && (id == null || !id.toString().equals("yonga:cortante"));
    }

    @Override
    public void doPostAttack(LivingEntity atacante, Entity objetivo, int nivel) {
        if (!atacante.level().isClientSide() && objetivo instanceof LivingEntity vivo && GolpeCargado.fue(atacante)
                && atacante.getRandom().nextFloat() < PROBABILIDAD[Math.min(nivel, 3) - 1]) {
            Aturdimiento.aturdir(vivo);
        }
        super.doPostAttack(atacante, objetivo, nivel);
    }
}
