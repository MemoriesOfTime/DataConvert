package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockButtonDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int button_pressed_bit = blockState.getByte("button_pressed_bit");
        int facing_direction = blockState.getInt("facing_direction");
        return button_pressed_bit << 3 | facing_direction & 0x7;
    }

}
