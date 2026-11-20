package com.lucarita.yonga.modulos.tabaco;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.UUID;

/**
 * Lo que le hace el tabaco al jugador.
 *
 * CADA CALADA: lo normal es un BOOST (cigarro: velocidad y prisa; puro: fuerza, resistencia y velocidad; los dos,
 * además, +1,5 bloques de ALCANCE: al pegar, al romper, al poner bloques y al usar cosas), pero hay una pequeña
 * probabilidad de WITHER fuerte y NÁUSEAS. Es cosa de LARGO PLAZO: depende de la NICOTINA acumulada (un cigarro
 * entero = 1 punto, un puro entero = 2,5), y crece con el cuadrado, así que los primeros cigarros casi nunca hacen
 * nada malo, pero tras unos cuantos ya es bastante probable. Y baja MUY despacio: un punto por cada día de Minecraft
 * (20 minutos) sin fumar.
 *   probabilidad mala = BASE + CURVA x nicotina²   (como mucho MAXIMA)
 *   0 cigarros: 0,5 %   ·   3: 4 %   ·   5: 10,5 %   ·   8: 26 %   ·   10: 40 %   ·   12 o más: 60 %
 * El BOOST solo hace efecto mientras lleves en una mano un cigarro (el del cigarro) o un puro (el del puro); el
 * wither y las náuseas, en cambio, se quedan aunque lo tires.
 * Todo se guarda en los datos del jugador (la nicotina se conserva aunque mueras).
 */
public final class Tabaco {

    private Tabaco() {}

    public static final float BASE = 0.005f, CURVA = 0.004f, MAXIMA = 0.6f;
    /** El alcance extra del boost (bloques). */
    public static final double ALCANCE = 1.5;
    private static final UUID ALCANCE_ID = UUID.fromString("b7f2c1d4-3e5a-4f60-9a8b-1c2d3e4f5a6b");
    /** Lo que tarda en irse un punto de nicotina sin fumar (un día de Minecraft), y lo que espera antes de empezar a bajar. */
    private static final int TICKS_POR_PUNTO = 24000, ESPERA_BAJAR = 1200;

    private static final String NICOTINA = "yonga_nicotina", ULTIMA = "yonga_ultima_calada";
    private static final String BOOST = "yonga_boost_";

    public static float nicotina(Player jugador) {
        return jugador.getPersistentData().getFloat(NICOTINA);
    }

    public static float probabilidadMala(Player jugador) {
        float n = nicotina(jugador);
        return Math.min(MAXIMA, BASE + CURVA * n * n);
    }

    /** Una calada: o boost, o wither y náuseas. Y más nicotina. */
    public static void calada(Player jugador, FumableItem.Tipo tipo) {
        CompoundTag datos = jugador.getPersistentData();
        boolean mala = jugador.getRandom().nextFloat() < probabilidadMala(jugador);
        datos.putFloat(NICOTINA, nicotina(jugador) + tipo.nicotina());
        datos.putLong(ULTIMA, jugador.level().getGameTime());
        if (mala) {
            jugador.addEffect(new MobEffectInstance(MobEffects.WITHER, tipo.witherDuracion(), 1));
            jugador.addEffect(new MobEffectInstance(MobEffects.CONFUSION, tipo.nauseasDuracion(), 0));
        } else {
            String clave = BOOST + tipo.nombre();
            datos.putInt(clave, Math.max(datos.getInt(clave), tipo.boost()));
        }
    }

    /** Los efectos del boost de cada tipo. */
    private static MobEffect[] efectos(String tipo) {
        return ModuloTabaco.PURO_TIPO.nombre().equals(tipo)
                ? new MobEffect[]{MobEffects.DAMAGE_BOOST, MobEffects.DAMAGE_RESISTANCE, MobEffects.MOVEMENT_SPEED}
                : new MobEffect[]{MobEffects.MOVEMENT_SPEED, MobEffects.DIG_SPEED};
    }

