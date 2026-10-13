package com.lucarita.yonga.core;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class MandobleItem extends SwordItem {

    // Registramos la clase para escuchar eventos cuando se crea
    public MandobleItem(Tier material, int dañoExtra, float velocidadAtaque, Properties propiedades) {
        super(material, dañoExtra, velocidadAtaque, propiedades);
        MinecraftForge.EVENT_BUS.register(this);
    }

    // 1. COMPROBACIÓN ACTIVA EN EL INVENTARIO
    // Esto se ejecuta cada "tick" (20 veces por segundo) mientras el ítem esté en el inventario del jugador
    @Override
    public void inventoryTick(ItemStack mandoble, Level nivel, Entity entidad, int slot, boolean estaSeleccionado) {
        if (estaSeleccionado && entidad instanceof Player jugador) {
            // Si el jugador lo tiene en la mano principal pero la mano izquierda (Offhand) no está vacía
            if (!jugador.getOffhandItem().isEmpty()) {
                // "Desequipamos" visualmente el ítem (guardamos el mandoble en otra ranura libre si se puede)
                // O de forma más simple, aplicamos fatiga para que no pueda usarlo, o cancelamos el click.
                // Como pediste que se vean las manos vacías, la forma más segura sin perder el ítem es desequiparlo
                int ranuraLibre = jugador.getInventory().getFreeSlot();
                if (ranuraLibre != -1) {
                    jugador.getInventory().setItem(ranuraLibre, mandoble.copy());
                    jugador.getMainHandItem().shrink(1);
                    // Opcional: jugador.displayClientMessage(Component.literal("Necesitas las dos manos libres."), true);
                }
            }
        }
        super.inventoryTick(mandoble, nivel, entidad, slot, estaSeleccionado);
    }

    // 2. LA GUARDIA (CLICK DERECHO)
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack item = jugador.getItemInHand(mano);
        // Si tiene el mandoble en la mano principal y la secundaria está vacía
        if (mano == InteractionHand.MAIN_HAND && jugador.getOffhandItem().isEmpty()) {
            jugador.startUsingItem(mano); // Inicia la animación de uso
            return InteractionResultHolder.consume(item);
        }
        return InteractionResultHolder.pass(item);
    }

    // Le decimos al juego que la animación de uso es hacer guardia (como un escudo)
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    // Cuánto tiempo máximo puede mantener la guardia (72000 ticks = infinito hasta que suelte el botón)
    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    // 3. EL EVENTO DE PARRY (CUANDO RECIBE DAÑO)
    // Esto es un evento global que escucha cualquier daño a cualquier entidad
    @SubscribeEvent
    public void alRecibirDaño(LivingHurtEvent evento) {
        if (evento.getEntity() instanceof Player jugador) {
            ItemStack itemPrincipal = jugador.getMainHandItem();

            // Si el jugador está usando nuestro mandoble y está en medio del "Uso" (haciendo guardia)
            if (itemPrincipal.getItem() == this && jugador.isUsingItem()) {

                // Reducimos un poco el daño recibido (la guardia funcionó a medias)
                evento.setAmount(evento.getAmount() * 0.5f);

                // Aplicamos el Cooldown al ítem (el jugador no podrá usar el mandoble durante 100 ticks = 5 segundos)
                jugador.getCooldowns().addCooldown(this, 100);

                // Detenemos la guardia a la fuerza
                jugador.stopUsingItem();

                // Podemos reproducir un sonido aquí si quisieras (ej: choque de metales)
            }
        }
    }
}