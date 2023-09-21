package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockWallDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int ease = wallConnectionType2Int(blockState.getString("wall_connection_type_east"));
        int south = wallConnectionType2Int(blockState.getString("wall_connection_type_south"));
        int west = wallConnectionType2Int(blockState.getString("wall_connection_type_west"));
        int north = wallConnectionType2Int(blockState.getString("wall_connection_type_north"));
        int post = blockState.getByte("wall_post_bit");
        return (post << 8) | (west << 6) | (south << 4) | (north << 2) | ease;
    }

    public int wallConnectionType2Int(String wallConnectionType) {
        return switch (wallConnectionType) {
            case "short" -> 1;
            case "tall" -> 2;
            default -> 0;
        };
    }
}
