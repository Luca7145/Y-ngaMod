package com.lucarita.yonga.core;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Material de la armadura de lucarita: un poco mejor que la netherita.
 *
 *                     netherita      lucarita
 *   Protección        3 / 8 / 6 / 3  igual (casco / peto / pantalones / botas)
 *   Dureza            3              3,5
 *   Resist. empuje    10%            15% por pieza
 *   Durabilidad (×)   37             40   (casco 440, peto 640, pantalones 600, botas 520)
 *   Encantabilidad    15             18
 *
 * La textura puesta: assets/yonga/textures/models/armor/lucarita_layer_1.png (casco, peto y botas)
 * y lucarita_layer_2.png (pantalones), de 64x32 como las de Minecraft. Se dibujan con
 * herramientas/dibujar_lucarita_armadura.py (los iconos, con herramientas/dibujar_lucarita.py).
 */
public enum ModArmaduras implements ArmorMaterial {
    LUCARITA("yonga:lucarita", 40, 3, 8, 6, 3, 18, 3.5f, 0.15f, SoundEvents.ARMOR_EQUIP_NETHERITE,
            () -> Ingredient.of(ModItems.LUCARITA.get())),
    /**
     * La túnica sagrada del Sacerdote: no protege NADA contra lo normal (0 de armadura), pero contra los mobs de fe
     * quita un porcentaje del daño (ver modulos/roles/oficios/Sagrado). Se repara con lana.
     */
    TUNICA("yonga:tunica", 15, 0, 0, 0, 0, 22, 0.0f, 0.0f, SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> Ingredient.of(ItemTags.WOOL)),
    /**
     * Las botas de muelles del Ingeniero: protegen como unas de hierro (2), y saltan y amortiguan (ver
     * modulos/roles/oficios/BotasMuelles). Se reparan con hierro.
     */
    MUELLES("yonga:muelles", 15, 0, 0, 0, 2, 9, 0.0f, 0.0f, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(net.minecraft.world.item.Items.IRON_INGOT));

    // Durabilidad base de cada pieza (la misma que usa Minecraft), se multiplica por el número del material
    private static final int[] DURABILIDAD_BASE = {11, 16, 15, 13};   // casco, peto, pantalones, botas

    private final String nombre;
    private final int multiplicadorDurabilidad;
    private final int casco, peto, pantalones, botas;
    private final int encantabilidad;
    private final float dureza;
    private final float resistenciaEmpuje;
    private final SoundEvent sonido;
    private final Supplier<Ingredient> reparacion;

    ModArmaduras(String nombre, int multiplicadorDurabilidad, int casco, int peto, int pantalones, int botas,
                 int encantabilidad, float dureza, float resistenciaEmpuje, SoundEvent sonido, Supplier<Ingredient> reparacion) {
        this.sonido = sonido;
        this.reparacion = reparacion;
        this.nombre = nombre;
        this.multiplicadorDurabilidad = multiplicadorDurabilidad;
        this.casco = casco;
        this.peto = peto;
        this.pantalones = pantalones;
        this.botas = botas;
        this.encantabilidad = encantabilidad;
        this.dureza = dureza;
        this.resistenciaEmpuje = resistenciaEmpuje;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type tipo) {
        return DURABILIDAD_BASE[tipo.ordinal()] * multiplicadorDurabilidad;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type tipo) {
        return switch (tipo) {
            case HELMET -> casco;
            case CHESTPLATE -> peto;
            case LEGGINGS -> pantalones;
            case BOOTS -> botas;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return encantabilidad;
    }

    @Override
    public SoundEvent getEquipSound() {
        return sonido;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return reparacion.get();
    }

    @Override
    public String getName() {
        return nombre;
    }

    @Override
    public float getToughness() {
        return dureza;
    }

    @Override
    public float getKnockbackResistance() {
        return resistenciaEmpuje;
    }
}
