package com.lucarita.yonga.modulos.armas;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.sangrado.Sangrado;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Todas las armas del mod. Lo que hace especial a cada una se define con sus Rasgos (ver ModuloArmas):
 *  - parry:          click derecho para ponerse en guardia y bloquear el siguiente golpe
 *  - contraataque:   tras un parry, el siguiente golpe (en 1 segundo) hace +50% de daño
 *  - sangrado:       probabilidad de causar Sangrado al golpear
 *  - alcance:        bloques extra de alcance de ataque
 *  - aturdir:        probabilidad de aturdir al objetivo
 *  - rompe escudos:  desactiva el escudo del rival, como un hacha
 *  - barrido:        si hace el ataque de barrido de las espadas
 *  - rango herrero:  rango mínimo de Herrero necesario para fabricarla
 *  - lanzable:       mantener click derecho y soltar para lanzarla (como un tridente; hay que recogerla)
 *  - combo:          3 golpes cargados seguidos al mismo enemigo (sin pasar 2 s entre ellos): el 3º hace ×1,75
 *  - barrido amplio: los golpes cargados dañan con el 50% a todos los que tengas delante (2,5 bloques)
 *  - dos manos:      mientras la llevas en la mano principal no puedes usar lo de la otra mano
 *
 * Las habilidades (sangrado, aturdir, contraataque) solo funcionan con golpes cargados.
 */
public class ArmaYonga extends SwordItem {

    public enum TipoParry { NINGUNO, CUERPO_A_CUERPO, TAMBIEN_PROYECTILES }

    /** Constructor "por piezas" de los rasgos de un arma: Rasgos.nuevo().parry().sangrado(...)... */
    public static class Rasgos {
        TipoParry parry = TipoParry.NINGUNO;
        boolean contraataque = false;
        Sangrado.Tabla sangrado = null;
        double alcanceExtra = 0;
        float probAturdir = 0;
        boolean rompeEscudos = false;
        boolean barrido = true;
        RangoRol rangoHerrero = RangoRol.NOVATO;
        boolean lanzable = false;
        boolean combo = false;
        boolean barridoAmplio = false;
        boolean dosManos = false;
        float perforaArmadura = 0;          // parte de la armadura (y su dureza) que no cuenta contra sus golpes

        public static Rasgos nuevo() { return new Rasgos(); }

        public Rasgos parry() { this.parry = TipoParry.CUERPO_A_CUERPO; return this; }
        public Rasgos parryTambienProyectiles() { this.parry = TipoParry.TAMBIEN_PROYECTILES; return this; }
        public Rasgos contraataque() { this.contraataque = true; return this; }
        public Rasgos sangrado(Sangrado.Tabla tabla) { this.sangrado = tabla; return this; }
        public Rasgos alcance(double bloquesExtra) { this.alcanceExtra = bloquesExtra; return this; }
        public Rasgos aturdir(float probabilidad) { this.probAturdir = probabilidad; return this; }
        public Rasgos rompeEscudos() { this.rompeEscudos = true; return this; }
        public Rasgos sinBarrido() { this.barrido = false; return this; }
        public Rasgos herrero(RangoRol rango) { this.rangoHerrero = rango; return this; }
        public Rasgos lanzable() { this.lanzable = true; return this; }
        public Rasgos combo() { this.combo = true; return this; }
        public Rasgos barridoAmplio() { this.barridoAmplio = true; this.barrido = false; return this; }
        public Rasgos dosManos() { this.dosManos = true; return this; }
        public Rasgos perforaArmadura(float parte) { this.perforaArmadura = parte; return this; }
    }

    // UUID fijo para el modificador de alcance (Minecraft lo necesita para no duplicarlo)
    private static final UUID UUID_ALCANCE = UUID.fromString("8f4a6c1e-3b2d-4e5f-9a7b-1c2d3e4f5a6b");

    private final Rasgos rasgos;
    private Multimap<Attribute, AttributeModifier> modificadoresConAlcance;

    public ArmaYonga(Tier material, int dañoExtra, float velocidadAtaque, Rasgos rasgos, Properties propiedades) {
        super(material, dañoExtra, velocidadAtaque, propiedades);
        this.rasgos = rasgos;
    }

