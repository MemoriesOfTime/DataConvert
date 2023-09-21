package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

import java.util.Map;

public class BlockChestCopperDataConvert extends BlockDataConvert {

    private static final Map<String, Integer> CHEST_IDS = Map.ofEntries(
            Map.entry("minecraft:copper_chest", 1286),
            Map.entry("minecraft:exposed_copper_chest", 1287),
            Map.entry("minecraft:weathered_copper_chest", 1288),
            Map.entry("minecraft:oxidized_copper_chest", 1289),
            Map.entry("minecraft:waxed_copper_chest", 1290),
            Map.entry("minecraft:waxed_exposed_copper_chest", 1291),
            Map.entry("minecraft:waxed_weathered_copper_chest", 1292),
            Map.entry("minecraft:waxed_oxidized_copper_chest", 1293)
    );

    @Override
    public CompoundTag convert(int version, CompoundTag old) {
        CompoundTag result = super.convert(version, old);
        if (result == null) {
            return null;
        }
        Integer correctId = CHEST_IDS.get(this.name);
        if (correctId != null) {
            result.putInt("id", correctId);
            result.putBoolean("stateOverload", false);
        }
        return result;
    }

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        if (blockState.contains("minecraft:cardinal_direction")) {
            String dir = blockState.getString("minecraft:cardinal_direction");
            return switch (dir) {
                case "north" -> 2;
                case "south" -> 3;
                case "west" -> 4;
                case "east" -> 5;
                default -> 0;
            };
        }
        return 0;
    }
}
