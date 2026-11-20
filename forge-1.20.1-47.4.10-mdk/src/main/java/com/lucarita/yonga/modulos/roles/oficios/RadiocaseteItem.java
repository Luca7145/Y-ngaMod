package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.red.RadiocasetePacket;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RADIOCASETE (Melómano). Un tocadiscos de bolsillo: click derecho abre su menú, con un hueco para un disco de
 * música y tres botones: pausa, continuar y volver a empezar. Al meter un disco empieza a sonar solo; al sacarlo,
 * se calla.
 *
 * La música sale del jugador que lo lleva y la oyen todos los que estén cerca (hasta 32 bloques), como un tocadiscos
 * que se mueve con él. Quien llega cuando ya está sonando lo oye por donde va, a la vez que los demás. Suena mientras el radiocasete siga en su inventario (en cualquier hueco); si lo tira o lo
 * guarda en un cofre, se para. Cada jugador solo puede tener uno sonando a la vez.
 *
 * El servidor lleva la cuenta (qué suena, si está en pausa y cuánto lleva) y avisa a los clientes con
 * RadiocasetePacket; el sonido lo pone cada cliente (ver client/roles/RadiocaseteCliente).
 */
public class RadiocaseteItem extends Item {

    private static final String CLAVE_DISCO = "Disco";
    private static final String CLAVE_SESION = "Sesion";

    /** Lo que está sonando en el radiocasete de cada jugador (solo en el servidor). */
    private static final class Estado {
        final long sesion;
        final RecordItem disco;
        int tocado;                 // ticks que lleva sonando (sin contar las pausas)
        boolean pausado;

        Estado(long sesion, RecordItem disco) {
            this.sesion = sesion;
            this.disco = disco;
        }
    }

    private static final Map<UUID, Estado> SONANDO = new HashMap<>();

    public RadiocaseteItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    // ---------- EL DISCO DE DENTRO ----------

    public static ItemStack disco(ItemStack radio) {
        CompoundTag nbt = radio.getTag();
        return nbt != null && nbt.contains(CLAVE_DISCO) ? ItemStack.of(nbt.getCompound(CLAVE_DISCO)) : ItemStack.EMPTY;
    }

    public static void ponerDisco(ItemStack radio, ItemStack disco) {
        if (disco.isEmpty()) {
            if (radio.hasTag()) radio.getTag().remove(CLAVE_DISCO);
        } else {
            radio.getOrCreateTag().put(CLAVE_DISCO, disco.save(new CompoundTag()));
        }
    }

