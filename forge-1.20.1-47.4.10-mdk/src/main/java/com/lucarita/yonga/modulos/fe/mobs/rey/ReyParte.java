package com.lucarita.yonga.modulos.fe.mobs.rey;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.PartEntity;

/**
 * Un trozo del Rey de Arenisca (un hueso o uno de sus sellos): su caja sigue al esqueleto. Los golpes se los pasa al
 * rey (que decide si hacen algo: solo le duelen en los sellos) y el clic derecho es para AGARRARSE a él.
 */
public class ReyParte extends PartEntity<ReyDeArenisca> {

    /** El hueso (0..14) o, si es un sello, -1. */
    public final int hueso;
    /** El sello (0..2) o, si es un hueso, -1. */
    public final int sello;

    public ReyParte(ReyDeArenisca rey, int hueso, int sello) {
        super(rey);
        this.hueso = hueso;
        this.sello = sello;
        this.refreshDimensions();
    }

    /** Coloca la caja (la del hueso girado, envuelta en una caja recta). */
    public void colocar(AABB caja) {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        this.setPosRaw((caja.minX + caja.maxX) * 0.5, caja.minY, (caja.minZ + caja.maxZ) * 0.5);
        this.setBoundingBox(caja);
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
        return !this.isRemoved() && this.getParent().hurtParte(this, fuente, cantidad);
    }

    @Override
    public InteractionResult interact(Player jugador, InteractionHand mano) {
        if (mano != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide()) {
            this.getParent().agarrar(jugador);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean is(Entity otra) {
        return this == otra || this.getParent() == otra;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(2.0f, 2.0f);
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
