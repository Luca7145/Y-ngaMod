package com.lucarita.yonga.modulos.habitantes.muebles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lo que guarda un mueble: 9 casillas (vasija, ánfora, cesto, escritorio), 27 (cómoda, nevera) o el disco del
 * gramófono. Admite "LootTable" (así algunas vasijas de las aldeas traen cosas dentro).
 * La nevera, además, enfría: un cubo de agua que pase un rato dentro se vuelve hielo (y el cubo vacío).
 */
public class MuebleBlockEntity extends RandomizableContainerBlockEntity {

    private NonNullList<ItemStack> objetos;
    private int frio;

    public MuebleBlockEntity(BlockPos pos, BlockState estado) {
        super(Muebles.ENTIDAD.get(), pos, estado);
        int huecos = estado.getBlock() instanceof MuebleBlock m ? Math.max(1, m.config().huecos()) : 9;
        objetos = NonNullList.withSize(huecos, ItemStack.EMPTY);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return objetos;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nuevos) {
        objetos = nuevos;
    }

    @Override
    public int getContainerSize() {
        return objetos.size();
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventario) {
        if (objetos.size() >= 27) return ChestMenu.threeRows(id, inventario, this);
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventario, this, 1);
    }

    @Override
    public void load(CompoundTag datos) {
        super.load(datos);
        objetos = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(datos)) ContainerHelper.loadAllItems(datos, objetos);
        frio = datos.getInt("Frio");
    }

    @Override
    protected void saveAdditional(CompoundTag datos) {
        super.saveAdditional(datos);
        if (!trySaveLootTable(datos)) ContainerHelper.saveAllItems(datos, objetos);
        datos.putInt("Frio", frio);
    }

    // ------------------------------------------------------------------ gramófono

    void tocar() {
        if (level != null && !getItem(0).isEmpty()) {
            level.levelEvent(null, 1010, worldPosition, Item.getId(getItem(0).getItem()));
        }
    }

    void parar() {
        if (level != null) level.levelEvent(1011, worldPosition, 0);
    }

    // ------------------------------------------------------------------ nevera

    /** Cada minuto, un cubo de agua de dentro se vuelve un bloque de hielo (y deja el cubo, si cabe). */
    static void enfriar(Level nivel, BlockPos pos, MuebleBlockEntity be) {
        if (++be.frio < 1200) return;
        be.frio = 0;
        for (int i = 0; i < be.objetos.size(); i++) {
            if (!be.objetos.get(i).is(Items.WATER_BUCKET)) continue;
            int libre = -1;
            for (int j = 0; j < be.objetos.size(); j++) {
                if (be.objetos.get(j).isEmpty()) { libre = j; break; }
            }
            if (libre < 0) return;
            be.objetos.set(i, new ItemStack(Items.ICE));
            be.objetos.set(libre, new ItemStack(Items.BUCKET));
            be.setChanged();
            return;
        }
    }
}
