package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.Epoca;
import com.lucarita.yonga.modulos.habitantes.Habitante;
import com.lucarita.yonga.modulos.habitantes.ModuloHabitantes;
import com.lucarita.yonga.modulos.habitantes.Oficio;
import com.lucarita.yonga.modulos.habitantes.red.EditorPacket;
import com.lucarita.yonga.modulos.habitantes.red.GuardarEditorPacket;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

/**
 * El libro del creador (solo creativo): todo lo de un habitante en cuatro pestañas, con una vista previa a la
 * izquierda que cambia al momento. No se guarda nada hasta pulsar "Guardar".
 *   Aspecto · Persona (nombre, edad, época, carácter) · Trabajo · Contigo (opinión, pareja, embarazo, notas).
 */
public class EditorScreen extends Screen {

    private static final String[] PIELES = {"muy clara", "clara", "clara-media", "media", "morena", "oscura"};
    private static final String[] OJOS = {"marrones", "oscuros", "azules", "verdes", "miel", "grises"};
    private static final String[] PEINADOS_M = {"larga con flequillo", "larga con raya", "trenza", "moño", "coleta",
            "media melena", "dos trenzas", "corta con flequillo"};
    private static final String[] PEINADOS_H = {"corto con flequillo", "rapado", "melena", "tonsura", "raya al lado",
            "tazón", "rizado", "despeinado"};
    private static final String[] PELOS = {"negro", "castaño oscuro", "castaño", "castaño claro", "rubio", "pelirrojo",
            "gris", "blanco"};
    private static final String[] CARAS = {"normal", "rubor / cicatriz", "pecas", "lunar", "sonrisa", "mofletes"};
    private static final String[] BARBAS = {"ninguna", "de tres días", "completa", "bigote", "perilla", "larga"};
    private static final String[] CAIDAS = {"recto", "algo caído", "caído", "muy caído"};
    private static final String[] RASGOS = {"amable", "gruñón", "tímido", "charlatán", "desconfiado", "alegre",
            "avaricioso", "generoso", "orgulloso", "curioso", "supersticioso", "bromista"};
    private static final String[] ORIENTACIONES = {"le atrae el otro sexo", "le atrae su mismo sexo", "le atraen ambos"};
    private static final String[] PAREJAS = {"nada", "novios", "casados"};
    private static final String[] PESTANAS = {"Aspecto", "Persona", "Trabajo", "Contigo"};

    private final int id;
    private final CompoundTag t;
    private int pestana;
    private Habitante vista;

    public EditorScreen(int id, CompoundTag datos) {
        super(Component.literal("Libro del creador"));
        this.id = id;
        this.t = datos;
    }

    public static void abrir(EditorPacket p) {
        Minecraft.getInstance().setScreen(new EditorScreen(p.idHabitante(), p.datos()));
    }

    // =====================================================================================================

    private int x1() {
        return Math.max(130, this.width / 2 - 100);
    }

    private int x2() {
        return x1() + 170;
    }

