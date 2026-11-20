package com.lucarita.yonga.modulos.tabaco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La planta de tabaco: un cultivo como el trigo (8 fases, en tierra de cultivo, con harina de huesos...). Cuando
 * está madura (con sus flores rosas) da hojas de tabaco frescas y semillas.
 */
public class PlantaTabacoBlock extends CropBlock {

    private static final VoxelShape[] FORMAS = {
            Block.box(0, 0, 0, 16, 3, 16), Block.box(0, 0, 0, 16, 5, 16), Block.box(0, 0, 0, 16, 7, 16),
            Block.box(0, 0, 0, 16, 9, 16), Block.box(0, 0, 0, 16, 11, 16), Block.box(0, 0, 0, 16, 13, 16),
            Block.box(0, 0, 0, 16, 15, 16), Block.box(0, 0, 0, 16, 16, 16)};

    public PlantaTabacoBlock(Properties propiedades) {
        super(propiedades);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModuloTabaco.SEMILLAS.get();
    }

    @Override
    public VoxelShape getShape(BlockState estado, BlockGetter nivel, BlockPos pos, CollisionContext contexto) {
        return FORMAS[this.getAge(estado)];
    }
}
