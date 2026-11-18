package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Taladro del Ingeniero: un pico a motor que funciona con combustible, como un horno.
 *
 *  - Click derecho: abre su DEPÓSITO, un hueco donde metes (o sacas) el combustible. Vale cualquier
 *    combustible del horno. Se ve la llama de lo que está ardiendo.
 *  - Al picar con combustible en el depósito, el motor ENCIENDE uno y se pone a arder como en un horno:
 *    se va consumiendo con el tiempo (lo uses o no, aunque lo guardes) hasta que se apaga. Cuando se
 *    apaga, al volver a picar enciende el siguiente.
 *    Un carbón arde 80 s, un bloque de carbón 13 min, un cubo de lava 16 min y un lingote de lucarita 2 h.
 *  - Además, cada 3x3 le quita 2 s más de llama (picar sin parar lo gasta bastante antes).
 *  - Encendido (o con combustible en el depósito): pica como uno de netherita y rompe los 8 bloques de
 *    alrededor del que picas (un 3x3 de frente a ti), si tienen la misma dureza o menos y no guardan
 *    cosas (cofres, hornos...). Agachado pica solo uno.
 *  - Apagado y sin combustible: pica como uno de piedra.
 *  - Durabilidad: 2500 (netherita: 2031). Cada 3x3 la gasta como 2,5 bloques, no como 9. Se repara con lucarita.
 */
public class TaladroItem extends PickaxeItem {

    // ---- Ajustes ----
    private static final int DURABILIDAD = 2500;
    public static final int LLAMA_POR_USO = 40;               // cada 3x3 quita 2 s más de llama
    private static final double DESGASTE_EXTRA = 1.5;         // + el 1 normal del bloque central = 2,5 por 3x3
    private static final Tier CON_COMBUSTIBLE = Tiers.NETHERITE;
    private static final Tier SIN_COMBUSTIBLE = Tiers.STONE;

    public static final String CLAVE_DEPOSITO = "yonga_deposito";       // el combustible guardado
    private static final String CLAVE_ARDE_HASTA = "yonga_arde_hasta";   // hasta qué momento arde (tiempo del mundo)
    private static final String CLAVE_ARDE_TOTAL = "yonga_arde_total";   // lo que duraba lo que está ardiendo
    private static final String CLAVE_DESGASTE = "yonga_desgaste";

    // Mientras rompe los 8 de alrededor (para no gastar durabilidad por cada uno ni repetir el 3x3)
    private static boolean taladrando = false;

    public TaladroItem(Properties propiedades) {
        super(Tiers.NETHERITE, 1, -2.8f, propiedades.durability(DURABILIDAD));
    }

    // ---------- COMBUSTIBLE (como un horno) ----------

    // El "reloj" del mundo, en los dos lados (para saber si sigue ardiendo sin tocar el objeto cada tick:
    // si cambiáramos sus datos a cada momento, se cortaría el picado)
    private static long reloj = 0;

    public static void alTickMundo(TickEvent.LevelTickEvent evento) {
        if (evento.phase == TickEvent.Phase.END && evento.level.dimension() == Level.OVERWORLD) {
            reloj = evento.level.getGameTime();
        }
    }

    /** Ticks que le quedan a la llama (0 = apagado). */
    public static int llama(ItemStack taladro) {
        return taladro.hasTag() ? (int) Math.max(0, taladro.getTag().getLong(CLAVE_ARDE_HASTA) - reloj) : 0;
    }

    public static int llamaTotal(ItemStack taladro) {
        return taladro.hasTag() ? taladro.getTag().getInt(CLAVE_ARDE_TOTAL) : 0;
    }

    /** ¿Está ardiendo ahora? (para el dibujo: ventanita encendida) */
    public static boolean cargado(ItemStack taladro) {
        return llama(taladro) > 0;
    }

    public static ItemStack deposito(ItemStack taladro) {
        return taladro.hasTag() && taladro.getTag().contains(CLAVE_DEPOSITO)
                ? ItemStack.of(taladro.getTag().getCompound(CLAVE_DEPOSITO)) : ItemStack.EMPTY;
    }

