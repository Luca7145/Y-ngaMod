package com.lucarita.yonga.modulos.fe.mobs;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Dónde aparecen los mobs de fe. Se hace en código (y no en un JSON de Forge) para que, con el módulo de fe
 * desactivado, el JSON no apunte a mobs que no existen. Lo usa data/yonga/forge/biome_modifier/mobs_de_fe.json.
 *
 * Este tipo se registra SIEMPRE (esté o no activo el módulo), porque el JSON siempre está.
 *
 * Los biomas van por ETIQUETAS PROPIAS (data/yonga/tags/worldgen/biome): bosques, taigas, junglas, llanuras, sabanas,
 * montañas, desiertos y mundo_normal. Cada una junta la etiqueta de Minecraft, las comunes de otros mods (forge:, c:)
 * y los biomas de Regions Unexplored (si no está instalado, se ignoran). Así los mods de biomas que "se comen" los de
 * Minecraft no dejan sin mobs: para añadir los biomas de otro mod basta un datapack que añada sus biomas a estas
 * etiquetas, sin tocar el código.
 *
 * El suelo donde pueden aparecer (esSuelo) también vale para la tierra y la hierba de otros mods.
 */
public final class AparicionMobsFe implements BiomeModifier {

    // ---- Ajustes: peso (lo frecuente que es frente a otros mobs) y tamaño del grupo ----
    private static final int PESO_HADAS = 10;
    private static final int GRUPO_MIN = 1;
    private static final int GRUPO_MAX = 3;   // solas, en pareja o de tres en tres
    // Enanos: en cualquier bioma del mundo normal, pocos y casi siempre solos
    private static final int PESO_ENANOS = 3;
    // Leones de guerra: en sabanas, llanuras y mesetas, entre los demás animales (pocos: 1 o 2)
    private static final int PESO_LEONES = 3;
    // Duendes: en cualquier bioma frondoso, a cualquier hora, solos o en pareja
    private static final int PESO_DUENDES = 8;
    // Enanas madre: en llanuras (sin árboles que las frenen), poco frecuentes y siempre solas
    private static final int PESO_MADRES = 1;
    // Gnomos: un poco en cualquier sitio con vegetación (biomas frondosos y llanuras), solos o en pareja
    private static final int PESO_GNOMOS = 3;
    // Gárgolas (de noche, como los monstruos): sobre todo en montañas, y algo en llanuras, bosques y taigas (pueblos)
    private static final int PESO_GARGOLAS_MONTANA = 20, PESO_GARGOLAS_PUEBLOS = 5;

    // ---- Las etiquetas de biomas ----
    public static final TagKey<Biome> BOSQUES = etiqueta("bosques");
    public static final TagKey<Biome> TAIGAS = etiqueta("taigas");
    public static final TagKey<Biome> JUNGLAS = etiqueta("junglas");
    public static final TagKey<Biome> LLANURAS = etiqueta("llanuras");
    public static final TagKey<Biome> SABANAS = etiqueta("sabanas");
    public static final TagKey<Biome> MONTANAS = etiqueta("montanas");
    public static final TagKey<Biome> MUNDO_NORMAL = etiqueta("mundo_normal");
    /** Todo lo que tiene mucha vegetación: bosques, taigas, junglas, pantanos, manglares, cerezos... */
    public static final TagKey<Biome> FRONDOSOS = etiqueta("frondosos");
    /** Mesetas y tierras baldías (la "mesa"), y las sabanas altas. */
    public static final TagKey<Biome> MESETAS = etiqueta("mesetas");
    /** Bloques extra donde pueden aparecer (para añadir suelos raros de otros mods con un datapack). */
    public static final TagKey<Block> SUELO = TagKey.create(Registries.BLOCK, new ResourceLocation(YongaMain.MOD_ID, "suelo_mobs_fe"));

    private static TagKey<Biome> etiqueta(String nombre) {
        return TagKey.create(Registries.BIOME, new ResourceLocation(YongaMain.MOD_ID, nombre));
    }

    /** ¿Se puede aparecer encima de este bloque? Tierra o hierba de cualquier mod (o lo de la etiqueta suelo_mobs_fe). */
    public static boolean esSuelo(BlockState debajo) {
        return debajo.is(BlockTags.DIRT) || debajo.is(SUELO) || debajo.is(BlockTags.ANIMALS_SPAWNABLE_ON)
                || debajo.getBlock() instanceof SpreadingSnowyDirtBlock;
    }

    public static final AparicionMobsFe INSTANCIA = new AparicionMobsFe();

    public static final DeferredRegister<Codec<? extends BiomeModifier>> TIPOS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, YongaMain.MOD_ID);

    public static final RegistryObject<Codec<AparicionMobsFe>> CODEC = TIPOS.register("mobs_de_fe",
            () -> Codec.unit(INSTANCIA));

    private AparicionMobsFe() {}

    public static void registrar(IEventBus modEventBus) {
        TIPOS.register(modEventBus);
    }

    @Override
    public void modify(Holder<Biome> bioma, Phase fase, ModifiableBiomeInfo.BiomeInfo.Builder constructor) {
        if (fase != Phase.ADD || !Modulo.FE.activo()) {
            return;
        }
        if (bioma.is(FRONDOSOS)) {
            var apariciones = constructor.getMobSpawnSettings();
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.HADA.get(), PESO_HADAS, GRUPO_MIN, GRUPO_MAX));
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.HADA_OSCURA.get(), PESO_HADAS, GRUPO_MIN, GRUPO_MAX));
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.DUENDE.get(), PESO_DUENDES, 1, 2));
        }
        if (bioma.is(FRONDOSOS) || bioma.is(LLANURAS)) {
            constructor.getMobSpawnSettings().addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.GNOMO.get(), PESO_GNOMOS, 1, 2));
        }
        if (bioma.is(MONTANAS)) {
            constructor.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ModuloFe.GARGOLA.get(), PESO_GARGOLAS_MONTANA, 1, 2));
        } else if (bioma.is(LLANURAS) || bioma.is(BOSQUES) || bioma.is(TAIGAS)) {
            constructor.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ModuloFe.GARGOLA.get(), PESO_GARGOLAS_PUEBLOS, 1, 1));
        }
        if (bioma.is(LLANURAS)) {
            var apariciones = constructor.getMobSpawnSettings();
            apariciones.addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ModuloFe.ENANO_MADRE.get(), PESO_MADRES, 1, 1));
            apariciones.addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ModuloFe.ENANO_MADRE_OSCURO.get(), PESO_MADRES, 1, 1));
        }
        if (bioma.is(SABANAS) || bioma.is(LLANURAS) || bioma.is(MESETAS)) {
            var apariciones = constructor.getMobSpawnSettings();
            apariciones.addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ModuloFe.LEON_ARQUERO.get(), PESO_LEONES, 1, 2));
            apariciones.addSpawn(MobCategory.CREATURE,
                    new MobSpawnSettings.SpawnerData(ModuloFe.LEON_ESPADACHIN.get(), PESO_LEONES, 1, 1));
        }
        if (bioma.is(MUNDO_NORMAL)) {
            var apariciones = constructor.getMobSpawnSettings();
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.ENANO.get(), PESO_ENANOS, 1, 1));
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.ENANO_OSCURO.get(), PESO_ENANOS, 1, 1));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC.get();
    }
}
