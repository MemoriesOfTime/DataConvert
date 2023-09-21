package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 树叶方块数据转换
 * data = persistent_bit << 1 | update_bit
 *
 * @author LT_Name
 */
public class BlockLeavesDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int updateBit = blockState.getByte("update_bit");
        int persistentBit = blockState.getByte("persistent_bit");
        return persistentBit << 1 | updateBit;
    }

}
