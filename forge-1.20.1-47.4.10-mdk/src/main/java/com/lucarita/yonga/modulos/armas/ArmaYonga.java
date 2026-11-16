package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.modulos.sangrado.Sangrado;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

/**
 * Todas las armas del mod. Cada una puede tener (o no) dos rasgos:
 *  - parry:    click derecho para ponerse en guardia y bloquear el siguiente golpe cuerpo a cuerpo.
 *  - sangrado: probabilidad de causar Sangrado al golpear (solo si el módulo de sangrado está activo).
 */
public class ArmaYonga extends SwordItem {

    // Ticks de enfriamiento tras bloquear un golpe (20 ticks = 1 segundo)
    private static final int ENFRIAMIENTO_PARRY = 20;

    private final boolean parry;
    private final boolean sangrado;

    public ArmaYonga(Tier material, int dañoExtra, float velocidadAtaque, boolean parry, boolean sangrado, Properties propiedades) {
        super(material, dañoExtra, velocidadAtaque, propiedades);
        this.parry = parry;
        this.sangrado = sangrado;
    }

    public boolean tieneParry() {
        return parry;
    }

    // ---------- PARRY ----------

    // Al hacer click derecho, iniciamos la guardia
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        if (!parry) {
            return super.use(nivel, jugador, mano);
        }
        ItemStack item = jugador.getItemInHand(mano);
        jugador.startUsingItem(mano);
        return InteractionResultHolder.consume(item);
    }

    // Animación de bloquear
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return parry ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    // 72000 ticks = se mantiene la guardia hasta que el jugador suelte el botón
    @Override
    public int getUseDuration(ItemStack stack) {
        return parry ? 72000 : super.getUseDuration(stack);
    }

    /**
     * Un único escuchador para TODAS las armas con parry (se registra una vez en ModuloArmas).
     * Usamos LivingAttackEvent para bloquear el 100% del daño antes de que ocurra.
     */
    public static void alSerAtacado(LivingAttackEvent evento) {
        if (!(evento.getEntity() instanceof Player jugador) || !jugador.isUsingItem()) {
            return;
        }

        // Miramos el ítem con el que está haciendo guardia (vale tanto en mano principal como secundaria)
        ItemStack enUso = jugador.getUseItem();
        if (!(enUso.getItem() instanceof ArmaYonga arma) || !arma.parry) {
            return;
        }

        // Solo se bloquean golpes cuerpo a cuerpo: quien golpea directamente tiene que ser un ser vivo.
        // Así quedan fuera flechas y tridentes (el golpe directo es el proyectil) y también caídas,
        // lava, fuego, ahogamiento, el propio sangrado... (no tienen entidad que golpee).
        if (!(evento.getSource().getDirectEntity() instanceof LivingEntity)) {
            return;
        }

        // Cancelamos el ataque por completo (0 daño, sin retroceso)
        evento.setCanceled(true);

        // Rompemos la guardia y ponemos un pequeño enfriamiento para que no se pueda spamear
        jugador.stopUsingItem();
        jugador.getCooldowns().addCooldown(arma, ENFRIAMIENTO_PARRY);

        // Sonido de escudo para que se note el bloqueo
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ---------- SANGRADO ----------

    // Se ejecuta cada vez que golpeas a una entidad con el arma
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (sangrado && !atacante.level().isClientSide()) {
            // Si el módulo de sangrado está desactivado, esto simplemente no hace nada
            Sangrado.intentarAplicar(objetivo, atacante.getRandom(), Sangrado.DURACION_ARMAS);
        }
        // Método original: el arma pierde durabilidad al golpear
        return super.hurtEnemy(stack, objetivo, atacante);
    }
}
