package com.lucarita.yonga.client;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.client.fe.BarraFe;
import com.lucarita.yonga.client.fe.BarraMana;
import com.lucarita.yonga.client.fe.OrbeFeRenderer;
import com.lucarita.yonga.client.roles.CapaParapente;
import com.lucarita.yonga.client.roles.CapaAntifaz;
import com.lucarita.yonga.client.roles.CapaArmonica;
import com.lucarita.yonga.client.roles.CapaMedallon;
import com.lucarita.yonga.client.roles.MochilaScreen;
import com.lucarita.yonga.client.fe.FeTeclas;
import com.lucarita.yonga.client.roles.RolesTeclas;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.armas.ArmaYonga;
import com.lucarita.yonga.modulos.armas.ModuloArmas;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.HuecoCara;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import com.lucarita.yonga.modulos.sangrado.ModuloSangrado;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
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
    private static final ResourceLocation PROPIEDAD_ABIERTO = new ResourceLocation(YongaMain.MOD_ID, "abierto");

    private ClientSetup() {}

    /** Añade el botón "Config" de Yönga en el menú de Mods. */
    public static void registrarPantallaConfig() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, pantallaAnterior) -> new ModulosScreen(pantallaAnterior)));
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (Modulo.ROLES.activo()) {
            event.enqueueWork(() -> {
                // "yonga:abierto" = 1 mientras se planea: el modelo del objeto cambia a uno invisible en la mano
                ItemProperties.register(ModuloRoles.PARAPENTE.get(), PROPIEDAD_ABIERTO,
                        (stack, nivel, entidad, semilla) ->
                                entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
                // "yonga:blocking" = 1 mientras se levanta el escudo-tambor: cambia al modelo de bloquear
                ItemProperties.register(ModuloRoles.ESCUDO_TAMBOR.get(), PROPIEDAD_GUARDIA,
                        (stack, nivel, entidad, semilla) ->
                                entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
                // Arco-lira: las mismas propiedades que el arco normal para las texturas de tensar
                ItemProperties.register(ModuloRoles.ARCO_LIRA.get(), new ResourceLocation("pull"),
                        (stack, nivel, entidad, semilla) -> entidad == null || entidad.getUseItem() != stack ? 0.0F
                                : (stack.getUseDuration() - entidad.getUseItemRemainingTicks()) / 20.0F);
                ItemProperties.register(ModuloRoles.ARCO_LIRA.get(), new ResourceLocation("pulling"),
                        (stack, nivel, entidad, semilla) ->
                                entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
                // Pantalla de la mochila
                MenuScreens.register(ModuloRoles.MENU_MOCHILA.get(), MochilaScreen::new);
                // El hueco de cara se oculta en el inventario creativo (allí Minecraft lo pondría encima de la barra rápida)
                HuecoCara.ocultar = () -> Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen;
            });
        }
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
            // "Above" la comida = se dibuja justo después, en la fila de encima
            event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(), "barra_mana", BarraMana::dibujarHud);
        }
    }

    @SubscribeEvent
    public static void registrarTeclas(RegisterKeyMappingsEvent event) {
        if (Modulo.FE.activo()) {
            event.register(FeTeclas.REZAR);
            event.register(FeTeclas.BLASFEMAR);
        }
        if (Modulo.ROLES.activo()) {
            event.register(RolesTeclas.SILENCIAR_NOTAS);
            event.register(RolesTeclas.TOCAR_ARMONICA);
        }
    }

    @SubscribeEvent
    public static void registrarRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (Modulo.FE.activo()) {
            event.registerEntityRenderer(ModuloFe.ORBE_FE.get(), OrbeFeRenderer::new);
        }
        if (Modulo.ROLES.activo()) {
            // La nota de la espada-keytar no tiene dibujo: solo su rastro de notitas
            event.registerEntityRenderer(ModuloRoles.NOTA_PROYECTIL.get(), NoopRenderer::new);
        }
    }

    // Modelo 3D de la vela del parapente
    @SubscribeEvent
    public static void registrarModelos(EntityRenderersEvent.RegisterLayerDefinitions event) {
        if (Modulo.ROLES.activo()) {
            event.registerLayerDefinition(CapaParapente.CAPA, CapaParapente::crearCapa);
            event.registerLayerDefinition(CapaAntifaz.CAPA, CapaAntifaz::crearCapa);
            event.registerLayerDefinition(CapaArmonica.CAPA, CapaArmonica::crearCapa);
            event.registerLayerDefinition(CapaMedallon.CAPA, CapaMedallon::crearCapa);
        }
    }

    // Añade la vela a los dos tipos de jugador (brazos anchos y brazos finos)
    @SubscribeEvent
    public static void agregarCapas(EntityRenderersEvent.AddLayers event) {
        if (!Modulo.ROLES.activo()) {
            return;
        }
        for (String tipoSkin : event.getSkins()) {
            PlayerRenderer renderJugador = event.getSkin(tipoSkin);
            if (renderJugador != null) {
                renderJugador.addLayer(new CapaParapente(renderJugador, event.getEntityModels()));
                renderJugador.addLayer(new CapaAntifaz(renderJugador, event.getEntityModels()));
                renderJugador.addLayer(new CapaArmonica(renderJugador, event.getEntityModels()));
                renderJugador.addLayer(new CapaMedallon(renderJugador, event.getEntityModels()));
            }
        }
    }
}