    public static void fijarDeposito(ItemStack taladro, ItemStack combustible) {
        if (combustible.isEmpty()) {
            if (taladro.hasTag()) taladro.getTag().remove(CLAVE_DEPOSITO);
        } else {
            taladro.getOrCreateTag().put(CLAVE_DEPOSITO, combustible.save(new CompoundTag()));
        }
    }

    /** ¿Funciona como taladro? (ardiendo, o con algo en el depósito que encenderá al picar) */
    public static boolean listo(ItemStack taladro) {
        return cargado(taladro) || ForgeHooks.getBurnTime(deposito(taladro), RecipeType.SMELTING) > 0;
    }

    /** Si está apagado, enciende un combustible del depósito. Devuelve si está ardiendo. */
    private static boolean encender(ItemStack taladro, Player jugador) {
        if (cargado(taladro)) {
            return true;
        }
        ItemStack combustible = deposito(taladro);
        int dura = ForgeHooks.getBurnTime(combustible, RecipeType.SMELTING);
        if (combustible.isEmpty() || dura <= 0) {
            return false;
        }
        ItemStack resto = combustible.getCraftingRemainingItem();   // el cubo vacío de la lava
        combustible.shrink(1);
        fijarDeposito(taladro, combustible.isEmpty() && !resto.isEmpty() ? resto : combustible);
        if (!combustible.isEmpty() && !resto.isEmpty() && !jugador.getInventory().add(resto)) {
            jugador.drop(resto, false);
        }
        taladro.getOrCreateTag().putLong(CLAVE_ARDE_HASTA, reloj + dura);
        taladro.getOrCreateTag().putInt(CLAVE_ARDE_TOTAL, dura);
        jugador.level().playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        return true;
    }

    private static void gastarLlama(ItemStack taladro, int ticks) {
        long hasta = taladro.getOrCreateTag().getLong(CLAVE_ARDE_HASTA);
        taladro.getTag().putLong(CLAVE_ARDE_HASTA, Math.max(reloj, hasta - ticks));
    }

