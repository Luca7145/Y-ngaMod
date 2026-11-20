package com.lucarita.yonga.core;

import com.google.common.base.Suppliers;
import com.lucarita.yonga.YongaMain;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

/**
 * Nuestros discos de música en los cofres de estructuras, como los de Minecraft (mazmorras, mansiones del bosque y
 * ciudades antiguas). Cada JSON de data/yonga/loot_modifiers dice en qué cofre (condición forge:loot_table_id), con qué
 * probabilidad sale un disco y cuáles pueden salir (uno al azar).
 */
public class DiscosEnCofres extends LootModifier {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> MODIFICADORES =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, YongaMain.MOD_ID);

    public static final Supplier<Codec<DiscosEnCofres>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(i -> codecStart(i)
                    .and(Codec.FLOAT.fieldOf("probabilidad").forGetter(m -> m.probabilidad))
                    .and(ForgeRegistries.ITEMS.getCodec().listOf().fieldOf("discos").forGetter(m -> m.discos))
                    .apply(i, DiscosEnCofres::new)));

    public static final RegistryObject<Codec<DiscosEnCofres>> DISCOS = MODIFICADORES.register("discos_en_cofres", CODEC);

    private final float probabilidad;
    private final List<Item> discos;

    public DiscosEnCofres(LootItemCondition[] condiciones, float probabilidad, List<Item> discos) {
        super(condiciones);
        this.probabilidad = probabilidad;
        this.discos = discos;
    }

    public static void registrar(IEventBus bus) {
        MODIFICADORES.register(bus);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generado, LootContext contexto) {
        if (!discos.isEmpty() && contexto.getRandom().nextFloat() < probabilidad) {
            generado.add(new ItemStack(discos.get(contexto.getRandom().nextInt(discos.size()))));
        }
        return generado;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
