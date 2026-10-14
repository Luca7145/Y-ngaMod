package com.lucarita.yonga;

import com.lucarita.yonga.core.ModBlocks;
import com.lucarita.yonga.core.ModCreativeTabs;
import com.lucarita.yonga.core.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(YongaMain.MOD_ID)
public class YongaMain
{
    public static final String MOD_ID = "yonga";
    private static final Logger LOGGER = LogUtils.getLogger();

    public YongaMain(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Registramos nuestros ítems
        ModItems.register(modEventBus);

        ModBlocks.register(modEventBus);
        // Registramos nuestra pestaña creativa
        ModCreativeTabs.register(modEventBus);

        // Aquí registraremos nuestros futuros módulos, ítems y bloques

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);

        // Registra el archivo de configuración modular
        context.registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("Inicializando núcleo de Yonga Mod...");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("Servidor de Yonga Mod iniciado correctamente");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // event.enqueueWork asegura que esto se cargue de forma segura en el cliente
            event.enqueueWork(() -> {

                // Registramos la propiedad "blocking" para la Espada Caballeresca
                net.minecraft.client.renderer.item.ItemProperties.register(ModItems.ESPADA_CABALLERESCA.get(),
                        new net.minecraft.resources.ResourceLocation("yonga", "blocking"),
                        (stack, nivel, entidad, semilla) -> {
                            return entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F;
                        });

                // Hacemos lo mismo para la Ropera
                net.minecraft.client.renderer.item.ItemProperties.register(ModItems.ROPERA.get(),
                        new net.minecraft.resources.ResourceLocation("yonga", "blocking"),
                        (stack, nivel, entidad, semilla) -> {
                            return entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F;
                        });

            });
        }
    }
}