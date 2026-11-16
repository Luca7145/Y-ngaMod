package com.lucarita.yonga.client;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.ModulosConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Pantalla Mods -> Yönga -> Config: un interruptor por cada módulo.
 * Se genera sola a partir de la lista de Modulo, así que los módulos nuevos aparecen aquí sin tocar nada.
 */
public class ModulosScreen extends Screen {

    private static final int ANCHO_BOTON = 240;
    private static final int ALTO_FILA = 24;

    private final Screen pantallaAnterior;
    private final Map<Modulo, Boolean> valores = new EnumMap<>(Modulo.class);

    public ModulosScreen(Screen pantallaAnterior) {
        super(Component.translatable("yonga.config.titulo"));
        this.pantallaAnterior = pantallaAnterior;
        for (Modulo modulo : Modulo.values()) {
            valores.put(modulo, ModulosConfig.valorGuardado(modulo));
        }
    }

    @Override
    protected void init() {
        int x = this.width / 2 - ANCHO_BOTON / 2;
        int y = 50;

        for (Modulo modulo : Modulo.values()) {
            Component descripcion = Component.translatable("yonga.modulo." + modulo.id() + ".desc");
            addRenderableWidget(CycleButton.onOffBuilder(valores.get(modulo))
                    .withTooltip(valor -> Tooltip.create(descripcion))
                    .create(x, y, ANCHO_BOTON, 20,
                            Component.translatable("yonga.modulo." + modulo.id()),
                            (boton, valor) -> valores.put(modulo, valor)));
            y += ALTO_FILA;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, boton -> onClose())
                .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graficos, int ratonX, int ratonY, float parcial) {
        renderBackground(graficos);
        graficos.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        super.render(graficos, ratonX, ratonY, parcial);

        // Aviso si lo elegido no coincide con lo que está cargado ahora mismo
        if (hayCambiosRespectoALoCargado()) {
            graficos.drawCenteredString(this.font, Component.translatable("yonga.config.reiniciar"),
                    this.width / 2, this.height - 44, 0xFFFF55);
        }
    }

    private boolean hayCambiosRespectoALoCargado() {
        for (Modulo modulo : Modulo.values()) {
            if (valores.get(modulo) != modulo.activo()) {
                return true;
            }
        }
        return false;
    }

    // Al salir (botón Hecho o Esc) se guarda el archivo y se vuelve a la pantalla de Mods
    @Override
    public void onClose() {
        ModulosConfig.guardar(valores);
        this.minecraft.setScreen(pantallaAnterior);
    }
}
