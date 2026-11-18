package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Compañeros: mobs domados con la flauta.
 *
 *  - Click derecho con la flauta a un mob (el que sea) = domarlo: le salen corazones y te sigue.
 *  - Click derecho otra vez = se queda quieto. Otra vez = vuelve a seguirte.
 *  - Si alguien te ataca a ti o a él, o si tú atacas a alguien (como los lobos), va a por ese alguien
 *    (si puede atacar: un cerdo no tiene con qué). Si está quieto, no. Por su cuenta no ataca a nadie, y
 *    nunca a su dueño. Cuando el enemigo muere, si te alejas más de 16 bloques o si en 30 s no ha podido con
 *    él, deja la pelea y vuelve contigo.
 *  - Solo un compañero por jugador, y no se puede soltar: para domar otro, el que tienes tiene que morir.
 *  - Vida máxima que se puede domar (ver limiteDeVida): 10 normalmente; el Melómano 20 desde Oficial
 *    y 50 siendo Maestro. Los jefes (Wither, dragón...) nunca.
 *  - No desaparece (como un mob con nombre), te sigue aunque te alejes (si te alejas mucho, aparece a tu
 *    lado) y sigue siendo tuyo aunque se convierta (un aldeano en zombi, un cerdo en piglin zombi...).
 *
 * Cómo se guarda: en el mob (datos "yonga_companero": dueño, quieto, a quién puede atacar) y en el
 * mundo (CompanerosGuardados: qué compañero tiene cada jugador).
 */
public final class Companeros {

    // ---- Ajustes ----
    public static final float VIDA_NORMAL = 10;
    public static final float VIDA_OFICIAL = 20;
    public static final float VIDA_MAESTRO = 50;
    private static final double DISTANCIA_SEGUIR = 6;        // si estás más lejos, viene
    private static final double DISTANCIA_PARAR = 3;         // aquí ya se para
    private static final double DISTANCIA_TELETRANSPORTE = 24;
    private static final double DISTANCIA_PELEA = 16;         // si en plena pelea te alejas más, la deja y vuelve contigo
    private static final long DURACION_MAXIMA_PELEA = 20 * 30; // si en 30 s no lo ha matado (no llega...), lo deja
    private static final double VELOCIDAD = 1.2;

    private static final String CLAVE = "yonga_companero";

    private Companeros() {}

    // ---------- DATOS DEL MOB ----------

    private static CompoundTag datos(Entity mob) {
        return mob.getPersistentData().getCompound(CLAVE);
    }

    public static boolean esCompanero(Entity mob) {
        return mob instanceof Mob && datos(mob).hasUUID("dueno");
    }

    public static UUID dueno(Entity mob) {
        return datos(mob).hasUUID("dueno") ? datos(mob).getUUID("dueno") : null;
    }

    public static boolean quieto(Entity mob) {
        return datos(mob).getBoolean("quieto");
    }

    private static void guardar(Entity mob, CompoundTag datos) {
        mob.getPersistentData().put(CLAVE, datos);
    }

    private static Player jugadorDueno(Mob mob) {
        UUID dueno = dueno(mob);
        return dueno == null ? null : mob.level().getPlayerByUUID(dueno);
    }

    /** Hasta cuánta vida máxima puede domar este jugador. */
    public static float limiteDeVida(Player jugador) {
        if (RolDatos.obtenerRol(jugador) == Rol.MELOMANO) {
            RangoRol rango = RolDatos.obtenerRango(jugador);
            if (rango == RangoRol.MAESTRO) return VIDA_MAESTRO;
            if (rango.ordinal() >= RangoRol.OFICIAL.ordinal()) return VIDA_OFICIAL;
        }
        return VIDA_NORMAL;
    }

    // ---------- CLICK DERECHO CON LA FLAUTA ----------

