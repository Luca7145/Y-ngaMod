package com.lucarita.yonga.modulos.fe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Orbe de fe: como un orbe de experiencia, pero al recogerlo da (azul) o quita (negro) puntos de fe.
 * Lo sueltan los mobs al morir a manos de un jugador (ver OrbesFe).
 * El movimiento (caer, rebotar, ir hacia el jugador cercano) está copiado del orbe de experiencia de Minecraft.
 */
public class OrbeFe extends Entity {

    // Valor sincronizado con los clientes para que sepan de qué color y tamaño dibujarlo
    private static final EntityDataAccessor<Float> VALOR = SynchedEntityData.defineId(OrbeFe.class, EntityDataSerializers.FLOAT);

    private static final int VIDA_MAXIMA = 6000;      // 5 minutos, como la experiencia
    private static final double DISTANCIA_ATRACCION = 8.0;

    private int edad;
    private Player seguido;

    public OrbeFe(EntityType<? extends OrbeFe> tipo, Level nivel) {
        super(tipo, nivel);
    }

    /** Crea un orbe en (x, y, z) con un valor: positivo = azul (suma fe), negativo = negro (resta fe). */
    public static OrbeFe crear(ServerLevel nivel, double x, double y, double z, float valor) {
        OrbeFe orbe = new OrbeFe(ModuloFe.ORBE_FE.get(), nivel);
        orbe.setPos(x, y, z);
        orbe.setYRot((float) (nivel.random.nextDouble() * 360.0));
        orbe.setDeltaMovement((nivel.random.nextDouble() * 0.2 - 0.1) * 2.0,
                nivel.random.nextDouble() * 0.2 * 2.0,
                (nivel.random.nextDouble() * 0.2 - 0.1) * 2.0);
        orbe.entityData.set(VALOR, valor);
        return orbe;
    }

    public float valor() {
        return this.entityData.get(VALOR);
    }

    public boolean esOscuro() {
        return valor() < 0;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VALOR, 0.0f);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        // Gravedad (o flotar en el agua)
        if (this.isEyeInFluid(FluidTags.WATER)) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x * 0.99, Math.min(v.y + 5.0E-4, 0.06), v.z * 0.99);
        } else if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
        }

        // Si se ha quedado dentro de un bloque, lo empuja fuera
        if (!this.level().noCollision(this.getBoundingBox())) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
        }

        // Una vez por segundo busca el jugador más cercano
        if (this.tickCount % 20 == 1) {
            this.seguido = this.level().getNearestPlayer(this, DISTANCIA_ATRACCION);
        }
        if (this.seguido != null && (this.seguido.isSpectator() || this.seguido.isDeadOrDying())) {
            this.seguido = null;
        }

        // Va hacia él, más rápido cuanto más cerca
        if (this.seguido != null) {
            Vec3 hacia = new Vec3(this.seguido.getX() - this.getX(),
                    this.seguido.getY() + this.seguido.getEyeHeight() / 2.0 - this.getY(),
                    this.seguido.getZ() - this.getZ());
            double distancia2 = hacia.lengthSqr();
            if (distancia2 < DISTANCIA_ATRACCION * DISTANCIA_ATRACCION) {
                double fuerza = 1.0 - Math.sqrt(distancia2) / DISTANCIA_ATRACCION;
                this.setDeltaMovement(this.getDeltaMovement().add(hacia.normalize().scale(fuerza * fuerza * 0.1)));
            }
        }

        this.move(MoverType.SELF, this.getDeltaMovement());

        // Rozamiento y rebote
        float rozamiento = this.onGround() ? 0.6f * 0.98f : 0.98f;
        this.setDeltaMovement(this.getDeltaMovement().multiply(rozamiento, 0.98, rozamiento));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.9, 1.0));
        }

        if (++this.edad >= VIDA_MAXIMA) {
            this.discard();
        }
    }

    // Al tocarlo un jugador: le da o le quita fe y desaparece
    @Override
    public void playerTouch(Player jugador) {
        if (this.level().isClientSide() || !(jugador instanceof ServerPlayer servidor) || this.isRemoved()) {
            return;
        }
        FeDatos.ganarFe(servidor, valor());
        float tono = esOscuro() ? 0.5f : 1.4f;
        this.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.15f, tono + this.random.nextFloat() * 0.2f);
        this.discard();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag datos) {
        datos.putFloat("Valor", valor());
        datos.putShort("Edad", (short) this.edad);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag datos) {
        this.entityData.set(VALOR, datos.getFloat("Valor"));
        this.edad = datos.getShort("Edad");
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
