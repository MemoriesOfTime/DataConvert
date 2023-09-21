package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;
import lombok.Getter;

/**
 * @author LT_Name
 */
public abstract class BlockDataConvert {

    @Getter
    protected String name;
    @Getter
    protected int id;

    public CompoundTag convert(int version, CompoundTag old) {
        if (!old.contains("id")) {
            throw new RuntimeException("BlockState does not contain id:\n" + old.toSNBT());
        }
        this.name = old.getString("name");
        this.id = old.getInt("id");
        if (this.id == -1) {
            if (this.name.startsWith("minecraft:") && this.name.endsWith("_wall")) {
                this.id = 139;
            } else if (this.name.startsWith("minecraft:") && this.name.endsWith("_coral")) {
                this.id = 386;
            } else {
                throw new RuntimeException("BlockState does not contain id:\n" + old.toSNBT());
            }
        }
        short data = (short) this.calculateData(version, old.getCompound("states"));
        if (data == -1) {
            return null;
        }
        old.putShort("data", data);
        old.putInt("id", this.id);
        return old;
    }

    /**
     * 根据方块状态计算方块特殊值
     *
     * @param version 版本
     * @param blockState 方块状态
     * @return 方块特殊值
     */
    public abstract int calculateData(int version, CompoundTag blockState);

}
