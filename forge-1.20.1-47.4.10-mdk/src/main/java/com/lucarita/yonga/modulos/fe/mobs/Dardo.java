package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/**
 * Dardo de cerbatana del duende. Hace poco daño, pero lleva el efecto de su tipo:
 *  0 = veneno (Veneno II, 7 s), 1 = náusea severa (Náusea, 15 s), 2 = lentitud (Lentitud III, 6 s).
 * No se puede recoger y desaparece al rato de clavarse.
 */
public class Dardo extends AbstractArrow {

    public static final int VENENO = 0, NAUSEA = 1, LENTITUD = 2;
    private static final EntityDataAccessor<Integer> TIPO = SynchedEntityData.defineId(Dardo.class, EntityDataSerializers.INT);
    private static final double DANO = 1.0;      // ~2 de daño con la velocidad del soplido

    public Dardo(EntityType<? extends Dardo> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public Dardo(Level nivel, LivingEntity tirador, int tipo) {
        super(ModuloFe.DARDO.get(), tirador, nivel);
        this.entityData.set(TIPO, tipo);
        this.setBaseDamage(DANO);
        this.pickup = Pickup.DISALLOWED;
        this.setSoundEvent(SoundEvents.ARROW_HIT);
    }

    public int tipo() {
        return this.entityData.get(TIPO);
    }

    public static MobEffectInstance efecto(int tipo) {
        return switch (tipo) {
            case NAUSEA -> new MobEffectInstance(MobEffects.CONFUSION, 300, 1);
            case LENTITUD -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2);
            default -> new MobEffectInstance(MobEffects.POISON, 140, 1);
        };
    }

    public static Vector3f color(int tipo) {
        return switch (tipo) {
            case NAUSEA -> new Vector3f(0.95f, 0.75f, 0.2f);
            case LENTITUD -> new Vector3f(0.5f, 0.75f, 1.0f);
            default -> new Vector3f(0.35f, 0.9f, 0.3f);
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIPO, VENENO);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity victima) {
        super.doPostHurtEffects(victima);
        victima.addEffect(efecto(tipo()), this.getEffectSource());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround && this.tickCount > 1) {
            this.level().addParticle(new DustParticleOptions(color(tipo()), 0.5f), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
        }
        if (!this.level().isClientSide && this.inGround && this.inGroundTime > 100) {
            this.discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.putInt("Tipo", tipo());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        this.entityData.set(TIPO, datos.getInt("Tipo"));
    }
}
