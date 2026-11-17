package com.lucarita.yonga.modulos.roles;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * Los oficios que puede tener un jugador.
 *
 * Para añadir uno nuevo: añade una línea aquí (id e icono) y sus textos en los archivos de idioma
 * ("yonga.rol.<id>" y "yonga.rol.<id>.desc"). El menú de elección lo muestra solo.
 * ¡No cambies el id de un rol que ya existe! Es lo que se guarda en el mundo.
 */
public enum Rol {
    HERRERO("herrero", Items.ANVIL),
    EXPLORADOR("explorador", Items.COMPASS),
    SACERDOTE("sacerdote", Items.CANDLE),
    INGENIERO("ingeniero", Items.PISTON),
    MELOMANO("melomano", Items.JUKEBOX);

    private final String id;
    private final ItemLike icono;

    Rol(String id, ItemLike icono) {
        this.id = id;
        this.icono = icono;
    }

    public String id() {
        return id;
    }

    /** Objeto que se dibuja junto al nombre del rol en el menú. */
    public ItemStack icono() {
        return new ItemStack(icono);
    }

    public Component nombre() {
        return Component.translatable("yonga.rol." + id);
    }

    public Component descripcion() {
        return Component.translatable("yonga.rol." + id + ".desc");
    }

    /** El rol con ese id, o null si no existe (o si el texto está vacío = sin rol). */
    public static Rol porId(String id) {
        for (Rol rol : values()) {
            if (rol.id.equals(id)) {
                return rol;
            }
        }
        return null;
    }
}
