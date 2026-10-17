package com.lucarita.yonga.client;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.core.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventBusSubscriber {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Enlaza nuestra partícula registrada con la clase visual que acabamos de crear
        event.registerSpriteSet(ModParticles.SANGRE_GOTEO.get(), SangreGoteoParticle.Provider::new);
    }
}