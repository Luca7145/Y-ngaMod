package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.modulos.habitantes.red.FamiliaPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tu árbol familiar (tecla J): arriba tus padres; en medio tus hermanos, tú y tus parejas; debajo tus hijos, cada
 * grupo colgando de la pareja con quien los tuviste; abajo del todo tus nietos. Los que han muerto, en gris.
 */
public class FamiliaScreen extends Screen {

    private static final int ALTO = 26, HUECO = 6;
    private static final int AZUL = 0xFF5B8CC9, ROSA = 0xFFD07AA0, GRIS = 0xFF808080, LINEA = 0xFFCFC2A0;

    private final FamiliaPacket datos;

    /** Una caja ya colocada. */
    private record Caja(int x, int y, int w, FamiliaPacket.Nodo nodo, String titulo, String sub, int color) {
        int centro() {
            return x + w / 2;
        }
    }

    private FamiliaScreen(FamiliaPacket datos) {
        super(Component.translatable("yonga.familia.titulo"));
        this.datos = datos;
    }

    public static void abrir(FamiliaPacket p) {
        Minecraft.getInstance().setScreen(new FamiliaScreen(p));
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
                .bounds(this.width / 2 - 50, this.height - 26, 100, 20).build());
    }

    // ---------- textos ----------

    private static String palabra(FamiliaPacket.Nodo n) {
        boolean m = n.mujer();
        String s = switch (n.papel()) {
            case FamiliaPacket.PADRE -> m ? "Madre" : "Padre";
            case FamiliaPacket.HERMANO -> m ? "Hermana" : "Hermano";
            case FamiliaPacket.PAREJA -> n.estado() == 2 ? (m ? "Esposa" : "Esposo")
                    : n.estado() == 1 ? (m ? "Novia" : "Novio") : (m ? "Madre de tus hijos" : "Padre de tus hijos");
            case FamiliaPacket.HIJO -> m ? "Hija" : "Hijo";
            default -> m ? "Nieta" : "Nieto";
        };
        if (n.etapa() == 0) s += " · bebé";
        else if (n.etapa() == 1) s += " · niñ" + (m ? "a" : "o");
        else if (n.etapa() == 2) s += " · joven";
        if (!n.vivo()) s += " · †";
        return s;
    }

    private static int color(FamiliaPacket.Nodo n) {
        return !n.vivo() ? GRIS : n.mujer() ? ROSA : AZUL;
    }

    // ---------- colocar ----------

    private int anchoCaja(int cuantas) {
        int libre = this.width - 20;
        return Math.max(46, Math.min(92, (libre - (cuantas - 1) * HUECO) / Math.max(1, cuantas)));
    }

    private List<Caja> fila(List<FamiliaPacket.Nodo> nodos, int y, int w, int centro) {
        List<Caja> cajas = new ArrayList<>();
        int total = nodos.size() * w + Math.max(0, nodos.size() - 1) * HUECO;
        int x = Math.max(10, Math.min(this.width - 10 - total, centro - total / 2));
        for (FamiliaPacket.Nodo n : nodos) {
            cajas.add(new Caja(x, y, w, n, n.nombre(), palabra(n), color(n)));
            x += w + HUECO;
        }
        return cajas;
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFE8C87A);

        List<FamiliaPacket.Nodo> padres = new ArrayList<>(), hermanos = new ArrayList<>(), parejas = new ArrayList<>(),
                hijos = new ArrayList<>(), nietos = new ArrayList<>();
        for (FamiliaPacket.Nodo n : datos.nodos()) {
            switch (n.papel()) {
                case FamiliaPacket.PADRE -> padres.add(n);
                case FamiliaPacket.HERMANO -> hermanos.add(n);
                case FamiliaPacket.PAREJA -> parejas.add(n);
                case FamiliaPacket.HIJO -> hijos.add(n);
                default -> nietos.add(n);
            }
        }
        int alto = this.height - 60;
        int y0 = 28, y1 = 28 + alto / 4, y2 = 28 + alto / 2, y3 = 28 + alto * 3 / 4;
        int centro = this.width / 2;

        // Fila del medio: hermanos · tú · parejas
        List<FamiliaPacket.Nodo> medio = new ArrayList<>(hermanos);
        FamiliaPacket.Nodo yo = new FamiliaPacket.Nodo(-1, datos.yo(), datos.yoMujer(), true, true, 0, -1, -1);
        medio.add(yo);
        medio.addAll(parejas);
        int w = anchoCaja(Math.max(medio.size(), Math.max(hijos.size(), nietos.size())));
        List<Caja> cajasMedio = fila(medio, y1, w, centro);
        Caja cajaYo = cajasMedio.get(hermanos.size());
        cajasMedio.set(hermanos.size(), new Caja(cajaYo.x(), cajaYo.y(), cajaYo.w(), yo, datos.yo(), "Tú", 0xFFE8C87A));
        cajaYo = cajasMedio.get(hermanos.size());
        List<Caja> cajasParejas = cajasMedio.subList(hermanos.size() + 1, cajasMedio.size());

        // Padres, encima de ti y tus hermanos
        if (!padres.isEmpty()) {
            List<Caja> cp = fila(padres, y0, w, centro);
            int barra = y0 + ALTO + (y1 - y0 - ALTO) / 2;
            int izq = cajasMedio.get(0).centro(), der = cajaYo.centro();
            for (Caja c : cp) linea(g, c.centro(), c.y() + ALTO, c.centro(), barra);
            linea(g, Math.min(cp.get(0).centro(), izq), barra, Math.max(cp.get(cp.size() - 1).centro(), der), barra);
            for (int i = 0; i <= hermanos.size(); i++) {
                Caja c = cajasMedio.get(i);
                linea(g, c.centro(), barra, c.centro(), c.y());
            }
            for (Caja c : cp) caja(g, c);
        }

        // Hijos, en grupos según la pareja con quien los tuviste (−1: el otro progenitor no se sabe)
        List<FamiliaPacket.Nodo> hijosOrdenados = new ArrayList<>();
        for (int gpo = -1; gpo < parejas.size(); gpo++) {
            for (FamiliaPacket.Nodo n : hijos) if (n.grupo() == gpo) hijosOrdenados.add(n);
        }
        List<Caja> cajasHijos = fila(hijosOrdenados, y2, w, centro);
        for (int gpo = -1; gpo < parejas.size(); gpo++) {
            List<Caja> grupo = new ArrayList<>();
            for (Caja c : cajasHijos) if (c.nodo().grupo() == gpo) grupo.add(c);
            if (grupo.isEmpty()) continue;
            int bajo = y1 + ALTO + 4 + (gpo + 1) * 3;        // cada pareja, un poco más abajo, para que no se crucen
            int desde = cajaYo.centro();
            int origen = desde;
            if (gpo >= 0 && gpo < cajasParejas.size()) {
                Caja pareja = cajasParejas.get(gpo);
                linea(g, desde, cajaYo.y() + ALTO, desde, bajo);
                linea(g, pareja.centro(), pareja.y() + ALTO, pareja.centro(), bajo);
                linea(g, Math.min(desde, pareja.centro()), bajo, Math.max(desde, pareja.centro()), bajo);
                origen = (desde + pareja.centro()) / 2;
            } else {
                linea(g, desde, cajaYo.y() + ALTO, desde, bajo);
            }
            int barra = y2 - 8;
            linea(g, origen, bajo, origen, barra);
            linea(g, Math.min(origen, grupo.get(0).centro()), barra, Math.max(origen, grupo.get(grupo.size() - 1).centro()), barra);
            for (Caja c : grupo) linea(g, c.centro(), barra, c.centro(), c.y());
        }

        // Nietos, debajo de su padre o madre
        List<FamiliaPacket.Nodo> nietosOrdenados = new ArrayList<>();
        for (int i = 0; i < hijos.size(); i++) {
            for (FamiliaPacket.Nodo n : nietos) if (n.grupo() == i) nietosOrdenados.add(n);
        }
        List<Caja> cajasNietos = fila(nietosOrdenados, y3, w, centro);
        for (Caja hijo : cajasHijos) {
            int indice = hijos.indexOf(hijo.nodo());
            List<Caja> grupo = new ArrayList<>();
            for (Caja c : cajasNietos) if (c.nodo().grupo() == indice) grupo.add(c);
            if (grupo.isEmpty()) continue;
            int barra = y3 - 8;
            linea(g, hijo.centro(), hijo.y() + ALTO, hijo.centro(), barra);
            linea(g, Math.min(hijo.centro(), grupo.get(0).centro()), barra,
                    Math.max(hijo.centro(), grupo.get(grupo.size() - 1).centro()), barra);
            for (Caja c : grupo) linea(g, c.centro(), barra, c.centro(), c.y());
        }

        for (Caja c : cajasMedio) caja(g, c);
        for (Caja c : cajasHijos) caja(g, c);
        for (Caja c : cajasNietos) caja(g, c);
        if (datos.nodos().isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("yonga.familia.vacia"), centro, y1 + ALTO + 12, 0xFFAAAAAA);
        }
        super.render(g, ratonX, ratonY, parcial);
    }

    private static void linea(GuiGraphics g, int x1, int y1, int x2, int y2) {
        if (x1 == x2) g.fill(x1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2) + 1, LINEA);
        else g.fill(Math.min(x1, x2), y1, Math.max(x1, x2) + 1, y1 + 1, LINEA);
    }

    private void caja(GuiGraphics g, Caja c) {
        g.fill(c.x() - 1, c.y() - 1, c.x() + c.w() + 1, c.y() + ALTO + 1, c.color());
        g.fill(c.x(), c.y(), c.x() + c.w(), c.y() + ALTO, 0xF0201810);
        String nombre = this.font.plainSubstrByWidth(c.titulo(), c.w() - 6);
        String sub = this.font.plainSubstrByWidth(c.sub(), c.w() - 6);
        g.drawCenteredString(this.font, nombre, c.centro(), c.y() + 4, c.nodo().vivo() ? 0xFFFFFFFF : 0xFFAAAAAA);
        g.pose().pushPose();
        g.pose().translate(c.centro(), c.y() + 15, 0);
        g.pose().scale(0.75f, 0.75f, 1.0f);
        g.drawCenteredString(this.font, this.font.plainSubstrByWidth(sub, (int) ((c.w() - 4) / 0.75f)), 0, 0, 0xFFBFB39A);
        g.pose().popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
