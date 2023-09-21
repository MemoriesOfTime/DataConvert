package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

import java.util.Map;

public class BlockCopperGolemStatueDataConvert extends BlockDataConvert {

    private static final Map<String, Integer> STATUE_IDS = Map.ofEntries(
            Map.entry("minecraft:copper_golem_statue", 1294),
            Map.entry("minecraft:exposed_copper_golem_statue", 1295),
            Map.entry("minecraft:weathered_copper_golem_statue", 1296),
            Map.entry("minecraft:oxidized_copper_golem_statue", 1297),
            Map.entry("minecraft:waxed_copper_golem_statue", 1298),
            Map.entry("minecraft:waxed_exposed_copper_golem_statue", 1299),
            Map.entry("minecraft:waxed_weathered_copper_golem_statue", 1300),
            Map.entry("minecraft:waxed_oxidized_copper_golem_statue", 1301)
    );

    @Override
    public CompoundTag convert(int version, CompoundTag old) {
        CompoundTag result = super.convert(version, old);
        if (result == null) {
            return null;
        }
        Integer correctId = STATUE_IDS.get(this.name);
        if (correctId != null) {
            result.putInt("id", correctId);
        }
        return result;
    }

    @Override
    public int calculateData(int version, CompoundTag blockState) {
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
