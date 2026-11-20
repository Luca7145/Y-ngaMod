package com.lucarita.yonga.modulos.armas;

import com.lucarita.yonga.core.GolpeCargado;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Parry y contraataque de las armas.
 */
public final class CombateEventos {

    // Enfriamiento tras bloquear (20 ticks = 1 segundo)
    private static final int ENFRIAMIENTO_PARRY = 20;
    private static final int ENFRIAMIENTO_PARRY_PROYECTIL = 40;

    // Contraataque: ventana de tiempo tras un parry y multiplicador de daño
    private static final int VENTANA_CONTRAATAQUE = 20;
    private static final float MULTIPLICADOR_CONTRAATAQUE = 1.5f;

    // Jugador -> momento (en ticks del mundo) hasta el que puede contraatacar
    private static final Map<UUID, Long> CONTRAATAQUE_HASTA = new HashMap<>();

    // Combo de la katana: a quién le lleva golpes seguidos, cuántos y cuándo fue el último
    private static final int VENTANA_COMBO = 40;          // 2 segundos entre golpe y golpe
    private static final float MULTIPLICADOR_COMBO = 1.75f;
    private record Combo(UUID objetivo, int golpes, long ultimo) {}
    private static final Map<UUID, Combo> COMBOS = new HashMap<>();
    // Jugador -> el enemigo al que el próximo golpe le hace el ×1,75 (lo decide alGolpear y lo cobra alHacerDano)
    private static final Map<UUID, UUID> GOLPE_DE_COMBO = new HashMap<>();

    private CombateEventos() {}

    // ---------- COMBO (katana) ----------

    /** Cada click de ataque: lleva la cuenta del combo (solo golpes cargados al mismo enemigo, sin pausas largas). */
    public static void alGolpear(AttackEntityEvent evento) {
        if (!(evento.getEntity() instanceof ServerPlayer jugador)) {
            return;
        }
        GOLPE_DE_COMBO.remove(jugador.getUUID());
        if (!(jugador.getMainHandItem().getItem() instanceof ArmaYonga arma) || !arma.rasgos().combo
                || !(evento.getTarget() instanceof LivingEntity objetivo)) {
            COMBOS.remove(jugador.getUUID());
            return;
        }
        boolean cargado = jugador.getAttackStrengthScale(0.5f) > 0.9f;
        long ahora = jugador.level().getGameTime();
        Combo antes = COMBOS.get(jugador.getUUID());
        if (!cargado) {
            COMBOS.remove(jugador.getUUID());                 // aporrear corta el combo
            return;
        }
        int golpes = (antes != null && antes.objetivo().equals(objetivo.getUUID()) && ahora - antes.ultimo() <= VENTANA_COMBO)
                ? antes.golpes() + 1 : 1;
        if (golpes >= 3) {
            GOLPE_DE_COMBO.put(jugador.getUUID(), objetivo.getUUID());
            COMBOS.remove(jugador.getUUID());                 // vuelve a empezar
        } else {
            COMBOS.put(jugador.getUUID(), new Combo(objetivo.getUUID(), golpes, ahora));
        }
    }

    // ---------- DOS MANOS (mandoble) ----------

    private static boolean manosOcupadas(Player jugador) {
        return jugador.getMainHandItem().getItem() instanceof ArmaYonga arma && arma.rasgos().dosManos;
    }

