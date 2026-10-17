package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, YongaMain.MOD_ID);

    // Registramos el Sangrado. Categoría HARMFUL (Negativo) y color rojo oscuro intenso (0x8A0303) para las partículas de las pociones.
    public static final RegistryObject<MobEffect> SANGRADO = MOB_EFFECTS.register("sangrado",
            () -> new SangradoEffect(MobEffectCategory.HARMFUL, 0x8A0303));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}