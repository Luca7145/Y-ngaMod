package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.roles.red.EscanerPacket;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ESCÁNER (Ingeniero, Experto). Una tablet con sonar para encontrar minerales, de forma orientativa. Se lleva en la
 * mano y se mira como el mapa de Minecraft: con las dos manos si la otra está libre, y al mirar hacia abajo se acerca
 * (lo dibuja client/roles/EscanerCliente desde mixin/ItemInHandRendererMixin).
 *
 *  - FUNCIONA CON EL MINERAL QUE BUSCA: con el mineral en la otra mano, click derecho: se gasta uno y el escáner
 *    busca ese mineral durante 10 minutos (encendido y en la mano). Más del mismo suma otros 10 minutos (hasta una
 *    hora); uno distinto cambia lo que busca y empieza de nuevo con 10 minutos. Sirve el lingote, la gema, el mineral
 *    en bruto o la propia mena, de cualquier mod que use las etiquetas normales de Forge (forge:ores/...; por ejemplo
 *    el zinc de Create). Cuando se le acaba, se apaga.
 *  - Click derecho sin mineral en la otra mano: encender / apagar.
 *  - SONAR: encendido y en la mano, cada 3 segundos barre 32 bloques alrededor (24 arriba y abajo) y manda al jugador
 *    las vetas que encuentra, pero solo aproximadas: agrupadas en zonas de 4 bloques, movidas un poco al azar (el
 *    error cambia cada minuto) y como mucho las 12 más cercanas. En la tablet salen como puntos rojos, girando contigo.
 *    Así sabes hacia dónde cavar, pero no el bloque exacto ni a qué altura.
 */
public class EscanerItem extends Item {

    public static final int RADIO = 32;
    public static final int ALTO = 24;
    private static final int CADA = 60;                     // ticks entre barridos
    private static final int MAX_MANCHAS = 12;
    private static final int CELDA = 4;                     // tamaño de las zonas en que se agrupa lo encontrado
    private static final int ERROR = 3;                     // bloques que se puede mover cada mancha

    public static final int POR_MUESTRA = 10 * 60 * 20;     // cada mineral, 10 minutos encendido
    public static final int MAX_ENERGIA = 6 * POR_MUESTRA;  // como mucho una hora

    private static final String CLAVE_ENERGIA = "Energia";
    private static final String CLAVE_ENCENDIDO = "Encendido";
    private static final String CLAVE_BUSCA = "Busca";       // la etiqueta de bloques que busca
    private static final String CLAVE_MUESTRA = "Muestra";   // el objeto que se le metió (se dibuja en la tablet)

    public EscanerItem(Properties propiedades) {
        super(propiedades.stacksTo(1));
    }

    // ---------- DATOS DE LA TABLET ----------

    public static int energia(ItemStack escaner) {
        return escaner.hasTag() ? escaner.getTag().getInt(CLAVE_ENERGIA) : 0;
    }

    public static void ponerEnergia(ItemStack escaner, int energia) {
        escaner.getOrCreateTag().putInt(CLAVE_ENERGIA, Math.max(0, Math.min(MAX_ENERGIA, energia)));
    }

    public static boolean encendido(ItemStack escaner) {
        return escaner.hasTag() && escaner.getTag().getBoolean(CLAVE_ENCENDIDO);
    }

    public static void ponerEncendido(ItemStack escaner, boolean encendido) {
        escaner.getOrCreateTag().putBoolean(CLAVE_ENCENDIDO, encendido);
    }

    /** El objeto que se le metió de muestra (vacío si no busca nada). */
    public static ItemStack muestra(ItemStack escaner) {
        if (!escaner.hasTag() || !escaner.getTag().contains(CLAVE_MUESTRA)) return ItemStack.EMPTY;
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(escaner.getTag().getString(CLAVE_MUESTRA)));
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    @Nullable
    private static TagKey<Block> busca(ItemStack escaner) {
        if (!escaner.hasTag() || !escaner.getTag().contains(CLAVE_BUSCA)) return null;
        return TagKey.create(Registries.BLOCK, new ResourceLocation(escaner.getTag().getString(CLAVE_BUSCA)));
    }

