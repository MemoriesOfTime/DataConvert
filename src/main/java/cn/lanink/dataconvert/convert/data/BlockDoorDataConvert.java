package cn.lanink.dataconvert.convert.data;

import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;


/**
 * @author LT_Name
 */
public class BlockDoorDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int open_bit = blockState.getByte("open_bit");
        int upper_block_bit = blockState.getByte("upper_block_bit");
        int door_hinge_bit = blockState.getByte("door_hinge_bit");
        int direction;
        if (blockState.contains("minecraft:cardinal_direction")) {
            BlockFace cardinal_direction = BlockFace.valueOf(blockState.getString("minecraft:cardinal_direction").toUpperCase());
            direction = cardinal_direction.getHorizontalIndex();
        } else {
            direction = blockState.getInt("direction");
        }

        return door_hinge_bit << 4 | upper_block_bit << 3 | open_bit << 2 | direction & 0x3;
    }

}
