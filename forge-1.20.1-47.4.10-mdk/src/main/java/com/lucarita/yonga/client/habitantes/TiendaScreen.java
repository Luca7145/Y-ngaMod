package com.lucarita.yonga.client.habitantes;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.habitantes.Oficio;
import com.lucarita.yonga.modulos.habitantes.Tienda;
import com.lucarita.yonga.modulos.habitantes.TiendaMenu;
import com.lucarita.yonga.modulos.habitantes.red.HabitantesRed;
import com.lucarita.yonga.modulos.habitantes.red.TiendaAccionPacket;
import com.lucarita.yonga.modulos.habitantes.red.TiendaEstadoPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * La tienda de un habitante:
 *   arriba, su nivel, la barra de experiencia (verde) y la de trabajo (ámbar);
 *   "A la venta": 9 huecos con el lote, su precio en monedas debajo; clic = comprar un lote, mayúsculas = todos;
 *   y abajo tu inventario. Un precio pactado hablando sale en verde (rebajado) o en rojo (subido).
 * Todo lo dibuja con rectángulos: no necesita textura.
 */
public class TiendaScreen extends AbstractContainerScreen<TiendaMenu> {

    private static final int ANCHO = 196, ALTO = 164;
    private static final int VENTA_Y = 38, PASO_VENTA = 20;
    private static final int BARRA_Y = 19, BARRA_ANCHO = 86;

    private static final int FONDO = 0xFFC6C6C6, OSCURO = 0xFF373737, CLARO = 0xFFFFFFFF, HUECO = 0xFF8B8B8B;
    private static final int TEXTO = 0x404040;

    public TiendaScreen(TiendaMenu menu, Inventory inventario, Component titulo) {
        super(menu, inventario, titulo);
        this.imageWidth = ANCHO;
        this.imageHeight = ALTO;
        this.inventoryLabelX = TiendaMenu.INVENTARIO_X;
        this.inventoryLabelY = TiendaMenu.INVENTARIO_Y - 11;
    }

