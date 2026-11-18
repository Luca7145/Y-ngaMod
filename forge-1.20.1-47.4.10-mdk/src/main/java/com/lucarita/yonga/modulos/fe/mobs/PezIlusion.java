package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.modulos.fe.ModuloFe;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Una ilusión del Pez del viento (fases 2 y 3). Es idéntica a él y hace exactamente lo mismo, pero desde otro
 * lado: se coloca girada alrededor de su objetivo (la mitad del círculo si hay una, un cuarto si hay tres), a la
 * misma altura sobre el suelo, mirando y moviéndose igual. Sus "ataques" se ven (rayo, onda...) pero no hacen
 * nada. No tiene colisión. Al primer golpe se deshace en luz y el de verdad tarda un rato en sacar otra.
 */
public class PezIlusion extends PezDelViento {

    private UUID dueno;
    private int indice, total;
    private int accionAnterior = -1;
    private static final double DISTANCIA_MINIMA = 12.0;
    private Vec3 ultimaDireccion = new Vec3(1, 0, 0);
    /** Lo lejos (en horizontal) del punto donde cae el de verdad a lo que caen las ilusiones en el picado. */
    private static final double SEPARACION_PICADO = 16.0;
    private static final double MAX_SALTO = 3.0;          // más que esto de golpe: se desliza hasta su sitio
    private boolean enPicado;
    private Vec3 inicioPicado = Vec3.ZERO, finPicado = Vec3.ZERO, inicioReal = Vec3.ZERO;

    public PezIlusion(EntityType<? extends PezIlusion> tipo, Level nivel) {
        super(tipo, nivel);
        this.xpReward = 0;
    }

    @Override
    public boolean esIlusion() {
        return true;
    }

    public int indice() {
        return indice;
    }

    /** La coloca en su sitio respecto al de verdad (la ilusión número k de un total de "total" posiciones). */
    public void copiarDe(PezDelViento real, int k, int total) {
        this.dueno = real.getUUID();
        this.indice = k;
        this.total = total;
        this.moveTo(real.getX(), real.getY(), real.getZ(), real.getYRot(), real.getXRot());
        imitar(real);
    }

    /** Reparte las ilusiones de nuevo (cambia el número de posiciones): la número k sigue siendo la k. */
    public void repartir(int total) {
        this.total = total;
    }

    @Override
    protected void customServerAiStep() {
        if (!(this.level() instanceof ServerLevel nivel) || dueno == null
                || !(nivel.getEntity(dueno) instanceof PezDelViento real) || !real.isAlive() || real.isDeadOrDying()) {
            deshacer();
            return;
        }
        imitar(real);
        efectos();
    }

    private void imitar(PezDelViento real) {
        int accionReal = real.accion();
        if (accionReal == PICADO || accionReal == ATURDIDO) {
            picarComoEl(real, accionReal);
        } else {
            enPicado = false;
            imitarPosicion(real);
        }
        sincronizarAccion(real);
    }

