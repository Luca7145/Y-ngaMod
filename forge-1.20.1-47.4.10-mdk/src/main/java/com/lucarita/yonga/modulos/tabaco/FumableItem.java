package com.lucarita.yonga.modulos.tabaco;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * El cigarro y el puro. No se apilan (cada uno es único). Tienen tres estados:
 *  - SIN ENCENDER: se enciende con el mechero en la otra mano (clic derecho, mires adonde mires; vale también al
 *    revés: el mechero en la principal y esto en la secundaria). Gasta un uso del mechero.
 *  - ENCENDIDO: se consume solo con el tiempo, echa humo si lo llevas en una mano, y se FUMA manteniendo clic derecho
 *    (cada calada lo gasta más). Cada calada: o un BOOST (lo normal) o WITHER fuerte y NÁUSEAS (ver Tabaco). Si lo
 *    tiras encendido sobre un bloque que arde, se consume y le prende fuego.
 *  - GASTADO: la colilla. Ya no sirve.
 * El puro dura más, da un boost más fuerte y largo, pero engancha más (más nicotina por calada).
 */
public class FumableItem extends Item {

    public static final int SIN_ENCENDER = 0, ENCENDIDO = 1, GASTADO = 2;
    private static final String ESTADO = "Estado", TIEMPO = "Tiempo";
    private static final int DURACION_CALADA = 30;

    /** Todo lo que distingue al cigarro del puro. */
    public record Tipo(String nombre, int duracion, int costeCalada, float nicotina, int boost,
                       int witherDuracion, int nauseasDuracion) {}

    private final Tipo tipo;

    public FumableItem(Tipo tipo, Properties propiedades) {
        super(propiedades.stacksTo(1));
        this.tipo = tipo;
    }

    public Tipo tipo() {
        return tipo;
    }

    // ---------- ESTADO ----------

    public static int estado(ItemStack stack) {
        CompoundTag datos = stack.getTag();
        return datos == null ? SIN_ENCENDER : datos.getInt(ESTADO);
    }

    public static boolean encendido(ItemStack stack) {
        return stack.getItem() instanceof FumableItem && estado(stack) == ENCENDIDO;
    }

    /** Sin encender y sin nada más: el único que cabe en la cajetilla. */
    public static boolean nuevo(ItemStack stack) {
        return stack.getItem() instanceof FumableItem && !stack.hasTag();
    }

    private static int tiempo(ItemStack stack) {
        CompoundTag datos = stack.getTag();
        return datos == null ? 0 : datos.getInt(TIEMPO);
    }

    private static void gastar(ItemStack stack, int ticks) {
        int quedan = tiempo(stack) - ticks;
        if (quedan <= 0) {
            CompoundTag datos = stack.getOrCreateTag();
            datos.putInt(ESTADO, GASTADO);
            datos.remove(TIEMPO);
        } else {
            stack.getOrCreateTag().putInt(TIEMPO, quedan);
        }
    }

