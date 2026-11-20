package com.lucarita.yonga.core;

import com.lucarita.yonga.YongaMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Ítems del núcleo: siempre están, actives los módulos que actives.
 * Los ítems de cada módulo están en su propio paquete (modulos/armas, modulos/sangrado...).
 */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<SoundEvent> SONIDOS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YongaMain.MOD_ID);

    public static final RegistryObject<Item> MONEDA = ITEMS.register("moneda",
            () -> new Item(new Item.Properties()));

    // Solo sirve de icono para la pestaña creativa (no aparece dentro de ella)
    public static final RegistryObject<Item> ICONO = ITEMS.register("icono_yonga",
            () -> new Item(new Item.Properties()));

    // El lingote de lucarita también es combustible: dura lo que 8 cubos de lava (funde 800 objetos)
    public static final int COMBUSTIBLE_LUCARITA = 8 * 20000;   // en ticks (un cubo de lava = 20000)

    public static final RegistryObject<Item> LUCARITA = ITEMS.register("lucarita",
            () -> new Item(new Item.Properties()) {
                @Override
                public int getBurnTime(ItemStack objeto, @Nullable RecipeType<?> tipoReceta) {
                    return COMBUSTIBLE_LUCARITA;
                }
            });

    // El material crudo
    public static final RegistryObject<Item> RAW_LUCARITA = ITEMS.register("raw_lucarita",
            () -> new Item(new Item.Properties()));

    // El ítem que representa al bloque en tu inventario
    public static final RegistryObject<Item> LUCARITA_ORE_ITEM = ITEMS.register("lucarita_ore",
            () -> new BlockItem(ModBlocks.LUCARITA_ORE.get(), new Item.Properties()));

    // ---------- DISCOS DE MÚSICA ----------
    // Suenan en el tocadiscos y en el radiocasete; los sueltan los creepers que mata un esqueleto y salen en cofres
    // (etiquetas de minecraft y DiscosEnCofres). Para cada uno: el .ogg (MONO) en assets/yonga/sounds, su entrada en
    // sounds.json, el título en el idioma (".desc") y el dibujo (herramientas/dibujar_discos.py).
    // La duración va en ticks: segundos x 20.

    public static final RegistryObject<Item> DISCO_MAMADA_SOBRE_MAMADA_V1 = disco("disco_mamada_sobre_mamada_v1", 4174);   // 3:28
    public static final RegistryObject<Item> DISCO_MAMADA_SOBRE_MAMADA = disco("disco_mamada_sobre_mamada", 4421);         // 3:41
    public static final RegistryObject<Item> DISCO_MAMADA_SOBRE_MAMADA_V3 = disco("disco_mamada_sobre_mamada_v3", 4010);   // 3:20
    public static final RegistryObject<Item> DISCO_MEDIA_VITA = disco("disco_media_vita", 3520);                           // 2:56
    public static final RegistryObject<Item> DISCO_BAKA_MITAI = disco("disco_baka_mitai", 5826);                           // 4:51
    public static final RegistryObject<Item> DISCO_CABALLO_HOMOSEXUAL = disco("disco_caballo_homosexual", 6250);           // 5:12
    public static final RegistryObject<Item> DISCO_PAPA_PALPA = disco("disco_papa_palpa", 2887);                           // 2:24
    public static final RegistryObject<Item> DISCO_CALL_ME_MAYBE = disco("disco_call_me_maybe", 3986);                     // 3:19
    public static final RegistryObject<Item> DISCO_CARAMELLDANSEN = disco("disco_caramelldansen", 3504);                   // 2:55
    public static final RegistryObject<Item> DISCO_VIRTUAL_INSANITY = disco("disco_virtual_insanity", 6823);        // 5:41
    public static final RegistryObject<Item> DISCO_PIPO_PIPO = disco("disco_pipo_pipo", 5300);                      // 4:24
    public static final RegistryObject<Item> DISCO_SEXBOMB_RAYMAN = disco("disco_sexbomb_rayman", 3610);            // 3:00
    public static final RegistryObject<Item> DISCO_ITS_RAINING_MEN = disco("disco_its_raining_men", 5883);          // 4:54
    public static final RegistryObject<Item> DISCO_PITITO_CON_EL_CIERRE = disco("disco_pitito_con_el_cierre", 4085); // 3:24
    public static final RegistryObject<Item> DISCO_SOMEWHERE_ON_KAMINO = disco("disco_somewhere_on_kamino", 4616);  // 3:50
    public static final RegistryObject<Item> DISCO_PUTON_DEL_BARRIO = disco("disco_puton_del_barrio", 2974);        // 2:28
    public static final RegistryObject<Item> DISCO_FLYDAY_CHINATOWN = disco("disco_flyday_chinatown", 4181);        // 3:29
    public static final RegistryObject<Item> DISCO_STAY_WITH_ME = disco("disco_stay_with_me", 6236);                // 5:11
    public static final RegistryObject<Item> DISCO_DRUNKEN_SAILOR = disco("disco_drunken_sailor", 3241);            // 2:42
    public static final RegistryObject<Item> DISCO_CHUM_BUCKET_RHUMBA = disco("disco_chum_bucket_rhumba", 2177);    // 1:48

    /** Registra el sonido y el disco con el mismo nombre. */
    private static RegistryObject<Item> disco(String nombre, int ticks) {
        RegistryObject<SoundEvent> sonido = SONIDOS.register(nombre,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YongaMain.MOD_ID, nombre)));
        return ITEMS.register(nombre,
                () -> new RecordItem(15, sonido, new Item.Properties().stacksTo(1).rarity(Rarity.RARE), ticks));
    }

    // ---------- MODERACIÓN ----------

    // Guante del moderador: solo en creativo (sin receta). Click derecho a un jugador: abre su inventario como un cofre
    public static final RegistryObject<Item> GUANTE_MODERADOR = ITEMS.register("guante_moderador",
            () -> new com.lucarita.yonga.core.moderacion.GuanteModeradorItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        SONIDOS.register(eventBus);
    }
}
