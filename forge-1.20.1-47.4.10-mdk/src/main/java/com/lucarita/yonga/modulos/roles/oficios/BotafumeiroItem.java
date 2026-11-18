package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.ModItems;
import com.lucarita.yonga.core.ModTiers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * Botafumeiro del Sacerdote: un mangual cuya bola es un incensario colgado de una cadena.
 *
 *  - Click izquierdo: golpea como un mangual (9 de daño, lento). No hace barrido.
 *  - Mantener click derecho: lo volteas para coger impulso (sale humo, cada vez más). Al soltar, el
 *    incensario sale disparado unido por la cadena. Cuanto más cargado, más lejos, más rápido y más daño.
 *  - Carga al máximo (1,5 s): sale en llamas y prende fuego a lo que golpea.
 *  - El incensario vuelve solo (al llegar al final de la cadena o al golpear algo). Mientras está fuera,
 *    click derecho lo hace volver ya. Hasta que vuelve no se puede lanzar otra vez.
 *  - Durabilidad: 2200 (lucarita). Se gasta 1 por golpe y 1 por lanzamiento. Se repara con lucarita.
 */
public class BotafumeiroItem extends SwordItem {

    // ---- Ajustes ----
    private static final int DANO_EXTRA = 4;                 // en el juego: 1 + 4 (lucarita) + 4 = 9
    private static final float VELOCIDAD_ATAQUE = -3.0f;     // 1,0 golpes por segundo (lento, como un mangual)
    private static final int DURABILIDAD = 2200;
    public static final int TICKS_CARGA_MAXIMA = 30;         // 1,5 s
    private static final int TICKS_CARGA_MINIMA = 5;
    // Lanzamiento según la carga (de 0 a 1)
    private static final float DANO_MINIMO = 4, DANO_MAXIMO = 12;
    private static final double ALCANCE_MINIMO = 5, ALCANCE_MAXIMO = 10;
    private static final double VELOCIDAD_MINIMA = 0.8, VELOCIDAD_MAXIMA = 1.8;

    private static final String CLAVE_LANZADO = "yonga_lanzado";
    private static final String CLAVE_SOLO_INCENSARIO = "yonga_solo_incensario";

    /**
     * Qué modelo usar (propiedad "yonga:parte"): 0 = entero, 1 = solo el mango (el incensario está lanzado),
     * 2 = solo el incensario (lo usa el dibujo del incensario en vuelo).
     */
    public static float parte(ItemStack objeto) {
        if (!objeto.hasTag()) return 0;
        if (objeto.getTag().getBoolean(CLAVE_SOLO_INCENSARIO)) return 2;
        return objeto.getTag().getBoolean(CLAVE_LANZADO) ? 1 : 0;
    }

    public static void marcarSoloIncensario(ItemStack objeto) {
        objeto.getOrCreateTag().putBoolean(CLAVE_SOLO_INCENSARIO, true);
    }