    public static void alInteractuar(PlayerInteractEvent.EntityInteract evento) {
        if (!evento.getItemStack().is(ModuloRoles.FLAUTA.get()) || !(evento.getTarget() instanceof Mob mob)) {
            return;
        }
        // En los dos lados: que no se abra el comercio del aldeano, ni se monte, etc.
        evento.setCanceled(true);
        evento.setCancellationResult(InteractionResult.SUCCESS);
        if (evento.getEntity() instanceof ServerPlayer jugador && !Fantasma.es(jugador)) {
            usarFlauta(jugador, mob);
        }
    }

    private static void usarFlauta(ServerPlayer jugador, Mob mob) {
        UUID dueno = dueno(mob);
        // Ya es tuyo: quieto / sígueme
        if (jugador.getUUID().equals(dueno)) {
            CompoundTag datos = datos(mob);
            boolean quieto = !datos.getBoolean("quieto");
            datos.putBoolean("quieto", quieto);
            if (quieto) {
                datos.remove("objetivo");
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            guardar(mob, datos);
            tocar(jugador, mob, false);
            aviso(jugador, quieto ? "yonga.flauta.quieto" : "yonga.flauta.sigue", ChatFormatting.LIGHT_PURPLE, mob.getDisplayName());
            return;
        }
        if (dueno != null) {
            aviso(jugador, "yonga.flauta.de_otro", ChatFormatting.RED, mob.getDisplayName());
            return;
        }
        if (mob.getType().is(Tags.EntityTypes.BOSSES)) {
            aviso(jugador, "yonga.flauta.demasiado_fuerte", ChatFormatting.RED, mob.getDisplayName(), (int) limiteDeVida(jugador));
            return;
        }
        float limite = limiteDeVida(jugador);
        if (mob.getMaxHealth() > limite) {
            aviso(jugador, "yonga.flauta.demasiado_fuerte", ChatFormatting.RED, mob.getDisplayName(), (int) limite);
            return;
        }
        CompanerosGuardados guardados = CompanerosGuardados.de(jugador.server);
        CompanerosGuardados.Companero anterior = guardados.companero(jugador.getUUID());
        if (anterior != null) {
            if (sigueVivo(jugador, anterior)) {
                aviso(jugador, "yonga.flauta.ya_tienes", ChatFormatting.RED);
                return;
            }
            guardados.quitar(jugador.getUUID());
        }

        // ¡Domado!
        CompoundTag datos = new CompoundTag();
        datos.putUUID("dueno", jugador.getUUID());
        datos.putBoolean("quieto", false);
        guardar(mob, datos);
        guardados.fijar(jugador.getUUID(), mob);
        mob.setPersistenceRequired();
        mob.setTarget(null);
        desinflar(mob);
        anadirComportamiento(mob);
        tocar(jugador, mob, true);
        aviso(jugador, "yonga.flauta.domado", ChatFormatting.LIGHT_PURPLE, mob.getDisplayName());
    }

    /**
     * Si es un creeper que se estaba hinchando para explotar, se deshincha. Hace falta porque quien lo
     * deshincha normalmente es su propio comportamiento de explotar, y al domarlo los de compañero (quieto,
     * seguir al dueño) le quitan el sitio: sin esto, el creeper explotaba aunque te alejaras.
     * Si lo han encendido con un mechero, explota igual (eso no tiene vuelta atrás en Minecraft).
     */
    private static void desinflar(Mob mob) {
        if (mob instanceof Creeper creeper && !creeper.isIgnited() && creeper.getSwellDir() > 0) {
            creeper.setSwellDir(-1);
        }
    }

    /**
     * ¿Sigue existiendo el compañero que tenía?
     *  - Si está cargado en algún mundo: sí (si está vivo y sigue siendo suyo).
     *  - Si no, miramos dónde se le vio por última vez: si ese chunk está cargado y no está ahí, ya no
     *    existe. Si el chunk no está cargado (está lejos), contamos que sí existe.
     *  - Si no sabemos dónde estaba (datos de antes de este arreglo), contamos que no.
     */
    private static boolean sigueVivo(ServerPlayer jugador, CompanerosGuardados.Companero companero) {
        for (ServerLevel nivel : jugador.server.getAllLevels()) {
            Entity entidad = nivel.getEntity(companero.mob());
            if (entidad != null) {
                return entidad.isAlive() && jugador.getUUID().equals(dueno(entidad));
            }
        }
        if (!companero.conPosicion()) {
            return false;
        }
        for (ServerLevel nivel : jugador.server.getAllLevels()) {
            if (nivel.dimension().location().toString().equals(companero.dimension())) {
                return !nivel.areEntitiesLoaded(ChunkPos.asLong(companero.chunkX(), companero.chunkZ()));
            }
        }
        return false;
    }

    private static void tocar(ServerPlayer jugador, Mob mob, boolean domado) {
        ServerLevel nivel = jugador.serverLevel();
        if (domado) {
            nivel.playSound(null, mob.getX(), mob.getY(), mob.getZ(), ModuloRoles.SONIDO_FLAUTA_ENCANTO.get(), SoundSource.RECORDS, 1.0f, 1.0f);
            nivel.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() * 0.8, mob.getZ(), 7, 0.4, 0.3, 0.4, 0.05);
        } else {
            Musica.tocarNota(jugador, ModuloRoles.SONIDO_FLAUTA_NOTA.get());
            nivel.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private static void aviso(ServerPlayer jugador, String clave, ChatFormatting color, Object... argumentos) {
        jugador.displayClientMessage(Component.translatable(clave, argumentos).withStyle(color), true);
    }

    // ---------- COMPORTAMIENTO (seguir y quedarse quieto) ----------

    // Los mobs "normales" usan objetivos (goals). Al cargarse el mob se vuelven a poner (no se guardan).
    private static void anadirComportamiento(Mob mob) {
        mob.goalSelector.addGoal(0, new QuietoGoal(mob));
        mob.goalSelector.addGoal(1, new SeguirDuenoGoal(mob));
    }

    public static void alUnirseAlMundo(EntityJoinLevelEvent evento) {
        if (!evento.getLevel().isClientSide() && evento.getEntity() instanceof Mob mob && esCompanero(mob)) {
            anadirComportamiento(mob);
        }
    }

    private static class QuietoGoal extends Goal {
        private final Mob mob;

        QuietoGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return quieto(mob);
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }
    }

    private static class SeguirDuenoGoal extends Goal {
        private final Mob mob;
        private int espera;

        SeguirDuenoGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player dueno = jugadorDueno(mob);
            return dueno != null && !quieto(mob) && mob.getTarget() == null && !dueno.isSpectator()
                    && mob.distanceToSqr(dueno) > DISTANCIA_SEGUIR * DISTANCIA_SEGUIR;
        }

        @Override
        public boolean canContinueToUse() {
            Player dueno = jugadorDueno(mob);
            return dueno != null && !quieto(mob) && mob.getTarget() == null
                    && mob.distanceToSqr(dueno) > DISTANCIA_PARAR * DISTANCIA_PARAR;
        }

        @Override
        public void start() {
            espera = 0;
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            Player dueno = jugadorDueno(mob);
            if (dueno == null) {
                return;
            }
            mob.getLookControl().setLookAt(dueno, 10.0f, mob.getMaxHeadXRot());
            if (--espera > 0) {
                return;
            }
            espera = 10;
            if (!acercarseOTeletransportar(mob, dueno)) {
                mob.getNavigation().moveTo(dueno, VELOCIDAD);
            }
        }
    }

