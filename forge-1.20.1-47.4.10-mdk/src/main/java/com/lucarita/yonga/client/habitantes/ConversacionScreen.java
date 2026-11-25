package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.ia.Conversaciones;
import com.lucarita.yonga.modulos.habitantes.red.AbrirConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.ConversacionPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.LineaConversacionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * La conversación con un habitante: abajo, un panel con lo último que os habéis dicho y una caja para escribir (Intro
 * para decirlo); a su derecha, todos los botones en una columna. El juego sigue corriendo detrás.
 */
public class ConversacionScreen extends Screen {

    private static final int ANCHO_MAX = 380, ALTO_LINEAS = 120;

    private final int idHabitante;
    private final boolean tienda;
    private final boolean encargos;
    private final List<Component> lineas;
    /** −1 sin botones de amor; 0 aún nada (Declararte); 1 novios y 2 casados (Pasar la noche juntos). */
    private int relacion;
    /** 0 a lo suyo · 1 te sigue · 2 quieto. */
    private int seguir;
    private boolean confirmando;
    private EditBox caja;
    private boolean esperando;
    /** La cerró el servidor: no hace falta avisarle. */
    private boolean cerrada;

    private ConversacionScreen(AbrirConversacionPacket paquete) {
        super(paquete.titulo());
        this.idHabitante = paquete.idHabitante();
        this.tienda = paquete.tienda();
        this.encargos = paquete.encargos();
        this.lineas = new ArrayList<>(paquete.lineas());
        this.relacion = paquete.relacion();
        this.seguir = paquete.seguir();
    }

    public static void recibir(com.lucarita.yonga.modulos.habitantes.red.RelacionPacket paquete) {
        if (Minecraft.getInstance().screen instanceof ConversacionScreen pantalla && pantalla.idHabitante == paquete.idHabitante()) {
            if (paquete.relacion() == -2) {             // empieza la noche juntos: a la cama
                pantalla.onClose();
                return;
            }
            pantalla.relacion = paquete.relacion();
            pantalla.seguir = paquete.seguir();
            String escrito = pantalla.caja == null ? "" : pantalla.caja.getValue();
            pantalla.rebuildWidgets();
            pantalla.caja.setValue(escrito);
        }
    }

    public static void abrir(AbrirConversacionPacket paquete) {
        Minecraft.getInstance().setScreen(new ConversacionScreen(paquete));
    }

    public static void recibir(LineaConversacionPacket paquete) {
        if (Minecraft.getInstance().screen instanceof ConversacionScreen pantalla && pantalla.idHabitante == paquete.idHabitante()) {
            pantalla.lineas.add(paquete.linea());
            pantalla.esperando = false;
            if (paquete.cerrar()) {
                // Se acabó la conversación (le ha echado o le ha atacado): se queda abierta para leer lo que dijo,
                // pero ya no se puede escribir
                pantalla.cerrada = true;
                pantalla.caja.setEditable(false);
                pantalla.caja.setValue("");
            }
        }
    }

    /** Ancho de la columna de botones y separación con la conversación. */
    private static final int ANCHO_COL = 120, HUECO = 8, ALTO_BOTON = 20, PASO = 22;

    /** Dónde quedó todo al colocarlo (para pintar los fondos). */
    private int panelX, panelW, panelArriba, colArriba, altoLineas;

    private int ancho() {
        return Math.max(160, Math.min(ANCHO_MAX - 60, this.width - 20 - ANCHO_COL - HUECO));
    }

    private int izquierda() {
        return (this.width - (ancho() + HUECO + ANCHO_COL)) / 2;
    }

    private Button boton(String clave, Button.OnPress accion) {
        return Button.builder(Component.translatable(clave), accion).bounds(0, 0, ANCHO_COL, ALTO_BOTON).build();
    }

