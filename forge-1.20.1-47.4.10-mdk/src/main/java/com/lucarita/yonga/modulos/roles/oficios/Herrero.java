package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import com.lucarita.yonga.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.EnumMap;
import java.util.Map;

/**
 * Habilidades del oficio Herrero (sus armas, armaduras y herramientas van en sus recetas).
 *
 *  - Maña con el yunque: reparar, encantar con libros o cambiar el nombre en el yunque le cuesta menos
 *    niveles de experiencia, y menos cuanto más rango tiene. Como mínimo cuesta 1 nivel.
 *    (Lo aplica AnvilMenuMixin, en el servidor.)
 *    El límite de "¡Demasiado caro!" de Minecraft (40 niveles) se mira con el precio de antes del descuento.
 *  - Fundición doble: al sacar del horno lingotes de hierro, oro, cobre o lucarita, cada lingote tiene
 *    probabilidad de salir doble (10% Novato, 20% Aprendiz, 30% Oficial, 40% Experto, 50% Maestro).
 *    Solo lo que saca él a mano (no lo que sacan las tolvas).
 */
public final class Herrero {

    // Cuánto paga del precio normal del yunque según su rango
    private static final Map<RangoRol, Double> PRECIO_YUNQUE = new EnumMap<>(Map.of(
            RangoRol.NOVATO, 0.9,      // -10%
            RangoRol.APRENDIZ, 0.8,    // -20%
            RangoRol.OFICIAL, 0.7,     // -30%
            RangoRol.EXPERTO, 0.6,     // -40%
            RangoRol.MAESTRO, 0.5      // -50%
    ));

    // Probabilidad de lingote extra al fundir, según su rango
    private static final Map<RangoRol, Double> FUNDICION_DOBLE = new EnumMap<>(Map.of(
            RangoRol.NOVATO, 0.10,
            RangoRol.APRENDIZ, 0.20,
            RangoRol.OFICIAL, 0.30,
            RangoRol.EXPERTO, 0.40,
            RangoRol.MAESTRO, 0.50
    ));

    private Herrero() {}

    private static boolean esLingote(ItemStack objeto) {
        return objeto.is(Items.IRON_INGOT) || objeto.is(Items.GOLD_INGOT) || objeto.is(Items.COPPER_INGOT)
                || objeto.is(ModItems.LUCARITA.get());
    }

    public static void alFundir(PlayerEvent.ItemSmeltedEvent evento) {
        ItemStack sacado = evento.getSmelting();
        if (!(evento.getEntity() instanceof ServerPlayer jugador) || RolDatos.obtenerRol(jugador) != Rol.HERRERO
                || !esLingote(sacado)) {
            return;
        }
        double probabilidad = FUNDICION_DOBLE.getOrDefault(RolDatos.obtenerRango(jugador), 0.0);
        int extra = 0;
        for (int i = 0; i < sacado.getCount(); i++) {
            if (jugador.getRandom().nextDouble() < probabilidad) {
                extra++;
            }
        }
        if (extra > 0) {
            ItemStack regalo = new ItemStack(sacado.getItem(), extra);
            if (!jugador.getInventory().add(regalo)) {
                jugador.drop(regalo, false);
            }
            jugador.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                    SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 0.4f, 1.4f);
            jugador.displayClientMessage(Component.translatable("yonga.herrero.fundicion_doble", extra, regalo.getHoverName())
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    /** Niveles que paga este jugador por algo que en el yunque cuesta "precio" niveles. */
    public static int precioYunque(Player jugador, int precio) {
        if (precio <= 0 || !Modulo.ROLES.activo() || RolDatos.obtenerRol(jugador) != Rol.HERRERO) {
            return precio;
        }
        double factor = PRECIO_YUNQUE.getOrDefault(RolDatos.obtenerRango(jugador), 1.0);
        return Math.max(1, (int) Math.round(precio * factor));
    }
}
