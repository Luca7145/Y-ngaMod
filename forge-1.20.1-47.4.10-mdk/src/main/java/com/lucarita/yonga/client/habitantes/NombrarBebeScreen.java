package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.NombreBebePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Ha nacido tu bebé: ponle nombre. Sale en cuanto no haya otra pantalla abierta. Viene escrito un nombre propuesto
 * (el que le tocaba); si cierras sin aceptar, se queda con ese.
 */
public class NombrarBebeScreen extends Screen {

    private static int pendiente = -1;
    private static boolean pendienteMujer;
    private static String pendienteNombre = "";

    private final int idBebe;
    private final boolean mujer;
    private final String propuesto;
    private EditBox caja;

    public NombrarBebeScreen(int idBebe, boolean mujer, String propuesto) {
        super(Component.translatable(mujer ? "yonga.bebe.nombre.titulo.f" : "yonga.bebe.nombre.titulo.m"));
        this.idBebe = idBebe;
        this.mujer = mujer;
        this.propuesto = propuesto;
    }

    public static void pedir(int id, boolean mujer, String nombre) {
        pendiente = id;
        pendienteMujer = mujer;
        pendienteNombre = nombre;
    }

    /** Desde el tick del cliente. */
    static void quizaAbrir() {
        Minecraft mc = Minecraft.getInstance();
        if (pendiente >= 0 && mc.player != null && mc.screen == null) {
            int id = pendiente;
            pendiente = -1;
            mc.setScreen(new NombrarBebeScreen(id, pendienteMujer, pendienteNombre));
        }
    }

    static void olvidar() {
        pendiente = -1;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100, y = this.height / 2 - 10;
        this.caja = new EditBox(this.font, x, y, 200, 20, Component.translatable("yonga.bebe.nombre.caja"));
        this.caja.setMaxLength(32);
        this.caja.setValue(this.propuesto);
        addRenderableWidget(this.caja);
        setInitialFocus(this.caja);
        addRenderableWidget(Button.builder(Component.translatable("yonga.bebe.nombre.aceptar"), b -> aceptar())
                .bounds(this.width / 2 - 50, y + 28, 100, 20).build());
    }

    private void aceptar() {
        String nombre = this.caja.getValue().strip();
        if (nombre.isEmpty()) nombre = this.propuesto;
        HabitantesRed.CANAL.sendToServer(new NombreBebePacket(this.idBebe, nombre));
        this.onClose();
    }

    @Override
    public boolean keyPressed(int tecla, int codigo, int mods) {
        if (tecla == 257 || tecla == 335) {          // Intro
            aceptar();
            return true;
        }
        return super.keyPressed(tecla, codigo, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float parcial) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 44, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("yonga.bebe.nombre.explica"), this.width / 2,
                this.height / 2 - 28, 0xAAAAAA);
        super.render(g, x, y, parcial);
    }
}
