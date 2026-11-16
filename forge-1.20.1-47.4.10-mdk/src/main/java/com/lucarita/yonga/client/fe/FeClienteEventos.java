package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.Oracion;
import com.lucarita.yonga.modulos.fe.OracionCliente;
import com.lucarita.yonga.modulos.fe.red.FeRed;
import com.lucarita.yonga.modulos.fe.red.PedirOracionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Eventos del cliente del sistema de fe: botón del inventario, teclas de rezar/blasfemar
 * y bajar el cuerpo del jugador cuando se arrodilla.
 */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FeClienteEventos {

    private static final int ANCHO_BOTON = 24;
    private static final int ALTO_BOTON = 16;
    private static final int ANCHO_INVENTARIO = 176;

    private static Button botonFe;

    // Jugadores a los que hemos bajado el cuerpo en este frame (para deshacerlo después)
    private static final Set<Integer> BAJADOS = new HashSet<>();

    private FeClienteEventos() {}

    /** Pide al servidor rezar (1), blasfemar (-1) o parar (0). Si ya hacías eso mismo, paras. */
    public static void pedirOracion(int direccion) {
        FeRed.CANAL.sendToServer(new PedirOracionPacket(direccion));
    }

    /** ¿Está NUESTRO jugador rezando (1), blasfemando (-1) o nada (0)? */
    public static int miDireccion() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return Oracion.NADA;
        }
        OracionCliente.Estado estado = OracionCliente.estado(minecraft.player.getId());
        return estado == null ? Oracion.NADA : estado.direccion();
    }

    // ---------- BOTÓN EN EL INVENTARIO ----------

    @SubscribeEvent
    public static void alAbrirPantalla(ScreenEvent.Init.Post evento) {
        if (!Modulo.FE.activo() || !(evento.getScreen() instanceof InventoryScreen inventario)) {
            return;
        }

        botonFe = Button.builder(Component.translatable("yonga.fe.boton"),
                        boton -> Minecraft.getInstance().setScreen(new FeScreen()))
                .bounds(posicionX(inventario), posicionY(inventario), ANCHO_BOTON, ALTO_BOTON)
                .tooltip(Tooltip.create(Component.translatable("yonga.fe.menu.titulo")))
                .build();
        evento.addListener(botonFe);
    }

    // Al abrir/cerrar el libro de recetas el inventario se mueve, así que recolocamos el botón cada frame
    @SubscribeEvent
    public static void alDibujarPantalla(ScreenEvent.Render.Pre evento) {
        if (botonFe != null && evento.getScreen() instanceof InventoryScreen inventario) {
            botonFe.setX(posicionX(inventario));
            botonFe.setY(posicionY(inventario));
        }
    }

    // Esquina superior derecha, justo encima del inventario
    private static int posicionX(InventoryScreen inventario) {
        return inventario.getGuiLeft() + ANCHO_INVENTARIO - ANCHO_BOTON;
    }

    private static int posicionY(InventoryScreen inventario) {
        return inventario.getGuiTop() - ALTO_BOTON - 2;
    }

    // ---------- TECLAS ----------

    @SubscribeEvent
    public static void alTickCliente(TickEvent.ClientTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !Modulo.FE.activo()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }
        while (FeTeclas.REZAR.consumeClick()) {
            pedirOracion(Oracion.REZAR);
        }
        while (FeTeclas.BLASFEMAR.consumeClick()) {
            pedirOracion(Oracion.BLASFEMAR);
        }
    }

    // Al salir del mundo olvidamos quién estaba rezando
    @SubscribeEvent
    public static void alDesconectar(ClientPlayerNetworkEvent.LoggingOut evento) {
        OracionCliente.limpiar();
    }

    // ---------- BAJAR EL CUERPO AL ARRODILLARSE ----------

    // Prioridad mínima: así somos los últimos y nadie cancela el evento después de que hayamos movido el modelo
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void antesDeDibujarJugador(RenderPlayerEvent.Pre evento) {
        float f = PoseOracion.progreso(evento.getEntity());
        if (f < 0) {
            return;
        }
        evento.getPoseStack().pushPose();
        evento.getPoseStack().translate(0.0, -PoseOracion.BAJADA_CUERPO * f, 0.0);
        BAJADOS.add(evento.getEntity().getId());
    }

    @SubscribeEvent
    public static void despuesDeDibujarJugador(RenderPlayerEvent.Post evento) {
        if (BAJADOS.remove(evento.getEntity().getId())) {
            evento.getPoseStack().popPose();
        }
    }
}
