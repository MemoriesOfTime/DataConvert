package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 活板门方块数据转换
 * data = direction | (upside_down_bit << 2) | (open_bit << 3)
 *
 * @author LT_Name
 */
public class BlockTrapdoorDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int direction = blockState.getInt("direction");
        int upsideDownBit = blockState.getByte("upside_down_bit");
        int openBit = blockState.getByte("open_bit");

        return direction | (upsideDownBit << 2) | (openBit << 3);
    }

}
