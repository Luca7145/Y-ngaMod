package com.lucarita.yonga.modulos;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Lista de módulos del mod que se pueden activar o desactivar.
 *
 * Para añadir un módulo nuevo (por ejemplo el sistema de fe):
 *   1. Añade una entrada aquí: FE("fe")
 *   2. Añade sus textos en los archivos de idioma: "yonga.modulo.fe" y "yonga.modulo.fe.desc"
 *   3. Crea su paquete (modulos/fe) con una clase ModuloFe que tenga un método registrar(IEventBus)
 *   4. Llama a ModuloFe.registrar(...) en YongaMain dentro de un "if (Modulo.FE.activo())"
 *   5. En sus recetas JSON añade la condición {"type": "yonga:modulo_activo", "modulo": "fe"}
 *
 * El archivo de configuración y la pantalla del juego se generan solos a partir de esta lista.
 */
public enum Modulo {
    ARMAS("armas"),
    SANGRADO("sangrado"),
    FE("fe"),
    ROLES("roles"),
    TABACO("tabaco");

    private final String id;

    // Valor guardado en el archivo de configuración (puede cambiar desde la pantalla de opciones)
    ForgeConfigSpec.BooleanValue opcion;

    // Si el módulo está cargado EN ESTA PARTIDA. Se fija al arrancar el juego y no cambia hasta reiniciar,
    // porque los ítems, efectos, etc. solo se pueden registrar al arrancar.
    private boolean activo = true;

    Modulo(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    /** ¿Está este módulo cargado ahora mismo? Úsalo siempre antes de tocar algo de otro módulo. */
    public boolean activo() {
        return activo;
    }

    void fijarActivo(boolean valor) {
        this.activo = valor;
    }

    public static Modulo porId(String id) {
        for (Modulo modulo : values()) {
            if (modulo.id.equals(id)) {
                return modulo;
            }
        }
        return null;
    }
}
