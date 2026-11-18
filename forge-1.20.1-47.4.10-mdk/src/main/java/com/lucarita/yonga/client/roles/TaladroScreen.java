package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.oficios.TaladroMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Pantalla del depósito del taladro. Usa el fondo del horno de Minecraft: deja su hueco de combustible y
 * su llama, y tapa el hueco de lo que se funde, la flecha y el resultado. Al lado de la llama pone el
 * tiempo que le queda (minutos:segundos).
 */
public class TaladroScreen extends AbstractContainerScreen<TaladroMenu> {

    private static final ResourceLocation HORNO = new ResourceLocation("minecraft", "textures/gui/container/furnace.png");
    private static final int PANEL = 0xFFC6C6C6;

    public TaladroScreen(TaladroMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
    }

    @Override
    protected void renderBg(GuiGraphics graficos, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos, y = this.topPos;
        graficos.blit(HORNO, x, y, 0, 0, this.imageWidth, this.imageHeight);
        graficos.fill(x + 54, y + 15, x + 74, y + 35, PANEL);        // hueco de lo que se funde
        graficos.fill(x + 78, y + 33, x + 104, y + 51, PANEL);       // flecha
        graficos.fill(x + 110, y + 29, x + 142, y + 61, PANEL);      // resultado
        // La llama (de 13 de alto, como la del horno)
        float llama = menu.llama();
        if (llama > 0) {
            int alto = Math.max(1, Math.round(llama * 13));
            graficos.blit(HORNO, x + 56, y + 36 + 12 - alto + 1, 176, 12 - alto + 1, 14, alto);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graficos, int ratonX, int ratonY) {
        super.renderLabels(graficos, ratonX, ratonY);
        int segundos = menu.segundosLlama();
        Component texto = segundos > 0
                ? Component.translatable("yonga.taladro.pantalla_ardiendo", segundos / 60, String.format("%02d", segundos % 60))
                : Component.translatable("yonga.taladro.pantalla_apagado");
        graficos.drawString(this.font, texto, 76, 40, segundos > 0 ? 0xC05000 : 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);
        super.render(graficos, ratonX, ratonY, parcial);
        renderTooltip(graficos, ratonX, ratonY);
    }
}
