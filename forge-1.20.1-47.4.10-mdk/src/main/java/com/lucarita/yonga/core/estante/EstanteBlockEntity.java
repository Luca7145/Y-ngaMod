package com.lucarita.yonga.core.estante;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lo que guarda un estante: 3 ranuras (0 = izquierda, 1 = centro, 2 = derecha, vistas de frente), cada una con un
 * montón entero. Es un contenedor normal, así que las tolvas pueden meter y sacar cosas (y los tubos de otros mods).
 * Se manda al cliente para que dibuje lo que tiene.
 */
public class EstanteBlockEntity extends BlockEntity implements Container {

    public static final int RANURAS = 3;
    private final NonNullList<ItemStack> objetos = NonNullList.withSize(RANURAS, ItemStack.EMPTY);
    private LazyOptional<InvWrapper> capacidad = LazyOptional.of(() -> new InvWrapper(this));

    public EstanteBlockEntity(BlockPos pos, BlockState estado) {
        super(Estantes.ENTIDAD.get(), pos, estado);
    }

    // ---------- guardar y sincronizar ----------

    @Override
    public void load(CompoundTag datos) {
        super.load(datos);
        objetos.clear();
        ContainerHelper.loadAllItems(datos, objetos);
    }

    @Override
    protected void saveAdditional(CompoundTag datos) {
        super.saveAdditional(datos);
        ContainerHelper.saveAllItems(datos, objetos, true);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag datos = new CompoundTag();
        ContainerHelper.saveAllItems(datos, objetos, true);
        return datos;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Guarda, avisa a los comparadores y manda el cambio a los clientes. */
    private void cambiado() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState estado = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, estado, estado, 3);
        }
    }

    // ---------- contenedor ----------

    @Override
    public int getContainerSize() {
        return RANURAS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : objetos) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int ranura) {
        return objetos.get(ranura);
    }

    @Override
    public ItemStack removeItem(int ranura, int cantidad) {
        ItemStack quitado = ContainerHelper.removeItem(objetos, ranura, cantidad);
        if (!quitado.isEmpty()) cambiado();
        return quitado;
    }

    @Override
    public ItemStack removeItemNoUpdate(int ranura) {
        ItemStack quitado = ContainerHelper.takeItem(objetos, ranura);
        if (!quitado.isEmpty()) cambiado();
        return quitado;
    }

    @Override
    public void setItem(int ranura, ItemStack objeto) {
        objetos.set(ranura, objeto);
        if (objeto.getCount() > this.getMaxStackSize()) {
            objeto.setCount(this.getMaxStackSize());
        }
        cambiado();
    }

    @Override
    public boolean stillValid(Player jugador) {
        return Container.stillValidBlockEntity(this, jugador);
    }

    @Override
    public void clearContent() {
        objetos.clear();
        cambiado();
    }

    /** La señal de comparador: 1 por la izquierda, 2 por el centro, 4 por la derecha (sumadas: hasta 7). */
    public int senal() {
        int s = 0;
        for (int i = 0; i < RANURAS; i++) {
            if (!objetos.get(i).isEmpty()) s |= 1 << i;
        }
        return s;
    }

    // ---------- tolvas y tubos de otros mods ----------

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction lado) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !this.remove) {
            return capacidad.cast();
        }
        return super.getCapability(cap, lado);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        capacidad.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        capacidad = LazyOptional.of(() -> new InvWrapper(this));
    }
}
