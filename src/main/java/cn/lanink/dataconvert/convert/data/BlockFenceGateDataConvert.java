package cn.lanink.dataconvert.convert.data;

import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockFenceGateDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int in_wall_bit = blockState.getByte("in_wall_bit");
        int open_bit = blockState.getByte("open_bit");
        int direction = 0;
        if (blockState.contains("minecraft:cardinal_direction")) {
            BlockFace cardinal_direction = BlockFace.valueOf(blockState.getString("minecraft:cardinal_direction").toUpperCase());
            direction = cardinal_direction.getHorizontalIndex();
        } else {
            direction = blockState.getInt("direction");
        }

        return in_wall_bit << 3 | open_bit << 2 | (direction & 0x3);
    }

}
