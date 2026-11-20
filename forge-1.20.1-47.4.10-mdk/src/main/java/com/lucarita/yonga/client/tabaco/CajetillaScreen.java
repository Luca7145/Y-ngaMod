package com.lucarita.yonga.client.tabaco;

import com.lucarita.yonga.modulos.tabaco.CajetillaMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Pantalla de la cajetilla: el fondo del cofre con una sola fila, de la que solo se deja el hueco del centro. */
public class CajetillaScreen extends AbstractContainerScreen<CajetillaMenu> {

    private static final ResourceLocation FONDO_COFRE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int COLOR_PANEL = 0xFFC6C6C6;

    public CajetillaScreen(CajetillaMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        this.imageHeight = 114 + 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graficos, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos, y = this.topPos;
        graficos.blit(FONDO_COFRE, x, y, 0, 0, this.imageWidth, 18 + 17);
        graficos.blit(FONDO_COFRE, x, y + 18 + 17, 0, 126, this.imageWidth, 96);
        for (int i = 0; i < 9; i++) {
            if (i == 4) continue;                 // el único hueco: el del centro
            int hx = x + 7 + i * 18, hy = y + 17;
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
