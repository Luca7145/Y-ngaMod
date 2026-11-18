package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Onda sonora de la espada-keytar (su ataque especial con la barra de ritmo llena).
 *
 * Vuela en línea recta, sin gravedad, unos 45 bloques. ATRAVIESA a todos los mobs y jugadores que pille
 * por el camino (a cada uno le hace daño una sola vez) y los empuja; se deshace al chocar con un bloque.
 * A otros jugadores solo les hace daño si el PvP lo permite.
 * No tiene dibujo propio: lo que se ve es el rastro (ondas y notas).
 */
public class NotaProyectil extends ThrowableProjectile {

    private static final float DANO = 8.0f;             // 4 corazones
    private static final double EMPUJE = 1.2;
    private static final int VIDA = 25;                  // ticks

    // A quién ha golpeado ya (para no dañarlo dos veces)
    private final Set<UUID> golpeados = new HashSet<>();

    public NotaProyectil(EntityType<? extends NotaProyectil> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public NotaProyectil(Level nivel, LivingEntity quienLanza) {
        super(ModuloRoles.NOTA_PROYECTIL.get(), quienLanza, nivel);
    }

    @Override
    protected void defineSynchedData() {
    }

    // Sin gravedad: va recta
    @Override
    protected float getGravity() {
        return 0.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // Rastro: una onda sónica cada 2 ticks y notitas de colores alrededor
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.NOTE,
                        this.getX() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getY() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.getZ() + (this.random.nextDouble() - 0.5) * 0.8,
                        this.random.nextDouble(), 0.0, 0.0);
            }
        } else if (this.tickCount > VIDA) {
            this.discard();
        }
    }

    // Choca con cualquier ser vivo (menos soportes de armadura) que aún no haya golpeado.
    // A los jugadores solo les hace daño si el PvP lo permite (lo comprueba Minecraft al hacer el daño).
    @Override
    protected boolean canHitEntity(Entity entidad) {
        return super.canHitEntity(entidad)
                && entidad instanceof LivingEntity && !(entidad instanceof ArmorStand)
                && !golpeados.contains(entidad.getUUID());
    }

    // Al golpear a alguien: daño y empujón, y sigue su camino (atraviesa)
    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        Entity entidad = resultado.getEntity();
        golpeados.add(entidad.getUUID());
        if (this.level().isClientSide()) {
            return;
        }
        if (entidad.hurt(this.damageSources().thrown(this, this.getOwner()), DANO) && entidad instanceof LivingEntity vivo) {
            Vec3 direccion = this.getDeltaMovement();
            vivo.knockback(EMPUJE, -direccion.x, -direccion.z);
            vivo.hurtMarked = true;
        }
    }

    // Al chocar con un bloque se deshace
    @Override
    protected void onHitBlock(BlockHitResult resultado) {
        super.onHitBlock(resultado);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }
}
