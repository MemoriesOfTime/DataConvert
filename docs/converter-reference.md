# 转换器参考文档

## 转换器类型

根据方块类型选择合适的转换器：

| 方块类型 | 转换器类 | 状态字段 |
|---------|---------|----------|
| 无状态方块（木板、栅栏等） | `BlockEmptyStatesDataConvert` | 无 |
| 按钮 | `BlockButtonDataConvert` | `button_pressed_bit`, `facing_direction` |
| 门 | `BlockDoorDataConvert` | `open_bit`, `upper_block_bit`, `door_hinge_bit`, `direction` |
| 栅栏门 | `BlockFenceGateDataConvert` | `in_wall_bit`, `open_bit`, `direction` |
| 原木 | `BlockLogDataConvert` | `pillar_axis` |
| 去皮原木 | `BlockStrippedLogDataConvert` | `pillar_axis` |
| 木头 | `BlockWoodDataConvert` | `pillar_axis`, `stripped_bit` |
| 台阶 | `BlockSlabDataConvert` | `minecraft:vertical_half` 或 `top_slot_bit` |
| 双层台阶 | `BlockDoubleSlabDataConvert` | `minecraft:vertical_half` 或 `top_slot_bit` |
| 楼梯 | `BlockStairsDataConvert` | `upside_down_bit`, `weirdo_direction` |
| 活板门 | `BlockTrapdoorDataConvert` | `direction`, `upside_down_bit`, `open_bit` |
| 压力板 | `BlockPressurePlateDataConvert` | `redstone_signal` |
| 立式告示牌 | `BlockStandingSignDataConvert` | `ground_sign_direction` |
| 墙上告示牌 | `BlockWallSignDataConvert` | `facing_direction` |
| 悬挂告示牌 | `BlockHangingSignDataConvert` | `facing_direction`, `ground_sign_direction`, `hanging`, `attached_bit` |
| 树叶 | `BlockLeavesDataConvert` | `update_bit`, `persistent_bit` |
| 树苗 | `BlockSaplingDataConvert` | `age_bit` |
| 珊瑚 | `BlockCoralDataConvert` | `coral_color`, `dead_bit` |
| 墙 | `BlockWallDataConvert` | `wall_connection_type_east`, `wall_connection_type_north`, `wall_connection_type_south`, `wall_connection_type_west`, `wall_post_bit` |

## 转换器工作原理

所有转换器都继承自 `BlockDataConvert` 基类，核心方法是 `calculateData`：

```java
public abstract int calculateData(int version, CompoundTag blockState);
```

该方法根据方块状态（blockState）计算方块的特殊值（data）。

### 转换流程

1. 接收方块状态数据（CompoundTag 格式）
2. 从状态中提取相关字段
3. 按照 Nukkit-MOT 的 meta 计算规则进行位运算
4. 返回计算后的 data 值

### 示例：按钮转换器

```java
public class BlockButtonDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int button_pressed_bit = blockState.getByte("button_pressed_bit");
        int facing_direction = blockState.getInt("facing_direction");
        return button_pressed_bit << 3 | facing_direction & 0x7;
    }
}
```

计算公式：`data = (pressed << 3) | (facing & 0x7)`

### 示例：门转换器

```java
public class BlockDoorDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int open_bit = blockState.getByte("open_bit");
        int upper_block_bit = blockState.getByte("upper_block_bit");
        int door_hinge_bit = blockState.getByte("door_hinge_bit");
        int direction;
        if (blockState.contains("minecraft:cardinal_direction")) {
            BlockFace cardinal_direction = BlockFace.valueOf(
                blockState.getString("minecraft:cardinal_direction").toUpperCase());
            direction = cardinal_direction.getHorizontalIndex();
        } else {
            direction = blockState.getInt("direction");
        }
        return door_hinge_bit << 4 | upper_block_bit << 3 | open_bit << 2 | direction & 0x3;
    }
}
```

计算公式：`data = (hinge << 4) | (upper << 3) | (open << 2) | (direction & 0x3)`

## 转换器文件位置

所有转换器位于 `src/main/java/cn/lanink/dataconvert/convert/data/` 目录下。
