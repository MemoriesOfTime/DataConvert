package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 通用无状态方块转换
 *
 * @author LT_Name
 */
public class BlockEmptyStatesDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        return 0;
    }

}