    public BotafumeiroItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD_ATAQUE, propiedades.durability(DURABILIDAD));
    }

    // ---------- ¿Tiene el incensario fuera? (para el modelo: solo el mango) ----------

    public static boolean lanzado(ItemStack objeto) {
        return objeto.hasTag() && objeto.getTag().getBoolean(CLAVE_LANZADO);
    }

    public static void marcarLanzado(ItemStack objeto, boolean lanzado) {
        if (!(objeto.getItem() instanceof BotafumeiroItem)) {
            return;
        }
        if (lanzado) {
            objeto.getOrCreateTag().putBoolean(CLAVE_LANZADO, true);
        } else if (objeto.hasTag()) {
            objeto.getTag().remove(CLAVE_LANZADO);
            if (objeto.getTag().isEmpty()) {
                objeto.setTag(null);
            }
        }
    }

    // Si por lo que sea el incensario ya no existe (se descargó, se cayó el servidor...), vuelve a la mano
    @Override
    public void inventoryTick(ItemStack objeto, Level nivel, Entity entidad, int hueco, boolean enMano) {
        if (!nivel.isClientSide() && lanzado(objeto) && entidad.tickCount % 10 == 0
                && !IncensarioEntity.lanzado(entidad.getUUID())) {
            marcarLanzado(objeto, false);
        }
    }

    // No hace barrido como una espada
    @Override
    public boolean canPerformAction(ItemStack objeto, ToolAction accion) {
        return accion != ToolActions.SWORD_SWEEP && super.canPerformAction(objeto, accion);
    }

    @Override
    public boolean isValidRepairItem(ItemStack objeto, ItemStack material) {
        return material.is(ModItems.LUCARITA.get());
    }

    // Que no se mueva la mano arriba y abajo cada vez que cambia el NBT (lanzado / no lanzado)
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        return cambioDeHueco || antes.getItem() != despues.getItem();
    }

    // ---------- CARGAR Y LANZAR ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack objeto = jugador.getItemInHand(mano);
        if (lanzado(objeto)) {
            if (!nivel.isClientSide()) {
                IncensarioEntity.llamar(jugador.getUUID());
            }
            return InteractionResultHolder.consume(objeto);
        }
        jugador.startUsingItem(mano);
        return InteractionResultHolder.consume(objeto);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack objeto) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack objeto) {
        return 72000;
    }

    public static float carga(int ticksUsando) {
        return Mth.clamp((float) ticksUsando / TICKS_CARGA_MAXIMA, 0, 1);
    }

    // Mientras se carga: humo cada vez más, y llamas al llegar al máximo
    @Override
    public void onUseTick(Level nivel, LivingEntity usuario, ItemStack objeto, int ticksRestantes) {
        int usando = getUseDuration(objeto) - ticksRestantes;
        if (nivel.isClientSide()) {
            // A la altura de la mano y un poco hacia atrás y al lado, para que no tape la vista
            Vec3 mirada = usuario.getLookAngle();
            Vec3 lado = mirada.cross(new Vec3(0, 1, 0)).normalize().scale(usuario.getMainArm() == HumanoidArm.RIGHT ? 0.45 : -0.45);
            Vec3 mano = usuario.getEyePosition().add(mirada.scale(0.1)).add(lado).subtract(0, 0.55, 0);
            float carga = carga(usando);
            if (usuario.getRandom().nextFloat() < 0.3f + carga * 0.7f) {
                nivel.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, mano.x, mano.y, mano.z, 0, 0.02, 0);
            }
            if (carga >= 1) {
                nivel.addParticle(ParticleTypes.FLAME, mano.x + (usuario.getRandom().nextDouble() - 0.5) * 0.4,
                        mano.y, mano.z + (usuario.getRandom().nextDouble() - 0.5) * 0.4, 0, 0.03, 0);
            }
        } else {
            if (usando == TICKS_CARGA_MAXIMA) {
                nivel.playSound(null, usuario.getX(), usuario.getY(), usuario.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
            } else if (usando % 10 == 0 && usando < TICKS_CARGA_MAXIMA) {
                nivel.playSound(null, usuario.getX(), usuario.getY(), usuario.getZ(), SoundEvents.CHAIN_STEP, SoundSource.PLAYERS, 0.6f, 0.8f + usando / 40f);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack objeto, Level nivel, LivingEntity usuario, int ticksRestantes) {
        int usando = getUseDuration(objeto) - ticksRestantes;
        if (nivel.isClientSide() || !(usuario instanceof ServerPlayer jugador) || usando < TICKS_CARGA_MINIMA
                || lanzado(objeto)) {
            return;
        }
        float carga = carga(usando);
        boolean ardiendo = carga >= 1;
        IncensarioEntity.lanzar(jugador, carga,
                Mth.lerp(carga, DANO_MINIMO, DANO_MAXIMO),
                Mth.lerp(carga, ALCANCE_MINIMO, ALCANCE_MAXIMO),
                Mth.lerp(carga, VELOCIDAD_MINIMA, VELOCIDAD_MAXIMA),
                ardiendo);
        marcarLanzado(objeto, true);
        objeto.hurtAndBreak(1, jugador, j -> j.broadcastBreakEvent(jugador.getUsedItemHand()));
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 0.7f);
        if (ardiendo) {
            nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.7f, 1.0f);
        }
    }

    // ---------- GOLPEAR ----------

    @Override
    public boolean hurtEnemy(ItemStack objeto, LivingEntity objetivo, LivingEntity atacante) {
        objeto.hurtAndBreak(1, atacante, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
}
