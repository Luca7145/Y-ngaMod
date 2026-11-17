package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Parapente del Explorador (como el de Zelda: Breath of the Wild, pero sin barra de cansancio).
 *
 * En el aire, mantén click derecho para abrirlo: caes muy despacio y avanzas hacia donde miras.
 * Al soltar, tocar el suelo o meterte en el agua se cierra. Mientras planeas no hay daño de caída.
 * Gasta durabilidad como las élitras: 1 punto por segundo planeando, 432 en total (unos 7 minutos).
 * Como las élitras, no se rompe del todo: se queda en 1 y deja de funcionar hasta que lo repares (con cuero).
 */
public class ParapenteItem extends Item {

    public static final int DURABILIDAD = 432;              // la misma que las élitras

    private static final double CAIDA_MAXIMA = 0.1;         // bloques por tick hacia abajo (2 bloques/segundo)
    private static final double VELOCIDAD_OBJETIVO = 0.7;   // al rozarse con el aire queda en ~7-8 bloques/segundo
    private static final double GIRO = 0.12;                // lo rápido que se adapta a donde miras (0 a 1)
    private static final int TICKS_POR_DURABILIDAD = 20;    // 1 punto de durabilidad por segundo

    public ParapenteItem(Properties propiedades) {
        super(propiedades.durability(DURABILIDAD));
    }

    /** ¿Se puede planear ahora mismo? Hay que estar en el aire, sin élitras, sin montura, sin volar en creativo. */
    public static boolean puedePlanear(LivingEntity entidad) {
        if (entidad.onGround() || entidad.isInWater() || entidad.isInLava() || entidad.isFallFlying() || entidad.isPassenger()) {
            return false;
        }
        return !(entidad instanceof Player jugador && jugador.getAbilities().flying);
    }

    /** Como las élitras: con 1 de durabilidad deja de funcionar. */
    public static boolean roto(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    // Click derecho en el aire: se abre
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack parapente = jugador.getItemInHand(mano);
        if (!puedePlanear(jugador) || roto(parapente)) {
            return InteractionResultHolder.pass(parapente);
        }
        jugador.startUsingItem(mano);
        if (!nivel.isClientSide()) {
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA, SoundSource.PLAYERS, 1.0f, 1.2f);
        }
        return InteractionResultHolder.consume(parapente);
    }

    // Se mantiene abierto mientras se mantenga pulsado (o hasta que no se pueda planear)
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // Cada tick mientras está abierto (en el cliente y en el servidor)
    @Override
    public void onUseTick(Level nivel, LivingEntity entidad, ItemStack parapente, int ticksRestantes) {
        if (!puedePlanear(entidad) || roto(parapente)) {
            entidad.stopUsingItem();
            return;
        }

        // Planeo: caída lenta y avance hacia donde mira
        Vec3 velocidad = entidad.getDeltaMovement();
        Vec3 mirada = entidad.getLookAngle();
        Vec3 direccion = new Vec3(mirada.x, 0, mirada.z);
        direccion = direccion.lengthSqr() < 1.0E-4 ? Vec3.ZERO : direccion.normalize().scale(VELOCIDAD_OBJETIVO);

        double x = Mth.lerp(GIRO, velocidad.x, direccion.x);
        double z = Mth.lerp(GIRO, velocidad.z, direccion.z);
        double y = Math.max(velocidad.y, -CAIDA_MAXIMA);   // si sube (p. ej. tras un salto) le dejamos subir
        entidad.setDeltaMovement(x, y, z);
        entidad.fallDistance = 0;                          // sin daño de caída mientras planea

        // Durabilidad: 1 por segundo (solo en el servidor). Nunca baja de 1, como las élitras.
        int ticksUsado = getUseDuration(parapente) - ticksRestantes;
        if (!nivel.isClientSide() && ticksUsado > 0 && ticksUsado % TICKS_POR_DURABILIDAD == 0) {
            if (parapente.getDamageValue() < parapente.getMaxDamage() - 2) {
                // Respeta Irrompibilidad, como cualquier herramienta
                parapente.hurtAndBreak(1, entidad, e -> e.broadcastBreakEvent(entidad.getUsedItemHand()));
            } else {
                parapente.setDamageValue(parapente.getMaxDamage() - 1);   // se queda "roto", pero no desaparece
            }
        }
    }

    // Se repara con cuero en el yunque
    @Override
    public boolean isValidRepairItem(ItemStack parapente, ItemStack material) {
        return material.is(Items.LEATHER);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        if (roto(stack)) {
            lineas.add(Component.translatable("yonga.parapente.roto").withStyle(ChatFormatting.RED));
        }
        super.appendHoverText(stack, nivel, lineas, flag);
    }

    // Pose de brazos (agarrando el parapente por encima de la cabeza). Solo cliente.
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumidor) {
        consumidor.accept(com.lucarita.yonga.client.roles.ParapenteCliente.EXTENSIONES);
    }
}