    /** Si el dueño está muy lejos, el compañero aparece a su lado. Devuelve true si se ha teletransportado. */
    private static boolean acercarseOTeletransportar(Mob mob, Player dueno) {
        if (mob.distanceToSqr(dueno) > DISTANCIA_TELETRANSPORTE * DISTANCIA_TELETRANSPORTE && dueno.onGround()) {
            mob.getNavigation().stop();
            mob.teleportTo(dueno.getX(), dueno.getY(), dueno.getZ());
            return true;
        }
        return false;
    }

    /**
     * Los mobs con "cerebro" (aldeanos, piglins, cabras, ajolotes...) no usan objetivos: se les guía cada
     * medio segundo diciéndoles a dónde ir (o que no vayan a ninguna parte) y a quién atacar.
     */
    public static void alTickMob(LivingEvent.LivingTickEvent evento) {
        if (!(evento.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !esCompanero(mob)) {
            return;
        }
        // Apuntamos dónde está cada 5 s (para saber si sigue existiendo, ver sigueVivo)
        if (mob.tickCount % 100 == 0 && mob.getServer() != null) {
            CompanerosGuardados guardados = CompanerosGuardados.de(mob.getServer());
            CompanerosGuardados.Companero apuntado = guardados.companero(dueno(mob));
            if (apuntado != null && apuntado.mob().equals(mob.getUUID())) {
                guardados.fijar(dueno(mob), mob);
            }
        }
        // Un creeper compañero sin nadie a quien atacar no sigue hinchándose
        if (mob.getTarget() == null) {
            desinflar(mob);
        }
        // ¿Se acabó la pelea? (el enemigo ha muerto, ya no puede atacarlo o el dueño se ha alejado)
        LivingEntity enemigo = mob.getTarget();
        if (enemigo == null && mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)) {
            enemigo = mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        }
        if (enemigo != null) {
            Player duenoCerca = jugadorDueno(mob);
            boolean lejos = duenoCerca != null && mob.distanceToSqr(duenoCerca) > DISTANCIA_PELEA * DISTANCIA_PELEA;
            boolean demasiadoLarga = mob.level().getGameTime() - datos(mob).getLong("desde") > DURACION_MAXIMA_PELEA;
            if (!enemigo.isAlive() || !puedeAtacar(mob, enemigo) || lejos || demasiadoLarga) {
                olvidarPelea(mob);
            }
        }
        var cerebro = mob.getBrain();
        boolean conCerebro = cerebro.checkMemory(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED);
        // A quién NO debe atacar (todos menos el permitido)
        if (cerebro.checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT)) {
            LivingEntity objetivo = cerebro.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (objetivo != null && !puedeAtacar(mob, objetivo)) {
                cerebro.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
        if (!conCerebro) {
            return;
        }
        if (quieto(mob)) {
            cerebro.eraseMemory(MemoryModuleType.WALK_TARGET);
            mob.getNavigation().stop();
            return;
        }
        Player dueno = jugadorDueno(mob);
        if (dueno == null || mob.tickCount % 10 != 0 || cerebro.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return;
        }
        if (!acercarseOTeletransportar(mob, dueno) && mob.distanceToSqr(dueno) > DISTANCIA_SEGUIR * DISTANCIA_SEGUIR) {
            cerebro.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(dueno, (float) VELOCIDAD, (int) DISTANCIA_PARAR));
        }
    }

