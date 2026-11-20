package com.lucarita.yonga.modulos.roles.oficios;

import com.lucarita.yonga.modulos.fe.mobs.MobFe;
import com.lucarita.yonga.modulos.roles.ModuloRoles;
import com.lucarita.yonga.modulos.roles.cara.Cara;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Los objetos sagrados del Sacerdote:
 *  - TÚNICA SAGRADA (capucha, túnica, faldón y sandalias): contra lo normal es como no llevar nada (0 de armadura),
 *    pero contra los MOBS DE FE (luminosos y oscuros, también los jefes y sus proyectiles) cada pieza quita un
 *    porcentaje del daño: 18 + 27 + 23 + 17 = 85 % con las cuatro (la lucarita, como mucho, un 80 %).
 *  - HALO (en la cara): un 35 % menos de daño de los mobs de fe OSCURA.
 *  - CUERNOS (en la cara): un 35 % menos de daño de los mobs de fe LUMINOSA.
 * Todo se multiplica: túnica completa + halo contra un mob oscuro = solo te llega un 10 % del daño.
 */
public final class Sagrado {

    private Sagrado() {}

    public static final float HALO = 0.35f, CUERNOS = 0.35f;

    /** Una pieza de la túnica: armadura que solo protege contra los mobs de fe. */
    public static class PiezaTunica extends ArmorItem {
        private final float proteccion;

        public PiezaTunica(ArmorMaterial material, Type tipo, float proteccion, Properties propiedades) {
            super(material, tipo, propiedades);
            this.proteccion = proteccion;
        }

        public float proteccion() {
            return proteccion;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
            lineas.add(Component.translatable("yonga.sagrado.tunica").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            super.appendHoverText(stack, nivel, lineas, flag);
        }
    }

    /** El halo o los cuernos: objetos de cara. */
    public static class Distintivo extends Item {
        private final String texto;

        public Distintivo(String texto, Properties propiedades) {
            super(propiedades.stacksTo(1));
            this.texto = texto;
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level nivel, List<Component> lineas, TooltipFlag flag) {
            lineas.add(Component.translatable(texto).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            super.appendHoverText(stack, nivel, lineas, flag);
        }
    }

    /** Se registra en ModuloRoles. */
    public static void alRecibirDano(LivingHurtEvent evento) {
        if (!(evento.getEntity() instanceof Player jugador) || !(evento.getSource().getEntity() instanceof MobFe mob)) {
            return;
        }
        float tunica = 0;
        for (EquipmentSlot hueco : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (jugador.getItemBySlot(hueco).getItem() instanceof PiezaTunica pieza) {
                tunica += pieza.proteccion();
            }
        }
        float queda = 1.0f - Math.min(tunica, 0.95f);
        ItemStack cara = Cara.obtener(jugador);
        if (mob.esOscuro() && cara.is(ModuloRoles.HALO.get())) {
            queda *= 1.0f - HALO;
        } else if (!mob.esOscuro() && cara.is(ModuloRoles.CUERNOS.get())) {
            queda *= 1.0f - CUERNOS;
        }
        if (queda < 1.0f) {
            evento.setAmount(evento.getAmount() * queda);
        }
    }
}
