package com.lucarita.yonga.modulos.roles.cara;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.red.CaraPacket;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * El hueco de cara del inventario (encima del escudo) y lo que hacen los objetos que van en él.
 *
 * Objetos de cara: el antifaz (los mobs te ven a la mitad de distancia) y los medallones (dan efectos, ver Medallon).
 * Qué se puede poner: los objetos de la etiqueta "yonga:cara" (data/yonga/tags/items/cara.json).
 * Para añadir un objeto nuevo de cara, basta con añadirlo a esa etiqueta.
 */
public final class Cara {

    public static final TagKey<Item> OBJETOS_DE_CARA = ItemTags.create(new ResourceLocation(YongaMain.MOD_ID, "cara"));

    // Antifaz: los mobs te detectan a la mitad de distancia
    private static final double VISIBILIDAD_ANTIFAZ = 0.5;

    // Medallones: duración de sus efectos (se renuevan cada segundo). Más de 10 s para que la visión
    // nocturna no parpadee; al quitarse el medallón se borran en el momento.
    private static final int DURACION_EFECTO_MEDALLON = 260;
    private static final int TICKS_ENTRE_RENOVACIONES = 20;

    // Lo último que mandamos a los clientes de cada jugador (para avisar solo cuando cambia)
    private static final Map<UUID, ItemStack> ULTIMO_ENVIADO = new HashMap<>();

    private Cara() {}

    /** Lo que lleva el jugador en la cara (vacío si nada). */
    public static ItemStack obtener(Player jugador) {
        return ((AccesoCara) jugador).yonga$cara().getItem(0);
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    /** Antifaz: Minecraft multiplica la distancia a la que te detecta un mob por esto. */
    public static void alCalcularVisibilidad(LivingEvent.LivingVisibilityEvent evento) {
        if (evento.getEntity() instanceof Player jugador && obtener(jugador).is(ModuloRoles.ANTIFAZ.get())) {
            evento.modifyVisibility(VISIBILIDAD_ANTIFAZ);
        }
    }

    // ---------- QUE LOS DEMÁS VEAN LO QUE LLEVAS EN LA CARA ----------

    /** Cada tick: si ha cambiado lo que lleva en la cara, se lo contamos a él y a todos los que lo ven. */
    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) {
            return;
        }
        ItemStack actual = obtener(jugador);
        ItemStack ultimo = ULTIMO_ENVIADO.get(jugador.getUUID());
        boolean haCambiado = ultimo == null || !ItemStack.matches(ultimo, actual);

        // Medallones: quitar los efectos del que se ha quitado y renovar los del que lleva puesto
        if (haCambiado && ultimo != null && ultimo.getItem() instanceof MedallonItem anterior) {
            quitarEfectos(jugador, anterior.medallon());
        }
        if (actual.getItem() instanceof MedallonItem medallon && (haCambiado || jugador.tickCount % TICKS_ENTRE_RENOVACIONES == 0)) {
            darEfectos(jugador, medallon.medallon());
        }

        if (haCambiado) {
            ULTIMO_ENVIADO.put(jugador.getUUID(), actual.copy());
            RolesRed.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> jugador),
                    new CaraPacket(jugador.getId(), actual.copy()));
        }
    }

    // ---------- MEDALLONES ----------

    private static void darEfectos(ServerPlayer jugador, Medallon medallon) {
        for (MobEffect efecto : medallon.efectos()) {
            // nivel I, "ambiental" (como los de un faro), sin partículas, con icono
            jugador.addEffect(new MobEffectInstance(efecto, DURACION_EFECTO_MEDALLON, 0, true, false, true));
        }
    }

    /** Quita solo los efectos que puso el medallón (no una poción tuya que dure más o sea más fuerte). */
    private static void quitarEfectos(ServerPlayer jugador, Medallon medallon) {
        for (MobEffect efecto : medallon.efectos()) {
            MobEffectInstance activo = jugador.getEffect(efecto);
            if (activo != null && activo.isAmbient() && activo.getAmplifier() == 0
                    && activo.getDuration() <= DURACION_EFECTO_MEDALLON) {
                jugador.removeEffect(efecto);
            }
        }
    }

    /** Cuando alguien empieza a ver a un jugador (se acerca, entra...), le contamos qué lleva en la cara. */
    public static void alEmpezarAVer(PlayerEvent.StartTracking evento) {
        if (evento.getTarget() instanceof ServerPlayer visto && evento.getEntity() instanceof ServerPlayer observador) {
            ItemStack objeto = obtener(visto);
            if (!objeto.isEmpty()) {
                RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> observador), new CaraPacket(visto.getId(), objeto.copy()));
            }
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        ULTIMO_ENVIADO.remove(evento.getEntity().getUUID());
    }

    /**
     * Al reaparecer (o volver del End) el jugador es un objeto nuevo: copiamos lo que llevaba en la cara.
     * Si murió sin "keepInventory", el objeto ya se soltó al suelo (ver PlayerMixin) y no queda nada que copiar.
     */
    public static void alClonar(PlayerEvent.Clone evento) {
        ItemStack anterior = obtener(evento.getOriginal());
        ((AccesoCara) evento.getEntity()).yonga$cara().setItem(0, anterior.copy());
        ULTIMO_ENVIADO.remove(evento.getEntity().getUUID());   // que se vuelva a enviar
    }
}
