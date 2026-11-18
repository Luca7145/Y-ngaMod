package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.client.fe.FeTeclas;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Teclas de los oficios. Se pueden cambiar en Opciones > Controles > Controles de teclado, apartado "Yönga".
 */
public final class RolesTeclas {

    // Silencia / vuelve a activar las notas de los instrumentos del Melómano (las habilidades siguen igual)
    public static final KeyMapping SILENCIAR_NOTAS = new KeyMapping("key.yonga.silenciar_notas", GLFW.GLFW_KEY_M, FeTeclas.CATEGORIA);

    // Toca la armónica (si la llevas en la cara)
    public static final KeyMapping TOCAR_ARMONICA = new KeyMapping("key.yonga.tocar_armonica", GLFW.GLFW_KEY_H, FeTeclas.CATEGORIA);

    private RolesTeclas() {}
}
