package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockStrippedLogDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        String axis = blockState.getString("pillar_axis");
        return switch (axis) {
            case "x" -> 1;
            case "z" -> 2;
            default -> 0; //y
        };
    }

}
