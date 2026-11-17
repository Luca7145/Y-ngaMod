package com.lucarita.yonga.modulos.fe.hechizos;

import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.ManaDatos;
import com.lucarita.yonga.modulos.fe.TipoMana;
import net.minecraft.ChatFormatting;
import com.lucarita.yonga.modulos.roles.RolCliente;
import com.lucarita.yonga.modulos.roles.oficios.Sacerdote;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Libro de hechizo: click derecho para lanzar su hechizo.
 * Comprueba el tipo de maná y que haya suficiente; si sale bien, gasta el maná y pone el enfriamiento
 * (el enfriamiento se ve en el propio libro, como el de las perlas de Ender).
 */
public class LibroHechizo extends Item {

    private final Hechizo hechizo;

    public LibroHechizo(Hechizo hechizo, Properties propiedades) {
        super(propiedades);
        this.hechizo = hechizo;
    }

    public Hechizo hechizo() {
        return hechizo;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack libro = jugador.getItemInHand(mano);
        if (nivel.isClientSide() || !(jugador instanceof ServerPlayer servidor)) {
            return InteractionResultHolder.success(libro);
        }

        // ¿Tiene el tipo de maná correcto?
        if (ManaDatos.tipo(servidor) != hechizo.tipo()) {
            servidor.displayClientMessage(Component.translatable("yonga.hechizo.tipo_incorrecto",
                    hechizo.tipo().nombre()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(libro);
        }
        // ¿Tiene suficiente?
        if (ManaDatos.obtener(servidor) < hechizo.coste()) {
            servidor.displayClientMessage(Component.translatable("yonga.hechizo.sin_mana",
                    hechizo.coste(), hechizo.tipo().nombre()).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(libro);
        }
        // Lo lanzamos; solo si sale bien gasta maná y empieza el enfriamiento
        if (!hechizo.efecto().lanzar(servidor)) {
            return InteractionResultHolder.fail(libro);
        }
        ManaDatos.gastar(servidor, hechizo.coste());
        servidor.getCooldowns().addCooldown(this, hechizo.enfriamiento());
        return InteractionResultHolder.success(libro);
    }

    // Brillo de objeto encantado
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        // Una sola línea discreta: coste y enfriamiento ("30 Maná · 10 s"), y "II" si tu sacerdote lo tiene mejorado
        ChatFormatting color = hechizo.tipo() == TipoMana.OSCURO ? ChatFormatting.DARK_PURPLE : ChatFormatting.DARK_AQUA;
        MutableComponent linea = Component.translatable("yonga.hechizo.resumen",
                hechizo.coste(), hechizo.tipo().nombre(), hechizo.enfriamiento() / 20).withStyle(color);
        if (nivel != null && nivel.isClientSide() && Modulo.ROLES.activo()
                && Sacerdote.hechizoMejorado(RolCliente.rol(), RolCliente.rango(), hechizo)) {
            linea.append(Component.literal(" · II").withStyle(ChatFormatting.GOLD));
        }
        lineas.add(linea);
        super.appendHoverText(stack, nivel, lineas, flag);
    }
}