    /** Con un arma a dos manos no se puede usar nada de la otra mano (ni escudo, ni comida, ni bloques). */
    public static void alUsarObjeto(PlayerInteractEvent.RightClickItem evento) {
        if (evento.getHand() == InteractionHand.OFF_HAND && manosOcupadas(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alUsarEnBloque(PlayerInteractEvent.RightClickBlock evento) {
        if (evento.getHand() == InteractionHand.OFF_HAND && manosOcupadas(evento.getEntity())) {
            evento.setCanceled(true);
        }
    }

    public static void alEmpezarAUsar(LivingEntityUseItemEvent.Start evento) {
        if (evento.getEntity() instanceof Player jugador && jugador.getUsedItemHand() == InteractionHand.OFF_HAND
                && manosOcupadas(jugador)) {
            evento.setCanceled(true);
        }
    }

    // ---------- PARRY ----------

    /** Usamos LivingAttackEvent para bloquear el 100% del daño antes de que ocurra. */
    public static void alSerAtacado(LivingAttackEvent evento) {
        // Solo en el servidor (en un mundo de un jugador el cliente comparte la lista de contraataques)
        if (!(evento.getEntity() instanceof ServerPlayer jugador) || !jugador.isUsingItem()) {
            return;
        }

        // El ítem con el que está haciendo guardia (vale en cualquier mano)
        ItemStack enUso = jugador.getUseItem();
        if (!(enUso.getItem() instanceof ArmaYonga arma) || !arma.tieneParry()) {
            return;
        }

        // ¿Qué le golpea? Si no hay nada que golpee (caída, lava, fuego, sangrado...) no se puede bloquear
        Entity golpeDirecto = evento.getSource().getDirectEntity();
        int enfriamiento;
        if (golpeDirecto instanceof LivingEntity) {
            enfriamiento = ENFRIAMIENTO_PARRY;
        } else if (golpeDirecto instanceof Projectile && arma.rasgos().parry == ArmaYonga.TipoParry.TAMBIEN_PROYECTILES) {
            enfriamiento = ENFRIAMIENTO_PARRY_PROYECTIL;
        } else {
            return;
        }

        // Bloqueo: 0 daño, sin retroceso. La flecha rebota.
        evento.setCanceled(true);
        jugador.stopUsingItem();
        jugador.getCooldowns().addCooldown(arma, enfriamiento);
        jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);

        if (arma.rasgos().contraataque) {
            CONTRAATAQUE_HASTA.put(jugador.getUUID(), jugador.level().getGameTime() + VENTANA_CONTRAATAQUE);
        }
    }

    // ---------- CONTRAATAQUE ----------

    /** Si el jugador acaba de hacer parry con un arma de contraataque, su siguiente golpe cargado hace +50%. */
    public static void alHacerDano(LivingHurtEvent evento) {
        if (!(evento.getSource().getEntity() instanceof ServerPlayer jugador)
                || evento.getSource().getDirectEntity() != jugador) {   // solo cuerpo a cuerpo, solo en el servidor
            return;
        }
        // El tercer golpe del combo de la katana
        UUID combo = GOLPE_DE_COMBO.get(jugador.getUUID());
        if (combo != null && combo.equals(evento.getEntity().getUUID())) {
            GOLPE_DE_COMBO.remove(jugador.getUUID());
            evento.setAmount(evento.getAmount() * MULTIPLICADOR_COMBO);
            LivingEntity golpeado = evento.getEntity();
            jugador.level().playSound(null, golpeado.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.6f);
            if (jugador.level() instanceof ServerLevel nivel) {
                nivel.sendParticles(ParticleTypes.SWEEP_ATTACK, golpeado.getX(), golpeado.getY(0.5), golpeado.getZ(), 1, 0, 0, 0, 0);
                nivel.sendParticles(ParticleTypes.CRIT, golpeado.getX(), golpeado.getY(0.5), golpeado.getZ(), 15, 0.3, 0.4, 0.3, 0.3);
            }
        }
        Long hasta = CONTRAATAQUE_HASTA.get(jugador.getUUID());
        if (hasta == null) {
            return;
        }
        CONTRAATAQUE_HASTA.remove(jugador.getUUID());   // se gasta con el primer golpe, acierte o no las condiciones

        boolean conArmaDeContraataque = jugador.getMainHandItem().getItem() instanceof ArmaYonga arma && arma.rasgos().contraataque;
        if (jugador.level().getGameTime() > hasta || !conArmaDeContraataque || !GolpeCargado.fue(jugador)) {
            return;
        }

        evento.setAmount(evento.getAmount() * MULTIPLICADOR_CONTRAATAQUE);
        LivingEntity objetivo = evento.getEntity();
        jugador.level().playSound(null, objetivo.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.2f);
        if (jugador.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.ENCHANTED_HIT, objetivo.getX(), objetivo.getY(0.5), objetivo.getZ(),
                    12, 0.3, 0.4, 0.3, 0.2);
        }
    }

    /**
     * Armas que perforan armaduras (martillo de guerra): el daño se sube lo justo para que, tras pasar por la armadura
     * entera, haga lo mismo que haría si el objetivo llevara solo la parte que no se perfora. Va con prioridad baja,
     * después de los demás cambios de daño. Contra quien no lleva armadura no cambia nada.
     */
    public static void alPerforar(LivingHurtEvent evento) {
        DamageSource fuente = evento.getSource();
        if (!(fuente.getEntity() instanceof ServerPlayer jugador) || fuente.getDirectEntity() != jugador
                || fuente.is(DamageTypeTags.BYPASSES_ARMOR)
                || !(jugador.getMainHandItem().getItem() instanceof ArmaYonga arma) || arma.rasgos().perforaArmadura <= 0) {
            return;
        }
        LivingEntity objetivo = evento.getEntity();
        float armadura = objetivo.getArmorValue();
        float dureza = (float) objetivo.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float dano = evento.getAmount();
        if (armadura <= 0 || dano <= 0) {
            return;
        }
        float queda = 1 - arma.rasgos().perforaArmadura;
        float buscado = CombatRules.getDamageAfterAbsorb(dano, armadura * queda, dureza * queda);
        // El daño que, con la armadura entera, deja "buscado" (la armadura quita como mucho el 80%, así que basta ×5)
        float bajo = dano, alto = dano * 5;
        for (int i = 0; i < 30; i++) {
            float medio = (bajo + alto) / 2;
            if (CombatRules.getDamageAfterAbsorb(medio, armadura, dureza) < buscado) bajo = medio; else alto = medio;
        }
        evento.setAmount(alto);
        if (armadura >= 8 && jugador.level() instanceof ServerLevel nivel) {
            nivel.playSound(null, objetivo.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.35f, 1.5f);
            nivel.sendParticles(ParticleTypes.CRIT, objetivo.getX(), objetivo.getY(0.6), objetivo.getZ(), 8, 0.3, 0.3, 0.3, 0.2);
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        CONTRAATAQUE_HASTA.remove(evento.getEntity().getUUID());
        COMBOS.remove(evento.getEntity().getUUID());
        GOLPE_DE_COMBO.remove(evento.getEntity().getUUID());
    }
}
