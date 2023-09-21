package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * Converts crafter states to Nukkit legacy data.
 */
public class BlockCrafterDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int orientation = switch (blockState.getString("orientation")) {
            case "down_north" -> 1;
            case "down_south" -> 2;
            case "down_west" -> 3;
            case "east_up" -> 4;
            case "north_up" -> 5;
            case "south_up" -> 6;
            case "up_east" -> 7;
            case "up_north" -> 8;
            case "up_south" -> 9;
            case "up_west" -> 10;
            case "west_up" -> 11;
            case "down_east" -> 0;
            default -> 0;
        };
        return orientation | (getBit(blockState, "crafting") << 4) | (getBit(blockState, "triggered_bit") << 5);
    }

    private int getBit(CompoundTag blockState, String name) {
        if (!blockState.contains(name)) {
            return 0;
        }
        return blockState.getByte(name) != 0 ? 1 : 0;
    }
}
