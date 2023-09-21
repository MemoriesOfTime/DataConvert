package cn.lanink.dataconvert.convert.data;

import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.Map;

/**
 * Converts shelf states to Nukkit legacy data.
 */
public class BlockShelfDataConvert extends BlockDataConvert {

    private static final Map<String, Integer> SHELF_IDS = Map.ofEntries(
            Map.entry("minecraft:oak_shelf", 1302),
            Map.entry("minecraft:spruce_shelf", 1303),
            Map.entry("minecraft:birch_shelf", 1304),
            Map.entry("minecraft:jungle_shelf", 1305),
            Map.entry("minecraft:acacia_shelf", 1306),
            Map.entry("minecraft:dark_oak_shelf", 1307),
            Map.entry("minecraft:mangrove_shelf", 1308),
            Map.entry("minecraft:cherry_shelf", 1309),
            Map.entry("minecraft:pale_oak_shelf", 1310),
            Map.entry("minecraft:bamboo_shelf", 1311),
            Map.entry("minecraft:crimson_shelf", 1312),
            Map.entry("minecraft:warped_shelf", 1313)
    );

    @Override
    public CompoundTag convert(int version, CompoundTag old) {
        CompoundTag result = super.convert(version, old);
        if (result == null) {
            return null;
        }
        Integer correctId = SHELF_IDS.get(this.name);
        if (correctId != null) {
            result.putInt("id", correctId);
        }
        return result;
    }

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int type = blockState.getInt("powered_shelf_type") & 0b11;
        int powered = (blockState.contains("powered_bit") ? blockState.getByte("powered_bit") : 0) & 0b1;
        int direction = getDirection(blockState) & 0b11;
        return type | (powered << 2) | (direction << 3);
    }

    private int getDirection(CompoundTag blockState) {
        if (blockState.contains("minecraft:cardinal_direction")) {
            String dir = blockState.getString("minecraft:cardinal_direction");
            return switch (dir) {
                case "south" -> 0;
                case "west" -> 1;
                case "north" -> 2;
                case "east" -> 3;
                default -> 0;
            };
        }
        return 0;
    }
}
