package com.lucarita.yonga.modulos;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Map;

/**
 * Archivo config/yonga-modulos.toml con qué módulos están activados.
 *
 * Ojo: NO se registra con context.registerConfig(...) como una config normal de Forge, porque Forge
 * carga esas configs DESPUÉS de que el mod se construya, y nosotros necesitamos saber qué módulos
 * están activos justo al principio (para decidir qué ítems registrar). Por eso lo leemos a mano.
 */
public final class ModulosConfig {

    public static final String NOMBRE_ARCHIVO = "yonga-modulos.toml";
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment(
                "Activa o desactiva partes del mod Yonga.",
                "Los cambios se aplican al REINICIAR el juego.",
                "En multijugador, el servidor y los jugadores deben tener los mismos modulos activados.",
                "Si desactivas un modulo en un mundo ya existente, sus objetos desapareceran de ese mundo."
        ).push("modulos");

        for (Modulo modulo : Modulo.values()) {
            modulo.opcion = builder.define(modulo.id(), true);
        }

        builder.pop();
        SPEC = builder.build();
    }

    private ModulosConfig() {}

    /** Lee (o crea) el archivo y fija qué módulos están activos en esta partida. Llamar al principio del constructor del mod. */
    public static void cargar() {
        Path ruta = FMLPaths.CONFIGDIR.get().resolve(NOMBRE_ARCHIVO);

        CommentedFileConfig archivo = CommentedFileConfig.builder(ruta, TomlFormat.instance())
                .sync()
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build();
        archivo.load();

        // Enlaza el archivo con la especificación. Si faltan valores (o el archivo es nuevo) los rellena y lo guarda.
        SPEC.setConfig(archivo);

        for (Modulo modulo : Modulo.values()) {
            modulo.fijarActivo(modulo.opcion.get());
        }
    }

    /** Valor que hay escrito en el archivo (puede ser distinto de lo que está activo si se cambió y aún no se ha reiniciado). */
    public static boolean valorGuardado(Modulo modulo) {
        return modulo.opcion.get();
    }

    /** Escribe los nuevos valores en el archivo. Se aplicarán al reiniciar. */
    public static void guardar(Map<Modulo, Boolean> valores) {
        valores.forEach((modulo, valor) -> modulo.opcion.set(valor));
        SPEC.save();
    }

}
