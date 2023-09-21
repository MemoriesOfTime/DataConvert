package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockDoubleSlabDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        if (blockState.contains("minecraft:vertical_half")) {
            return switch (blockState.getString("minecraft:vertical_half").toLowerCase()) {
                case "top" -> 1;
                case "bottom" -> 0;
                default -> 0;
            };
        }
        return blockState.getByte("top_slot_bit") > 0 ? 1 : 0;
    }

}
