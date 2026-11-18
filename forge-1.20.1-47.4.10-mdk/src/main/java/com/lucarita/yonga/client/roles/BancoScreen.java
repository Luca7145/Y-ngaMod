package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.modulos.roles.construccion.BancoMenu;
import com.lucarita.yonga.modulos.roles.construccion.RecetaBanco;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Pantalla del banco de ingeniero: la misma que la del cortapiedras de Minecraft (usa su fondo),
 * con la lista de diseños que puede hacer quien la usa.
 */
public class BancoScreen extends AbstractContainerScreen<BancoMenu> {

    private static final ResourceLocation FONDO = new ResourceLocation("minecraft", "textures/gui/container/stonecutter.png");
    private static final int COLUMNAS = 4, FILAS = 3, X_LISTA = 52, Y_LISTA = 14;

    private float desplazamiento;
    private boolean arrastrando;
    private int primero;
    private boolean mostrar;

    public BancoScreen(BancoMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        menu.alCambiar(this::alCambiarContenido);
        --this.titleLabelY;
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);
        super.render(graficos, ratonX, ratonY, parcial);
        renderTooltip(graficos, ratonX, ratonY);
    }

    @Override
    protected void renderBg(GuiGraphics graficos, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos, y = this.topPos;
        graficos.blit(FONDO, x, y, 0, 0, this.imageWidth, this.imageHeight);
        int barra = (int) (41.0f * this.desplazamiento);
        graficos.blit(FONDO, x + 119, y + 15 + barra, 176 + (hayBarra() ? 0 : 12), 0, 12, 15);
        int ultimo = this.primero + COLUMNAS * FILAS;
        dibujarBotones(graficos, ratonX, ratonY, x + X_LISTA, y + Y_LISTA, ultimo);
        dibujarDisenos(graficos, x + X_LISTA, y + Y_LISTA, ultimo);
    }

    @Override
    protected void renderTooltip(GuiGraphics graficos, int ratonX, int ratonY) {
        super.renderTooltip(graficos, ratonX, ratonY);
        if (!this.mostrar) return;
        int x = this.leftPos + X_LISTA, y = this.topPos + Y_LISTA;
        List<RecetaBanco> recetas = this.menu.recetas();
        for (int i = this.primero; i < this.primero + COLUMNAS * FILAS && i < this.menu.cuantas(); i++) {
            int n = i - this.primero;
            int bx = x + n % COLUMNAS * 16, by = y + n / COLUMNAS * 18 + 2;
            if (ratonX >= bx && ratonX < bx + 16 && ratonY >= by && ratonY < by + 18) {
                graficos.renderTooltip(this.font, recetas.get(i).resultado(this.minecraft.level.registryAccess()), ratonX, ratonY);
            }
        }
    }

    private void dibujarBotones(GuiGraphics graficos, int ratonX, int ratonY, int x, int y, int ultimo) {
        for (int i = this.primero; i < ultimo && i < this.menu.cuantas(); i++) {
            int n = i - this.primero;
            int bx = x + n % COLUMNAS * 16, by = y + n / COLUMNAS * 18 + 2;
            int v = this.imageHeight;
            if (i == this.menu.elegida()) v += 18;
            else if (ratonX >= bx && ratonY >= by && ratonX < bx + 16 && ratonY < by + 18) v += 36;
            graficos.blit(FONDO, bx, by - 1, 0, v, 16, 18);
        }
    }

    private void dibujarDisenos(GuiGraphics graficos, int x, int y, int ultimo) {
        List<RecetaBanco> recetas = this.menu.recetas();
        for (int i = this.primero; i < ultimo && i < this.menu.cuantas(); i++) {
            int n = i - this.primero;
            graficos.renderItem(recetas.get(i).resultado(this.minecraft.level.registryAccess()),
                    x + n % COLUMNAS * 16, y + n / COLUMNAS * 18 + 2);
        }
    }

    @Override
    public boolean mouseClicked(double ratonX, double ratonY, int boton) {
        this.arrastrando = false;
        if (this.mostrar) {
            int x = this.leftPos + X_LISTA, y = this.topPos + Y_LISTA;
            for (int i = this.primero; i < this.primero + COLUMNAS * FILAS; i++) {
                int n = i - this.primero;
                double dx = ratonX - (x + n % COLUMNAS * 16), dy = ratonY - (y + n / COLUMNAS * 18);
                if (dx >= 0 && dy >= 0 && dx < 16 && dy < 18 && this.menu.clickMenuButton(this.minecraft.player, i)) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0f));
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                    return true;
                }
            }
            int bx = this.leftPos + 119, by = this.topPos + 9;
            if (ratonX >= bx && ratonX < bx + 12 && ratonY >= by && ratonY < by + 54) {
                this.arrastrando = true;
            }
        }
        return super.mouseClicked(ratonX, ratonY, boton);
    }

    @Override
    public boolean mouseDragged(double ratonX, double ratonY, int boton, double dx, double dy) {
        if (this.arrastrando && hayBarra()) {
            int arriba = this.topPos + 14, abajo = arriba + 54;
            this.desplazamiento = Mth.clamp(((float) ratonY - arriba - 7.5f) / ((abajo - arriba) - 15.0f), 0.0f, 1.0f);
            this.primero = (int) (this.desplazamiento * filasFuera() + 0.5) * COLUMNAS;
            return true;
        }
        return super.mouseDragged(ratonX, ratonY, boton, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double ratonX, double ratonY, double delta) {
        if (hayBarra()) {
            int fuera = filasFuera();
            this.desplazamiento = Mth.clamp(this.desplazamiento - (float) delta / fuera, 0.0f, 1.0f);
            this.primero = (int) (this.desplazamiento * fuera + 0.5) * COLUMNAS;
        }
        return true;
    }

    private boolean hayBarra() {
        return this.mostrar && this.menu.cuantas() > COLUMNAS * FILAS;
    }

    private int filasFuera() {
        return (this.menu.cuantas() + COLUMNAS - 1) / COLUMNAS - FILAS;
    }

    private void alCambiarContenido() {
        this.mostrar = this.menu.hayEntrada();
        if (!this.mostrar) {
            this.desplazamiento = 0.0f;
            this.primero = 0;
        }
    }
}
