package com.lucarita.yonga.client.fe;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/**
 * El susto del Astado: al agarrarte, su cara salta a pantalla completa (se acerca de golpe, tiembla, destello
 * rojo) con un chillido, y se desvanece. Dura 1,5 s. Se dibuja por encima de todo (ver ClientSetup).
 */
public final class SustoAstado {

    private static final ResourceLocation CARA = new ResourceLocation(YongaMain.MOD_ID, "textures/gui/astado_susto.png");
    private static final int TAMANO_IMAGEN = 384;
    private static final int DURACION = 30;          // ticks
    private static final int ACERCARSE = 4;          // ticks que tarda en llegar a la cara
    private static final int DESVANECER = 10;        // ticks del final en los que se va

    private static long inicio = -1000;
    private static final RandomSource AZAR = RandomSource.create();

    private SustoAstado() {}

    public static void lanzar() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        inicio = mc.level.getGameTime();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(ModuloFe.SONIDO_ASTADO_SUSTO.get(), 1.0f, 1.0f));
    }

    /** Capa del HUD. */
    public static void dibujar(ForgeGui gui, GuiGraphics graficos, float parcial, int ancho, int alto) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float t = mc.level.getGameTime() - inicio + parcial;
        if (t < 0 || t > DURACION) {
            return;
        }
        float opacidad = t > DURACION - DESVANECER ? (DURACION - t) / DESVANECER : 1.0f;

        // Fondo negro (y un destello rojo al principio)
        int negro = ((int) (opacidad * 235) << 24);
        graficos.fill(0, 0, ancho, alto, negro);
        if (t < 3) {
            graficos.fill(0, 0, ancho, alto, ((int) ((1 - t / 3) * 150) << 24) | 0xB00000);
        }

        // La cara: se acerca de golpe y luego tiembla
        float acercar = Mth.clamp(t / ACERCARSE, 0.0f, 1.0f);
        float escala = Mth.lerp(acercar * acercar, 0.35f, 1.15f) + Mth.sin(t * 2.7f) * 0.015f;
        int lado = (int) (Math.max(ancho, alto) * escala);
        float temblor = (1.0f - t / DURACION) * 10.0f;
        int x = (ancho - lado) / 2 + (int) ((AZAR.nextFloat() - 0.5f) * 2 * temblor);
        int y = (alto - lado) / 2 + (int) ((AZAR.nextFloat() - 0.5f) * 2 * temblor);

        RenderSystem.enableBlend();
        graficos.setColor(1.0f, 1.0f - 0.2f * (1 - acercar), 1.0f - 0.2f * (1 - acercar), opacidad);
        graficos.blit(CARA, x, y, lado, lado, 0, 0, TAMANO_IMAGEN, TAMANO_IMAGEN, TAMANO_IMAGEN, TAMANO_IMAGEN);
        graficos.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
