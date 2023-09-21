package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 树苗方块数据转换
 * data = age_bit
 *
 * @author LT_Name
 */
public class BlockSaplingDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        return blockState.getByte("age_bit");
    }

}
