package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.cara.Cara;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

/**
 * IMÁN (Ingeniero, Novato). Va en el hueco de la cara (se ve colgado en el pecho, ver client/roles/CapaIman) y atrae
 * hacia ti los objetos sueltos y la experiencia que haya a 7 bloques o menos. Siempre está encendido.
 *
 * No atrae lo que acabas de tirar tú (mientras no se pueda recoger) ni lo que está encima de una tolva (para no
 * romper granjas ni sistemas de recogida).
 */
public class ImanItem extends Item {

    public static final double RADIO = 7.0;
    private static final double FUERZA = 0.12;

    public ImanItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    // ---------- EVENTOS (se registra en ModuloRoles) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        Player jugador = evento.player;
        if (evento.phase != TickEvent.Phase.END || jugador.isSpectator() || jugador.tickCount % 2 != 0) return;
        if (!(Cara.obtener(jugador).getItem() instanceof ImanItem)) return;

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
