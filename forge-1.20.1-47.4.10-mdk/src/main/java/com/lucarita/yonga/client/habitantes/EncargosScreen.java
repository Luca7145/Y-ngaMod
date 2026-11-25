package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.red.EncargoAccionPacket;
import com.lucarita.yonga.modulos.habitantes.red.EncargosPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.taberna.Taberna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * El tablón de encargos: una hoja por encargo, con lo que hay que hacer, la recompensa y su estado. "Entregar" en
 * los de traer (se quitan del inventario) y "Aceptar" en los de cazar (desde entonces cuentan tus muertes).
 */
public class EncargosScreen extends Screen {

    private static final int ANCHO = 300, ALTO_FILA = 34;

    private EncargosPacket datos;

    private EncargosScreen(EncargosPacket datos) {
        super(Component.translatable("yonga.encargos.titulo"));
        this.datos = datos;
    }

    public static void abrir(EncargosPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof EncargosScreen pantalla && pantalla.datos.campana().equals(p.campana())) {
            pantalla.datos = p;
            pantalla.rebuildWidgets();
        } else {
            mc.setScreen(new EncargosScreen(p));
        }
    }

    private int izquierda() {
        return (this.width - ANCHO) / 2;
    }

    private int arriba() {
        return Math.max(20, (this.height - Math.max(1, datos.encargos().size()) * ALTO_FILA) / 2);
    }

    @Override
    protected void init() {
        int x = izquierda(), y = arriba();
        for (EncargosPacket.Entrada e : datos.encargos()) {
            int estado = e.estado();
            if (estado == 0 || estado == 1 && !e.caza()) {
                Component texto = Component.translatable(e.caza() ? "yonga.encargos.aceptar" : "yonga.encargos.entregar");
                int accion = e.caza() ? Taberna.ACEPTAR : Taberna.ENTREGAR;
                addRenderableWidget(Button.builder(texto, b -> HabitantesRed.CANAL.sendToServer(
                        new EncargoAccionPacket(datos.campana(), e.id(), accion)))
                        .bounds(x + ANCHO - 68, y + 7, 64, 20).build());
            }
            y += ALTO_FILA;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, Math.min(this.height - 24, y + 6), 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        this.renderBackground(g);
        int x = izquierda(), y = arriba();
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 14, 0xFFE8C87A);
        if (datos.encargos().isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable(datos.tabernero() ? "yonga.encargos.ninguno"
                    : "yonga.encargos.sin_tabernero"), this.width / 2, y + 10, 0xFFCCCCCC);
        }
        for (EncargosPacket.Entrada e : datos.encargos()) {
            boolean cerrado = e.estado() >= 2;
            // La hoja: papel claro, más apagado si ya está cumplido
            g.fill(x, y, x + ANCHO, y + ALTO_FILA - 3, cerrado ? 0xC0504436 : 0xE0E9DDB8);
            g.fill(x, y, x + ANCHO, y + 1, 0x40000000);
            g.renderItem(e.icono(), x + 6, y + 8);
            int color = cerrado ? 0xFFBBB09A : 0xFF3A2A18;
            g.drawString(this.font, e.texto(), x + 28, y + 5, color, false);
            Component linea2;
            if (e.estado() == 3) {
                linea2 = Component.translatable("yonga.encargos.tuyo");
            } else if (e.estado() == 2) {
                linea2 = Component.translatable("yonga.encargos.cumplido_por", e.ganador());
            } else if (e.estado() == 1) {
                linea2 = Component.translatable("yonga.encargos.llevas", e.progreso(), e.total(), e.monedas());
            } else {
                linea2 = Component.translatable(e.monedas() == 1 ? "yonga.encargos.recompensa1" : "yonga.encargos.recompensa", e.monedas());
            }
            g.drawString(this.font, linea2, x + 28, y + 17, cerrado ? 0xFF9A907C : 0xFF6B5432, false);
            y += ALTO_FILA;
        }
        super.render(g, ratonX, ratonY, parcial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
