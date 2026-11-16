package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.modulos.fe.FeCliente;
import com.lucarita.yonga.modulos.fe.RangoFe;
import com.lucarita.yonga.modulos.fe.TipoMana;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * Medidor de maná: barra + número, en el lado derecho, encima de la comida.
 * Azul para el maná (rangos positivos), negra con borde morado para el maná oscuro (rangos negativos).
 * El Ateo no tiene medidor.
 */
public final class BarraMana {

    // Tamaño de la barra en el HUD (la fila de la comida mide 81 píxeles; dejamos sitio al número)
    public static final int ANCHO_HUD = 58;
    public static final int ALTO = 5;
    private static final int ALTO_FILA = 10;

    // Colores (formato 0xAARRGGBB)
    private record Colores(int borde, int fondo, int relleno, int brillo, int texto) {}

    private static final Colores AZUL = new Colores(0xFF000000, 0xFF0C1A33, 0xFF2F6BFF, 0xFF8AB8FF, 0xFF55AAFF);
    private static final Colores NEGRO = new Colores(0xFF6A4C8C, 0xFF2E2E2E, 0xFF050505, 0xFF3B2A4D, 0xFFB79AD6);

    private BarraMana() {}

    private static Colores colores(TipoMana tipo) {
        return tipo == TipoMana.OSCURO ? NEGRO : AZUL;
    }

    /** Dibuja una barra de maná del tipo indicado, de cualquier ancho. */
    public static void dibujar(GuiGraphics graficos, int x, int y, int ancho, float progreso, TipoMana tipo) {
        Colores c = colores(tipo);
        int relleno = Math.round(Math.max(0f, Math.min(1f, progreso)) * ancho);
        graficos.fill(x - 1, y - 1, x + ancho + 1, y + ALTO + 1, c.borde());
        graficos.fill(x, y, x + ancho, y + ALTO, c.fondo());
        if (relleno > 0) {
            graficos.fill(x, y, x + relleno, y + ALTO, c.relleno());
            graficos.fill(x, y, x + relleno, y + 1, c.brillo());   // línea de brillo arriba
        }
    }

    public static int colorTexto(TipoMana tipo) {
        return colores(tipo).texto();
    }

    /** Se dibuja justo después de la comida, y sube lo que venga encima (burbujas de aire) para hacerse hueco. */
    public static void dibujarHud(ForgeGui gui, GuiGraphics graficos, float parcial, int anchoPantalla, int altoPantalla) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()) {
            return;
        }
        RangoFe rango = FeCliente.rango();
        TipoMana tipo = rango.tipoMana();
        if (tipo == TipoMana.NINGUNO) {
            return;
        }

        int maximo = rango.manaMaximo();
        double mana = FeCliente.mana();

        int x = anchoPantalla / 2 + 10;                 // mismo borde izquierdo que la comida
        int y = altoPantalla - gui.rightHeight;          // fila libre encima de la comida
        dibujar(graficos, x, y + 2, ANCHO_HUD, (float) (mana / maximo), tipo);

        String numero = String.valueOf((int) Math.floor(mana));
        graficos.drawString(minecraft.font, numero, x + ANCHO_HUD + 4, y + 1, colorTexto(tipo), true);

        gui.rightHeight += ALTO_FILA;
    }
}
