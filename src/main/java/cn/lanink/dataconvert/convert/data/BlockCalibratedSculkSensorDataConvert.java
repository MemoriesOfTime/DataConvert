package cn.lanink.dataconvert.convert.data;

import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 校域传感器的方块状态转换器 (Calibrated Sculk Sensor)
 * <p>
 * 该方块在 1.20.0 (protocol 589) 引入，具有 4 朝向 × 3 相位 = 12 个状态。
 * <p>
 * 朝向状态字段存在版本差异:
 * <ul>
 *     <li>589-594: {@code direction} (int 0-3)</li>
 *     <li>618+:    {@code minecraft:cardinal_direction} (string: south/west/north/east)</li>
 * </ul>
 * 两者都映射到 {@link BlockFace#getHorizontalIndex()}:
 * south=0, west=1, north=2, east=3。
 * <p>
 * 调色板 data 编码 (与 NetEase 权威数据一致):
 * {@code data = horizontalIndex * 3 + sculk_sensor_phase}
 *
 * @author LT_Name
 */
public class BlockCalibratedSculkSensorDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int direction;
        if (blockState.contains("minecraft:cardinal_direction")) {
            BlockFace cardinalDirection = BlockFace.valueOf(
                    blockState.getString("minecraft:cardinal_direction").toUpperCase());
            direction = cardinalDirection.getHorizontalIndex();
        } else {
            direction = blockState.getInt("direction");
        }

        int phase = blockState.getInt("sculk_sensor_phase");

        return direction * 3 + phase;
    }

}
