package com.lucarita.yonga.client;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.client.fe.BarraFe;
import com.lucarita.yonga.client.fe.BarraMana;
import com.lucarita.yonga.client.fe.AstadoRenderer;
import com.lucarita.yonga.client.fe.EnanoRenderer;
import com.lucarita.yonga.client.fe.CentinelaRenderer;
import com.lucarita.yonga.client.fe.DardoRenderer;
import com.lucarita.yonga.client.fe.ModeloPez;
import com.lucarita.yonga.client.fe.PezRenderer;
import com.lucarita.yonga.client.fe.ModeloZor;
import com.lucarita.yonga.client.fe.ModeloZorTramo;
import com.lucarita.yonga.client.fe.ZorRenderer;
import com.lucarita.yonga.client.fe.GargolaRenderer;
import com.lucarita.yonga.client.fe.ModeloGargola;
import com.lucarita.yonga.client.fe.GnomoRenderer;
import com.lucarita.yonga.client.fe.ModeloGnomo;
import com.lucarita.yonga.client.fe.ModeloCentinela;
import com.lucarita.yonga.client.fe.DuendeRenderer;
import com.lucarita.yonga.client.fe.LeonRenderer;
import com.lucarita.yonga.client.fe.ModeloDuende;
import com.lucarita.yonga.client.fe.ModeloAstado;
import com.lucarita.yonga.client.fe.ModeloLeon;
import com.lucarita.yonga.client.fe.SustoAstado;
import com.lucarita.yonga.client.fe.HadaRenderer;
import com.lucarita.yonga.client.fe.ModeloEnano;
import com.lucarita.yonga.client.fe.OrbeFeRenderer;
import com.lucarita.yonga.client.roles.CapaParapente;
import com.lucarita.yonga.client.roles.CapaAntifaz;
import com.lucarita.yonga.client.roles.CapaArmonica;
import com.lucarita.yonga.client.roles.IncensarioRenderer;
import com.lucarita.yonga.client.armas.LanzaRenderer;
import com.lucarita.yonga.client.roles.TaladroScreen;
import com.lucarita.yonga.client.roles.BancoScreen;
import com.lucarita.yonga.modulos.roles.oficios.BotafumeiroItem;
import com.lucarita.yonga.modulos.roles.oficios.TaladroItem;
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
    private static final ResourceLocation PROPIEDAD_USANDO = new ResourceLocation(YongaMain.MOD_ID, "usando");

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
                // Taladro: con combustible se ve encendido
                ItemProperties.register(ModuloRoles.TALADRO.get(), new ResourceLocation(YongaMain.MOD_ID, "cargado"),
                        (stack, nivel, entidad, semilla) -> TaladroItem.cargado(stack) ? 1.0F : 0.0F);
                // Botafumeiro: entero / solo el mango (incensario lanzado) / solo el incensario
                ItemProperties.register(ModuloRoles.BOTAFUMEIRO.get(), new ResourceLocation(YongaMain.MOD_ID, "parte"),
                        (stack, nivel, entidad, semilla) -> BotafumeiroItem.parte(stack));
                // "yonga:usando" = 1 mientras se carga: con el brazo levantado se usa el dibujo sin reflejar
                ItemProperties.register(ModuloRoles.BOTAFUMEIRO.get(), PROPIEDAD_USANDO,
                        (stack, nivel, entidad, semilla) ->
                                entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
                // Pantalla de la mochila
                MenuScreens.register(ModuloRoles.MENU_MOCHILA.get(), MochilaScreen::new);
                MenuScreens.register(ModuloRoles.MENU_TALADRO.get(), TaladroScreen::new);
                MenuScreens.register(ModuloRoles.MENU_BANCO.get(), BancoScreen::new);
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
            ItemProperties.register(ModuloArmas.LANZA.get(), PROPIEDAD_USANDO,
                    (stack, nivel, entidad, semilla) ->
                            entidad != null && entidad.isUsingItem() && entidad.getUseItem() == stack ? 1.0F : 0.0F);
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
            // El susto del Astado, por encima de todo
            event.registerAboveAll("susto_astado", SustoAstado::dibujar);
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
        if (Modulo.ARMAS.activo()) {
            event.registerEntityRenderer(ModuloArmas.LANZA_ENTIDAD.get(), LanzaRenderer::new);
        }
        if (Modulo.FE.activo()) {
            event.registerEntityRenderer(ModuloFe.ORBE_FE.get(), OrbeFeRenderer::new);
            event.registerEntityRenderer(ModuloFe.HADA.get(), HadaRenderer::hada);
            event.registerEntityRenderer(ModuloFe.HADA_OSCURA.get(), HadaRenderer::hadaOscura);
            event.registerEntityRenderer(ModuloFe.ENANO.get(), EnanoRenderer::enano);
            event.registerEntityRenderer(ModuloFe.ENANO_OSCURO.get(), EnanoRenderer::enanoOscuro);
            event.registerEntityRenderer(ModuloFe.ASTADO.get(), AstadoRenderer::new);
            event.registerEntityRenderer(ModuloFe.LEON_ARQUERO.get(), LeonRenderer::new);
            event.registerEntityRenderer(ModuloFe.LEON_ESPADACHIN.get(), LeonRenderer::new);
            event.registerEntityRenderer(ModuloFe.DUENDE.get(), DuendeRenderer::new);
            event.registerEntityRenderer(ModuloFe.CENTINELA.get(), CentinelaRenderer::new);
            event.registerEntityRenderer(ModuloFe.GNOMO.get(), GnomoRenderer::new);
            event.registerEntityRenderer(ModuloFe.GARGOLA.get(), GargolaRenderer::new);
            event.registerEntityRenderer(ModuloFe.PEZ_DEL_VIENTO.get(), PezRenderer::new);
            event.registerEntityRenderer(ModuloFe.PEZ_ILUSION.get(), PezRenderer::new);
            event.registerEntityRenderer(ModuloFe.ZOR.get(), ZorRenderer::new);
            event.registerEntityRenderer(ModuloFe.ENANO_MADRE.get(), EnanoRenderer::madre);
            event.registerEntityRenderer(ModuloFe.ENANO_MADRE_OSCURO.get(), EnanoRenderer::madreOscura);
            event.registerEntityRenderer(ModuloFe.DARDO.get(), DardoRenderer::new);
        }
        if (Modulo.ROLES.activo()) {
            // La nota de la espada-keytar no tiene dibujo: solo su rastro de notitas
            event.registerEntityRenderer(ModuloRoles.NOTA_PROYECTIL.get(), NoopRenderer::new);
            event.registerEntityRenderer(ModuloRoles.INCENSARIO.get(), IncensarioRenderer::new);
        }
    }

    // Modelo 3D de la vela del parapente
    @SubscribeEvent
    public static void registrarModelos(EntityRenderersEvent.RegisterLayerDefinitions event) {
        if (Modulo.FE.activo()) {
            event.registerLayerDefinition(ModeloEnano.CAPA, ModeloEnano::crearCapa);
            event.registerLayerDefinition(ModeloAstado.CAPA, ModeloAstado::crearCapa);
            event.registerLayerDefinition(ModeloLeon.CAPA, ModeloLeon::crearCapa);
            event.registerLayerDefinition(ModeloDuende.CAPA, ModeloDuende::crearCapa);
            event.registerLayerDefinition(ModeloCentinela.CAPA, ModeloCentinela::crearCapa);
            event.registerLayerDefinition(ModeloGnomo.CAPA, ModeloGnomo::crearCapa);
            event.registerLayerDefinition(ModeloGargola.CAPA, ModeloGargola::crearCapa);
            event.registerLayerDefinition(ModeloPez.CAPA, ModeloPez::crearCapa);
            event.registerLayerDefinition(ModeloZor.CAPA, ModeloZor::crearCapa);
            event.registerLayerDefinition(ModeloZorTramo.CAPA, ModeloZorTramo::crearCapa);
        }
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
