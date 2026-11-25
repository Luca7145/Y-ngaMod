package com.lucarita.yonga.modulos.habitantes.muebles;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Entidad invisible sobre la que vas montado al sentarte en un mueble. Desaparece en cuanto te levantas (mayúsculas)
 * o se rompe el mueble.
 */
public class Asiento extends Entity {

    public Asiento(EntityType<?> tipo, Level nivel) {
        super(tipo, nivel);
        this.noPhysics = true;
    }

    /** Sienta al jugador en el mueble (si no hay ya alguien). */
    static boolean sentar(Level nivel, BlockPos pos, BlockState estado, double altura, Player jugador) {
        if (!nivel.getEntitiesOfClass(Asiento.class, new net.minecraft.world.phys.AABB(pos)).isEmpty()) {
            return false;
        }
        Asiento asiento = new Asiento(Muebles.ASIENTO.get(), nivel);
        float giro = estado.hasProperty(HorizontalDirectionalBlock.FACING)
                ? estado.getValue(HorizontalDirectionalBlock.FACING).toYRot() : 0f;
        asiento.moveTo(pos.getX() + 0.5, pos.getY() + altura / 16.0 - 0.1, pos.getZ() + 0.5, giro, 0f);
        nivel.addFreshEntity(asiento);
        jugador.startRiding(asiento);
        jugador.setYRot(giro);
        jugador.setYHeadRot(giro);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && (getPassengers().isEmpty()
                || !(level().getBlockState(blockPosition()).getBlock() instanceof MuebleBlock))) {
            discard();
        }
    }

    /** Al levantarte, sales por delante del mueble (o por donde haya sitio). */
    @Override
    public net.minecraft.world.phys.Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity pasajero) {
        BlockPos base = blockPosition();
        net.minecraft.core.Direction frente = net.minecraft.core.Direction.fromYRot(getYRot());
        for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[]{frente, frente.getClockWise(),
                frente.getCounterClockWise(), frente.getOpposite()}) {
            net.minecraft.world.phys.Vec3 sitio = net.minecraft.world.entity.vehicle.DismountHelper
                    .findSafeDismountLocation(pasajero.getType(), level(), base.relative(d), false);
            if (sitio != null) return sitio;
        }
        return super.getDismountLocationForPassenger(pasajero);
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag datos) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag datos) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