    /**
     * Qué menas se buscan con este objeto: la etiqueta forge:ores/<material>. El material sale de las etiquetas del
     * objeto (forge:ingots/, gems/, raw_materials/, dusts/, nuggets/) o, si es una mena, de las del bloque.
     */
    @Nullable
    public static TagKey<Block> menasDe(ItemStack objeto) {
        if (objeto.isEmpty()) return null;
        if (objeto.is(Items.COAL)) return menas("coal");
        if (objeto.is(Items.NETHERITE_SCRAP) || objeto.is(Items.ANCIENT_DEBRIS)) return menas("netherite_scrap");
        List<String> materiales = new ArrayList<>();
        objeto.getTags().forEach(etiqueta -> materialDe(etiqueta.location(), materiales));
        if (objeto.getItem() instanceof BlockItem bloque) {
            bloque.getBlock().defaultBlockState().getTags().forEach(etiqueta -> materialDe(etiqueta.location(), materiales));
        }
        for (String material : materiales) {
            TagKey<Block> menas = menas(material);
            if (!ForgeRegistries.BLOCKS.tags().getTag(menas).isEmpty()) {
                return menas;
            }
        }
        return null;
    }

    private static void materialDe(ResourceLocation etiqueta, List<String> materiales) {
        if (!etiqueta.getNamespace().equals("forge")) return;
        String ruta = etiqueta.getPath();
        for (String tipo : new String[]{"ores/", "ingots/", "gems/", "raw_materials/", "dusts/", "nuggets/", "storage_blocks/"}) {
            if (ruta.startsWith(tipo) && ruta.length() > tipo.length() && !ruta.substring(tipo.length()).contains("/")) {
                materiales.add(ruta.substring(tipo.length()));
            }
        }
    }

