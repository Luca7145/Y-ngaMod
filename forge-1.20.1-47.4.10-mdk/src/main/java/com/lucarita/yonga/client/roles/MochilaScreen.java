package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pantalla de la mochila. Usa el fondo del cofre de Minecraft (filas de 9) y tapa los huecos sobrantes
 * de la última fila si no estuviera completa (ahora 18 = 2 filas y 27 = 3 filas, sin huecos tapados).
 */
public class MochilaScreen extends AbstractContainerScreen<MochilaMenu> {

    private static final ResourceLocation FONDO_COFRE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int COLOR_PANEL = 0xFFC6C6C6;   // el gris de fondo de los menús de Minecraft

    private final int filas;

    public MochilaScreen(MochilaMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        this.filas = menu.filas();
        this.imageHeight = 114 + filas * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graficos, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos;
        int y = this.topPos;
        // Parte de arriba del cofre (con tantas filas como necesitemos) y parte del inventario
        graficos.blit(FONDO_COFRE, x, y, 0, 0, this.imageWidth, filas * 18 + 17);
        graficos.blit(FONDO_COFRE, x, y + filas * 18 + 17, 0, 126, this.imageWidth, 96);

        // Tapar los marcos de huecos que no existen
        for (int i = menu.huecos(); i < filas * 9; i++) {
            int hx = x + 7 + (i % 9) * 18;
            int hy = y + 17 + (i / 9) * 18;
            graficos.fill(hx, hy, hx + 18, hy + 18, COLOR_PANEL);
        }
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);
        super.render(graficos, ratonX, ratonY, parcial);
        renderTooltip(graficos, ratonX, ratonY);
    }
}