    private int fila(int i) {
        return 44 + i * 22;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        if (this.vista == null && this.minecraft != null && this.minecraft.level != null) {
            this.vista = ModuloHabitantes.HABITANTE.get().create(this.minecraft.level);
        }
        actualizarVista();
        // Pestañas
        int ancho = 76, x = x1();
        for (int i = 0; i < PESTANAS.length; i++) {
            int cual = i;
            Button b = Button.builder(Component.literal(PESTANAS[i]), btn -> {
                this.pestana = cual;
                this.init();
            }).bounds(x + i * (ancho + 4), 16, ancho, 20).build();
            b.active = i != this.pestana;
            addRenderableWidget(b);
        }
        boolean mujer = t.getBoolean("mujer");
        switch (this.pestana) {
            case 0 -> {
                interruptor(x1(), fila(0), "Sexo", "mujer", "mujer", "hombre");
                ciclo(x1(), fila(1), "Piel", "piel", PIELES.length, i -> PIELES[i]);
                ciclo(x1(), fila(2), "Ojos", "ojos", OJOS.length, i -> OJOS[i]);
                int ep = com.lucarita.yonga.modulos.habitantes.Catalogo.dibujo(
                        com.lucarita.yonga.modulos.habitantes.Epoca.de(t.getInt("epoca"))).ordinal();
                int sx = mujer ? 1 : 0;
                ciclo(x1(), fila(3), "Peinado", "peinado", Habitante.PEINADOS,
                        i -> com.lucarita.yonga.modulos.habitantes.Catalogo.PEINADO[ep][sx][i]);
                ciclo(x1(), fila(4), "Pelo", "colorPelo", PELOS.length, i -> PELOS[i]);
                ciclo(x1(), fila(5), "Ropa", "ropa", Habitante.ROPAS,
                        i -> com.lucarita.yonga.modulos.habitantes.Catalogo.ROPA[ep][sx][i]);
                ciclo(x1(), fila(6), "Complemento", "complemento", Habitante.COMPLEMENTOS,
                        i -> com.lucarita.yonga.modulos.habitantes.Catalogo.COMPLEMENTO[ep][sx][i]);
                ciclo(x2(), fila(0), "Cara", "cara", CARAS.length, i -> CARAS[i]);
                ciclo(x2(), fila(1), "Barba", "barba", Habitante.BARBAS,
                        i -> com.lucarita.yonga.modulos.habitantes.Catalogo.BARBA[ep][i] + (mujer ? " (no se ve)" : ""));
                ciclo(x2(), fila(2), "Pecho", "pecho", Habitante.PECHOS, i -> (i + 1) + " de 8" + (mujer ? "" : " (no se ve)"));
                ciclo(x2(), fila(3), "Caída", "caida", CAIDAS.length, i -> CAIDAS[i]);
                ciclo(x2(), fila(6), "Rasgo", "marca", Habitante.MARCAS,
                        i -> com.lucarita.yonga.modulos.habitantes.Catalogo.MARCA[ep][sx][i]);
                deslizador(x2(), fila(4), "Altura", "altura", 0.85, 1.12, v -> String.format("%.2f", v));
                deslizador(x2(), fila(5), "Complexión", "complexion", 0.85, 1.20, v -> String.format("%.2f", v));
            }
            case 1 -> {
                texto(x1(), fila(0), 160, "nombre", "Nombre", 32);
                deslizador(x1(), fila(1), "Edad", "edad", 0.0, 3.0, EditorScreen::edad);
                ciclo(x1(), fila(2), "Época", "epoca", Epoca.values().length, i -> nombreEpoca(Epoca.values()[i]));
                ciclo(x2(), fila(0), "Carácter", "rasgo1", RASGOS.length, i -> RASGOS[i]);
                ciclo(x2(), fila(1), "y además", "rasgo2", RASGOS.length, i -> RASGOS[i]);
                ciclo(x2(), fila(2), "Amor", "orientacion", ORIENTACIONES.length, i -> ORIENTACIONES[i]);
                deslizador(x2(), fila(3), "Regateo (rebaja máx.)", "flex", 2, 40, v -> Math.round(v) + " %");
            }
            case 2 -> {
                ciclo(x1(), fila(0), "Oficio", "oficio", Oficio.values().length + 1, i -> i == 0 ? "ninguno"
                        : Component.translatable("yonga.oficio." + Oficio.values()[i - 1].id()).getString(), -1);
                ciclo(x1(), fila(1), "Nivel", "nivel", 5, i -> String.valueOf(i + 1), 1);
                interruptor(x1(), fila(2), "Holgazán (si no tiene oficio)", "holgazan", "sí", "no");
                interruptor(x1(), fila(3), "Guardia", "guardia", "sí", "no");
            }
            default -> {
                deslizador(x1(), fila(0), "Opinión de ti", "opinion", -100, 100, v -> String.valueOf(Math.round(v)));
                interruptor(x1(), fila(1), "Sabe tu nombre", "sabeNombre", "sí", "no");
                interruptor(x1(), fila(2), "Enfadado contigo", "enfadado", "sí", "no");
                interruptor(x1(), fila(3), "Olvidar la conversación", "olvidar", "sí", "no");
                Button pareja = ciclo(x2(), fila(0), "Pareja", "pareja", PAREJAS.length, i -> PAREJAS[i]);
                pareja.active = t.getBoolean("puedePareja");
                AbstractSliderButton emb = deslizador(x2(), fila(1), "Embarazo", "embarazo", 0.0, 0.99,
                        v -> v <= 0.001 ? "no" : Math.round(v * 100) + " %");
                emb.active = mujer && t.getFloat("edad") >= 3.0f;
                texto(x1(), fila(5), 330, "notas", "Lo que sabe de ti (notas de la IA)", 3000);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("Guardar"), b -> guardar())
                .bounds(this.width / 2 - 104, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> this.onClose())
                .bounds(this.width / 2 + 4, this.height - 28, 100, 20).build());
    }

    private void guardar() {
        HabitantesRed.CANAL.sendToServer(new GuardarEditorPacket(this.id, this.t));
        this.onClose();
    }

    private static String edad(double v) {
        String[] etapas = {"bebé", "niño", "joven", "adulto"};
        if (v >= 3.0) return "adulto";
        int e = (int) v;
        return etapas[e] + " (" + Math.round((v - e) * 100) + " %)";
    }

    private static String nombreEpoca(Epoca e) {
        String id = e.id();
        return id.substring(0, 1).toUpperCase() + id.substring(1);
    }

    // ---------- piezas ----------

    private Button ciclo(int x, int y, String etiqueta, String clave, int n, IntFunction<String> nombre) {
        return ciclo(x, y, etiqueta, clave, n, nombre, 0);
    }

