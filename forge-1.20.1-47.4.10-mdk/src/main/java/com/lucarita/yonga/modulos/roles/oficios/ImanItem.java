package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * IMÁN (Ingeniero, Novato). Llevándolo en la mano secundaria atrae hacia ti los objetos sueltos y la experiencia que
 * haya a 7 bloques o menos. Click derecho lo enciende y lo apaga (por si no quieres que recoja algo); apagado no
 * brilla.
 *
 * No atrae lo que acabas de tirar tú (mientras no se pueda recoger) ni lo que está encima de una tolva (para no
 * romper granjas ni sistemas de recogida).
 */
public class ImanItem extends Item {

    public static final double RADIO = 7.0;
    private static final double FUERZA = 0.12;
    private static final String CLAVE_APAGADO = "Apagado";

    public ImanItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    public static boolean encendido(ItemStack iman) {
        return !iman.hasTag() || !iman.getTag().getBoolean(CLAVE_APAGADO);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack iman = jugador.getItemInHand(mano);
        boolean ahora = !encendido(iman);
        if (ahora) {
            iman.removeTagKey(CLAVE_APAGADO);
        } else {
            iman.getOrCreateTag().putBoolean(CLAVE_APAGADO, true);
        }
        nivel.playSound(jugador, jugador.blockPosition(), ahora ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.3f, 2.0f);
        return InteractionResultHolder.sidedSuccess(iman, nivel.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack iman) {
        return encendido(iman);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        return cambioDeHueco || !ItemStack.isSameItem(antes, despues);
    }

    @Override
    public void appendHoverText(ItemStack iman, @Nullable Level nivel, List<Component> texto, TooltipFlag flag) {
        texto.add(Component.translatable(encendido(iman) ? "yonga.iman.encendido" : "yonga.iman.apagado")
                .withStyle(encendido(iman) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }

    // ---------- EVENTOS (se registra en ModuloRoles) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        Player jugador = evento.player;
        if (evento.phase != TickEvent.Phase.END || jugador.isSpectator() || jugador.tickCount % 2 != 0) return;
        ItemStack iman = jugador.getOffhandItem();
        if (!(iman.getItem() instanceof ImanItem) || !encendido(iman)) return;

        Vec3 centro = jugador.position().add(0, jugador.getBbHeight() * 0.4, 0);
        AABB zona = jugador.getBoundingBox().inflate(RADIO);
        for (ItemEntity objeto : jugador.level().getEntitiesOfClass(ItemEntity.class, zona,
                o -> o.isAlive() && !o.hasPickUpDelay() && !sobreTolva(o))) {
            atraer(objeto, centro);
        }
        for (ExperienceOrb orbe : jugador.level().getEntitiesOfClass(ExperienceOrb.class, zona, Entity::isAlive)) {
            atraer(orbe, centro);
        }
    }

    private static void atraer(Entity cosa, Vec3 centro) {
        Vec3 hacia = centro.subtract(cosa.position());
        double distancia = hacia.length();
        if (distancia > RADIO || distancia < 0.3) return;
        Vec3 tiron = hacia.scale(FUERZA / distancia * (1.5 - distancia / RADIO));
        cosa.setDeltaMovement(cosa.getDeltaMovement().scale(0.8).add(tiron));
        cosa.hurtMarked = true;
    }

    private static boolean sobreTolva(ItemEntity objeto) {
        return objeto.level().getBlockState(objeto.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.HOPPER)
                || objeto.level().getBlockState(objeto.blockPosition()).is(net.minecraft.world.level.block.Blocks.HOPPER);
    }
}
