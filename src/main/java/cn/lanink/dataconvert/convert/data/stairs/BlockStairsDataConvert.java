package cn.lanink.dataconvert.convert.data.stairs;

import cn.lanink.dataconvert.convert.data.BlockDataConvert;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockStairsDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int upsideDownBit = blockState.getByte("upside_down_bit");
        int weirdoDirection = blockState.getInt("weirdo_direction");
        return weirdoDirection & 0x3 | upsideDownBit << 2;
    }

}