    // Que no se mueva la mano cada vez que cambian sus datos (encender, gastar...)
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        return cambioDeHueco || antes.getItem() != despues.getItem();
    }

    // ---------- CLICK DERECHO: abrir el depósito ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack taladro = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            NetworkHooks.openScreen(servidor,
                    new SimpleMenuProvider((id, inventario, j) -> new TaladroMenu(id, inventario, mano), taladro.getHoverName()),
                    datos -> datos.writeEnum(mano));
        }
        return InteractionResultHolder.sidedSuccess(taladro, nivel.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack taladro, @Nullable Level nivel, List<Component> texto, TooltipFlag avanzado) {
        int segundos = llama(taladro) / 20;
        if (segundos > 0) {
            texto.add(Component.translatable("yonga.taladro.ardiendo", segundos / 60, String.format("%02d", segundos % 60)).withStyle(ChatFormatting.GOLD));
        } else {
            texto.add(Component.translatable("yonga.taladro.apagado").withStyle(ChatFormatting.GRAY));
        }
        ItemStack combustible = deposito(taladro);
        if (!combustible.isEmpty()) {
            texto.add(Component.translatable("yonga.taladro.deposito", combustible.getCount(), combustible.getHoverName()).withStyle(ChatFormatting.YELLOW));
        }
        texto.add(Component.translatable("yonga.taladro.como_cargar").withStyle(ChatFormatting.DARK_GRAY));
    }

    // ---------- PICAR: como uno de netherita con combustible, como uno de piedra sin él ----------

    private static Tier nivel(ItemStack taladro) {
        return listo(taladro) ? CON_COMBUSTIBLE : SIN_COMBUSTIBLE;
    }

    @Override
    public float getDestroySpeed(ItemStack taladro, BlockState estado) {
        return estado.is(BlockTags.MINEABLE_WITH_PICKAXE) ? nivel(taladro).getSpeed() : 1.0f;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack taladro, BlockState estado) {
        return estado.is(BlockTags.MINEABLE_WITH_PICKAXE) && TierSortingRegistry.isCorrectTierForDrops(nivel(taladro), estado);
    }

    @Override
    public boolean isValidRepairItem(ItemStack taladro, ItemStack material) {
        return material.is(ModItems.LUCARITA.get());
    }

    // Los bloques de alrededor no gastan durabilidad uno a uno (se cobra todo junto, ver alRomper)
    @Override
    public boolean mineBlock(ItemStack taladro, Level nivel, BlockState estado, BlockPos pos, LivingEntity minero) {
        if (taladrando) {
            return true;
        }
        return super.mineBlock(taladro, nivel, estado, pos, minero);
    }

    // ---------- EL 3x3 ----------

    /** Antes de romper el bloque que picas: rompe los 8 de alrededor (si hay combustible). */
    public static void alRomper(BlockEvent.BreakEvent evento) {
        if (taladrando || !(evento.getPlayer() instanceof ServerPlayer jugador) || !(evento.getLevel() instanceof ServerLevel nivel)) {
            return;
        }
        if (jugador.isShiftKeyDown()) {
            ItemStack enMano = jugador.getMainHandItem();
            if (enMano.getItem() instanceof TaladroItem) {
                encender(enMano, jugador);   // agachado pica uno solo, pero con el motor en marcha
            }
            return;
        }
        ItemStack taladro = jugador.getMainHandItem();
        if (!(taladro.getItem() instanceof TaladroItem) || !encender(taladro, jugador)) {
            return;
        }
        BlockPos centro = evento.getPos();
        float durezaCentro = evento.getState().getDestroySpeed(nivel, centro);
        if (durezaCentro < 0) {
            return;
        }
        Direction cara = caraPicada(jugador, centro);
        int rotos = 0;
        taladrando = true;
        try {
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (a == 0 && b == 0) {
                        continue;
                    }
                    BlockPos pos = switch (cara.getAxis()) {
                        case Y -> centro.offset(a, 0, b);
                        case X -> centro.offset(0, a, b);
                        case Z -> centro.offset(a, b, 0);
                    };
                    BlockState estado = nivel.getBlockState(pos);
                    float dureza = estado.getDestroySpeed(nivel, pos);
                    // (el agua y la lava tienen dureza 100: nunca se "rompen")
                    if (estado.isAir() || dureza < 0 || dureza > durezaCentro || nivel.getBlockEntity(pos) != null) {
                        continue;
                    }
                    if (jugador.gameMode.destroyBlock(pos)) {
                        rotos++;
                    }
                }
            }
        } finally {
            taladrando = false;
        }
        // Gasta llama y la durabilidad extra (el bloque del centro gasta su 1 normal)
        gastarLlama(taladro, LLAMA_POR_USO);
        if (rotos > 0 && !jugador.isCreative()) {
            double desgaste = taladro.getOrCreateTag().getDouble(CLAVE_DESGASTE) + DESGASTE_EXTRA * rotos / 8.0;
            int entero = (int) desgaste;
            taladro.getOrCreateTag().putDouble(CLAVE_DESGASTE, desgaste - entero);
            if (entero > 0) {
                taladro.hurtAndBreak(entero, jugador, j -> j.broadcastBreakEvent(net.minecraft.world.entity.EquipmentSlot.MAINHAND));
            }
        }
        nivel.playSound(null, centro, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    /** Qué cara del bloque está picando (para que el 3x3 quede de frente). */
    private static Direction caraPicada(ServerPlayer jugador, BlockPos centro) {
        HitResult golpe = jugador.pick(jugador.getBlockReach(), 1.0f, false);
        if (golpe instanceof BlockHitResult bloque && bloque.getType() == HitResult.Type.BLOCK && bloque.getBlockPos().equals(centro)) {
            return bloque.getDirection();
        }
        float inclinacion = jugador.getXRot();
        if (inclinacion > 45) return Direction.UP;
        if (inclinacion < -45) return Direction.DOWN;
        return jugador.getDirection().getOpposite();
    }
}
