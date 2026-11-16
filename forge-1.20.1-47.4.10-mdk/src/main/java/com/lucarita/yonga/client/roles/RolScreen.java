package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolCliente;
import com.lucarita.yonga.modulos.roles.red.ElegirRolPacket;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Menú de oficios.
 *  - La primera vez (sin oficio) sale solo al entrar al mundo y no se puede cerrar sin elegir.
 *  - Después se abre con el botón "Oficio" del inventario: muestra tu oficio, rango y experiencia,
 *    y te deja cambiar de oficio (con confirmación, porque pierdes el rango).
 */
public class RolScreen extends Screen {

    // Barra verde de experiencia de Minecraft (icons.png): fondo en v=64, relleno en v=69
    private static final ResourceLocation ICONOS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final int ANCHO_BARRA = 182;

    private static final int ANCHO_BOTON = 160;
    private static final int ALTO_FILA = 24;
    private static final int Y_TITULO = 15;
    private static final int Y_LISTA = 78;

    private static final int COLOR_TEXTO = 0xFFFFFF;
    private static final int COLOR_SECUNDARIO = 0xAAAAAA;
    private static final int COLOR_RANGO = 0x80FF20;   // verde, como los números de experiencia

    private final boolean primeraVez;

    public RolScreen(boolean primeraVez) {
        super(Component.translatable(primeraVez ? "yonga.rol.menu.titulo_elegir" : "yonga.rol.menu.titulo"));
        this.primeraVez = primeraVez;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - ANCHO_BOTON / 2 + 10;   // +10 deja hueco a la izquierda para el icono
        int y = Y_LISTA;

        for (Rol rol : Rol.values()) {
            Button boton = Button.builder(rol.nombre(), b -> elegir(rol))
                    .bounds(x, y, ANCHO_BOTON, 20)
                    .tooltip(Tooltip.create(rol.descripcion()))
                    .build();
            boton.active = rol != RolCliente.rol();   // tu oficio actual no se puede volver a elegir
            addRenderableWidget(boton);
            y += ALTO_FILA;
        }

        if (!primeraVez) {
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(this.width / 2 - 60, y + 8, 120, 20)
                    .build());
        }
    }

    private void elegir(Rol rol) {
        Rol actual = RolCliente.rol();

        // Sin oficio todavía: se elige directamente
        if (actual == null) {
            enviar(rol);
            return;
        }

        // Cambiar de oficio: pedimos confirmación porque se pierde el rango
        this.minecraft.setScreen(new ConfirmScreen(
                si -> {
                    if (si) {
                        enviar(rol);
                    } else {
                        this.minecraft.setScreen(this);
                    }
                },
                Component.translatable("yonga.rol.confirmar.titulo", rol.nombre()),
                Component.translatable("yonga.rol.confirmar.mensaje", actual.nombre(), RolCliente.rango().nombre())));
    }

    private void enviar(Rol rol) {
        RolesRed.CANAL.sendToServer(new ElegirRolPacket(rol.id()));
        this.minecraft.setScreen(null);
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);
        int centroX = this.width / 2;

        graficos.drawCenteredString(this.font, this.title, centroX, Y_TITULO, COLOR_TEXTO);

        Rol actual = RolCliente.rol();
        if (actual == null) {
            graficos.drawCenteredString(this.font, Component.translatable("yonga.rol.menu.sin_rol"),
                    centroX, Y_TITULO + 20, COLOR_SECUNDARIO);
            graficos.drawCenteredString(this.font, Component.translatable("yonga.rol.menu.aviso"),
                    centroX, Y_TITULO + 32, COLOR_SECUNDARIO);
        } else {
            dibujarOficioActual(graficos, actual, centroX);
        }

        super.render(graficos, ratonX, ratonY, parcial);

        // Iconos a la izquierda de cada botón
        int xIcono = centroX - ANCHO_BOTON / 2 - 12;
        int y = Y_LISTA + 2;
        for (Rol rol : Rol.values()) {
            graficos.renderItem(rol.icono(), xIcono, y);
            y += ALTO_FILA;
        }
    }

    // "Herrero - Aprendiz", barra verde y "12 / 70" (o "Rango máximo")
    private void dibujarOficioActual(GuiGraphics graficos, Rol actual, int centroX) {
        int experiencia = RolCliente.experiencia();
        RangoRol rango = RolCliente.rango();
        RangoRol siguiente = rango.siguiente();

        Component linea = Component.translatable("yonga.rol.menu.actual", actual.nombre(), rango.nombre());
        graficos.drawCenteredString(this.font, linea, centroX, Y_TITULO + 20, COLOR_RANGO);

        int xBarra = centroX - ANCHO_BARRA / 2;
        int yBarra = Y_TITULO + 34;
        graficos.blit(ICONOS, xBarra, yBarra, 0, 64, ANCHO_BARRA, 5);
        int relleno = (int) (RangoRol.progreso(experiencia) * ANCHO_BARRA);
        if (relleno > 0) {
            graficos.blit(ICONOS, xBarra, yBarra, 0, 69, relleno, 5);
        }

        Component progreso = siguiente == null
                ? Component.translatable("yonga.rol.menu.maximo")
                : Component.translatable("yonga.rol.menu.progreso", experiencia, siguiente.experienciaNecesaria(), siguiente.nombre());
        graficos.drawCenteredString(this.font, progreso, centroX, yBarra + 9, COLOR_SECUNDARIO);
    }

    // La primera vez no se puede cerrar con Esc: hay que elegir oficio
    @Override
    public boolean shouldCloseOnEsc() {
        return !primeraVez;
    }
}
