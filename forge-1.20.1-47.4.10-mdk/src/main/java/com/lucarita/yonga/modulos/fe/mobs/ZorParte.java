package com.lucarita.yonga.modulos.fe.mobs;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.entity.PartEntity;

/**
 * Un tramo del cuerpo de Zor (como las partes del dragón del End). No piensa ni se mueve solo: Zor lo coloca cada
 * tick sobre el rastro de su cabeza. Se le puede golpear (cuerpo a cuerpo y con flechas) y el daño se lo lleva Zor.
 */
public class ZorParte extends PartEntity<ZorElQuemado> {

    public final int indice;
    private final EntityDimensions tamano;

    public ZorParte(ZorElQuemado zor, int indice, float ancho) {
        super(zor);
        this.indice = indice;
        this.tamano = EntityDimensions.scalable(ancho, ancho);
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag datos) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag datos) {
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        return !this.isRemoved() && this.getParent().hurtParte(this, fuente, cantidad);   // Zor decide qué le hace daño
    }

    @Override
    public boolean is(Entity otra) {
        return this == otra || this.getParent() == otra;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return tamano;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return this.getParent().getPickResult();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