    // ---------- ATACAR: solo a quien os ataque ----------

    private static boolean puedeAtacar(Mob mob, LivingEntity objetivo) {
        if (quieto(mob) || objetivo.getUUID().equals(dueno(mob))) {
            return false;
        }
        CompoundTag datos = datos(mob);
        return datos.hasUUID("objetivo") && datos.getUUID("objetivo").equals(objetivo.getUUID());
    }

    public static void alCambiarObjetivo(LivingChangeTargetEvent evento) {
        if (esCompanero(evento.getEntity()) && evento.getNewTarget() != null
                && !puedeAtacar((Mob) evento.getEntity(), evento.getNewTarget())) {
            evento.setCanceled(true);
        }
    }

    public static void alRecibirDano(LivingHurtEvent evento) {
        if (!(evento.getSource().getEntity() instanceof LivingEntity atacante)) {
            return;
        }
        LivingEntity victima = evento.getEntity();
        // Atacan al compañero
        if (esCompanero(victima)) {
            defender((Mob) victima, atacante);
            return;
        }
        // Atacan al dueño
        if (victima instanceof ServerPlayer jugador) {
            Mob mob = companeroCargado(jugador);
            if (mob != null) {
                defender(mob, atacante);
            }
        }
        // El dueño ataca a alguien: el compañero le ayuda (como los lobos)
        if (atacante instanceof ServerPlayer dueno) {
            Mob mob = companeroCargado(dueno);
            if (mob != null && victima != mob) {
                defender(mob, victima);
            }
        }
    }

