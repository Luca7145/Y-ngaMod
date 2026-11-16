package com.lucarita.yonga.client.fe;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas del sistema de fe. Se pueden cambiar en Opciones > Controles > Controles de teclado, apartado "Yönga".
 */
public final class FeTeclas {

    public static final String CATEGORIA = "key.categories.yonga";

    public static final KeyMapping REZAR = new KeyMapping("key.yonga.rezar", GLFW.GLFW_KEY_R, CATEGORIA);
    public static final KeyMapping BLASFEMAR = new KeyMapping("key.yonga.blasfemar", GLFW.GLFW_KEY_B, CATEGORIA);

    private FeTeclas() {}
}
