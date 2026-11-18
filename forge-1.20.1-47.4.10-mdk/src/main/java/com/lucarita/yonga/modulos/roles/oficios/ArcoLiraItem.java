package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;

/**
 * Arco-lira del Melómano: una lira de lucarita que dispara flechas. Se sostiene en vertical (cuerdas
 * verticales) y la flecha va en horizontal, tensando las cuerdas hacia atrás. Por lo demás funciona como
 * un arco normal (usa flechas, admite los encantamientos de arco).
 *
 *  - Cada flecha que disparas suena con una nota de lira (abajo = grave, arriba = agudo, como los demás).
 *  - Ritmo (el mismo del bajo, el keytar, el banjo y la guitarra): cada flecha TENSADA DEL TODO suma 20,
 *    y la barra aguanta 6 s entre flecha y flecha (tensar lleva su tiempo). Las flechas a medio tensar
 *    suenan pero no suman.
 *  - La flecha que llena la barra es la FLECHA SÓNICA: va recta (sin caer), más rápida, hace el triple de
 *    daño, atraviesa hasta 5 enemigos, empuja mucho y revienta con un estallido sónico al chocar. Luego la
 *    barra vuelve a 0.
 *  - Durabilidad: como los demás instrumentos (2200). Se gasta 1 por flecha, como un arco. Se repara con lucarita.
 */
public class ArcoLiraItem extends BowItem {

    // ---- Ajustes ----
    private static final int DURABILIDAD = 2200;
    private static final int RITMO_POR_FLECHA = 20;          // 5 flechas tensadas del todo llenan la barra
    private static final long PAUSA_ARCO = 120;              // 6 s entre flecha y flecha
    private static final double MULTIPLICADOR_DANO = 2.0;          // con la velocidad extra: unos 18 de daño (9 corazones) tensada del todo
    private static final double MULTIPLICADOR_VELOCIDAD = 1.5;
    private static final byte ATRAVIESA = 5;
    private static final int EMPUJE = 3;

    public static final String MARCA_SONICA = "yonga_flecha_sonica";

    // Entre releaseUsing y customArrow (los dos en el mismo momento, en el servidor)
    private static boolean siguienteEsSonica = false;
    private static AbstractArrow ultimaFlecha = null;

    public ArcoLiraItem(Properties propiedades) {
        super(propiedades.durability(DURABILIDAD));
    }

    @Override
    public boolean isValidRepairItem(ItemStack arco, ItemStack material) {
        return material.is(ModItems.LUCARITA.get());
    }

    // Al soltar el click derecho: el arco normal crea y dispara la flecha (y nos la pasa en customArrow)
    @Override
    public void releaseUsing(ItemStack arco, Level nivel, LivingEntity tirador, int tiempoRestante) {
        if (!(tirador instanceof ServerPlayer jugador)) {
            super.releaseUsing(arco, nivel, tirador, tiempoRestante);
            return;
        }
        boolean tensadaDelTodo = getPowerForTime(getUseDuration(arco) - tiempoRestante) >= 1.0f;
        siguienteEsSonica = tensadaDelTodo && Musica.ritmo(jugador) + RITMO_POR_FLECHA >= Musica.RITMO_MAXIMO;
        ultimaFlecha = null;
        try {
            super.releaseUsing(arco, nivel, tirador, tiempoRestante);
        } finally {
            siguienteEsSonica = false;
        }
        if (ultimaFlecha == null) {
            return;   // no ha llegado a disparar (sin flechas o casi sin tensar)
        }
        Musica.tocarNota(jugador, ModuloRoles.SONIDO_LIRA_NOTA.get());
        if (ultimaFlecha.getPersistentData().getBoolean(MARCA_SONICA)) {
            // Más rápida que una flecha normal
            ultimaFlecha.setDeltaMovement(ultimaFlecha.getDeltaMovement().scale(MULTIPLICADOR_VELOCIDAD));
            ServerLevel servidor = jugador.serverLevel();
            servidor.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                    ModuloRoles.SONIDO_LIRA_SONICA.get(), SoundSource.RECORDS, 1.5f, 1.0f);
            servidor.sendParticles(ParticleTypes.SONIC_BOOM, jugador.getX(), jugador.getEyeY(), jugador.getZ(), 1, 0, 0, 0, 0);
            Musica.vaciarRitmo(jugador);
        } else if (tensadaDelTodo) {
            Musica.sumarRitmo(jugador, RITMO_POR_FLECHA, PAUSA_ARCO);
        }
        ultimaFlecha = null;
    }

    // Aquí el arco nos deja cambiar la flecha antes de dispararla
    @Override
    public AbstractArrow customArrow(AbstractArrow flecha) {
        ultimaFlecha = flecha;
        if (siguienteEsSonica) {
            flecha.getPersistentData().putBoolean(MARCA_SONICA, true);
            flecha.setBaseDamage(flecha.getBaseDamage() * MULTIPLICADOR_DANO);
            flecha.setNoGravity(true);
            flecha.setPierceLevel(ATRAVIESA);
            flecha.setKnockback(EMPUJE);
            flecha.setGlowingTag(true);
        }
        return flecha;
    }

    // ---------- LA FLECHA SÓNICA AL CHOCAR ----------

    public static void alImpactar(ProjectileImpactEvent evento) {
        if (!(evento.getProjectile() instanceof AbstractArrow flecha)
                || !flecha.getPersistentData().getBoolean(MARCA_SONICA)
                || !(flecha.level() instanceof ServerLevel nivel)) {
            return;
        }
        var punto = evento.getRayTraceResult().getLocation();
        nivel.sendParticles(ParticleTypes.SONIC_BOOM, punto.x, punto.y, punto.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 8; i++) {
            nivel.sendParticles(ParticleTypes.NOTE, punto.x, punto.y + 0.3, punto.z, 0, i / 8.0, 0, 0, 1.0);
        }
        if (evento.getRayTraceResult().getType() == HitResult.Type.BLOCK) {
            // Al clavarse ya no es sónica (que no siga brillando en la pared)
            flecha.getPersistentData().remove(MARCA_SONICA);
            flecha.setGlowingTag(false);
            flecha.setNoGravity(false);
        }
    }
}