    /** El compañero del jugador, si está cargado en su mismo mundo. */
    private static Mob companeroCargado(ServerPlayer jugador) {
        CompanerosGuardados.Companero companero = CompanerosGuardados.de(jugador.server).companero(jugador.getUUID());
        if (companero != null && jugador.serverLevel().getEntity(companero.mob()) instanceof Mob mob && esCompanero(mob)) {
            return mob;
        }
        return null;
    }

    private static void olvidarPelea(Mob mob) {
        CompoundTag datos = datos(mob);
        datos.remove("objetivo");
        guardar(mob, datos);
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getNavigation().stop();
    }

    /** ¿Tiene con qué atacar? (los zombis, lobos, abejas... sí; los cerdos, vacas, ovejas... no) */
    private static boolean puedeLuchar(Mob mob) {
        return mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    private static void defender(Mob mob, LivingEntity atacante) {
        if (quieto(mob) || !puedeLuchar(mob) || atacante == mob || atacante.getUUID().equals(dueno(mob)) || !atacante.isAlive()) {
            return;
        }
        CompoundTag datos = datos(mob);
        datos.putUUID("objetivo", atacante.getUUID());
        datos.putLong("desde", mob.level().getGameTime());
        guardar(mob, datos);
        mob.setTarget(atacante);
        if (mob.getBrain().checkMemory(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, atacante);
        }
    }

    // ---------- MUERTE Y CONVERSIÓN ----------

    /**
     * El compañero desaparece del mundo: si es para siempre (muere, explota como un creeper, lo borra un
     * comando...) se libera el hueco y se avisa al dueño. Si solo se descarga (chunk lejano, cambia de
     * dimensión...) no pasa nada.
     */
    public static void alSalirDelMundo(EntityLeaveLevelEvent evento) {
        Entity mob = evento.getEntity();
        if (evento.getLevel().isClientSide() || !esCompanero(mob) || mob.getRemovalReason() == null
                || !mob.getRemovalReason().shouldDestroy() || mob.getServer() == null) {
            return;
        }
        UUID dueno = dueno(mob);
        var servidor = mob.getServer();
        CompanerosGuardados guardados = CompanerosGuardados.de(servidor);
        if (guardados.quitar(dueno, mob.getUUID())) {
            // El aviso, un momento después: si se ha convertido (aldeano -> zombi...) ya tendrá compañero
            // otra vez (el nuevo) y no hay que avisar de nada
            Component nombre = mob.getDisplayName();
            servidor.execute(() -> {
                ServerPlayer jugador = servidor.getPlayerList().getPlayer(dueno);
                if (guardados.companero(dueno) == null && jugador != null) {
                    jugador.sendSystemMessage(Component.translatable("yonga.flauta.muerto", nombre).withStyle(ChatFormatting.GRAY));
                }
            });
        }
    }

    // Aldeano -> zombi, cerdo -> piglin zombi, zombi -> ahogado...: el nuevo sigue siendo tu compañero
    public static void alConvertirse(LivingConversionEvent.Post evento) {
        LivingEntity antes = evento.getEntity();
        if (antes.level().isClientSide() || !esCompanero(antes) || !(evento.getOutcome() instanceof Mob despues)
                || antes.level().getServer() == null) {
            return;
        }
        CompoundTag datos = datos(antes).copy();
        datos.remove("objetivo");
        guardar(despues, datos);
        despues.setPersistenceRequired();
        anadirComportamiento(despues);
        UUID dueno = dueno(antes);
        CompanerosGuardados guardados = CompanerosGuardados.de(antes.level().getServer());
        guardados.quitar(dueno, antes.getUUID());
        guardados.fijar(dueno, despues);
    }
}
