package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El gancho del GARFIO cuando está lanzado, unido al jugador por la cuerda (la dibuja GarfioRenderer).
 *
 *  - Vuela recto (cae un poco) hasta 40 bloques. Si no engancha nada, se suelta.
 *  - Al clavarse en un bloque tira del jugador hasta él, y lo deja pegado al bloque mientras siga enganchado.
 *    Mientras tanto no se acumula daño de caída.
 *  - Se suelta con mayúsculas, volviendo a hacer click derecho, dejando de llevar el garfio en la mano,
 *    alejándose demasiado o si se rompe el bloque donde estaba clavado.
 *
 * El tirón lo hace el cliente del propio jugador (en Minecraft el movimiento del jugador lo decide su cliente);
 * el servidor solo decide cuándo se engancha y cuándo se suelta.
 */
public class GarfioEntity extends Projectile {

    private static final EntityDataAccessor<Boolean> ENGANCHADO = SynchedEntityData.defineId(GarfioEntity.class, EntityDataSerializers.BOOLEAN);

    // ---- Ajustes ----
    public static final double ALCANCE = 40.0;
    private static final double VELOCIDAD = 2.6;
    private static final double TIRON_MAXIMO = 1.5;          // bloques por tick
    private static final double SUAVIDAD = 0.4;              // cuánto se acerca cada tick a la velocidad del tirón

    // Qué jugadores tienen el garfio lanzado (solo en el servidor)
    private static final Map<UUID, GarfioEntity> ACTIVOS = new HashMap<>();

    private BlockPos clavado;

    public GarfioEntity(EntityType<? extends GarfioEntity> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public static void lanzar(ServerPlayer jugador) {
        GarfioEntity garfio = new GarfioEntity(ModuloRoles.GARFIO_ENTIDAD.get(), jugador.level());
        garfio.setOwner(jugador);
        garfio.setPos(jugador.getX(), jugador.getEyeY() - 0.2, jugador.getZ());
        garfio.setDeltaMovement(jugador.getLookAngle().scale(VELOCIDAD));
        jugador.level().addFreshEntity(garfio);
        ACTIVOS.put(jugador.getUUID(), garfio);
        jugador.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.8f, 1.3f);
    }

    /** ¿Tiene este jugador el garfio fuera? (solo en el servidor) */
    public static boolean lanzado(UUID jugador) {
        GarfioEntity garfio = ACTIVOS.get(jugador);
        return garfio != null && !garfio.isRemoved();
    }

    /** Suelta el garfio de este jugador, si lo tiene fuera. */
    public static void soltar(UUID jugador) {
        GarfioEntity garfio = ACTIVOS.get(jugador);
        if (garfio != null && !garfio.isRemoved()) {
            garfio.soltar();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ENGANCHADO, false);
    }

    public boolean enganchado() {
        return this.entityData.get(ENGANCHADO);
    }

    private void soltar() {
        if (!this.level().isClientSide()) {
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIPWIRE_DETACH, SoundSource.PLAYERS, 0.8f, 1.2f);
            Entity dueno = getOwner();
            if (dueno != null && ACTIVOS.get(dueno.getUUID()) == this) {
                ACTIVOS.remove(dueno.getUUID());
            }
        }
        discard();
    }

    private static boolean loLleva(Player jugador) {
        return jugador.getMainHandItem().getItem() instanceof GarfioItem || jugador.getOffhandItem().getItem() instanceof GarfioItem;
    }

    @Override
    public void tick() {
        super.tick();
        Entity dueno = getOwner();
        if (!this.level().isClientSide()) {
            if (!(dueno instanceof Player jugador) || !jugador.isAlive() || jugador.isSpectator() || jugador.level() != this.level()
                    || distanceToSqr(jugador) > (ALCANCE + 8) * (ALCANCE + 8) || !loLleva(jugador)
                    || (enganchado() && (jugador.isShiftKeyDown() || clavado == null || this.level().getBlockState(clavado).isAir()))) {
                soltar();
                return;
            }
        }
        if (dueno == null) {
            return;
        }

        if (!enganchado()) {
            Vec3 movimiento = getDeltaMovement();
            HitResult choque = ProjectileUtil.getHitResultOnMoveVector(this, e -> false);
            if (choque.getType() == HitResult.Type.BLOCK && choque instanceof BlockHitResult bloque) {
                // Se queda justo en la superficie (en los dos lados, para que no se vea atravesar la pared)
                setPos(bloque.getLocation());
                setDeltaMovement(Vec3.ZERO);
                if (!this.level().isClientSide()) {
                    this.clavado = bloque.getBlockPos();
                    this.entityData.set(ENGANCHADO, true);
                    this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1.0f, 0.8f);
                    this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.0f);
                }
                return;
            }
            if (!this.level().isClientSide() && distanceTo(dueno) > ALCANCE) {
                soltar();
                return;
            }
            setPos(position().add(movimiento));
            setDeltaMovement(movimiento.scale(0.99).add(0, -0.03, 0));
            return;
        }

        setDeltaMovement(Vec3.ZERO);
        if (dueno instanceof Player jugador) {
            jugador.resetFallDistance();
            if (this.level().isClientSide() && jugador.isLocalPlayer()) {
                tirar(jugador);
            }
        }
    }

    /** Tira del jugador hacia el gancho (en su cliente). Cerca del gancho lo deja pegado contra el bloque. */
    private void tirar(Player jugador) {
        Vec3 centro = jugador.position().add(0, jugador.getBbHeight() * 0.5, 0);
        Vec3 hacia = position().subtract(centro);
        double distancia = hacia.length();
        if (distancia < 1.0E-3) {
            jugador.setDeltaMovement(Vec3.ZERO);
            return;
        }
        double velocidad = Math.min(TIRON_MAXIMO, 0.25 + distancia * 0.3);
        Vec3 deseado = hacia.scale(velocidad / distancia);
        Vec3 actual = jugador.getDeltaMovement();
        jugador.setDeltaMovement(actual.add(deseado.subtract(actual).scale(SUAVIDAD)));
    }

    @Override
    public void remove(RemovalReason razon) {
        super.remove(razon);
        Entity dueno = getOwner();
        if (!this.level().isClientSide() && dueno != null && ACTIVOS.get(dueno.getUUID()) == this) {
            ACTIVOS.remove(dueno.getUUID());
        }
    }

    // No se guarda en el mundo: al salir y volver, el garfio está otra vez en la mano
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distancia) {
        return distancia < 128 * 128;
    }
}
