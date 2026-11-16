package com.lucarita.yonga;

import com.lucarita.yonga.client.ClientSetup;
import com.lucarita.yonga.core.ModBlocks;
import com.lucarita.yonga.core.ModCreativeTabs;
import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.ModuloActivoCondition;
import com.lucarita.yonga.modulos.ModulosConfig;
import com.lucarita.yonga.modulos.armas.ModuloArmas;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.sangrado.ModuloSangrado;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(YongaMain.MOD_ID)
public class YongaMain
{
    public static final String MOD_ID = "yonga";
    private static final Logger LOGGER = LogUtils.getLogger();

    public YongaMain(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // 1. Lo primero: leer qué módulos están activados (config/yonga-modulos.toml)
        ModulosConfig.cargar();

        // 2. Núcleo: siempre se carga
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Condición "yonga:modulo_activo" para que las recetas de un módulo desactivado no se carguen
        CraftingHelper.register(ModuloActivoCondition.Serializer.INSTANCE);

        // 3. Módulos: solo se registran si están activados
        if (Modulo.ARMAS.activo()) {
            ModuloArmas.registrar(modEventBus);
        }
        if (Modulo.SANGRADO.activo()) {
            ModuloSangrado.registrar(modEventBus);
        }
        if (Modulo.FE.activo()) {
            ModuloFe.registrar(modEventBus);
        }
        // Futuros módulos aquí, siguiendo el mismo patrón

        for (Modulo modulo : Modulo.values()) {
            LOGGER.info("Yonga - modulo '{}': {}", modulo.id(), modulo.activo() ? "ACTIVADO" : "desactivado");
        }

        // 4. Cosas solo de cliente (pantalla de configuración en el menú de Mods)
        if (FMLEnvironment.dist.isClient()) {
            ClientSetup.registrarPantallaConfig();
        }
    }
}
