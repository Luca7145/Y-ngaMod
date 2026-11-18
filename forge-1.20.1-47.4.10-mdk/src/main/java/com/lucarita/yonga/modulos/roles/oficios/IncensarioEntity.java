package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El incensario del botafumeiro cuando se lanza. Va unido al jugador por la cadena (la dibuja
 * IncensarioRenderer).
 *
 *  - Sale disparado hacia donde miras. Cuando la cadena llega a su largo máximo, o al golpear a algo o
 *    chocar con un bloque, vuelve solo a tu mano (también si vuelves a hacer click derecho).
 *  - Golpea a cada enemigo una vez a la ida y otra a la vuelta como mucho.
 *  - Si se lanzó con la carga al máximo, va en llamas y prende fuego a lo que golpea.
 */
public class IncensarioEntity extends Projectile {

    private static final EntityDataAccessor<Boolean> VOLVIENDO = SynchedEntityData.defineId(IncensarioEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ARDIENDO = SynchedEntityData.defineId(IncensarioEntity.class, EntityDataSerializers.BOOLEAN);

    // ---- Ajustes ----
    private static final double VELOCIDAD_VUELTA = 1.4;
    private static final double DISTANCIA_RECOGER = 1.6;
    private static final int VIDA_MAXIMA = 20 * 10;        // por si acaso: a los 10 s vuelve sí o sí
    private static final int SEGUNDOS_FUEGO = 5;
    private static final double EMPUJE = 0.6;

    // Qué jugadores tienen el incensario lanzado ahora (para saber si el objeto de la mano lo tiene "fuera")
    private static final Map<UUID, IncensarioEntity> ACTIVOS = new HashMap<>();

    private float dano;
    private double alcance;
    private final Set<UUID> golpeadosIda = new HashSet<>();
    private final Set<UUID> golpeadosVuelta = new HashSet<>();

    public IncensarioEntity(EntityType<? extends IncensarioEntity> tipo, Level nivel) {
        super(tipo, nivel);
    }

    public static IncensarioEntity lanzar(ServerPlayer jugador, float carga, float dano, double alcance, double velocidad, boolean ardiendo) {
        IncensarioEntity incensario = new IncensarioEntity(ModuloRoles.INCENSARIO.get(), jugador.level());
        incensario.setOwner(jugador);
        incensario.setPos(jugador.getX(), jugador.getEyeY() - 0.3, jugador.getZ());
        incensario.dano = dano;
        incensario.alcance = alcance;
        incensario.entityData.set(ARDIENDO, ardiendo);
        incensario.setDeltaMovement(jugador.getLookAngle().scale(velocidad));
        jugador.level().addFreshEntity(incensario);
        ACTIVOS.put(jugador.getUUID(), incensario);
        return incensario;
    }

    /** ¿Tiene este jugador el incensario fuera? (solo en el servidor) */
    public static boolean lanzado(UUID jugador) {
        IncensarioEntity incensario = ACTIVOS.get(jugador);
        return incensario != null && !incensario.isRemoved();
    }

    /** Click derecho con el incensario fuera: que vuelva ya. */
    public static void llamar(UUID jugador) {
        IncensarioEntity incensario = ACTIVOS.get(jugador);
        if (incensario != null && !incensario.isRemoved()) {
            incensario.volver();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VOLVIENDO, false);
        this.entityData.define(ARDIENDO, false);
    }

    public boolean volviendo() {
        return this.entityData.get(VOLVIENDO);
    }

    public boolean ardiendo() {
        return this.entityData.get(ARDIENDO);
    }

    private void volver() {
        if (!volviendo()) {
            this.entityData.set(VOLVIENDO, true);
            this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Entity dueno = getOwner();
        if (!this.level().isClientSide()) {
            if (!(dueno instanceof Player jugador) || !jugador.isAlive() || jugador.level() != this.level()
                    || this.tickCount > VIDA_MAXIMA) {
                recoger();
                return;
            }
        }
        if (dueno == null) {
            return;
        }

        Vec3 posicion = position();
        Vec3 movimiento;
        if (!volviendo()) {
            movimiento = getDeltaMovement();
            HitResult choque = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (choque.getType() != HitResult.Type.MISS && !this.level().isClientSide()) {
                onHit(choque);
            }
            setDeltaMovement(movimiento.scale(0.97).add(0, -0.02, 0));   // un poco de peso
            if (!this.level().isClientSide() && distanceTo(dueno) >= alcance) {
                volver();
            }
        } else {
            this.noPhysics = true;
            Vec3 mano = dueno.getEyePosition().subtract(0, 0.5, 0);
            Vec3 haciaMano = mano.subtract(posicion);
            if (haciaMano.length() < DISTANCIA_RECOGER) {
                if (!this.level().isClientSide()) {
                    recoger();
                }
                return;
            }
            movimiento = haciaMano.normalize().scale(Math.min(VELOCIDAD_VUELTA, haciaMano.length()));
            setDeltaMovement(movimiento);
            if (!this.level().isClientSide()) {
                EntityHitResult golpe = ProjectileUtil.getEntityHitResult(this.level(), this, posicion, posicion.add(movimiento),
                        getBoundingBox().expandTowards(movimiento).inflate(1.0), this::canHitEntity);
                if (golpe != null) {
                    onHitEntity(golpe);
                }
            }
        }
        setPos(posicion.add(movimiento));

        // Humo de incienso (y llamas si va ardiendo), un poco por detrás del incensario. Cerca del dueño no
        // sale, para que no le tape la vista al lanzarlo y al volver
        if (this.level().isClientSide() && distanceToSqr(dueno) > 2.5 * 2.5) {
            Vec3 detras = position().subtract(movimiento.scale(0.6));
            this.level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, detras.x, detras.y + 0.2, detras.z, 0, 0.01, 0);
            if (ardiendo()) {
                this.level().addParticle(ParticleTypes.FLAME, detras.x, detras.y + 0.1, detras.z, 0, 0.02, 0);
            }
        }
    }

    @Override
    protected boolean canHitEntity(Entity entidad) {
        if (!(entidad instanceof LivingEntity) || entidad instanceof ArmorStand || entidad == getOwner() || !entidad.isAlive()) {
            return false;
        }
        return !(volviendo() ? golpeadosVuelta : golpeadosIda).contains(entidad.getUUID()) && super.canHitEntity(entidad);
    }

    @Override
    protected void onHitEntity(EntityHitResult resultado) {
        Entity entidad = resultado.getEntity();
        (volviendo() ? golpeadosVuelta : golpeadosIda).add(entidad.getUUID());
        if (entidad.hurt(this.damageSources().thrown(this, getOwner()), dano) && entidad instanceof LivingEntity vivo) {
            if (ardiendo()) {
                vivo.setSecondsOnFire(SEGUNDOS_FUEGO);
            }
            Vec3 direccion = getDeltaMovement().normalize();
            vivo.knockback(EMPUJE, -direccion.x, -direccion.z);
            vivo.hurtMarked = true;
        }
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1.0f, 0.7f);
        volver();   // como un mangual: golpea y rebota hacia ti
    }

    @Override
    protected void onHitBlock(BlockHitResult resultado) {
        super.onHitBlock(resultado);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 0.8f, 1.2f);
        volver();
    }

    /** Vuelve a la mano: se quita del mundo y el botafumeiro vuelve a tener su incensario. */
    private void recoger() {
        Entity dueno = getOwner();
        if (dueno instanceof Player jugador) {
            ACTIVOS.remove(jugador.getUUID(), this);
            for (ItemStack objeto : jugador.getInventory().items) {
                BotafumeiroItem.marcarLanzado(objeto, false);
            }
            BotafumeiroItem.marcarLanzado(jugador.getOffhandItem(), false);
            this.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        discard();
    }

    // No se guarda en el mundo: si se descarga, se pierde y el botafumeiro lo recupera solo
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag datos) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag datos) {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distancia) {
        return distancia < 64 * 64;
    }
}
