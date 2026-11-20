package com.lucarita.yonga.client.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.mixin.SoundEngineAccessor;
import com.lucarita.yonga.mixin.SoundManagerAccessor;
import com.lucarita.yonga.modulos.roles.red.RadiocasetePacket;
import com.mojang.blaze3d.audio.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * El sonido del radiocasete en cada cliente: una canción que sale del jugador que lo lleva y le sigue al moverse.
 * Se oye hasta 32 bloques (volumen 2 con atenuación lineal; el tocadiscos llega a 64).
 *
 * Para la pausa se pausa el canal de audio de esa canción (sin cortarla), y al seguir continúa donde estaba.
 * Quien empieza a oírla cuando ya iba por la mitad se salta lo ya sonado (ver getStream), así todos van a la vez.
 * Minecraft reanuda todos los sonidos al cerrar el menú de pausa, así que en cada tick se vuelven a pausar los que
 * deben estar en pausa.
 */
@Mod.EventBusSubscriber(modid = YongaMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RadiocaseteCliente {

    private static final float VOLUMEN = 2.0f;

    private static final Map<Integer, Cancion> SONANDO = new HashMap<>();

    private RadiocaseteCliente() {}

    public static void recibir(int idEntidad, int accion, Item disco, int desde) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        switch (accion) {
            case RadiocasetePacket.TOCAR -> {
                parar(idEntidad);
                Entity entidad = mc.level.getEntity(idEntidad);
                if (entidad == null || !(disco instanceof RecordItem record)) return;
                Cancion cancion = new Cancion(entidad, record, desde);
                SONANDO.put(idEntidad, cancion);
                mc.getSoundManager().play(cancion);
                mc.gui.setNowPlaying(record.getDisplayName());
            }
            case RadiocasetePacket.PAUSAR -> {
                Cancion cancion = SONANDO.get(idEntidad);
                if (cancion != null) {
                    cancion.pausada = true;
                    enCanal(cancion, Channel::pause);
                }
            }
            case RadiocasetePacket.CONTINUAR -> {
                Cancion cancion = SONANDO.get(idEntidad);
                if (cancion != null) {
                    cancion.pausada = false;
                    enCanal(cancion, Channel::unpause);
                }
            }
            case RadiocasetePacket.PARAR -> parar(idEntidad);
            default -> { }
        }
    }

    private static void parar(int idEntidad) {
        Cancion cancion = SONANDO.remove(idEntidad);
        if (cancion != null) {
            Minecraft.getInstance().getSoundManager().stop(cancion);
        }
    }

    private static void enCanal(Cancion cancion, Consumer<Channel> accion) {
        SoundManager sonidos = Minecraft.getInstance().getSoundManager();
        ChannelAccess.ChannelHandle canal = ((SoundEngineAccessor) ((SoundManagerAccessor) sonidos).yonga$motor()).yonga$canales().get(cancion);
        if (canal != null) {
            canal.execute(accion);
        }
    }

    @SubscribeEvent
    public static void alTick(TickEvent.ClientTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || SONANDO.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Iterator<Map.Entry<Integer, Cancion>> it = SONANDO.entrySet().iterator();
        while (it.hasNext()) {
            Cancion cancion = it.next().getValue();
            if (cancion.isStopped() || (cancion.empezada && !mc.getSoundManager().isActive(cancion))) {
                it.remove();                                  // se ha acabado
            } else if (cancion.pausada && !mc.isPaused()) {
                enCanal(cancion, Channel::pause);
            }
            if (mc.getSoundManager().isActive(cancion)) cancion.empezada = true;
        }
    }

    @SubscribeEvent
    public static void alDesconectar(ClientPlayerNetworkEvent.LoggingOut evento) {
        SONANDO.clear();
    }

    /** La canción, pegada a la entidad que lleva el radiocasete. */
    private static final class Cancion extends AbstractTickableSoundInstance {
        private final Entity entidad;
        boolean pausada;
        boolean empezada;

        private final int desde;

        Cancion(Entity entidad, RecordItem disco, int desde) {
            super(disco.getSound(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
            this.entidad = entidad;
            this.desde = desde;
            this.volume = VOLUMEN;
            this.attenuation = Attenuation.LINEAR;
            this.looping = false;
            mover();
        }

        private void mover() {
            this.x = entidad.getX();
            this.y = entidad.getY() + entidad.getBbHeight() * 0.5;
            this.z = entidad.getZ();
        }

        /**
         * Si hay que empezar a mitad, se salta lo que ya ha sonado: se descodifica y se tira (en segundo plano, antes de
         * que empiece a sonar), así que llega justo por donde van los demás.
         */
        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary sonidos, Sound sonido, boolean bucle) {
            CompletableFuture<AudioStream> flujo = sonidos.getStream(sonido.getPath(), bucle);
            if (desde <= 0) {
                return flujo;
            }
            long empezado = System.nanoTime();
            return flujo.thenApplyAsync(audio -> {
                // Lo que tarde en cargar y en saltar también cuenta: así no se queda por detrás de los demás
                double antes = (System.nanoTime() - empezado) / 1.0e9;
                saltar(audio, desde / 20.0 + antes);
                saltar(audio, (System.nanoTime() - empezado) / 1.0e9 - antes);
                return audio;
            }, Util.backgroundExecutor());
        }

        private static void saltar(AudioStream audio, double segundos) {
            AudioFormat formato = audio.getFormat();
            long quedan = (long) (segundos * formato.getSampleRate()) * formato.getFrameSize();
            try {
                while (quedan > 0) {
                    ByteBuffer trozo = audio.read((int) Math.min(quedan, 64 * 1024));
                    if (trozo == null || !trozo.hasRemaining()) break;
                    quedan -= trozo.remaining();
                }
            } catch (IOException e) {
                // Si falla, que suene desde donde se haya quedado
            }
        }

        @Override
        public void tick() {
            if (entidad.isRemoved()) {
                stop();
                return;
            }
            mover();
        }
    }
}
