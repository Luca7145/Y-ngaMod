package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.red.CronicaPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** La crónica de la aldea: un pergamino largo que se lee con la rueda del ratón. */
public class CronicaScreen extends Screen {

    private final List<Component> lineas;
    private final List<FormattedCharSequence> partidas = new ArrayList<>();
    private double desplazamiento;

    private CronicaScreen(CronicaPacket p) {
        super(Component.translatable("yonga.cronica.titulo"));
        this.lineas = p.lineas();
    }

    public static void abrir(CronicaPacket p) {
        Minecraft.getInstance().setScreen(new CronicaScreen(p));
    }

    private int ancho() {
        return Math.min(420, this.width - 40);
    }

    @Override
    protected void init() {
        partidas.clear();
        for (Component c : lineas) {
            List<FormattedCharSequence> trozos = this.font.split(c, ancho() - 16);
            if (trozos.isEmpty()) partidas.add(FormattedCharSequence.EMPTY);
            else partidas.addAll(trozos);
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    private int altoVisible() {
        return this.height - 64;
    }

    private int maximo() {
        return Math.max(0, partidas.size() * 10 - altoVisible() + 8);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        desplazamiento = Mth.clamp(desplazamiento - delta * 20, 0, maximo());
        return true;
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        this.renderBackground(g);
        int x = (this.width - ancho()) / 2, arriba = 28, abajo = arriba + altoVisible();
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFE8C87A);
        g.fill(x - 4, arriba - 4, x + ancho() + 4, abajo + 4, 0xE0201810);
        g.enableScissor(x, arriba, x + ancho(), abajo);
        int y = arriba + 4 - (int) desplazamiento;
        for (FormattedCharSequence linea : partidas) {
            if (y > arriba - 10 && y < abajo) g.drawString(this.font, linea, x + 8, y, 0xFFE6DCC6, false);
            y += 10;
        }
        g.disableScissor();
        if (maximo() > 0) {
            int barra = Math.max(12, altoVisible() * altoVisible() / (partidas.size() * 10 + 8));
            int pos = (int) ((altoVisible() - barra) * (desplazamiento / maximo()));
            g.fill(x + ancho() + 1, arriba + pos, x + ancho() + 3, arriba + pos + barra, 0xFFBFA878);
        }
        super.render(g, ratonX, ratonY, parcial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
