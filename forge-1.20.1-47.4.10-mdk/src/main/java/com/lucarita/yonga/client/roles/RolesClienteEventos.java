package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.RolCliente;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import com.lucarita.yonga.modulos.roles.red.SilenciarNotasPacket;
import com.lucarita.yonga.modulos.roles.red.TocarArmonicaPacket;
import com.lucarita.yonga.modulos.roles.cara.CaraCliente;
import com.lucarita.yonga.modulos.roles.cara.HuecoCara;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eventos del cliente del sistema de roles: botón "Oficio" en el inventario
 * y abrir el menú de elegir oficio cuando el servidor lo pide (primera vez que entras).
 */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RolesClienteEventos {

    private static final int ANCHO_BOTON = 44;
    private static final int ALTO_BOTON = 16;

    private static Button botonOficio;

    private RolesClienteEventos() {}

    // ---------- BOTÓN EN EL INVENTARIO (esquina superior izquierda; el de Fe está a la derecha) ----------

    @SubscribeEvent
    public static void alAbrirPantalla(ScreenEvent.Init.Post evento) {
        if (!Modulo.ROLES.activo() || !(evento.getScreen() instanceof InventoryScreen inventario)) {
            return;
        }
        botonOficio = Button.builder(Component.translatable("yonga.rol.boton"),
                        boton -> Minecraft.getInstance().setScreen(new RolScreen(RolCliente.rol() == null)))
                .bounds(posicionX(inventario), posicionY(inventario), ANCHO_BOTON, ALTO_BOTON)
                .tooltip(Tooltip.create(Component.translatable("yonga.rol.menu.titulo")))
                .build();
        evento.addListener(botonOficio);
    }

    @SubscribeEvent
    public static void alDibujarPantalla(ScreenEvent.Render.Pre evento) {
        if (botonOficio != null && evento.getScreen() instanceof InventoryScreen inventario) {
            botonOficio.setX(posicionX(inventario));
            botonOficio.setY(posicionY(inventario));
        }
    }

    private static int posicionX(InventoryScreen inventario) {
        return inventario.getGuiLeft();
    }

    private static int posicionY(InventoryScreen inventario) {
        return inventario.getGuiTop() - ALTO_BOTON - 2;
    }

    // ---------- HUECO DE CARA ----------

    // La textura del inventario no tiene marco en ese sitio: lo dibujamos (antes que los objetos)
    @SubscribeEvent
    public static void alDibujarFondo(ContainerScreenEvent.Render.Background evento) {
        if (!Modulo.ROLES.activo() || !(evento.getContainerScreen() instanceof InventoryScreen inventario)) {
            return;
        }
        GuiGraphics graficos = evento.getGuiGraphics();
        int x = inventario.getGuiLeft() + HuecoCara.X - 1;
        int y = inventario.getGuiTop() + HuecoCara.Y - 1;
        // Marco de hueco igual que los de Minecraft: sombra arriba-izquierda, brillo abajo-derecha
        graficos.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graficos.fill(x, y, x + 17, y + 1, 0xFF373737);
        graficos.fill(x, y, x + 1, y + 17, 0xFF373737);
        graficos.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graficos.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
    }

    // ---------- MENÚ AL ENTRAR ----------

    // Esperamos a que no haya ninguna pantalla abierta (p. ej. "Cargando terreno") para abrir el menú
    @SubscribeEvent
    public static void alTickCliente(TickEvent.ClientTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !Modulo.ROLES.activo()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.screen == null && RolCliente.consumirPeticionMenu()) {
            minecraft.setScreen(new RolScreen(RolCliente.rol() == null));
        }
        if (minecraft.player != null && minecraft.screen == null) {
            while (RolesTeclas.SILENCIAR_NOTAS.consumeClick()) {
                RolesRed.CANAL.sendToServer(new SilenciarNotasPacket());
            }
            while (RolesTeclas.TOCAR_ARMONICA.consumeClick()) {
                RolesRed.CANAL.sendToServer(new TocarArmonicaPacket());
            }
        }
    }

    @SubscribeEvent
    public static void alDesconectar(ClientPlayerNetworkEvent.LoggingOut evento) {
        RolCliente.limpiar();
        CaraCliente.limpiar();
    }
}