    /**
     * El picado: cada ilusión cae EN LÍNEA RECTA a su propio punto (a SEPARACION_PICADO del punto donde cae el de
     * verdad, cada una en su lado), al mismo ritmo que él, y se queda allí tumbada. Antes se recolocaban respecto al
     * jugador mientras caían, así que se deslizaban y delataban cuál era el original.
     */
    private void picarComoEl(PezDelViento real, int accionReal) {
        Vec3 aterrizaje = real.objetivoVuelo;
        if (!enPicado) {
            enPicado = true;
            inicioPicado = this.position();
            inicioReal = real.position();
            Vec3 rel = this.position().subtract(aterrizaje).multiply(1, 0, 1);
            Vec3 dir = rel.lengthSqr() > 0.25 ? rel.normalize() : ultimaDireccion;
            Vec3 fin = aterrizaje.add(dir.scale(SEPARACION_PICADO));
            finPicado = new Vec3(fin.x, suelo(fin), fin.z);
        }
        double p = 1.0;
        if (accionReal == PICADO) {
            double total = inicioReal.distanceTo(aterrizaje);
            p = total < 0.01 ? 1.0 : Mth.clamp(1.0 - real.position().distanceTo(aterrizaje) / total, 0.0, 1.0);
        }
        Vec3 sitio = inicioPicado.lerp(finPicado, p);
        this.setPos(sitio.x, sitio.y, sitio.z);
        Vec3 d = finPicado.subtract(inicioPicado);
        if (accionReal == PICADO && d.lengthSqr() > 0.01) {
            this.setDeltaMovement(real.getDeltaMovement().length() > 0 ? d.normalize().scale(real.getDeltaMovement().length()) : Vec3.ZERO);
            float giro = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0f;
            float cabeceo = (float) (-Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
            this.setYRot(giro);
            this.setXRot(cabeceo);
            this.setYBodyRot(giro);
            this.setYHeadRot(giro);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.setXRot(real.getXRot());
        }
        ultimaDireccion = finPicado.subtract(real.objetivoVuelo).multiply(1, 0, 1).normalize();
    }

    private void imitarPosicion(PezDelViento real) {
        LivingEntity objetivo = real.getTarget();
        Vec3 centro = objetivo != null ? objetivo.position() : real.position().add(0, -10, 0);
        double angulo = Math.PI * 2 * indice / Math.max(2, total);
        // Cada ilusión se queda en su lado: al menos a DISTANCIA_MINIMA del objetivo. Así, cuando el de verdad pasa
        // por encima (el picado) o aparece cerca, no se juntan todas en el mismo punto.
        Vec3 rel = real.position().subtract(centro).multiply(1, 0, 1);
        if (rel.length() < DISTANCIA_MINIMA) {
            Vec3 dir = rel.lengthSqr() > 0.25 ? rel.normalize() : ultimaDireccion;
            rel = dir.scale(DISTANCIA_MINIMA);
        }
        ultimaDireccion = rel.normalize();
        double cos = Math.cos(angulo), sin = Math.sin(angulo);
        Vec3 girado = new Vec3(rel.x * cos - rel.z * sin, 0, rel.x * sin + rel.z * cos);
        Vec3 sitio = centro.add(girado);
        // la misma altura sobre el suelo que el de verdad (así también "barre" y "revienta" contra el suelo)
        double sobreSuelo = real.getY() - real.suelo(real.position());
        double y = suelo(sitio) + sobreSuelo;
        Vec3 donde = new Vec3(sitio.x, y, sitio.z);
        // si su sitio ha saltado (tras el picado, o al cambiar el reparto), se desliza hasta él en vez de teletransportarse
        if (this.position().distanceTo(donde) > MAX_SALTO && this.tickCount > 1) {
            donde = this.position().lerp(donde, 0.15);
        }
        this.setPos(donde.x, donde.y, donde.z);
        Vec3 v = real.getDeltaMovement();
        this.setDeltaMovement(new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos));
        float giro = real.getYRot() + (float) Math.toDegrees(angulo);
        this.setYRot(giro);
        this.setXRot(real.getXRot());
        this.setYBodyRot(giro);
        this.setYHeadRot(giro);
        Vec3 r = real.haciaRayo;
        this.haciaRayo = new Vec3(r.x * cos - r.z * sin, r.y, r.x * sin + r.z * cos);
    }

    private void sincronizarAccion(PezDelViento real) {
        int accion = real.accion();
        if (accion != accion()) {
            if (accion == ATURDIDO && accionAnterior == PICADO) {
                golpeContraElSuelo();   // solo la parte visual (es una ilusión)
            }
            empezar(accion);
        }
        accionAnterior = accion;
        this.ticksAccion = real.ticksAccion();
    }

    /** No se mueve por su cuenta: la colocamos en su sitio cada tick (imitar). */
    @Override
    public void travel(Vec3 entrada) {
        this.calculateEntityAnimation(false);
    }

    /** Se deshace en luz. */
    public void deshacer() {
        if (this.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.5, this.getZ(), 80, 2.5, 1.5, 2.5, 0.25);
            nivel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 1.5, this.getZ(), 30, 2.0, 1.0, 2.0, 0.05);
            nivel.playSound(null, this.getX(), this.getY(), this.getZ(), ModuloFe.SONIDO_PEZ_ILUSION.get(),
                    this.getSoundSource(), 4.0f, 1.4f);
        }
        this.discard();
    }

    /** Cualquier golpe la deshace (y el de verdad tarda en sacar otra). */
    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        if (this.level() instanceof ServerLevel nivel && dueno != null && nivel.getEntity(dueno) instanceof PezDelViento real) {
            real.ilusionDeshecha();
        }
        deshacer();
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource fuente) {
        return null;
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity otro) {
        return otro instanceof PezDelViento || super.isAlliedTo(otro);
    }

    @Override
    public float getVoicePitch() {
        return Mth.clamp(super.getVoicePitch(), 0.9f, 1.1f);
    }
}
