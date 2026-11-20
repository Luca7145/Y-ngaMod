package com.lucarita.yonga.modulos.roles;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import com.lucarita.yonga.modulos.roles.cara.Medallon;
import com.lucarita.yonga.modulos.roles.cara.MedallonItem;
import com.lucarita.yonga.modulos.roles.oficios.BajoHachaItem;
import com.lucarita.yonga.modulos.roles.oficios.EspadaKeytarItem;
import com.lucarita.yonga.modulos.roles.oficios.PicoGuitarraItem;
import com.lucarita.yonga.modulos.roles.oficios.BanjoPalaItem;
import com.lucarita.yonga.modulos.roles.oficios.EscudoTamborItem;
import com.lucarita.yonga.modulos.roles.oficios.ArcoLiraItem;
import com.lucarita.yonga.modulos.roles.oficios.Armonica;
import com.lucarita.yonga.modulos.roles.oficios.ArmonicaItem;
import com.lucarita.yonga.modulos.roles.oficios.Fantasma;
import com.lucarita.yonga.modulos.roles.oficios.Herrero;
import com.lucarita.yonga.modulos.roles.oficios.TaladroItem;
import com.lucarita.yonga.modulos.roles.oficios.TaladroMenu;
import com.lucarita.yonga.modulos.roles.oficios.BotafumeiroItem;
import com.lucarita.yonga.modulos.roles.oficios.IncensarioEntity;
import com.lucarita.yonga.modulos.roles.oficios.FlautaItem;
import com.lucarita.yonga.modulos.roles.oficios.Companeros;
import com.lucarita.yonga.modulos.roles.oficios.ArmonicaItem;
import com.lucarita.yonga.modulos.roles.oficios.Fantasma;
import com.lucarita.yonga.modulos.roles.oficios.Herrero;
import com.lucarita.yonga.modulos.roles.oficios.BotafumeiroItem;
import com.lucarita.yonga.modulos.roles.oficios.IncensarioEntity;
import com.lucarita.yonga.modulos.roles.oficios.FlautaItem;
import com.lucarita.yonga.modulos.roles.oficios.Companeros;
import com.lucarita.yonga.modulos.roles.oficios.Musica;
import com.lucarita.yonga.modulos.roles.oficios.NotaProyectil;
import com.lucarita.yonga.modulos.roles.oficios.Explorador;
import com.lucarita.yonga.modulos.roles.oficios.ParapenteItem;
import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaItem;
import com.lucarita.yonga.modulos.roles.oficios.mochila.MochilaMenu;
import net.minecraft.world.inventory.MenuType;
import com.lucarita.yonga.modulos.roles.red.RolesRed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.crafting.RecipeType;
import com.lucarita.yonga.modulos.roles.construccion.RecetaBanco;
import com.lucarita.yonga.modulos.roles.construccion.Construccion;
import com.lucarita.yonga.modulos.roles.construccion.BancoMenu;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ArmorItem;
import com.lucarita.yonga.core.ModTiers;
import com.lucarita.yonga.core.ModArmaduras;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.PushReaction;
import com.lucarita.yonga.modulos.roles.oficios.SacoDormirBlock;
import com.lucarita.yonga.modulos.roles.oficios.Sagrado;
import com.lucarita.yonga.modulos.roles.oficios.IncensarioSueloBlock;
import com.lucarita.yonga.modulos.roles.oficios.GarfioItem;
import com.lucarita.yonga.modulos.roles.oficios.BaculoItem;
import com.lucarita.yonga.modulos.roles.oficios.BotasMuelles;
import com.lucarita.yonga.modulos.roles.oficios.EscanerItem;
import com.lucarita.yonga.modulos.roles.oficios.FocoBlock;
import com.lucarita.yonga.modulos.roles.oficios.ImanItem;
import com.lucarita.yonga.modulos.roles.oficios.TrampaPinchosBlock;
import com.lucarita.yonga.modulos.roles.oficios.RadiocaseteItem;
import com.lucarita.yonga.modulos.roles.oficios.RadiocaseteMenu;
import com.lucarita.yonga.modulos.roles.oficios.GarfioEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Módulo de roles (oficios): Herrero, Explorador, Sacerdote, Ingeniero, Melómano...
 * Cada jugador tiene un oficio y un rango dentro de él (Novato -> Maestro).
 * Solo se registra si el módulo está activado (ver YongaMain).
 * La parte visual (menú de oficios, botón del inventario) está en el paquete client/roles.
 */
