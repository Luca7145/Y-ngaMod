package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.FeCliente;
import com.lucarita.yonga.modulos.fe.FeComando;
import com.lucarita.yonga.modulos.fe.FeNiveles;
import com.lucarita.yonga.modulos.fe.Oracion;
import com.lucarita.yonga.modulos.fe.RangoFe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/**
 * Menú de fe (se abre con el botón "Fe" del inventario):
 *        12            <- nivel, en grande
 *      Fiel            <- rango
 *   [=======-----]     <- barra de progreso hacia el siguiente nivel
 *   5.3 / 31.4 puntos para el nivel 13
 */
public class FeScreen extends Screen {

    private static final int COLOR_NIVEL = 0xFFFF55;
    private static final int COLOR_TEXTO = 0xFFFFFF;
    private static final int COLOR_SECUNDARIO = 0xAAAAAA;

    public FeScreen() {
        super(Component.translatable("yonga.fe.menu.titulo"));
    }

    @Override
    protected void init() {
        int centroX = this.width / 2;
        int y = this.height / 2 + 45;
        int direccion = FeClienteEventos.miDireccion();

        // Rezar / Blasfemar (si ya lo estás haciendo, el botón sirve para parar). Cierran el menú para que veas la barra.
        addRenderableWidget(Button.builder(Component.translatable(
                                direccion == Oracion.REZAR ? "yonga.fe.menu.dejar_rezar" : "yonga.fe.menu.rezar"),
                        boton -> {
                            FeClienteEventos.pedirOracion(Oracion.REZAR);
                            this.minecraft.setScreen(null);
                        })
                .bounds(centroX - 122, y, 120, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable(
                                direccion == Oracion.BLASFEMAR ? "yonga.fe.menu.dejar_blasfemar" : "yonga.fe.menu.blasfemar"),
                        boton -> {
                            FeClienteEventos.pedirOracion(Oracion.BLASFEMAR);
                            this.minecraft.setScreen(null);
                        })
                .bounds(centroX + 2, y, 120, 20)
                .build());

        // Volver al inventario
        addRenderableWidget(Button.builder(Component.translatable("yonga.fe.menu.volver"),
                        boton -> this.minecraft.setScreen(new InventoryScreen(this.minecraft.player)))
                .bounds(centroX - 60, y + 24, 120, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);

        double puntos = FeCliente.puntos();
        int nivel = FeNiveles.nivel(puntos);
        RangoFe rango = RangoFe.deNivel(nivel);

        int centroX = this.width / 2;
        int y = this.height / 2 - 80;

        graficos.drawCenteredString(this.font, this.title, centroX, y, COLOR_TEXTO);
        y += 20;

        // Número de nivel, 3 veces más grande
        String textoNivel = String.valueOf(nivel);
        graficos.pose().pushPose();
        graficos.pose().translate(centroX, y, 0);
        graficos.pose().scale(3.0f, 3.0f, 1.0f);
        graficos.drawString(this.font, textoNivel, -this.font.width(textoNivel) / 2, 0, COLOR_NIVEL, true);
        graficos.pose().popPose();
        y += 32;

        // Rango
        graficos.drawCenteredString(this.font, rango.nombre(), centroX, y, COLOR_TEXTO);
        y += 16;

        // Barra
        BarraFe.dibujar(graficos, centroX - BarraFe.ANCHO / 2, y, FeNiveles.progreso(puntos));
        y += BarraFe.ALTO + 8;

        // Progreso en números
        Component progreso;
        if (Math.abs(nivel) >= FeNiveles.NIVEL_MAXIMO) {
            progreso = Component.translatable("yonga.fe.menu.maximo");
        } else {
            progreso = Component.translatable("yonga.fe.menu.progreso",
                    FeComando.formatear(FeNiveles.puntosEnNivel(puntos)),
                    FeComando.formatear(FeNiveles.costeNivel(Math.abs(nivel))),
                    FeNiveles.siguienteNivel(puntos));
        }
        graficos.drawCenteredString(this.font, progreso, centroX, y, COLOR_SECUNDARIO);
        y += 12;

        graficos.drawCenteredString(this.font,
                Component.translatable("yonga.fe.menu.total", FeComando.formatear(puntos)),
                centroX, y, COLOR_SECUNDARIO);

        super.render(graficos, ratonX, ratonY, parcial);
    }

    // Como el inventario, no pausa el juego
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