    private static TagKey<Block> menas(String material) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores/" + material));
    }

    // ---------- USO ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level nivel, Player jugador, InteractionHand mano) {
        ItemStack escaner = jugador.getItemInHand(mano);
        if (nivel.isClientSide()) {
            return InteractionResultHolder.success(escaner);
        }
        ItemStack otra = jugador.getItemInHand(mano == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        TagKey<Block> menas = menasDe(otra);
        if (menas != null) {
            // Cargar con el mineral de la otra mano
            boolean mismo = menas.equals(busca(escaner));
            if (mismo && energia(escaner) + POR_MUESTRA > MAX_ENERGIA) {
                nivel.playSound(null, jugador.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.4f);
                return InteractionResultHolder.fail(escaner);
            }
            CompoundTag nbt = escaner.getOrCreateTag();
            nbt.putString(CLAVE_BUSCA, menas.location().toString());
            nbt.putString(CLAVE_MUESTRA, ForgeRegistries.ITEMS.getKey(otra.getItem()).toString());
            ponerEnergia(escaner, mismo ? energia(escaner) + POR_MUESTRA : POR_MUESTRA);
            ponerEncendido(escaner, true);
            if (!jugador.getAbilities().instabuild) otra.shrink(1);
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.4f);
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 1.8f);
            return InteractionResultHolder.consume(escaner);
        }
        // Encender / apagar
        if (energia(escaner) <= 0 || busca(escaner) == null) {
            nivel.playSound(null, jugador.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.5f, 1.4f);
            return InteractionResultHolder.fail(escaner);
        }
        boolean ahora = !encendido(escaner);
        ponerEncendido(escaner, ahora);
        nivel.playSound(null, jugador.blockPosition(), ahora ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.4f, 1.8f);
        if (!ahora && jugador instanceof ServerPlayer servidor) {
            RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> servidor), new EscanerPacket(List.of()));
        }
        return InteractionResultHolder.consume(escaner);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack antes, ItemStack despues, boolean cambioDeHueco) {
        return cambioDeHueco || !ItemStack.isSameItem(antes, despues);
    }

    @Override
    public boolean isBarVisible(ItemStack escaner) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack escaner) {
        return Math.round(13.0f * energia(escaner) / MAX_ENERGIA);
    }

    @Override
    public int getBarColor(ItemStack escaner) {
        return 0xD02020;
    }

    // ---------- EL BARRIDO (servidor) ----------

    public static void alTick(TickEvent.PlayerTickEvent evento) {
        if (evento.phase != TickEvent.Phase.END || !(evento.player instanceof ServerPlayer jugador)) return;
        ItemStack escaner = enLaMano(jugador);
        if (escaner.isEmpty() || !encendido(escaner)) return;
        int energia = energia(escaner);
        if (energia <= 0) {
            ponerEncendido(escaner, false);
            jugador.level().playSound(null, jugador.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.5f, 1.6f);
            RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), new EscanerPacket(List.of()));
            return;
        }
        if (jugador.tickCount % 20 == 0) {
            ponerEnergia(escaner, energia - 20);                  // se descuenta de segundo en segundo
        }
        TagKey<Block> menas = busca(escaner);
        if (menas == null || jugador.tickCount % CADA != 0) return;
        List<EscanerPacket.Mancha> manchas = barrer((ServerLevel) jugador.level(), jugador, menas);
        RolesRed.CANAL.send(PacketDistributor.PLAYER.with(() -> jugador), new EscanerPacket(manchas));
    }

    /** El escáner que lleva en la mano (primero la principal), o vacío. */
    public static ItemStack enLaMano(Player jugador) {
        if (jugador.getMainHandItem().getItem() instanceof EscanerItem) return jugador.getMainHandItem();
        if (jugador.getOffhandItem().getItem() instanceof EscanerItem) return jugador.getOffhandItem();
        return ItemStack.EMPTY;
    }

    private static List<EscanerPacket.Mancha> barrer(ServerLevel nivel, ServerPlayer jugador, TagKey<Block> menas) {
        BlockPos centro = jugador.blockPosition();
        int minY = Math.max(nivel.getMinBuildHeight(), centro.getY() - ALTO);
        int maxY = Math.min(nivel.getMaxBuildHeight() - 1, centro.getY() + ALTO);
        Map<Long, int[]> zonas = new HashMap<>();             // zona -> suma x, y, z y cuántos

        for (int cx = SectionPos.blockToSectionCoord(centro.getX() - RADIO); cx <= SectionPos.blockToSectionCoord(centro.getX() + RADIO); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(centro.getZ() - RADIO); cz <= SectionPos.blockToSectionCoord(centro.getZ() + RADIO); cz++) {
                if (!nivel.hasChunk(cx, cz)) continue;
                LevelChunk chunk = nivel.getChunk(cx, cz);
                for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
                    LevelChunkSection seccion = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (seccion.hasOnlyAir() || !seccion.maybeHas(e -> e.is(menas))) continue;
                    for (int y = 0; y < 16; y++) {
                        int by = (sy << 4) + y;
                        if (by < minY || by > maxY) continue;
                        for (int z = 0; z < 16; z++) {
                            int bz = (cz << 4) + z;
                            for (int x = 0; x < 16; x++) {
                                int bx = (cx << 4) + x;
                                int dx = bx - centro.getX(), dz = bz - centro.getZ();
                                if (dx * dx + dz * dz > RADIO * RADIO) continue;
                                BlockState estado = seccion.getBlockState(x, y, z);
                                if (!estado.is(menas)) continue;
                                long zona = BlockPos.asLong(Math.floorDiv(bx, CELDA), Math.floorDiv(by, CELDA), Math.floorDiv(bz, CELDA));
                                int[] suma = zonas.computeIfAbsent(zona, k -> new int[4]);
                                suma[0] += bx;
                                suma[1] += by;
                                suma[2] += bz;
                                suma[3]++;
                            }
                        }
                    }
                }
            }
        }

        // Cada zona, una mancha: en su centro, movida un poco (el mismo error durante un minuto)
        long minuto = nivel.getGameTime() / 1200;
        List<EscanerPacket.Mancha> manchas = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : zonas.entrySet()) {
            int[] s = e.getValue();
            Random azar = new Random(e.getKey() * 31 + minuto * 7919 + jugador.getUUID().getLeastSignificantBits());
            int x = Math.round((float) s[0] / s[3]) + azar.nextInt(2 * ERROR + 1) - ERROR;
            int y = Math.round((float) s[1] / s[3]) + azar.nextInt(3) - 1;
            int z = Math.round((float) s[2] / s[3]) + azar.nextInt(2 * ERROR + 1) - ERROR;
            manchas.add(new EscanerPacket.Mancha(x, y, z, Math.min(s[3], 255)));
        }
        manchas.sort(Comparator.comparingDouble(m -> centro.distSqr(new BlockPos(m.x(), m.y(), m.z()))));
        return manchas.size() > MAX_MANCHAS ? new ArrayList<>(manchas.subList(0, MAX_MANCHAS)) : manchas;
    }
}
