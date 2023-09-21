# 转换器开发指南

## 概述

本文档介绍如何为新的方块类型创建转换器。

## 基础知识

### 转换器基类

所有转换器都继承自 `BlockDataConvert` 基类：

```java
package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;
import lombok.Getter;

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

    public abstract int calculateData(int version, CompoundTag blockState);
}
```

### 核心方法

`calculateData(int version, CompoundTag blockState)` 是转换器的核心方法：

- **输入**: 方块状态（CompoundTag 格式）
- **输出**: 方块的特殊值（data，0-15）

## 开发步骤

### 1. 分析方块状态

首先，需要了解方块有哪些状态字段。可以通过以下方式获取：

1. 查看 PMMP/CloudBurst 的方块状态文件
2. 使用 `CheckBlockStatesCount` 工具查看方块状态

例如，按钮的状态字段：
```json
{
  "button_pressed_bit": 0,
  "facing_direction": 2
}
```

### 2. 确定计算公式

根据 Nukkit-MOT 中方块的 meta 计算逻辑，确定状态字段如何组合成 data 值。

例如，按钮的计算公式：
```
data = (button_pressed_bit << 3) | (facing_direction & 0x7)
```

### 3. 创建转换器类

在 `src/main/java/cn/lanink/dataconvert/convert/data/` 目录下创建新类：

```java
package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

public class BlockMyBlockDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        // 从状态中提取字段
        int field1 = blockState.getByte("field1");
        int field2 = blockState.getInt("field2");
        
        // 按照 Nukkit-MOT 的规则计算 data
        return field1 << 2 | field2 & 0x3;
    }
}
```

### 4. 添加转换器映射

在 `RuntimeBlockStateConvertOld.java` 的 `convert` 方法中添加：

```java
dataConvertMap.put("minecraft:my_block", new BlockMyBlockDataConvert());
```

### 5. 测试验证

1. 运行转换命令
2. 使用 `CheckBlockStatesCount` 验证结果
3. 检查生成的 data 值是否与预期一致

## 常见模式

### 无状态方块

对于没有状态的方块（如木板、栅栏），使用 `BlockEmptyStatesDataConvert`：

```java
dataConvertMap.put("minecraft:oak_planks", new BlockEmptyStatesDataConvert());
```

### 单字段方块

对于只有一个状态字段的方块：

```java
public class BlockSaplingDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        return blockState.getByte("age_bit");
    }
}
```

### 多字段方块

对于有多个状态字段的方块，需要进行位运算组合：

```java
public class BlockTrapdoorDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int direction = blockState.getInt("direction");
        int upsideDownBit = blockState.getByte("upside_down_bit");
        int openBit = blockState.getByte("open_bit");
        return direction & 0x3 | upsideDownBit << 2 | openBit << 3;
    }
}
```

### 版本兼容

某些方块在不同版本可能有不同的状态字段：

```java
public class BlockDoorDataConvert extends BlockDataConvert {
    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int direction;
        if (blockState.contains("minecraft:cardinal_direction")) {
            // 新版本使用 cardinal_direction
            BlockFace cardinal_direction = BlockFace.valueOf(
                blockState.getString("minecraft:cardinal_direction").toUpperCase());
            direction = cardinal_direction.getHorizontalIndex();
        } else {
            // 旧版本使用 direction
            direction = blockState.getInt("direction");
        }
        // ...
    }
}
```

## 调试技巧

### 1. 打印状态信息

在开发过程中，可以打印状态信息帮助调试：

```java
log.info("Block state: {}", blockState.toSNBT());
```

### 2. 对比参考实现

查看 Nukkit-MOT 项目中相同方块的实现，确保计算逻辑一致。

### 3. 使用 CheckBlockStatesCount

运行检查工具，对比 PM 数量和 MOT 数量是否一致。

## 注意事项

1. **ID 必须正确**: 方块 ID 必须与 Nukkit-MOT 项目中的定义一致
2. **计算逻辑一致**: `calculateData` 返回的 data 值必须与 Nukkit-MOT 中方块的 meta 计算逻辑一致
3. **注释代码**: 添加完转换后记得注释掉相关代码，避免重复转换
4. **测试充分**: 添加新转换器后，务必进行充分测试
