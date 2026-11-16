package com.lucarita.yonga.client;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.client.fe.BarraFe;
import com.lucarita.yonga.client.fe.FeTeclas;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.armas.ArmaYonga;
import com.lucarita.yonga.modulos.armas.ModuloArmas;
import com.lucarita.yonga.modulos.sangrado.ModuloSangrado;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Todo lo que solo existe en el cliente (gráficos, pantallas...).
 */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private static final ResourceLocation PROPIEDAD_GUARDIA = new ResourceLocation(YongaMain.MOD_ID, "blocking");

    private ClientSetup() {}

    /** Añade el botón "Config" de Yönga en el menú de Mods. */
    public static void registrarPantallaConfig() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, pantallaAnterior) -> new ModulosScreen(pantallaAnterior)));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!Modulo.ARMAS.activo()) {
            return;
        }
        event.enqueueWork(() -> {
            // Propiedad "yonga:blocking" (1 mientras se hace guardia) para TODAS las armas con parry.
            // El JSON del modelo la usa para cambiar al modelo de guardia (p. ej. espada_caballeresca_guardia).
            ModuloArmas.ITEMS.getEntries().forEach(registro -> {
                if (registro.get() instanceof ArmaYonga arma && arma.tieneParry()) {
                    ItemProperties.register(arma, PROPIEDAD_GUARDIA,
                            (stack, nivel, entidad, semilla) ->
                                    entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
                }
            });
        });
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        if (Modulo.SANGRADO.activo()) {
            event.registerSpriteSet(ModuloSangrado.SANGRE_GOTEO.get(), SangreGoteoParticle.Provider::new);
        }
    }

    @SubscribeEvent
    public static void registrarOverlays(RegisterGuiOverlaysEvent event) {
        if (Modulo.FE.activo()) {
            // "Below" = se dibuja ANTES que los corazones, para poder subirlos y hacer hueco a la barra
            event.registerBelow(VanillaGuiOverlay.PLAYER_HEALTH.id(), "barra_fe", BarraFe::dibujarHud);
        }
    }

    @SubscribeEvent
    public static void registrarTeclas(RegisterKeyMappingsEvent event) {
        if (Modulo.FE.activo()) {
            event.register(FeTeclas.REZAR);
            event.register(FeTeclas.BLASFEMAR);
        }
    }
}
