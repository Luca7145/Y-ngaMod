package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Nota musical disparada por la espada-keytar. Vuela en línea recta (sin gravedad), deja un rastro de
 * notitas del color de la nota tocada y hace daño a lo primero que toque. Desaparece al chocar o al
 * cabo de un segundo (unos 30 bloques).
 * No tiene dibujo propio: lo que se ve es el rastro de notas.
 * A otros jugadores solo les hace daño si el PvP lo permite (Minecraft lo comprueba solo).
 */
public class NotaProyectil extends ThrowableProjectile {

    private static final float DANO = 4.0f;            // 2 corazones
    private static final int VIDA = 20;                 // ticks (1 segundo)

    // Color de las notitas (0 a 1, como en los bloques musicales), sincronizado con los clientes
    private static final EntityDataAccessor<Float> COLOR = SynchedEntityData.defineId(NotaProyectil.class, EntityDataSerializers.FLOAT);

    public NotaProyectil(EntityType<? extends NotaProyectil> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public NotaProyectil(Level nivel, LivingEntity quienDispara, float color) {
        super(ModuloRoles.NOTA_PROYECTIL.get(), quienDispara, nivel);
        this.entityData.set(COLOR, color);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(COLOR, 0.0f);
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
            // Rastro de notitas
            this.level().addParticle(ParticleTypes.NOTE, this.getX(), this.getY(), this.getZ(), this.entityData.get(COLOR), 0.0, 0.0);
        } else if (this.tickCount > VIDA) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        super.onHitEntity(resultado);
        resultado.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DANO);
    }

    @Override
    protected void onHit(HitResult resultado) {
        super.onHit(resultado);
        if (!this.level().isClientSide()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.6f);
            this.discard();
        }
    }
}