    /** Llega un estado nuevo del servidor: se guarda en el menú abierto si es de este habitante. */
    public static void recibir(TiendaEstadoPacket estado) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof TiendaMenu menu && menu.idHabitante == estado.idHabitante()) {
            menu.estado = estado;
        }
    }

    private void enviar(int accion, int indice) {
        HabitantesRed.CANAL.sendToServer(new TiendaAccionPacket(accion, indice, Screen.hasShiftDown()));
    }

    // =====================================================================================================
    // DIBUJO
    // =====================================================================================================

    @Override
    protected void renderBg(GuiGraphics g, float parcial, int ratonX, int ratonY) {
        int x = this.leftPos, y = this.topPos;
        // Panel con borde en relieve, como los de Minecraft
        g.fill(x, y, x + ANCHO, y + ALTO, OSCURO);
        g.fill(x, y, x + ANCHO - 1, y + ALTO - 1, CLARO);
        g.fill(x + 1, y + 1, x + ANCHO - 1, y + ALTO - 1, FONDO);

        for (int i = 0; i < 36; i++) {                     // huecos del inventario
            int columna = i % 9, fila = i / 9;
            int hy = fila < 3 ? TiendaMenu.INVENTARIO_Y + fila * 18 : TiendaMenu.INVENTARIO_Y + 58;
            hueco(g, x + TiendaMenu.INVENTARIO_X - 1 + columna * 18, y + hy - 1);
        }

        TiendaEstadoPacket estado = this.menu.estado;
        if (estado == null) {
            return;
        }

        // Barras: experiencia (solo adultos que aún pueden subir) y trabajo
        barra(g, x + 8, y + BARRA_Y, estado.puedeSubir()
                ? (float) (estado.xp() - estado.xpDesde()) / Math.max(1, estado.xpHasta() - estado.xpDesde()) : 1.0f,
                estado.puedeSubir() ? 0xFF7CC444 : 0xFF9AA08C);
        barra(g, x + ANCHO - 8 - BARRA_ANCHO, y + BARRA_Y, estado.barra(), 0xFFE0A030);

        // A la venta
        for (int i = 0; i < Tienda.HUECOS; i++) {
            int hx = x + 8 + i * PASO_VENTA, hy = y + VENTA_Y;
            hueco(g, hx, hy);
            if (i >= estado.ofertas().size()) continue;
            TiendaEstadoPacket.Linea l = estado.ofertas().get(i);
            if (l.stack().isEmpty()) continue;
            if (encima(ratonX, ratonY, hx, hy)) g.fill(hx + 1, hy + 1, hx + 17, hy + 17, 0x80FFFFFF);
            g.renderItem(l.stack(), hx + 1, hy + 1);
            g.renderItemDecorations(this.font, l.stack(), hx + 1, hy + 1);
            int color = l.trato() > 0 ? 0x2E8B2E : l.trato() < 0 ? 0xB03030 : 0x806010;
            precio(g, hx + 9, hy + 20, l.precio(), color);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics g, int ratonX, int ratonY) {
        g.drawString(this.font, this.title, 8, 6, TEXTO, false);
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXTO, false);
        TiendaEstadoPacket estado = this.menu.estado;
        if (estado == null) {
            return;
        }
        Component nivel = Component.translatable("yonga.nivel." + Oficio.NIVELES[Math.max(0, Math.min(4, estado.nivel() - 1))]);
        g.drawString(this.font, nivel, ANCHO - 8 - this.font.width(nivel), 6, TEXTO, false);
        g.drawString(this.font, Component.translatable("yonga.tienda.venta"), 8, VENTA_Y - 10, TEXTO, false);
    }

    @Override
    public void render(GuiGraphics g, int ratonX, int ratonY, float parcial) {
        this.renderBackground(g);
        super.render(g, ratonX, ratonY, parcial);
        this.renderTooltip(g, ratonX, ratonY);
        consejos(g, ratonX, ratonY);
    }

    private void consejos(GuiGraphics g, int ratonX, int ratonY) {
        TiendaEstadoPacket estado = this.menu.estado;
        if (estado == null) return;
        int x = this.leftPos, y = this.topPos;
        for (int i = 0; i < estado.ofertas().size(); i++) {
            TiendaEstadoPacket.Linea l = estado.ofertas().get(i);
            if (!l.stack().isEmpty() && encima(ratonX, ratonY, x + 8 + i * PASO_VENTA, y + VENTA_Y)) {
                List<Component> lineas = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, l.stack()));
                lineas.add(Component.translatable("yonga.tienda.quedan", l.cantidad()).withStyle(ChatFormatting.GRAY));
                g.renderComponentTooltip(this.font, lineas, ratonX, ratonY);
                return;
            }
        }
        if (ratonY >= y + BARRA_Y - 1 && ratonY < y + BARRA_Y + 6) {
            if (ratonX >= x + 8 && ratonX < x + 8 + BARRA_ANCHO) {
                g.renderTooltip(this.font, estado.puedeSubir()
                        ? Component.translatable("yonga.tienda.experiencia", estado.xp() - estado.xpDesde(), estado.xpHasta() - estado.xpDesde())
                        : Component.translatable(estado.nivel() >= 5 ? "yonga.tienda.maestro" : "yonga.tienda.joven"), ratonX, ratonY);
            } else if (ratonX >= x + ANCHO - 8 - BARRA_ANCHO && ratonX < x + ANCHO - 8) {
                g.renderTooltip(this.font, Component.translatable("yonga.tienda.trabajo", Math.round(estado.barra() * 100)), ratonX, ratonY);
            }
        }
    }

    private static boolean encima(double ratonX, double ratonY, int x, int y) {
        return ratonX >= x && ratonX < x + 18 && ratonY >= y && ratonY < y + 18;
    }

    private static void hueco(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, CLARO);
        g.fill(x, y, x + 17, y + 17, OSCURO);
        g.fill(x + 1, y + 1, x + 17, y + 17, HUECO);
    }

    private static void barra(GuiGraphics g, int x, int y, float lleno, int color) {
        g.fill(x, y, x + BARRA_ANCHO, y + 5, OSCURO);
        g.fill(x + 1, y + 1, x + 1 + Math.round((BARRA_ANCHO - 2) * Math.max(0, Math.min(1, lleno))), y + 4, color);
    }

    /** "3" y una moneda pequeña, centrados en x. */
    private void precio(GuiGraphics g, int x, int y, int monedas, int color) {
        String texto = String.valueOf(monedas);
        int ancho = this.font.width(texto) + 1 + 8;
        int inicio = x - ancho / 2;
        g.drawString(this.font, texto, inicio, y, color, false);
        g.pose().pushPose();
        g.pose().translate(inicio + this.font.width(texto) + 1, y, 0);
        g.pose().scale(0.5f, 0.5f, 1.0f);
        g.renderItem(new ItemStack(ModItems.MONEDA.get()), 0, 0);
        g.pose().popPose();
    }

    // =====================================================================================================
    // CLICS
    // =====================================================================================================

    @Override
    public boolean mouseClicked(double ratonX, double ratonY, int boton) {
        TiendaEstadoPacket estado = this.menu.estado;
        if (boton == 0 && estado != null) {
            for (int i = 0; i < estado.ofertas().size(); i++) {
                if (!estado.ofertas().get(i).stack().isEmpty()
                        && encima(ratonX, ratonY, this.leftPos + 8 + i * PASO_VENTA, this.topPos + VENTA_Y)) {
                    clic();
                    enviar(TiendaAccionPacket.COMPRAR, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(ratonX, ratonY, boton);
    }

    private void clic() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
