package com.lucarita.yonga.modulos.sangrado;

import com.lucarita.yonga.YongaMain;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de sangrado: el efecto, su partícula, el encantamiento Cortante y las vendas.
 * Solo se registra si el módulo está activado (ver YongaMain).
 */
public final class ModuloSangrado {

    public static final DeferredRegister<MobEffect> EFECTOS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, YongaMain.MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICULAS =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<Enchantment> ENCANTAMIENTOS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, YongaMain.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);

    // Efecto negativo, color rojo oscuro (0x8A0303)
    public static final RegistryObject<MobEffect> SANGRADO = EFECTOS.register("sangrado",
            () -> new SangradoEffect(MobEffectCategory.HARMFUL, 0x8A0303));

    public static final RegistryObject<SimpleParticleType> SANGRE_GOTEO =
            PARTICULAS.register("sangre_goteo", () -> new SimpleParticleType(true));

    // Encantamiento raro, para armas, activo en la mano principal
    public static final RegistryObject<Enchantment> CORTANTE = ENCANTAMIENTOS.register("cortante",
            () -> new CortanteEnchantment(Enchantment.Rarity.RARE, EnchantmentCategory.WEAPON, EquipmentSlot.MAINHAND));

    public static final RegistryObject<Item> VENDAS = ITEMS.register("vendas",
            () -> new VendaItem(new Item.Properties()
                    .food(new FoodProperties.Builder().alwaysEat().build())));

    private ModuloSangrado() {}

    public static void registrar(IEventBus modEventBus) {
        EFECTOS.register(modEventBus);
        PARTICULAS.register(modEventBus);
        ENCANTAMIENTOS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
