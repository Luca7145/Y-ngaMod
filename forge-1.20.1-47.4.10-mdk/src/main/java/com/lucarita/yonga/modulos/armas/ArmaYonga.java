package com.lucarita.yonga.modulos.armas;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.roles.RangoRol;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.sangrado.Sangrado;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
        if (!tieneParry()) {
            return super.use(nivel, jugador, mano);
        }
        ItemStack item = jugador.getItemInHand(mano);
        jugador.startUsingItem(mano);
        return InteractionResultHolder.consume(item);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return tieneParry() ? UseAnim.BLOCK : super.getUseAnimation(stack);
    }

    // 72000 ticks = se mantiene la guardia hasta que el jugador suelte el botón
    @Override
    public int getUseDuration(ItemStack stack) {
        return tieneParry() ? 72000 : super.getUseDuration(stack);
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

    // ---------- DESCRIPCIÓN (al pasar el ratón por encima) ----------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        if (rasgos.parry == TipoParry.CUERPO_A_CUERPO) {
            lineas.add(Component.translatable("yonga.arma.parry").withStyle(ChatFormatting.BLUE));
        } else if (rasgos.parry == TipoParry.TAMBIEN_PROYECTILES) {
            lineas.add(Component.translatable("yonga.arma.parry_proyectiles").withStyle(ChatFormatting.BLUE));
        }
        if (rasgos.contraataque) {
            lineas.add(Component.translatable("yonga.arma.contraataque").withStyle(ChatFormatting.BLUE));
        }
        if (rasgos.sangrado != null && Modulo.SANGRADO.activo()) {
            lineas.add(Component.translatable("yonga.arma.sangrado").withStyle(ChatFormatting.DARK_RED));
        }
        if (rasgos.probAturdir > 0) {
            lineas.add(Component.translatable("yonga.arma.aturdir", Math.round(rasgos.probAturdir * 100))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (rasgos.rompeEscudos) {
            lineas.add(Component.translatable("yonga.arma.rompe_escudos").withStyle(ChatFormatting.GOLD));
        }
        if (Modulo.ROLES.activo()) {
            lineas.add(Component.translatable("yonga.arma.fabrica", Rol.HERRERO.nombre(), rasgos.rangoHerrero.nombre())
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, nivel, lineas, flag);
    }
}
