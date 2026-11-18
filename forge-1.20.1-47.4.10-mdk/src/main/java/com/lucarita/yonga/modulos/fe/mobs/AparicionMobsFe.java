package com.lucarita.yonga.modulos.fe.mobs;

import com.lucarita.yonga.YongaMain;
import com.lucarita.yonga.modulos.Modulo;
import com.lucarita.yonga.modulos.fe.ModuloFe;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
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
 */
public final class AparicionMobsFe implements BiomeModifier {

    // ---- Ajustes: peso (lo frecuente que es frente a otros mobs) y tamaño del grupo ----
    private static final int PESO_HADAS = 10;
    private static final int GRUPO_MIN = 1;
    private static final int GRUPO_MAX = 3;   // solas, en pareja o de tres en tres
    // Enanos: en cualquier bioma del mundo normal, pocos y casi siempre solos
    private static final int PESO_ENANOS = 3;

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
        if (bioma.is(BiomeTags.IS_FOREST)) {
            var apariciones = constructor.getMobSpawnSettings();
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.HADA.get(), PESO_HADAS, GRUPO_MIN, GRUPO_MAX));
            apariciones.addSpawn(MobCategory.AMBIENT,
                    new MobSpawnSettings.SpawnerData(ModuloFe.HADA_OSCURA.get(), PESO_HADAS, GRUPO_MIN, GRUPO_MAX));
        }
        if (bioma.is(BiomeTags.IS_OVERWORLD)) {
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
