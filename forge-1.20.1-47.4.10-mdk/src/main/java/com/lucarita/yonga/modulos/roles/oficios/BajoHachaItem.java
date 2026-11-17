package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.core.GolpeCargado;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bajo-hacha del Melómano: un hacha de lucarita que además es un bajo.
 *
 *  - Click izquierdo: golpea como un hacha pesada (rompe escudos, como cualquier hacha).
 *  - Click derecho: toca una nota. El tono depende de hacia dónde miras: abajo = grave, arriba = agudo
 *    (2 octavas). Manteniendo pulsado se repite la nota.
 *  - Ritmo: cada nota (y cada golpe cargado con el hacha) llena la barra de ritmo, si no paras más de
 *    1,5 segundos. Con la barra llena, la siguiente nota es un ACORDE DE POTENCIA: una onda que empuja
 *    y daña a los monstruos de alrededor (no afecta a jugadores, animales ni aldeanos).
 */
public class BajoHachaItem extends AxeItem {

    // ---- Ajustes ----
    private static final float DANO_EXTRA = 6.0f;          // daño en el juego: 1 + 4 (lucarita) + 6 = 11
    private static final float VELOCIDAD = -3.0f;          // velocidad en el juego: 4 - 3 = 1.0
    public static final int DURABILIDAD = 2200;

    private static final int SEMITONOS = 24;               // 2 octavas
    private static final int RITMO_MAXIMO = 100;
    private static final int RITMO_POR_NOTA = 10;
    private static final int RITMO_POR_GOLPE = 15;
    private static final long PAUSA_MAXIMA = 30;           // ticks (1,5 s) sin tocar ni golpear y el ritmo se pierde

    private static final double RADIO_ACORDE = 4.0;
    private static final double EMPUJE_ACORDE = 1.5;
    private static final float DANO_ACORDE = 6.0f;          // 3 corazones

    private record Ritmo(int valor, long ultimo) {}
    private static final Map<UUID, Ritmo> RITMOS = new HashMap<>();

    public BajoHachaItem(Properties propiedades) {
        super(ModTiers.LUCARITA, DANO_EXTRA, VELOCIDAD, propiedades.durability(DURABILIDAD));
    }

    // Click derecho apuntando a un bloque: no descorteza troncos (como haría un hacha), así que sigue tocando
    @Override
    public InteractionResult useOn(UseOnContext contexto) {
        return InteractionResult.PASS;
    }

    // Click derecho: tocar
    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack bajo = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            tocar(servidor);
        }
        // "consume" = no hace el gesto de golpear al tocar
        return InteractionResultHolder.consume(bajo);
    }

    // Golpe con el hacha: también suma ritmo (si es un golpe cargado)
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity objetivo, LivingEntity atacante) {
        if (atacante instanceof ServerPlayer jugador && GolpeCargado.fue(jugador)) {
            int ritmo = sumarRitmo(jugador, RITMO_POR_GOLPE);
            mostrarRitmo(jugador, ritmo);
        }
        return super.hurtEnemy(stack, objetivo, atacante);
    }

    // ---------- TOCAR ----------

    private static void tocar(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();

        // ¿Barra llena? Entonces esta nota es el acorde de potencia
        if (ritmoActual(jugador) >= RITMO_MAXIMO) {
            acordeDePotencia(jugador);
            RITMOS.remove(jugador.getUUID());
            mostrarRitmo(jugador, 0);
            return;
        }

        // Nota según la mirada: getXRot() va de -90 (arriba) a 90 (abajo)
        int semitono = Mth.clamp(Math.round((90.0f - jugador.getXRot()) / 180.0f * SEMITONOS), 0, SEMITONOS);
        float tono = (float) Math.pow(2.0, (semitono - SEMITONOS / 2) / 12.0);   // 0,5 (grave) a 2,0 (agudo)
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_BAJO_NOTA.get(), SoundSource.RECORDS, 1.0f, tono);
        // Notita musical encima de la cabeza, del color de la nota (como en los bloques musicales)
        nivel.sendParticles(ParticleTypes.NOTE, jugador.getX(), jugador.getEyeY() + 0.6, jugador.getZ(),
                0, semitono / (double) SEMITONOS, 0.0, 0.0, 1.0);

        int ritmo = sumarRitmo(jugador, RITMO_POR_NOTA);
        mostrarRitmo(jugador, ritmo);
    }

    // Onda sonora: empuja y daña a los monstruos cercanos
    private static void acordeDePotencia(ServerPlayer jugador) {
        ServerLevel nivel = jugador.serverLevel();
        List<LivingEntity> monstruos = nivel.getEntitiesOfClass(LivingEntity.class,
                new AABB(jugador.blockPosition()).inflate(RADIO_ACORDE + 1),
                e -> e instanceof Enemy && e.isAlive() && e.distanceTo(jugador) <= RADIO_ACORDE);
        for (LivingEntity monstruo : monstruos) {
            monstruo.hurt(jugador.damageSources().playerAttack(jugador), DANO_ACORDE);
            monstruo.knockback(EMPUJE_ACORDE, jugador.getX() - monstruo.getX(), jugador.getZ() - monstruo.getZ());
            monstruo.hurtMarked = true;
        }
        nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                ModuloRoles.SONIDO_BAJO_ACORDE.get(), SoundSource.RECORDS, 1.5f, 1.0f);
        // Onda visual: un anillo de notas y un estallido sónico en el centro
        nivel.sendParticles(ParticleTypes.SONIC_BOOM, jugador.getX(), jugador.getY() + 1.0, jugador.getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 16; i++) {
            double angulo = Math.PI * 2 * i / 16;
            nivel.sendParticles(ParticleTypes.NOTE,
                    jugador.getX() + Math.cos(angulo) * RADIO_ACORDE * 0.6, jugador.getY() + 1.0,
                    jugador.getZ() + Math.sin(angulo) * RADIO_ACORDE * 0.6,
                    0, i / 16.0, 0.0, 0.0, 1.0);
        }
    }

    // ---------- RITMO ----------

    private static int ritmoActual(ServerPlayer jugador) {
        Ritmo ritmo = RITMOS.get(jugador.getUUID());
        if (ritmo == null || jugador.level().getGameTime() - ritmo.ultimo() > PAUSA_MAXIMA) {
            return 0;
        }
        return ritmo.valor();
    }

    private static int sumarRitmo(ServerPlayer jugador, int cantidad) {
        int nuevo = Math.min(RITMO_MAXIMO, ritmoActual(jugador) + cantidad);
        RITMOS.put(jugador.getUUID(), new Ritmo(nuevo, jugador.level().getGameTime()));
        return nuevo;
    }

    // Barra de ritmo en la barra de acción (encima del inventario rápido): ■■■■■□□□□□
    private static void mostrarRitmo(ServerPlayer jugador, int ritmo) {
        int llenos = ritmo / 10;
        MutableComponent barra = Component.literal("■".repeat(llenos))
                .withStyle(ritmo >= RITMO_MAXIMO ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE);
        barra.append(Component.literal("□".repeat(10 - llenos)).withStyle(ChatFormatting.DARK_GRAY));
        jugador.displayClientMessage(barra, true);
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        RITMOS.remove(evento.getEntity().getUUID());
    }
}