    /** Lo enciende con el mechero de la otra mano (gasta un uso del mechero). */
    public static boolean encender(Player jugador, InteractionHand manoFumable) {
        ItemStack fumable = jugador.getItemInHand(manoFumable);
        InteractionHand otra = manoFumable == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack mechero = jugador.getItemInHand(otra);
        if (!(fumable.getItem() instanceof FumableItem f) || estado(fumable) != SIN_ENCENDER || !(mechero.getItem() instanceof FlintAndSteelItem)) {
            return false;
        }
        Level nivel = jugador.level();
        nivel.playSound(jugador, jugador.getX(), jugador.getEyeY(), jugador.getZ(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 1.0f, nivel.getRandom().nextFloat() * 0.4f + 0.8f);
        if (!nivel.isClientSide()) {
            CompoundTag datos = fumable.getOrCreateTag();
            datos.putInt(ESTADO, ENCENDIDO);
            datos.putInt(TIEMPO, f.tipo.duracion());
            mechero.hurtAndBreak(1, jugador, p -> p.broadcastBreakEvent(otra));
        }
        return true;
    }

    // ---------- USAR: ENCENDER O FUMAR ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack stack = jugador.getItemInHand(mano);
        int estado = estado(stack);
        if (estado == SIN_ENCENDER && encender(jugador, mano)) {
            return InteractionResultHolder.sidedSuccess(stack, nivel.isClientSide());
        }
        if (estado == ENCENDIDO) {
            jugador.startUsingItem(mano);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;          // se lleva a la boca
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DURACION_CALADA;
    }

    /** Una calada. */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level nivel, LivingEntity quien) {
        if (estado(stack) != ENCENDIDO || !(quien instanceof Player jugador)) {
            return stack;
        }
        if (nivel instanceof ServerLevel servidor) {
            Tabaco.calada(jugador, tipo);
            gastar(stack, tipo.costeCalada());
            // el humo que se echa por la boca
            Vec3 boca = jugador.getEyePosition().add(jugador.getLookAngle().scale(0.4)).add(0, -0.15, 0);
            Vec3 mira = jugador.getLookAngle();
            for (int i = 0; i < 10; i++) {
                double v = 0.03 + i * 0.006;
                servidor.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, boca.x, boca.y, boca.z, 0, mira.x, mira.y + 0.2, mira.z, v);
            }
            nivel.playSound(null, jugador.getX(), jugador.getEyeY(), jugador.getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.25f, 1.7f);
        }
        jugador.getCooldowns().addCooldown(this, 10);
        return stack;
    }

    // ---------- SE CONSUME SOLO Y ECHA HUMO ----------

    @Override
    public void inventoryTick(ItemStack stack, Level nivel, Entity entidad, int hueco, boolean seleccionado) {
        if (nivel.isClientSide() || estado(stack) != ENCENDIDO) return;
        gastar(stack, 1);
        if (!(entidad instanceof Player jugador) || !(nivel instanceof ServerLevel servidor) || entidad.tickCount % 4 != 0) return;
        boolean enPrincipal = jugador.getMainHandItem() == stack, enSecundaria = jugador.getOffhandItem() == stack;
        if (!enPrincipal && !enSecundaria || estado(stack) != ENCENDIDO) return;
        Vec3 p = posicionMano(jugador, enPrincipal);
        servidor.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.005);
    }

    /** Más o menos dónde queda la punta, en la mano. */
    private static Vec3 posicionMano(Player jugador, boolean principal) {
        boolean derecha = (jugador.getMainArm() == HumanoidArm.RIGHT) == principal;
        double giro = Math.toRadians(jugador.yBodyRot);
        Vec3 frente = new Vec3(-Math.sin(giro), 0, Math.cos(giro));
        Vec3 aLaDerecha = new Vec3(-Math.cos(giro), 0, -Math.sin(giro));
        return jugador.position().add(0, jugador.isCrouching() ? 0.55 : 0.8, 0).add(frente.scale(0.45))
                .add(aLaDerecha.scale(derecha ? 0.38 : -0.38));
    }

    /** Tirado al suelo: si está encendido sigue consumiéndose, echa humo, y si cae sobre algo que arde, lo prende. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity objeto) {
        Level nivel = objeto.level();
        if (nivel.isClientSide() || estado(stack) != ENCENDIDO) return false;
        gastar(stack, 1);
        if (nivel instanceof ServerLevel servidor && objeto.tickCount % 5 == 0) {
            servidor.sendParticles(ParticleTypes.SMOKE, objeto.getX(), objeto.getY() + 0.15, objeto.getZ(), 1, 0.02, 0.02, 0.02, 0.005);
        }
        if (estado(stack) == ENCENDIDO && objeto.onGround()) {
            BlockPos aqui = objeto.blockPosition();
            BlockPos debajo = BlockPos.containing(objeto.getX(), objeto.getY() - 0.2, objeto.getZ());
            BlockState suelo = nivel.getBlockState(debajo);
            BlockState dentro = nivel.getBlockState(aqui);
            boolean ardeDebajo = suelo.isFlammable(nivel, debajo, Direction.UP);
            boolean ardeDentro = !dentro.isAir() && dentro.isFlammable(nivel, aqui, Direction.UP);   // alfombras, hierba seca...
            BlockPos fuego = ardeDentro ? aqui.above() : aqui;
            if ((ardeDebajo || ardeDentro) && BaseFireBlock.canBePlacedAt(nivel, fuego, Direction.UP)) {
                nivel.setBlock(fuego, BaseFireBlock.getState(nivel, fuego), 11);
                nivel.playSound(null, fuego, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
                objeto.discard();
                return true;
            }
        }
        return false;
    }

    // ---------- ASPECTO ----------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
        switch (estado(stack)) {
            case ENCENDIDO -> lineas.add(Component.translatable("yonga.tabaco.encendido", StringUtil.formatTickDuration(tiempo(stack)))
                    .withStyle(ChatFormatting.GOLD));
            case GASTADO -> lineas.add(Component.translatable("yonga.tabaco.gastado").withStyle(ChatFormatting.DARK_GRAY));
            default -> lineas.add(Component.translatable("yonga.tabaco.sin_encender").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, nivel, lineas, flag);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        // que no "rebote" en la mano cada vez que se consume un poco
        return cambioDeHueco || antes.getItem() != despues.getItem() || estado(antes) != estado(despues);
    }
}
