package com.lucarita.yonga;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ---- PLANTILLAS PARA TUS FUTUROS MÓDULOS ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_EXAMPLE_MODULE;

    static {
        BUILDER.push("Modulos");

        // Copia y pega este bloque para cada módulo nuevo que crees
        ENABLE_EXAMPLE_MODULE = BUILDER
                .comment("Activa o desactiva el contenido de este modulo")
                .define("enableExampleModule", true);

        BUILDER.pop();
    }

    // Construye la configuración final
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Lógica a ejecutar cuando se cargue el archivo (normalmente vacío)
    }
}