    /** Cada tick: la nicotina baja si no se fuma, y el boost se aplica solo con el cigarro / puro en la mano. */
    public static void alTick(TickEvent.PlayerTickEvent evento) {
        Player jugador = evento.player;
        if (evento.phase != TickEvent.Phase.END || jugador.level().isClientSide()) return;
        CompoundTag datos = jugador.getPersistentData();
        float nicotina = datos.getFloat(NICOTINA);
        if (nicotina > 0 && jugador.level().getGameTime() - datos.getLong(ULTIMA) > ESPERA_BAJAR) {
            datos.putFloat(NICOTINA, Math.max(0, nicotina - 1.0f / TICKS_POR_PUNTO));
        }
        boolean alcance = false;
        for (FumableItem.Tipo tipo : new FumableItem.Tipo[]{ModuloTabaco.CIGARRO_TIPO, ModuloTabaco.PURO_TIPO}) {
            String clave = BOOST + tipo.nombre();
            int quedan = datos.getInt(clave);
            if (quedan <= 0) continue;
            datos.putInt(clave, quedan - 1);
            if (enLaMano(jugador, tipo)) {
                alcance = true;
                if (jugador.tickCount % 10 == 0) {
                    for (MobEffect efecto : efectos(tipo.nombre())) {
                        jugador.addEffect(new MobEffectInstance(efecto, 40, 0, true, false, true));
                    }
                }
            }
        }
        ponerAlcance(jugador.getAttribute(ForgeMod.BLOCK_REACH.get()), alcance);
        ponerAlcance(jugador.getAttribute(ForgeMod.ENTITY_REACH.get()), alcance);
    }

    /** +1,5 de alcance (romper, poner y usar bloques; pegar y usar con entidades) mientras dure el boost en la mano. */
    private static void ponerAlcance(AttributeInstance atributo, boolean activo) {
        if (atributo == null) return;
        boolean tiene = atributo.getModifier(ALCANCE_ID) != null;
        if (activo && !tiene) {
            atributo.addTransientModifier(new AttributeModifier(ALCANCE_ID, "yonga_tabaco_alcance", ALCANCE, AttributeModifier.Operation.ADDITION));
        } else if (!activo && tiene) {
            atributo.removeModifier(ALCANCE_ID);
        }
    }

    private static boolean enLaMano(Player jugador, FumableItem.Tipo tipo) {
        for (ItemStack s : new ItemStack[]{jugador.getMainHandItem(), jugador.getOffhandItem()}) {
            if (s.getItem() instanceof FumableItem f && f.tipo() == tipo) return true;
        }
        return false;
    }

    /**
     * Mechero en la mano principal y el cigarro/puro sin encender en la secundaria, mirando a un bloque: se enciende
     * el cigarro (en vez de prender fuego al bloque).
     */
    public static void alClicBloque(PlayerInteractEvent.RightClickBlock evento) {
        Player jugador = evento.getEntity();
        if (evento.getHand() != InteractionHand.MAIN_HAND || !(jugador.getMainHandItem().getItem() instanceof FlintAndSteelItem)) return;
        ItemStack otra = jugador.getOffhandItem();
        if (!(otra.getItem() instanceof FumableItem) || FumableItem.estado(otra) != FumableItem.SIN_ENCENDER) return;
        if (FumableItem.encender(jugador, InteractionHand.OFF_HAND)) {
            evento.setCanceled(true);
            evento.setCancellationResult(InteractionResult.sidedSuccess(jugador.level().isClientSide()));
        }
    }

    /** La nicotina es a largo plazo: se conserva al morir (el boost no) y al volver del End (el boost también). */
    public static void alClonar(PlayerEvent.Clone evento) {
        CompoundTag antes = evento.getOriginal().getPersistentData();
        CompoundTag ahora = evento.getEntity().getPersistentData();
        String[] claves = evento.isWasDeath() ? new String[]{NICOTINA, ULTIMA}
                : new String[]{NICOTINA, ULTIMA, BOOST + ModuloTabaco.CIGARRO_TIPO.nombre(), BOOST + ModuloTabaco.PURO_TIPO.nombre()};
        for (String clave : claves) {
            if (antes.contains(clave)) ahora.put(clave, antes.get(clave).copy());
        }
    }
}
