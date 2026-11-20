package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.oficios.RadiocaseteMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pantalla del radiocasete: el fondo del cofre con una fila, en la que quedan el hueco del disco y, a su derecha,
 * los tres botones: pausa, continuar y volver a empezar.
 */
public class RadiocaseteScreen extends AbstractContainerScreen<RadiocaseteMenu> {

    private static final ResourceLocation FONDO_COFRE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
    private static final int COLOR_PANEL = 0xFFC6C6C6;
    private static final int ANCHO_BOTON = 22;

    public RadiocaseteScreen(RadiocaseteMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        this.imageHeight = 114 + 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos + RadiocaseteMenu.HUECO_X + 18 + 10;
        int y = this.topPos + RadiocaseteMenu.HUECO_Y - 1;
        boton(x, y, "❚❚", "yonga.radiocasete.pausa", RadiocaseteMenu.BOTON_PAUSA);
        boton(x + ANCHO_BOTON + 4, y, "▶", "yonga.radiocasete.continuar", RadiocaseteMenu.BOTON_CONTINUAR);
        boton(x + 2 * (ANCHO_BOTON + 4), y, "↺", "yonga.radiocasete.reiniciar", RadiocaseteMenu.BOTON_REINICIAR);
    }

    private void boton(int x, int y, String simbolo, String clave, int id) {
        addRenderableWidget(Button.builder(Component.literal(simbolo), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                    }
                })
                .bounds(x, y, ANCHO_BOTON, 18)
                .tooltip(Tooltip.create(Component.translatable(clave)))
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics graficos, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos, y = this.topPos;
        graficos.blit(FONDO_COFRE, x, y, 0, 0, this.imageWidth, 18 + 17);
        graficos.blit(FONDO_COFRE, x, y + 18 + 17, 0, 126, this.imageWidth, 96);
        int hueco = (RadiocaseteMenu.HUECO_X - 8) / 18;
        for (int i = 0; i < 9; i++) {
            if (i == hueco) continue;              // el hueco del disco
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
