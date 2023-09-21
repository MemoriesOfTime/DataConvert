package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * Converts lightning rod states to Nukkit legacy data.
 */
public class BlockLightningRodDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int facingDirection = blockState.getInt("facing_direction") & 0x7;
        int poweredBit = 0;
        if (blockState.contains("powered_bit")) {
            poweredBit = blockState.getByte("powered_bit") != 0 ? 1 : 0;
        }
        return poweredBit << 3 | facingDirection;
    }
}
