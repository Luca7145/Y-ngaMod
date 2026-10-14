package com.lucarita.yonga.core;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArmaParryItem extends SwordItem {

    public ArmaParryItem(Tier material, int dañoExtra, float velocidadAtaque, Properties propiedades) {
        super(material, dañoExtra, velocidadAtaque, propiedades);
        MinecraftForge.EVENT_BUS.register(this);
    }

    // Al hacer click derecho, iniciamos la guardia
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack item = jugador.getItemInHand(mano);
        jugador.startUsingItem(mano);
        return InteractionResultHolder.consume(item);
    }

    // Animación de bloquear
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // ¡NUEVO!: Usamos LivingAttackEvent para bloquear el 100% del daño antes de que ocurra
    @SubscribeEvent
    public void alSerAtacado(LivingAttackEvent evento) {
        // Comprobamos que el que recibe el golpe es un jugador
        if (evento.getEntity() instanceof Player jugador) {
            ItemStack itemPrincipal = jugador.getMainHandItem();

            // Verificamos si tiene nuestra arma en la mano y está en estado de guardia
            if (itemPrincipal.getItem() == this && jugador.isUsingItem()) {

                // 1. FILTRO DE PROYECTILES
                // Si la entidad que hace el daño es un proyectil (flecha, tridente, etc.), ignoramos el parry
                if (evento.getSource().getDirectEntity() instanceof Projectile) {
                    return; // Terminamos aquí, el jugador recibe el daño de la flecha
                }

                // 2. BLOQUEO DEL ATAQUE (Mobs y cuerpo a cuerpo)
                // Cancelamos el ataque por completo (0 daño, sin retroceso)
                evento.setCanceled(true);

                // 3. ROMPER LA GUARDIA
                // Forzamos al jugador a salir del estado de bloqueo
                jugador.stopUsingItem();

                // Opcional: Le ponemos un enfriamiento muy pequeño (ej: 1 segundo = 20 ticks)
                // para que no pueda spamear el click derecho a la velocidad de la luz.
                jugador.getCooldowns().addCooldown(this, 20);

                // 4. FEEDBACK VISUAL/SONORO
                // Reproducimos el sonido del escudo al bloquear para que se sienta bien
                jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }
}