    /** [<] Etiqueta: valor [>]. desplazamiento: lo que se suma al guardar (oficio: −1 = ninguno; nivel: +1). */
    private Button ciclo(int x, int y, String etiqueta, String clave, int n, IntFunction<String> nombre, int desplazamiento) {
        Button[] valor = new Button[1];
        Runnable refrescar = () -> valor[0].setMessage(Component.literal(etiqueta + ": "
                + nombre.apply(Mth.positiveModulo(t.getInt(clave) - desplazamiento, n))));
        IntFunction<Button.OnPress> mover = d -> b -> {
            t.putInt(clave, Mth.positiveModulo(t.getInt(clave) - desplazamiento + d, n) + desplazamiento);
            refrescar.run();
            if (clave.equals("mujer") || this.pestana == 0 || clave.equals("epoca")) actualizarVista();
            if (clave.equals("epoca")) {
                // El oficio pasa al de la nueva época que hace lo mismo (el granjero medieval, recolector en la prehistoria)
                int o = t.getInt("oficio");
                if (o >= 0 && o < Oficio.values().length) {
                    t.putInt("oficio", Oficio.values()[o].enEpoca(Epoca.de(t.getInt("epoca"))).ordinal());
                }
                this.init();
            }
        };
        addRenderableWidget(Button.builder(Component.literal("<"), mover.apply(-1)).bounds(x, y, 14, 20).build());
        valor[0] = addRenderableWidget(Button.builder(Component.empty(), mover.apply(1)).bounds(x + 15, y, 130, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), mover.apply(1)).bounds(x + 146, y, 14, 20).build());
        refrescar.run();
        return valor[0];
    }

    private void interruptor(int x, int y, String etiqueta, String clave, String si, String no) {
        Button[] b = new Button[1];
        b[0] = addRenderableWidget(Button.builder(Component.empty(), btn -> {
            t.putBoolean(clave, !t.getBoolean(clave));
            btn.setMessage(Component.literal(etiqueta + ": " + (t.getBoolean(clave) ? si : no)));
            actualizarVista();
            if (clave.equals("mujer")) this.init();          // cambian los peinados y lo que se ve
        }).bounds(x, y, 160, 20).build());
        b[0].setMessage(Component.literal(etiqueta + ": " + (t.getBoolean(clave) ? si : no)));
    }

    private AbstractSliderButton deslizador(int x, int y, String etiqueta, String clave, double min, double max,
                                            DoubleFunction<String> formato) {
        boolean entero = clave.equals("opinion") || clave.equals("flex");
        double actual = entero ? t.getInt(clave) : t.getFloat(clave);
        AbstractSliderButton s = new AbstractSliderButton(x, y, 160, 20, Component.empty(),
                Mth.clamp((actual - min) / (max - min), 0.0, 1.0)) {
            {
                updateMessage();
            }

            private double valor() {
                return min + this.value * (max - min);
            }

            @Override
            protected void updateMessage() {
                this.setMessage(Component.literal(etiqueta + ": " + formato.apply(valor())));
            }

            @Override
            protected void applyValue() {
                if (entero) t.putInt(clave, (int) Math.round(valor()));
                else t.putFloat(clave, (float) valor());
                actualizarVista();
            }
        };
        return addRenderableWidget(s);
    }

    private void texto(int x, int y, int ancho, String clave, String pista, int largo) {
        EditBox caja = new EditBox(this.font, x, y, ancho, 20, Component.literal(pista));
        caja.setMaxLength(largo);
        caja.setValue(t.getString(clave));
        caja.setHint(Component.literal(pista));
        caja.setResponder(v -> t.putString(clave, v));
        addRenderableWidget(caja);
    }

    // ---------- vista previa ----------

    private void actualizarVista() {
        if (this.vista == null) return;
        int aspecto = Habitante.componerAspecto(t.getInt("piel"), t.getInt("ojos"), t.getInt("colorPelo"), t.getInt("cara"),
                t.getInt("pecho"), t.getInt("caida"));
        int aspecto2 = Habitante.componerAspecto2(t.getInt("ropa"), t.getInt("peinado"), t.getInt("barba"),
                t.getInt("complemento"), t.getInt("marca"));
        boolean mujer = t.getBoolean("mujer");
        this.vista.ponerAspecto(mujer, aspecto, aspecto2, t.getFloat("altura"), t.getFloat("complexion"), t.getInt("epoca"),
                t.getFloat("edad"), mujer && t.getFloat("edad") >= 3.0f ? t.getFloat("embarazo") : 0.0f);
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        this.renderBackground(g);
        g.drawString(this.font, this.title, 10, 6, 0xFFD27F);
        if (this.vista != null) {
            int cx = Math.max(60, x1() / 2), cy = this.height / 2 + 60;
            float c = t.getFloat("edad");
            int escala = (int) (55 / Math.max(0.45f, Habitante.escala(c)));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx, cy, Math.min(110, escala),
                    cx - ratonX, cy - 90 - ratonY, this.vista);
        }
        if (this.pestana == 3) {
            g.drawString(this.font, Component.literal("Notas:"), x1(), fila(5) - 11, 0xAAAAAA);
        }
        super.render(g, ratonX, ratonY, parcial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
