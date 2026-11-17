package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.FeCliente;
import com.lucarita.yonga.modulos.fe.FeNiveles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * La barra amarilla de fe. Usa la textura de las barras de jefe de Minecraft (la amarilla),
 * así que no hace falta ninguna textura propia.
 */
public final class BarraFe {

    private static final ResourceLocation TEXTURA_BARRAS = new ResourceLocation("minecraft", "textures/gui/bars.png");
    public static final int ANCHO = 182;
    public static final int ALTO = 5;

    // Posición de la barra amarilla dentro de bars.png
    private static final int V_FONDO = 40;
    private static final int V_RELLENO = 45;

    // Cuánto subimos los corazones, la comida, la armadura... para hacer hueco a la barra
    private static final int HUECO_HUD = 11;

    private static final int COLOR_NUMERO = 0xFFDD33;   // amarillo dorado
    private static final int COLOR_CONTORNO = 0x000000;

    private BarraFe() {}

    /** Dibuja la barra en (x, y) rellena según el progreso (0.0 a 1.0). */
    public static void dibujar(GuiGraphics graficos, int x, int y, float progreso) {
        graficos.blit(TEXTURA_BARRAS, x, y, 0, V_FONDO, ANCHO, ALTO);
        int relleno = (int) (progreso * ANCHO);
        if (relleno > 0) {
            graficos.blit(TEXTURA_BARRAS, x, y, 0, V_RELLENO, relleno, ALTO);
        }
    }

    /**
     * Barra en el HUD, encima de la barra de experiencia (y del número de nivel de experiencia).
     * Se dibuja antes que los corazones y sube corazones/comida para que no se solapen.
     */
    public static void dibujarHud(ForgeGui gui, GuiGraphics graficos, float parcial, int anchoPantalla, int altoPantalla) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }

        double puntos = FeCliente.puntos();
        int x = anchoPantalla / 2 - ANCHO / 2;
        int y = altoPantalla - 40;   // la barra de experiencia está en altoPantalla - 29
        dibujar(graficos, x, y, FeNiveles.progreso(puntos, FeCliente.nivelMaximo()));

        // Número de nivel encima de la barra, como el de experiencia (y como él, no sale en el nivel 0)
        int nivel = FeNiveles.nivel(puntos);
        if (nivel != 0) {
            dibujarNumero(graficos, minecraft, String.valueOf(nivel), anchoPantalla / 2, y - 6);
        }

        gui.leftHeight += HUECO_HUD;
        gui.rightHeight += HUECO_HUD;
    }

    // Igual que el número de experiencia: texto con un contorno negro de 1 píxel alrededor
    private static void dibujarNumero(GuiGraphics graficos, Minecraft minecraft, String texto, int centroX, int y) {
        int x = centroX - minecraft.font.width(texto) / 2;
        graficos.drawString(minecraft.font, texto, x + 1, y, COLOR_CONTORNO, false);
        graficos.drawString(minecraft.font, texto, x - 1, y, COLOR_CONTORNO, false);
        graficos.drawString(minecraft.font, texto, x, y + 1, COLOR_CONTORNO, false);
        graficos.drawString(minecraft.font, texto, x, y - 1, COLOR_CONTORNO, false);
        graficos.drawString(minecraft.font, texto, x, y, COLOR_NUMERO, false);
    }
}
