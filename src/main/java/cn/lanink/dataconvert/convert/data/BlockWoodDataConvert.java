package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author LT_Name
 */
public class BlockWoodDataConvert extends BlockDataConvert {

    private int type;

    public BlockWoodDataConvert() {
        this(0);
    }

    public BlockWoodDataConvert(int type) {
        this.type = type;
    }

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int stripped_bit = 0;
        if (this.name.contains("stripped_")) {
            stripped_bit = 1;
        } else if (blockState.contains("stripped_bit")) {
            stripped_bit = blockState.getInt("stripped_bit");
        }
        String pillar_axis = blockState.getString("pillar_axis");
        int data = switch (pillar_axis) {
            case "x" -> 0x10;
            case "z" -> 0x20;
            default -> 0; //y
        };
        return data | stripped_bit << 3 | (type & 0x7);
    }

}
