package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, YongaMain.MOD_ID);

    // Registramos nuestra partícula simple
    public static final RegistryObject<SimpleParticleType> SANGRE_GOTEO =
            PARTICLES.register("sangre_goteo", () -> new SimpleParticleType(true));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}