    /**
     * A la izquierda, la conversación: el nombre arriba, lo que os habéis dicho y la caja para escribir (Intro para
     * decirlo). A la derecha, una columna con todos los botones, uno encima de otro: arriba lo que haces con él
     * (regalar, que te siga o se quede, lo del amor), luego tienda y encargos si los tiene, y abajo del todo "Adiós",
     * a la altura de la caja de escribir.
     */
    @Override
    protected void init() {
        int x = izquierda(), w = ancho();
        int abajo = this.height - 30;                   // fila de la caja de escribir
        this.caja = new EditBox(this.font, x, abajo, w, 20, Component.translatable("yonga.habla.escribe"));
        this.caja.setMaxLength(500);
        this.caja.setHint(Component.translatable("yonga.habla.escribe"));
        this.addRenderableWidget(this.caja);

        List<Button> acciones = new ArrayList<>();
        acciones.add(Button.builder(Component.translatable("yonga.habla.regalar"), b -> {
            if (!this.cerrada) enviar(ConversacionPacket.REGALAR, "");
        }).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("yonga.habla.regalar.ayuda")))
                .bounds(0, 0, ANCHO_COL, ALTO_BOTON).build());
        acciones.add(boton(this.seguir == 1 ? "yonga.habla.dejar" : "yonga.habla.seguir", b -> {
            if (!this.cerrada) enviar(this.seguir == 1 ? ConversacionPacket.DEJAR : ConversacionPacket.SEGUIR, "");
        }));
        acciones.add(boton(this.seguir == 2 ? "yonga.habla.libre" : "yonga.habla.quedarse", b -> {
            if (!this.cerrada) enviar(this.seguir == 2 ? ConversacionPacket.LIBRE : ConversacionPacket.QUEDARSE, "");
        }));
        if (this.relacion == 0) {
            acciones.add(boton("yonga.habla.declararse", b -> {
                if (!this.cerrada) enviar(ConversacionPacket.DECLARARSE, "");
            }));
        } else if (this.relacion > 0) {
            // Romper, preguntando antes por si se le da sin querer
            acciones.add(boton("yonga.habla.romper", b -> {
                if (this.cerrada || this.minecraft == null) return;
                this.confirmando = true;
                this.minecraft.setScreen(new net.minecraft.client.gui.screens.ConfirmScreen(si -> {
                    this.confirmando = false;
                    this.minecraft.setScreen(this);
                    if (si) enviar(ConversacionPacket.ROMPER, "");
                }, Component.translatable("yonga.habla.romper.titulo"), Component.translatable("yonga.habla.romper.seguro")));
            }));
        }
        if (this.relacion >= 0) {
            acciones.add(boton("yonga.habla.noche", b -> {
                if (!this.cerrada) enviar(ConversacionPacket.NOCHE, "");
            }));
        }
        List<Button> lugares = new ArrayList<>();
        if (this.tienda) lugares.add(boton("yonga.habla.tienda", b -> enviar(ConversacionPacket.TIENDA, "")));
        if (this.encargos) lugares.add(boton("yonga.habla.encargos", b -> enviar(ConversacionPacket.ENCARGOS, "")));
        Button adios = boton("yonga.habla.adios", b -> this.onClose());

        // Se colocan de abajo arriba: "Adiós" junto a la caja; encima, tienda y encargos; encima, las acciones
        int colX = x + w + HUECO;
        int y = abajo;
        adios.setPosition(colX, y);
        y -= 8;
        for (int i = lugares.size() - 1; i >= 0; i--) {
            y -= PASO;
            lugares.get(i).setPosition(colX, y);
        }
        if (!lugares.isEmpty()) y -= 8;
        for (int i = acciones.size() - 1; i >= 0; i--) {
            y -= PASO;
            acciones.get(i).setPosition(colX, y);
        }
        this.colArriba = y;
        for (Button b : acciones) this.addRenderableWidget(b);
        for (Button b : lugares) this.addRenderableWidget(b);
        this.addRenderableWidget(adios);

        // La conversación ocupa al menos lo mismo de alto que la columna
        this.altoLineas = Math.max(ALTO_LINEAS, (abajo - 6) - (this.colArriba + 16));
        this.panelX = x;
        this.panelW = w;
        this.panelArriba = abajo - 6 - this.altoLineas - 16;
        this.setInitialFocus(this.caja);
    }

    private void enviar(int tipo, String texto) {
        HabitantesRed.CANAL.sendToServer(new ConversacionPacket(this.idHabitante, tipo, texto));
    }

    private void decir() {
        String texto = this.caja.getValue().strip();
        if (texto.isEmpty() || this.esperando || this.cerrada) return;
        enviar(ConversacionPacket.HABLAR, texto);
        this.lineas.add(Conversaciones.lineaJugador(texto));
        this.caja.setValue("");
        this.esperando = true;
    }

    @Override
    public boolean keyPressed(int tecla, int codigo, int modificadores) {
        if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER) {
            decir();
            return true;
        }
        return super.keyPressed(tecla, codigo, modificadores);
    }

    @Override
    public void tick() {
        this.caja.tick();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        int x = this.panelX, w = this.panelW;
        int arriba = this.panelArriba, abajoLineas = this.height - 36;
        // Fondo de la conversación, con un borde fino y una línea bajo el nombre
        g.fill(x - 6, arriba - 6, x + w + 6, this.height - 4, 0xC0101014);
        g.renderOutline(x - 6, arriba - 6, w + 12, this.height - 4 - (arriba - 6), 0x60E8C87A);
        g.drawString(this.font, this.title, x, arriba, 0xFFE8C87A);
        g.fill(x, arriba + 11, x + w, arriba + 12, 0x40E8C87A);
        // Fondo de la columna de botones
        int colX = x + w + HUECO;
        g.fill(colX - 4, this.colArriba - 4, colX + ANCHO_COL + 4, this.height - 4, 0x90101014);

        // Las últimas líneas que quepan, de abajo arriba (con un poco de aire entre las de cada uno)
        List<FormattedCharSequence> partidas = new ArrayList<>();
        for (Component l : this.lineas) partidas.addAll(this.font.split(l, w));
        if (this.esperando) {
            int puntos = (int) (System.currentTimeMillis() / 400 % 3) + 1;
            partidas.add(Component.literal(".".repeat(puntos)).getVisualOrderText());
        }
        int caben = this.altoLineas / 10;
        int desde = Math.max(0, partidas.size() - caben);
        int y = abajoLineas - (partidas.size() - desde) * 10;
        for (int i = desde; i < partidas.size(); i++, y += 10) {
            g.drawString(this.font, partidas.get(i), x, y, 0xFFFFFFFF);
        }
        super.render(g, ratonX, ratonY, parcial);
    }

    @Override
    public void removed() {
        if (this.confirmando) return;                    // solo se ha ido un momento a la pregunta de "¿seguro?"
        if (!this.cerrada) {
            this.cerrada = true;
            enviar(ConversacionPacket.CERRAR, "");
        }
        super.removed();
    }


    @Override
    public void onClose() {
        super.onClose();
    }
}