    /** Un número que identifica a este radiocasete (para saber si sigue en el inventario del jugador). */
    private static long sesion(ItemStack radio, Player jugador) {
        CompoundTag nbt = radio.getOrCreateTag();
        if (!nbt.contains(CLAVE_SESION)) {
            nbt.putLong(CLAVE_SESION, jugador.getRandom().nextLong());
        }
        return nbt.getLong(CLAVE_SESION);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack radio = jugador.getItemInHand(mano);
        if (!nivel.isClientSide() && jugador instanceof ServerPlayer servidor) {
            sesion(radio, jugador);
            NetworkHooks.openScreen(servidor,
                    new SimpleMenuProvider((id, inventario, j) -> new RadiocaseteMenu(id, inventario, mano), radio.getHoverName()),
                    datos -> datos.writeEnum(mano));
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
        return InteractionResultHolder.sidedSuccess(radio, nivel.isClientSide());
    }

    // ---------- LOS BOTONES ----------

    /** Empieza el disco de este radiocasete desde el principio (si no tiene disco, se calla). */
    public static void tocar(ServerPlayer jugador, ItemStack radio) {
        ItemStack disco = disco(radio);
        if (!(disco.getItem() instanceof RecordItem record)) {
            parar(jugador);
            return;
        }
        SONANDO.put(jugador.getUUID(), new Estado(sesion(radio, jugador), record));
        enviar(jugador, RadiocasetePacket.tocar(jugador.getId(), record, 0));
    }

    public static void pausar(ServerPlayer jugador, ItemStack radio) {
        Estado estado = estadoDe(jugador, radio);
        if (estado != null && !estado.pausado) {
            estado.pausado = true;
            enviar(jugador, RadiocasetePacket.accion(jugador.getId(), RadiocasetePacket.PAUSAR));
        }
    }

    /** Sigue donde se pausó; si no estaba sonando (o ya había acabado), empieza el disco. */
    public static void continuar(ServerPlayer jugador, ItemStack radio) {
        Estado estado = estadoDe(jugador, radio);
        if (estado == null) {
            tocar(jugador, radio);
        } else if (estado.pausado) {
            estado.pausado = false;
            enviar(jugador, RadiocasetePacket.accion(jugador.getId(), RadiocasetePacket.CONTINUAR));
        }
    }

    public static void parar(ServerPlayer jugador) {
        if (SONANDO.remove(jugador.getUUID()) != null) {
            enviar(jugador, RadiocasetePacket.accion(jugador.getId(), RadiocasetePacket.PARAR));
        }
    }

    /** Calla el radiocasete solo si el que suena es este. */
    public static void pararSiEs(ServerPlayer jugador, ItemStack radio) {
        if (estadoDe(jugador, radio) != null) {
            parar(jugador);
        }
    }

    private static Estado estadoDe(ServerPlayer jugador, ItemStack radio) {
        Estado estado = SONANDO.get(jugador.getUUID());
        return estado != null && estado.sesion == sesion(radio, jugador) ? estado : null;
    }

    private static void enviar(ServerPlayer jugador, RadiocasetePacket paquete) {
        RolesRed.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> jugador), paquete);
    }

    // ---------- EVENTOS (se registran en ModuloRoles) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) return;
        Estado estado = SONANDO.get(jugador.getUUID());
        if (estado == null) return;
        if (!estado.pausado && ++estado.tocado >= estado.disco.getLengthInTicks()) {
            SONANDO.remove(jugador.getUUID());          // se ha acabado el disco (el sonido ya se para solo)
            return;
        }
        if (jugador.tickCount % 10 == 0 && !loLleva(jugador, estado)) {
            parar(jugador);
        }
    }

    /** ¿Sigue en su inventario el radiocasete que suena, con el mismo disco dentro? */
    private static boolean loLleva(ServerPlayer jugador, Estado estado) {
        Inventory inventario = jugador.getInventory();
        for (int i = 0; i < inventario.getContainerSize(); i++) {
            ItemStack objeto = inventario.getItem(i);
            if (objeto.getItem() instanceof RadiocaseteItem && objeto.hasTag()
                    && objeto.getTag().getLong(CLAVE_SESION) == estado.sesion) {
                return disco(objeto).is(estado.disco);
            }
        }
        // El que tiene abierto en el menú también cuenta (por si lo lleva cogido con el ratón)
        ItemStack enRaton = jugador.containerMenu.getCarried();
        return enRaton.getItem() instanceof RadiocaseteItem && enRaton.hasTag()
                && enRaton.getTag().getLong(CLAVE_SESION) == estado.sesion && disco(enRaton).is(estado.disco);
    }

    /** Alguien empieza a ver a este jugador: si su radiocasete suena, que lo oiga por donde va (no desde el principio). */
    public static void alEmpezarAVer(PlayerEvent.StartTracking evento) {
        if (!(evento.getTarget() instanceof ServerPlayer jugador) || !(evento.getEntity() instanceof ServerPlayer quien)) return;
        Estado estado = SONANDO.get(jugador.getUUID());
        if (estado == null) return;
        PacketDistributor.PacketTarget destino = PacketDistributor.PLAYER.with(() -> quien);
        RolesRed.CANAL.send(destino, RadiocasetePacket.tocar(jugador.getId(), estado.disco, estado.tocado));
        if (estado.pausado) {
            RolesRed.CANAL.send(destino, RadiocasetePacket.accion(jugador.getId(), RadiocasetePacket.PAUSAR));
        }
    }

    public static void alSalir(PlayerEvent.PlayerLoggedOutEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) parar(jugador);
    }

    public static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) parar(jugador);
    }

    public static void alMorir(net.minecraftforge.event.entity.living.LivingDeathEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) parar(jugador);
    }
}
