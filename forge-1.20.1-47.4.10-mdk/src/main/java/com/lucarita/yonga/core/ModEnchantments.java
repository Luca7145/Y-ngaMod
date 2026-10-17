package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, YongaMain.MOD_ID);

    // Registramos "Cortante" como un encantamiento Raro, para armas, y que se aplica cuando lo tienes en la Mano Principal
    public static final RegistryObject<Enchantment> CORTANTE = ENCHANTMENTS.register("cortante",
            () -> new CortanteEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}