package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModArmaduras;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * BOTAS DE MUELLES (Ingeniero, Oficial). Protegen como unas de hierro y llevan muelles en la suela:
 *  - Saltas más alto (como con Salto II: unos 2 bloques y medio).
 *  - Las caídas de hasta 12 bloques no hacen daño, y las de más altura hacen el daño de 12 bloques menos.
 *  - Al caer de más de 4 bloques, rebotan con un "boing".
 * Las mayúsculas no cambian nada: el salto extra siempre está.
 */
public class BotasMuelles extends ArmorItem {

    private static final double SALTO_EXTRA = 0.2;          // lo mismo que suma Salto II
    private static final float CAIDA_AMORTIGUADA = 9.0f;    // + los 3 bloques que ya no hacen daño = 12

    public BotasMuelles(Properties propiedades) {
        super(ModArmaduras.MUELLES, Type.BOOTS, propiedades);
    }

    private static boolean lasLleva(LivingEntity entidad) {
        return entidad.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof BotasMuelles;
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    public static void alSaltar(LivingEvent.LivingJumpEvent evento) {
        LivingEntity entidad = evento.getEntity();
        if (lasLleva(entidad)) {
            Vec3 v = entidad.getDeltaMovement();
            entidad.setDeltaMovement(v.x, v.y + SALTO_EXTRA, v.z);
            if (entidad instanceof Player) {
                entidad.level().playSound(null, entidad.getX(), entidad.getY(), entidad.getZ(), SoundEvents.CHAIN_STEP,
                        SoundSource.PLAYERS, 0.4f, 1.8f);
            }
        }
    }

    public static void alCaer(LivingFallEvent evento) {
        LivingEntity entidad = evento.getEntity();
        if (!lasLleva(entidad)) return;
        if (evento.getDistance() > 4 && !entidad.level().isClientSide()) {
            entidad.level().playSound(null, entidad.getX(), entidad.getY(), entidad.getZ(), SoundEvents.SLIME_JUMP,
                    SoundSource.PLAYERS, 0.8f, 0.7f);
        }
        evento.setDistance(Math.max(0, evento.getDistance() - CAIDA_AMORTIGUADA));
    }
}
