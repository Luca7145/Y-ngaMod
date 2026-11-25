package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.familia.Familias;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.SexoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Al entrar al mundo por primera vez: ¿tu personaje es hombre o mujer? (para los habitantes: con quién puedes tener
 * pareja, quién puede quedarse embarazada…). Sale cuando no hay otra pantalla abierta (p. ej. después de elegir
 * oficio). Se puede cambiar luego con /yonga sexo.
 */
public class SexoScreen extends Screen {

    private static boolean pendiente;

    public SexoScreen() {
        super(Component.translatable("yonga.sexo.titulo"));
    }

    /** El servidor lo pide: se abre en cuanto no haya otra pantalla. */
    public static void pedir() {
        pendiente = true;
    }

    /** Desde el tick del cliente. */
    static void quizaAbrir() {
        Minecraft mc = Minecraft.getInstance();
        if (pendiente && mc.player != null && mc.screen == null) {
            pendiente = false;
            mc.setScreen(new SexoScreen());
        }
    }

    static void olvidar() {
        pendiente = false;
    }

    @Override
    protected void init() {
        int y = this.height / 2 - 4;
        addRenderableWidget(Button.builder(Component.translatable("yonga.sexo.hombre"), b -> elegir(Familias.HOMBRE))
                .bounds(this.width / 2 - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("yonga.sexo.mujer"), b -> elegir(Familias.MUJER))
                .bounds(this.width / 2 + 4, y, 100, 20).build());
    }

    private void elegir(int sexo) {
        HabitantesRed.CANAL.sendToServer(new SexoPacket(sexo));
        this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;              // hay que elegir (luego se puede cambiar con /yonga sexo)
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float parcial) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("yonga.sexo.explica"), this.width / 2, this.height / 2 - 24, 0xAAAAAA);
        super.render(g, x, y, parcial);
    }
}