public final class ModuloRoles {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECETAS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, YongaMain.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TIPOS_RECETA =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<SoundEvent> SONIDOS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, YongaMain.MOD_ID);
    public static final DeferredRegister<Block> BLOQUES =
            DeferredRegister.create(ForgeRegistries.BLOCKS, YongaMain.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, YongaMain.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTIDADES_BLOQUE =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, YongaMain.MOD_ID);

    // ---------- OBJETOS DE OFICIO ----------

    // Explorador (se fabrica desde Oficial, ver data/yonga/recipes/parapente.json)
    public static final RegistryObject<Item> PARAPENTE = ITEMS.register("parapente",
            () -> new ParapenteItem(new Item.Properties()));

    // Explorador: mochila (Aprendiz) y mochila mejorada (Experto). (huecos, máximo por hueco)
    public static final RegistryObject<Item> MOCHILA = ITEMS.register("mochila",
            () -> new MochilaItem(18, 100, new Item.Properties()));
    public static final RegistryObject<Item> MOCHILA_MEJORADA = ITEMS.register("mochila_mejorada",
            () -> new MochilaItem(27, 200, new Item.Properties()));

    // Sacerdote: la túnica sagrada (Oficial). 0 de armadura, pero quita daño de los mobs de fe (ver Sagrado)
    public static final RegistryObject<Item> CAPUCHA_SAGRADA = ITEMS.register("capucha_sagrada",
            () -> new Sagrado.PiezaTunica(ModArmaduras.TUNICA, ArmorItem.Type.HELMET, 0.18f, new Item.Properties()));
    public static final RegistryObject<Item> TUNICA_SAGRADA = ITEMS.register("tunica_sagrada",
            () -> new Sagrado.PiezaTunica(ModArmaduras.TUNICA, ArmorItem.Type.CHESTPLATE, 0.27f, new Item.Properties()));
    public static final RegistryObject<Item> FALDON_SAGRADO = ITEMS.register("faldon_sagrado",
            () -> new Sagrado.PiezaTunica(ModArmaduras.TUNICA, ArmorItem.Type.LEGGINGS, 0.23f, new Item.Properties()));
    public static final RegistryObject<Item> SANDALIAS_SAGRADAS = ITEMS.register("sandalias_sagradas",
            () -> new Sagrado.PiezaTunica(ModArmaduras.TUNICA, ArmorItem.Type.BOOTS, 0.17f, new Item.Properties()));
    // Sacerdote: halo y cuernos (Experto). Van en la cara.
    public static final RegistryObject<Item> HALO = ITEMS.register("halo",
            () -> new Sagrado.Distintivo("yonga.sagrado.halo", new Item.Properties()));
    public static final RegistryObject<Item> CUERNOS = ITEMS.register("cuernos",
            () -> new Sagrado.Distintivo("yonga.sagrado.cuernos", new Item.Properties()));

    // Sacerdote: rosario (Novato). Va en la cara; rezas y blasfemas más rápido (ver Sacerdote)
    public static final RegistryObject<Item> ROSARIO = ITEMS.register("rosario",
            () -> new Item(new Item.Properties().stacksTo(1)));
    // Sacerdote: báculo (Oficial). Pega poco, pero cada golpe cargado gasta maná y pega mucho más
    public static final RegistryObject<Item> BACULO = ITEMS.register("baculo",
            () -> new BaculoItem(new Item.Properties()));

    // Explorador: saco de dormir (Aprendiz). Como una cama, más cómodo, y no guarda el punto de aparición.
    public static final RegistryObject<Block> SACO_DORMIR = BLOQUES.register("saco_dormir",
            () -> new SacoDormirBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).sound(SoundType.WOOL)
                    .strength(0.2f).noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Item> SACO_DORMIR_ITEM = ITEMS.register("saco_dormir",
            () -> new BedItem(SACO_DORMIR.get(), new Item.Properties().stacksTo(1)));

    // Sacerdote: incensarios de suelo (Aprendiz). Encendidos alejan a los mobs de fe del otro lado (ver IncensarioSueloBlock)
    public static final RegistryObject<Block> INCENSARIO_LUMINOSO = BLOQUES.register("incensario_luminoso",
            () -> new IncensarioSueloBlock(false, propiedadesIncensario(MapColor.GOLD)));
    public static final RegistryObject<Block> INCENSARIO_OSCURO = BLOQUES.register("incensario_oscuro",
            () -> new IncensarioSueloBlock(true, propiedadesIncensario(MapColor.COLOR_BLACK)));
    public static final RegistryObject<Item> INCENSARIO_LUMINOSO_ITEM = ITEMS.register("incensario_luminoso",
            () -> new BlockItem(INCENSARIO_LUMINOSO.get(), new Item.Properties()));
    public static final RegistryObject<Item> INCENSARIO_OSCURO_ITEM = ITEMS.register("incensario_oscuro",
            () -> new BlockItem(INCENSARIO_OSCURO.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<IncensarioSueloBlock.Entidad>> ENTIDAD_INCENSARIO_SUELO =
            ENTIDADES_BLOQUE.register("incensario_suelo", () -> BlockEntityType.Builder.of(IncensarioSueloBlock.Entidad::new,
                    INCENSARIO_LUMINOSO.get(), INCENSARIO_OSCURO.get()).build(null));

    private static BlockBehaviour.Properties propiedadesIncensario(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).sound(SoundType.LANTERN).strength(2.0f, 6.0f)
                .requiresCorrectToolForDrops().noOcclusion().pushReaction(PushReaction.DESTROY)
                .lightLevel(estado -> estado.getValue(IncensarioSueloBlock.ENCENDIDO) ? 10 : 0);
    }

    // Explorador: garfio (Maestro). Lanza un gancho que tira de ti hasta el bloque (ver GarfioEntity)
    public static final RegistryObject<Item> GARFIO = ITEMS.register("garfio",
            () -> new GarfioItem(new Item.Properties()));

    // Explorador: antifaz (Novato). Va en el hueco de cara; los mobs te detectan a la mitad de distancia.
    public static final RegistryObject<Item> ANTIFAZ = ITEMS.register("antifaz",
            () -> new Item(new Item.Properties().stacksTo(1)));

    // Explorador: medallones, se llevan en el hueco de cara y se ven en el pecho
    public static final RegistryObject<Item> MEDALLON_AGUA = ITEMS.register("medallon_agua",
            () -> new MedallonItem(Medallon.AGUA, new Item.Properties()));       // Novato
    public static final RegistryObject<Item> MEDALLON_TIERRA = ITEMS.register("medallon_tierra",
            () -> new MedallonItem(Medallon.TIERRA, new Item.Properties()));     // Aprendiz
    public static final RegistryObject<Item> MEDALLON_FUEGO = ITEMS.register("medallon_fuego",
            () -> new MedallonItem(Medallon.FUEGO, new Item.Properties()));      // Oficial
    public static final RegistryObject<Item> MEDALLON_AIRE = ITEMS.register("medallon_aire",
            () -> new MedallonItem(Medallon.AIRE, new Item.Properties()));       // Experto

    // Melómano: bajo-hacha
    public static final RegistryObject<Item> BAJO_HACHA = ITEMS.register("bajo_hacha",
            () -> new BajoHachaItem(new Item.Properties()));

    // Melómano: espada-keytar (dispara notas)
    public static final RegistryObject<Item> ESPADA_KEYTAR = ITEMS.register("espada_keytar",
            () -> new EspadaKeytarItem(new Item.Properties()));

    // Melómano: pico-guitarra (picar recarga el ritmo; con la barra llena pica todo alrededor)
    public static final RegistryObject<Item> PICO_GUITARRA = ITEMS.register("pico_guitarra",
            () -> new PicoGuitarraItem(new Item.Properties()));

    // Melómano: banjo-pala (cavar recarga el ritmo; con la barra llena cava lo blando a 2 bloques)
    public static final RegistryObject<Item> BANJO_PALA = ITEMS.register("banjo_pala",
            () -> new BanjoPalaItem(new Item.Properties()));

    // Melómano: escudo-tambor (para golpes; al parar 3, redoble que aturde)
    public static final RegistryObject<Item> ESCUDO_TAMBOR = ITEMS.register("escudo_tambor",
            () -> new EscudoTamborItem(new Item.Properties()));

    // Melómano: arco-lira (cada flecha es una nota; la que llena el ritmo es una flecha sónica)
    public static final RegistryObject<Item> ARCO_LIRA = ITEMS.register("arco_lira",
            () -> new ArcoLiraItem(new Item.Properties()));

    // Melómano: radiocasete (Aprendiz). Un tocadiscos de bolsillo que oyen los de alrededor
    public static final RegistryObject<Item> RADIOCASETE = ITEMS.register("radiocasete",
            () -> new RadiocaseteItem(new Item.Properties()));

    // Melómano: armónica. Va en el hueco de cara y se ve en la boca (ver Armonica: tecla H, blues y lamento)
    public static final RegistryObject<Item> ARMONICA = ITEMS.register("armonica",
            () -> new ArmonicaItem(new Item.Properties()));

    // ---------- HERRERO: armadura y herramientas de lucarita (un poco mejores que las de netherita) ----------

    public static final RegistryObject<Item> CASCO_LUCARITA = ITEMS.register("casco_lucarita",
            () -> new ArmorItem(ModArmaduras.LUCARITA, ArmorItem.Type.HELMET, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> PETO_LUCARITA = ITEMS.register("peto_lucarita",
            () -> new ArmorItem(ModArmaduras.LUCARITA, ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> PANTALONES_LUCARITA = ITEMS.register("pantalones_lucarita",
            () -> new ArmorItem(ModArmaduras.LUCARITA, ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> BOTAS_LUCARITA = ITEMS.register("botas_lucarita",
            () -> new ArmorItem(ModArmaduras.LUCARITA, ArmorItem.Type.BOOTS, new Item.Properties().fireResistant()));
    // Pico: 6 de daño (como el de netherita); pala: 6,5. Más rápidos y duraderos (ver ModTiers)
    public static final RegistryObject<Item> PICO_LUCARITA = ITEMS.register("pico_lucarita",
            () -> new PickaxeItem(ModTiers.LUCARITA, 1, -2.8f, new Item.Properties().fireResistant()));
    public static final RegistryObject<Item> PALA_LUCARITA = ITEMS.register("pala_lucarita",
            () -> new ShovelItem(ModTiers.LUCARITA, 1.5f, -3.0f, new Item.Properties().fireResistant()));

    // ---------- INGENIERO ----------

    // ---------- INGENIERO: imán (Novato), foco (Aprendiz), botas de muelles y trampa de pinchos (Oficial), escáner (Experto)

    public static final RegistryObject<Item> IMAN = ITEMS.register("iman", () -> new ImanItem(new Item.Properties()));

    public static final RegistryObject<Block> FOCO = BLOQUES.register("foco",
            () -> new FocoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.LANTERN)
                    .strength(2.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()   // no es un cubo entero: que se vea lo de al lado
                    .lightLevel(estado -> estado.getValue(FocoBlock.ENCENDIDO) ? 15 : 0)));
    public static final RegistryObject<Item> FOCO_ITEM = ITEMS.register("foco",
            () -> new BlockItem(FOCO.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<FocoBlock.Entidad>> ENTIDAD_FOCO = ENTIDADES_BLOQUE.register("foco",
            () -> BlockEntityType.Builder.of(FocoBlock.Entidad::new, FOCO.get()).build(null));

    public static final RegistryObject<Item> BOTAS_MUELLES = ITEMS.register("botas_muelles",
            () -> new BotasMuelles(new Item.Properties()));

    public static final RegistryObject<Block> TRAMPA_PINCHOS = BLOQUES.register("trampa_pinchos",
            () -> new TrampaPinchosBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL)
                    .strength(3.0f, 6.0f).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> TRAMPA_PINCHOS_ITEM = ITEMS.register("trampa_pinchos",
            () -> new BlockItem(TRAMPA_PINCHOS.get(), new Item.Properties()));

    public static final RegistryObject<Item> ESCANER = ITEMS.register("escaner", () -> new EscanerItem(new Item.Properties()));

    // Taladro: pico a motor con combustible (3x3)
    public static final RegistryObject<Item> TALADRO = ITEMS.register("taladro",
            () -> new TaladroItem(new Item.Properties().fireResistant()));

    // Sacerdote: botafumeiro (mangual con incensario que se lanza unido por la cadena)
    public static final RegistryObject<Item> BOTAFUMEIRO = ITEMS.register("botafumeiro",
            () -> new BotafumeiroItem(new Item.Properties()));

    // Melómano: flauta (doma un mob como compañero; ver Companeros)
    public static final RegistryObject<Item> FLAUTA = ITEMS.register("flauta",
            () -> new FlautaItem(new Item.Properties()));

    // ---------- ENTIDADES ----------

    // La nota que dispara la espada-keytar
    public static final RegistryObject<EntityType<NotaProyectil>> NOTA_PROYECTIL = ENTIDADES.register("nota_proyectil",
            () -> EntityType.Builder.<NotaProyectil>of(NotaProyectil::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("nota_proyectil"));

    // El incensario del botafumeiro cuando está lanzado
    public static final RegistryObject<EntityType<IncensarioEntity>> INCENSARIO = ENTIDADES.register("incensario",
            () -> EntityType.Builder.<IncensarioEntity>of(IncensarioEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(2)
                    .build("incensario"));

    // El gancho del garfio cuando está lanzado
    public static final RegistryObject<EntityType<GarfioEntity>> GARFIO_ENTIDAD = ENTIDADES.register("garfio",
            () -> EntityType.Builder.<GarfioEntity>of(GarfioEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1)
                    .build("garfio"));

    // ---------- SONIDOS (archivos en assets/yonga/sounds, nombres en assets/yonga/sounds.json) ----------

    public static final RegistryObject<SoundEvent> SONIDO_BAJO_NOTA = sonido("bajo_nota");
    public static final RegistryObject<SoundEvent> SONIDO_BAJO_ACORDE = sonido("bajo_acorde");
    public static final RegistryObject<SoundEvent> SONIDO_KEYTAR_NOTA = sonido("keytar_nota");
    public static final RegistryObject<SoundEvent> SONIDO_GUITARRA_NOTA = sonido("guitarra_nota");
    public static final RegistryObject<SoundEvent> SONIDO_GUITARRA_ACORDE = sonido("guitarra_acorde");
    public static final RegistryObject<SoundEvent> SONIDO_BANJO_NOTA = sonido("banjo_nota");
    public static final RegistryObject<SoundEvent> SONIDO_BANJO_PUNTEO = sonido("banjo_punteo");
    public static final RegistryObject<SoundEvent> SONIDO_TAMBOR_GOLPE = sonido("tambor_golpe");
    public static final RegistryObject<SoundEvent> SONIDO_TAMBOR_REDOBLE = sonido("tambor_redoble");
    public static final RegistryObject<SoundEvent> SONIDO_LIRA_NOTA = sonido("lira_nota");
    public static final RegistryObject<SoundEvent> SONIDO_LIRA_SONICA = sonido("lira_sonica");
    public static final RegistryObject<SoundEvent> SONIDO_ARMONICA_NOTA = sonido("armonica_nota");
    public static final RegistryObject<SoundEvent> SONIDO_ARMONICA_QUEJIDO = sonido("armonica_quejido");
    public static final RegistryObject<SoundEvent> SONIDO_ARMONICA_LAMENTO = sonido("armonica_lamento");
    public static final RegistryObject<SoundEvent> SONIDO_FLAUTA_NOTA = sonido("flauta_nota");
    public static final RegistryObject<SoundEvent> SONIDO_FLAUTA_ENCANTO = sonido("flauta_encanto");

    private static RegistryObject<SoundEvent> sonido(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(YongaMain.MOD_ID, nombre)));
    }

    // ---------- MENÚS ----------

    public static final RegistryObject<MenuType<MochilaMenu>> MENU_MOCHILA = MENUS.register("mochila",
            () -> IForgeMenuType.create(MochilaMenu::desdeRed));

    // ---------- BANCO DE INGENIERO (bloques de construcción: ver construccion/Construccion) ----------
    public static final RegistryObject<Block> BANCO_INGENIERO = Construccion.BANCO_INGENIERO;
    public static final RegistryObject<Item> BANCO_INGENIERO_ITEM = RegistryObject.create(
            new ResourceLocation(YongaMain.MOD_ID, "banco_ingeniero"), ForgeRegistries.ITEMS);
    public static final RegistryObject<RecipeType<RecetaBanco>> TIPO_BANCO = TIPOS_RECETA.register("banco_ingeniero",
            () -> RecipeType.simple(new ResourceLocation(YongaMain.MOD_ID, "banco_ingeniero")));
    public static final RegistryObject<RecipeSerializer<RecetaBanco>> RECETA_BANCO = RECETAS.register("banco_ingeniero",
            RecetaBanco.Serializer::new);
    public static final RegistryObject<MenuType<BancoMenu>> MENU_BANCO = MENUS.register("banco_ingeniero",
            () -> IForgeMenuType.create((id, inventario, datos) -> new BancoMenu(id, inventario)));

    // Hueco del disco del radiocasete
    public static final RegistryObject<MenuType<RadiocaseteMenu>> MENU_RADIOCASETE = MENUS.register("radiocasete",
            () -> IForgeMenuType.create(RadiocaseteMenu::desdeRed));

    // Depósito de combustible del taladro
    public static final RegistryObject<MenuType<TaladroMenu>> MENU_TALADRO = MENUS.register("taladro",
            () -> IForgeMenuType.create(TaladroMenu::desdeRed));

    // ---------- RECETAS ----------

    // "yonga:receta_oficio": receta con forma que solo puede usar quien tenga cierto oficio y rango
    public static final RegistryObject<RecipeSerializer<RecetaOficio>> RECETA_OFICIO =
            RECETAS.register("receta_oficio", RecetaOficio.Serializer::new);

    private ModuloRoles() {}

    public static void registrar(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        BLOQUES.register(modEventBus);
        RECETAS.register(modEventBus);
        MENUS.register(modEventBus);
        SONIDOS.register(modEventBus);
        ENTIDADES.register(modEventBus);
        ENTIDADES_BLOQUE.register(modEventBus);
        TIPOS_RECETA.register(modEventBus);
        Construccion.registrar(modEventBus);
        RolesRed.registrar();

        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alRegistrarComandos);

        // Hueco de cara (antifaz)
        MinecraftForge.EVENT_BUS.addListener(Cara::alCalcularVisibilidad);
        MinecraftForge.EVENT_BUS.addListener(Cara::alClonar);
        MinecraftForge.EVENT_BUS.addListener(Cara::alTick);
        MinecraftForge.EVENT_BUS.addListener(Cara::alUsarObjeto);
        MinecraftForge.EVENT_BUS.addListener(Cara::alEmpezarAVer);
        MinecraftForge.EVENT_BUS.addListener(Cara::alSalir);

        // Habilidades de oficio
        MinecraftForge.EVENT_BUS.addListener(Explorador::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(SacoDormirBlock::alGuardarAparicion);
        MinecraftForge.EVENT_BUS.addListener(Sagrado::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(BaculoItem::alAtacar);
        MinecraftForge.EVENT_BUS.addListener(BaculoItem::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(RadiocaseteItem::alTick);
        MinecraftForge.EVENT_BUS.addListener(ImanItem::alTick);
        MinecraftForge.EVENT_BUS.addListener(BotasMuelles::alSaltar);
        MinecraftForge.EVENT_BUS.addListener(BotasMuelles::alCaer);
        MinecraftForge.EVENT_BUS.addListener(EscanerItem::alTick);
        MinecraftForge.EVENT_BUS.addListener(RadiocaseteItem::alEmpezarAVer);
        MinecraftForge.EVENT_BUS.addListener(RadiocaseteItem::alSalir);
        MinecraftForge.EVENT_BUS.addListener(RadiocaseteItem::alCambiarDimension);
        MinecraftForge.EVENT_BUS.addListener(RadiocaseteItem::alMorir);
        MinecraftForge.EVENT_BUS.addListener(SacoDormirBlock::alDespertar);
        MinecraftForge.EVENT_BUS.addListener(IncensarioSueloBlock::alAparecer);
        MinecraftForge.EVENT_BUS.addListener(Musica::alSalir);
        MinecraftForge.EVENT_BUS.addListener(EscudoTamborItem::alBloquear);
        MinecraftForge.EVENT_BUS.addListener(EscudoTamborItem::alSalir);
        MinecraftForge.EVENT_BUS.addListener(ArcoLiraItem::alImpactar);
        MinecraftForge.EVENT_BUS.addListener(Armonica::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(Armonica::alTickMob);
        MinecraftForge.EVENT_BUS.addListener(Armonica::alCambiarObjetivo);
        MinecraftForge.EVENT_BUS.addListener(Armonica::alSalir);
        // Experiencia de oficio (ver ExperienciaOficio)
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alTick);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alSalir);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alHacerDano);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alFundir);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alFabricar);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alUsarYunque);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alUsarPiedraAfilar);
        MinecraftForge.EVENT_BUS.addListener(Herrero::alFundir);
        MinecraftForge.EVENT_BUS.addListener(TaladroItem::alRomper);
        MinecraftForge.EVENT_BUS.addListener(TaladroItem::alTickMundo);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alPonerBloque);
        MinecraftForge.EVENT_BUS.addListener(ExperienciaOficio::alRomperBloque);
        // Fantasma
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alCalcularVisibilidad);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alCambiarObjetivo);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alAtacar);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alGolpear);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alRomper);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alCalcularVelocidadRomper);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alPisotear);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alUsarCubo);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alClickDerechoBloque);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alClickDerechoObjeto);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alUsarEntidad);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alUsarEntidadExacto);
        MinecraftForge.EVENT_BUS.addListener(Fantasma::alTick);
        // Compañeros (flauta)
        MinecraftForge.EVENT_BUS.addListener(Companeros::alInteractuar);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alUnirseAlMundo);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alTickMob);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alCambiarObjetivo);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alRecibirDano);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alSalirDelMundo);
        MinecraftForge.EVENT_BUS.addListener(Companeros::alConvertirse);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alEntrar);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alReaparecer);
        MinecraftForge.EVENT_BUS.addListener(ModuloRoles::alCambiarDimension);
    }

    private static void alRegistrarComandos(RegisterCommandsEvent evento) {
        RolComando.registrar(evento.getDispatcher());
    }

    // Al entrar: si aún no tiene oficio, le sale el menú para elegirlo
    private static void alEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, !RolDatos.tieneRol(jugador));
        }
    }

    private static void alReaparecer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }

    private static void alCambiarDimension(PlayerEvent.PlayerChangedDimensionEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jugador) {
            RolDatos.sincronizar(jugador, false);
        }
    }
}
