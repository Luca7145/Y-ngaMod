package com.lucarita.yonga.modulos.armas;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * La lanza cuando se lanza. Vuela como un tridente, hace 10 de daño al primer enemigo, rebota y cae.
 * Se queda clavada donde caiga y hay que ir a recogerla (como un tridente sin Lealtad).
 */
public class LanzaEntity extends AbstractArrow {

    public static final float DANO = 10.0f;
    // La lanza que vuela, también en el cliente (si no, allí no sabría qué dibujar)
    private static final EntityDataAccessor<ItemStack> LANZA = SynchedEntityData.defineId(LanzaEntity.class, EntityDataSerializers.ITEM_STACK);
    private ItemStack lanza = ItemStack.EMPTY;
    private boolean yaGolpeo = false;

    public LanzaEntity(EntityType<? extends LanzaEntity> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public LanzaEntity(Level nivel, LivingEntity tirador, ItemStack lanza) {
        super(ModuloArmas.LANZA_ENTIDAD.get(), tirador, nivel);
        this.lanza = lanza.copy();
        this.entityData.set(LANZA, this.lanza.copy());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LANZA, ItemStack.EMPTY);
    }

    /** Para dibujarla en el aire (en el cliente, la que manda el servidor). */
    public ItemStack lanza() {
        ItemStack enviada = this.entityData.get(LANZA);
        return enviada.isEmpty() ? new ItemStack(ModuloArmas.LANZA.get()) : enviada;
    }

    @Override
    protected ItemStack getPickupItem() {
        return lanza.copy();
    }

    // Después del primer golpe ya no golpea a nadie más (cae)
    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 desde, Vec3 hasta) {
        return yaGolpeo ? null : super.findHitEntity(desde, hasta);
    }

    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        Entity objetivo = resultado.getEntity();
        Entity dueno = getOwner();
        yaGolpeo = true;
        if (objetivo.hurt(damageSources().trident(this, dueno == null ? this : dueno), DANO)
                && objetivo instanceof LivingEntity vivo && dueno instanceof LivingEntity atacante) {
            doPostHurtEffects(vivo);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.0f);
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag datos) {
        super.addAdditionalSaveData(datos);
        datos.put("Lanza", lanza.save(new CompoundTag()));
        datos.putBoolean("YaGolpeo", yaGolpeo);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag datos) {
        super.readAdditionalSaveData(datos);
        if (datos.contains("Lanza")) {
            lanza = ItemStack.of(datos.getCompound("Lanza"));
            this.entityData.set(LANZA, lanza.copy());
        }
        yaGolpeo = datos.getBoolean("YaGolpeo");
    }

    // Como el tridente: no desaparece al minuto si está clavada
    @Override
    public void tickDespawn() {
        if (pickup != Pickup.ALLOWED) {
            super.tickDespawn();
        }
    }

    @Override
    protected float getWaterInertia() {
        return 0.99f;
    }
}
