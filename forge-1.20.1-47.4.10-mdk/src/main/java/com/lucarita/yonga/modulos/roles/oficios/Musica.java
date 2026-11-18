package com.lucarita.yonga.modulos.roles.oficios;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import com.lucarita.yonga.modulos.roles.Rol;
import com.lucarita.yonga.modulos.roles.RolDatos;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que comparten los instrumentos del Melómano (bajo-hacha, espada-keytar...):
 *  - Tocar una nota según hacia dónde mira el jugador (abajo = grave, arriba = agudo, 2 octavas).
 *  - El RITMO: cada nota, cada golpe cargado (y cada bloque picado con el pico-guitarra) llenan una barra;
 *    si paras más de 1,5 s (3 s picando) se vacía. A los Melómanos les aguanta el doble.
 *    Con la barra llena, la siguiente nota es el ataque especial del instrumento.
 *    La barra es la misma para todos los instrumentos (si cambias de uno a otro, no la pierdes).
 */
public final class Musica {

    public static final int SEMITONOS = 24;                // 2 octavas
    public static final int RITMO_MAXIMO = 100;
    public static final int RITMO_POR_NOTA = 10;
    public static final int RITMO_POR_GOLPE = 15;
    private static final long PAUSA_MAXIMA = 30;           // ticks (1,5 s) sin tocar ni golpear y el ritmo se pierde

    public static final long PAUSA_PICANDO = 60;
    // Los Melómanos llevan el ritmo en la sangre: a ellos la barra les aguanta el doble sin vaciarse
    // (3 s en vez de 1,5; 6 s picando; 12 s con el arco-lira). Los demás roles pueden usar los
    // instrumentos que les den, pero con el tiempo normal.
    public static final long MULTIPLICADOR_MELOMANO = 2;           // picando bloques se aguanta más (3 s), que romper tarda

    private record Ritmo(int valor, long ultimo, long pausa) {}
    private static final Map<UUID, Ritmo> RITMOS = new HashMap<>();

    private Musica() {}

    // ---------- NOTAS ----------

    /**
     * Suena una nota con el sonido del instrumento, según hacia dónde mira el jugador, con su notita de color.
     * Devuelve el color de la nota (de 0 a 1, como en los bloques musicales).
     */
    public static float tocarNota(ServerPlayer jugador, SoundEvent sonido) {
        ServerLevel nivel = jugador.serverLevel();
        // getXRot() va de -90 (arriba) a 90 (abajo)
        int semitono = Mth.clamp(Math.round((90.0f - jugador.getXRot()) / 180.0f * SEMITONOS), 0, SEMITONOS);
        float tono = (float) Math.pow(2.0, (semitono - SEMITONOS / 2) / 12.0);   // 0,5 (grave) a 2,0 (agudo)
        if (!silenciado(jugador)) {
            nivel.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(), sonido, SoundSource.RECORDS, 1.0f, tono);
        }
        float color = semitono / (float) SEMITONOS;
        nivel.sendParticles(ParticleTypes.NOTE, jugador.getX(), jugador.getEyeY() + 0.6, jugador.getZ(), 0, color, 0.0, 0.0, 1.0);
        return color;
    }

    // ---------- SILENCIO ----------
    // Con la tecla de silenciar (M por defecto) las notas dejan de sonar (para ti y para los demás),
    // pero todo lo demás sigue igual: el ritmo se llena, salen las notitas de colores y los ataques
    // especiales funcionan (y suenan). Se guarda en el jugador, así que dura aunque salgas o mueras.

    private static final String CLAVE_SILENCIO = "yonga_notas_silenciadas";

    public static boolean silenciado(ServerPlayer jugador) {
        return jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getBoolean(CLAVE_SILENCIO);
    }

    public static void alternarSilencio(ServerPlayer jugador) {
        CompoundTag persistentes = jugador.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        boolean silencio = !persistentes.getBoolean(CLAVE_SILENCIO);
        persistentes.putBoolean(CLAVE_SILENCIO, silencio);
        jugador.getPersistentData().put(Player.PERSISTED_NBT_TAG, persistentes);
        jugador.displayClientMessage(Component.translatable(silencio ? "yonga.musica.silenciada" : "yonga.musica.activada")
                .withStyle(silencio ? ChatFormatting.GRAY : ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------- RITMO ----------

    public static int ritmo(ServerPlayer jugador) {
        Ritmo ritmo = RITMOS.get(jugador.getUUID());
        if (ritmo == null || jugador.level().getGameTime() - ritmo.ultimo() > ritmo.pausa()) {
            return 0;
        }
        return ritmo.valor();
    }

    public static boolean ritmoLleno(ServerPlayer jugador) {
        return ritmo(jugador) >= RITMO_MAXIMO;
    }

    /** Suma ritmo y enseña la barra. */
    public static void sumarRitmo(ServerPlayer jugador, int cantidad) {
        sumarRitmo(jugador, cantidad, PAUSA_MAXIMA);
    }

    /** Suma ritmo con una pausa máxima distinta hasta que se pierda (en ticks). */
    public static void sumarRitmo(ServerPlayer jugador, int cantidad, long pausa) {
        if (RolDatos.obtenerRol(jugador) == Rol.MELOMANO) {
            pausa *= MULTIPLICADOR_MELOMANO;
        }
        int nuevo = Math.min(RITMO_MAXIMO, ritmo(jugador) + cantidad);
        RITMOS.put(jugador.getUUID(), new Ritmo(nuevo, jugador.level().getGameTime(), pausa));
        mostrarRitmo(jugador, nuevo);
    }

    /** Vacía la barra (tras usar el ataque especial). */
    public static void vaciarRitmo(ServerPlayer jugador) {
        RITMOS.remove(jugador.getUUID());
        mostrarRitmo(jugador, 0);
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