    public Rasgos rasgos() {
        return rasgos;
    }

    public boolean tieneParry() {
        return rasgos.parry != TipoParry.NINGUNO;
    }

    public RangoRol rangoHerrero() {
        return rasgos.rangoHerrero;
    }

    // ---------- PARRY (el bloqueo en sí está en CombateEventos) ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        if (rasgos.lanzable) {
            jugador.startUsingItem(mano);
            return InteractionResultHolder.consume(jugador.getItemInHand(mano));
        }
        if (!tieneParry()) {
            return super.use(nivel, jugador, mano);
        }
        ItemStack item = jugador.getItemInHand(mano);
        jugador.startUsingItem(mano);
        return InteractionResultHolder.consume(item);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (rasgos.lanzable) return UseAnim.SPEAR;
        return tieneParry() ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    // 72000 ticks = se mantiene la guardia (o se carga el lanzamiento) hasta que el jugador suelte el botón
    @Override
    public int getUseDuration(ItemStack stack) {
        return tieneParry() || rasgos.lanzable ? 72000 : super.getUseDuration(stack);
    }

    // ---------- LANZAR (lanza) ----------

    private static final int CARGA_MINIMA_LANZAR = 10;   // medio segundo, como el tridente

    @Override
    public void releaseUsing(ItemStack stack, Level nivel, LivingEntity usuario, int ticksRestantes) {
        if (!rasgos.lanzable || !(usuario instanceof Player jugador) || getUseDuration(stack) - ticksRestantes < CARGA_MINIMA_LANZAR) {
            return;
        }
        if (!nivel.isClientSide()) {
            stack.hurtAndBreak(1, jugador, j -> j.broadcastBreakEvent(jugador.getUsedItemHand()));
            if (stack.isEmpty()) {
                return;
            }
            LanzaEntity lanza = new LanzaEntity(nivel, jugador, stack);
            lanza.shootFromRotation(jugador, jugador.getXRot(), jugador.getYRot(), 0.0f, 2.5f, 1.0f);
            if (jugador.getAbilities().instabuild) {
                lanza.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
            }
            nivel.addFreshEntity(lanza);
            nivel.playSound(null, lanza, net.minecraft.sounds.SoundEvents.TRIDENT_THROW, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.9f);
            if (!jugador.getAbilities().instabuild) {
                jugador.getInventory().removeItem(stack);
            }
        }
    }

    // ---------- BARRIDO AMPLIO (mandoble) ----------

    private static final double RADIO_BARRIDO = 2.5;
    private static final float PARTE_BARRIDO = 0.5f;

    private void barridoAmplio(LivingEntity objetivo, LivingEntity atacante) {
        float dano = (float) atacante.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * PARTE_BARRIDO;
        net.minecraft.world.phys.Vec3 mirada = atacante.getLookAngle().multiply(1, 0, 1).normalize();
        for (LivingEntity otro : atacante.level().getEntitiesOfClass(LivingEntity.class, atacante.getBoundingBox().inflate(RADIO_BARRIDO + 1, 1, RADIO_BARRIDO + 1))) {
            if (otro == atacante || otro == objetivo || !otro.isAlive() || otro instanceof net.minecraft.world.entity.decoration.ArmorStand
                    || atacante.distanceTo(otro) > RADIO_BARRIDO + 0.5) continue;
            net.minecraft.world.phys.Vec3 hacia = otro.position().subtract(atacante.position()).multiply(1, 0, 1).normalize();
            if (hacia.dot(mirada) < 0.2) continue;                         // solo los de delante
            if (otro instanceof Player p && atacante instanceof Player a && !a.canHarmPlayer(p)) continue;
            if (otro.hurt(atacante.damageSources().playerAttack((Player) atacante), dano)) {
                otro.knockback(0.6, -hacia.x, -hacia.z);
            }
        }
        if (atacante.level() instanceof net.minecraft.server.level.ServerLevel servidor) {
            double x = atacante.getX() + mirada.x * 1.5, z = atacante.getZ() + mirada.z * 1.5;
            servidor.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, x, atacante.getY(0.6), z, 3, 0.8, 0.1, 0.8, 0);
        }
        atacante.level().playSound(null, atacante.getX(), atacante.getY(), atacante.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.7f);
    }

    // ---------- AL GOLPEAR: SANGRADO Y ATURDIR ----------

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (!atacante.level().isClientSide() && GolpeCargado.fue(atacante)) {
            if (rasgos.sangrado != null) {
                // Si el módulo de sangrado está desactivado, esto no hace nada
                Sangrado.intentarAplicar(objetivo, atacante.getRandom(), rasgos.sangrado);
            }
            if (rasgos.probAturdir > 0 && atacante.getRandom().nextFloat() < rasgos.probAturdir) {
                Aturdimiento.aturdir(objetivo);
            }
            if (rasgos.barridoAmplio && atacante instanceof Player) {
                barridoAmplio(objetivo, atacante);
            }
        }
        // Método original: el arma pierde durabilidad al golpear
        return super.hurtEnemy(stack, objetivo, atacante);
    }

    // ---------- ALCANCE ----------

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND || rasgos.alcanceExtra <= 0) {
            return base;
        }
        // Se crea la primera vez que se pide (los atributos de Forge aún no existen al crear el ítem)
        if (modificadoresConAlcance == null) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(base);
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(UUID_ALCANCE, "Alcance del arma",
                    rasgos.alcanceExtra, AttributeModifier.Operation.ADDITION));
            modificadoresConAlcance = builder.build();
        }
        return modificadoresConAlcance;
    }

    // ---------- BARRIDO Y ESCUDOS ----------

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction accion) {
        if (accion == ToolActions.SWORD_SWEEP && !rasgos.barrido) {
            return false;
        }
        return super.canPerformAction(stack, accion);
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack escudo, LivingEntity defensor, LivingEntity atacante) {
        return rasgos.rompeEscudos || super.canDisableShield(stack, escudo, defensor, atacante);
    }

    // ---------- POSE A DOS MANOS (solo en el cliente) ----------

    // Ojo: Forge llama a esto mientras se crea el arma, cuando "rasgos" todavía no está puesto.
    // Por eso no se mira aquí si es a dos manos, sino cada vez que se dibuja (ver PoseDosManos.para).
    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumidor) {
        consumidor.accept(com.lucarita.yonga.client.armas.PoseDosManos.para(() -> rasgos != null && rasgos.dosManos));
    }

    // ---------- DESCRIPCIÓN (al pasar el ratón por encima) ----------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        // Una sola línea discreta con palabras clave (sin números: que se descubran jugando)
        List<String> rasgosVisibles = new ArrayList<>();
        if (tieneParry()) rasgosVisibles.add("yonga.arma.rasgo.parry");
        if (rasgos.contraataque) rasgosVisibles.add("yonga.arma.rasgo.contraataque");
        if (rasgos.sangrado != null && Modulo.SANGRADO.activo()) rasgosVisibles.add("yonga.arma.rasgo.sangrado");
        if (rasgos.probAturdir > 0) rasgosVisibles.add("yonga.arma.rasgo.aturdir");
        if (rasgos.rompeEscudos) rasgosVisibles.add("yonga.arma.rasgo.rompe_escudos");
        if (rasgos.lanzable) rasgosVisibles.add("yonga.arma.rasgo.lanzable");
        if (rasgos.combo) rasgosVisibles.add("yonga.arma.rasgo.combo");
        if (rasgos.barridoAmplio) rasgosVisibles.add("yonga.arma.rasgo.barrido_amplio");
        if (rasgos.dosManos) rasgosVisibles.add("yonga.arma.rasgo.dos_manos");
        if (rasgos.perforaArmadura > 0) rasgosVisibles.add("yonga.arma.rasgo.perfora_armadura");

        if (!rasgosVisibles.isEmpty()) {
            MutableComponent linea = Component.empty();
            for (int i = 0; i < rasgosVisibles.size(); i++) {
                if (i > 0) linea.append(" · ");
                linea.append(Component.translatable(rasgosVisibles.get(i)));
            }
            lineas.add(linea.withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        super.appendHoverText(stack, nivel, lineas, flag);
    }
}
