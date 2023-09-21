package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

import static cn.nukkit.block.BlockCoral.*;

/**
 * @author LT_Name
 */
public class BlockCoralDataConvert extends BlockDataConvert{

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        String string = blockState.getString("coral_color");
        if (string != null && !string.isEmpty()) {
            int data = switch (string) {
                case "blue" -> TYPE_TUBE;
                case "pink" -> TYPE_BRAIN;
                case "purple" -> TYPE_BUBBLE;
                case "red" -> TYPE_FIRE;
                case "yellow" -> TYPE_HORN;
                default -> throw new RuntimeException("Invalid color: " + string);
            };
            data = blockState.getByte("dead_bit") == 1 ? data | 0x8 : data;
            return data;
        }

        int data = TYPE_TUBE;
        if (this.name.contains("fire_coral")) {
            data = TYPE_FIRE;
        } else if (this.name.contains("brain_coral")) {
            data = TYPE_BRAIN;
        } else if (this.name.contains("bubble_coral")) {
            data = TYPE_BUBBLE;
        } else if (this.name.contains("horn_coral")) {
            data = TYPE_HORN;
        }

        if (this.name.contains("dead")) {
            data |= 0x8;
        }

        return data;
    }
}
