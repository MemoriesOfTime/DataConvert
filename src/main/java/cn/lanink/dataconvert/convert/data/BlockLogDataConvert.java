package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockLogDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        String pillar_axis = blockState.getString("pillar_axis");
        return switch (pillar_axis) {
            case "x" -> 4; //0b0100
            case "z" -> 8; //0b1000
            default -> 0; //y
        };
    }
}
