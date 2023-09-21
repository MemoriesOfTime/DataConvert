# 苍白橡木系列方块示例

## 概述

苍白橡木（Pale Oak）系列方块是 Minecraft 1.21.4 版本（协议版本 766+）新增的方块系列。

## 方块列表

| 方块名称 | ID | 转换器 | 说明 |
|---------|-----|--------|------|
| pale_oak_button | 1244 | BlockButtonDataConvert | 按钮 |
| pale_oak_door | 1245 | BlockDoorDataConvert | 门 |
| pale_oak_fence | 1246 | BlockEmptyStatesDataConvert | 栅栏（无状态） |
| pale_oak_fence_gate | 1247 | BlockFenceGateDataConvert | 栅栏门 |
| pale_oak_hanging_sign | 1248 | (待实现) | 悬挂告示牌 |
| stripped_pale_oak_log | 1249 | BlockStrippedLogDataConvert | 去皮原木 |
| pale_oak_log | 1250 | BlockLogDataConvert | 原木 |
| pale_oak_planks | 1251 | BlockEmptyStatesDataConvert | 木板（无状态） |
| pale_oak_pressure_plate | 1252 | BlockPressurePlateDataConvert | 压力板 |
| pale_oak_slab | 1253 | BlockSlabDataConvert | 台阶 |
| pale_oak_double_slab | 1254 | BlockDoubleSlabDataConvert | 双层台阶 |
| pale_oak_stairs | 1255 | BlockStairsDataConvert | 楼梯 |
| pale_oak_standing_sign | 1256 | BlockStandingSignDataConvert | 立式告示牌 |
| pale_oak_trapdoor | 1257 | BlockTrapdoorDataConvert | 活板门 |
| pale_oak_wall_sign | 1258 | BlockWallSignDataConvert | 墙上告示牌 |
| stripped_pale_oak_wood | 1259 | BlockStrippedLogDataConvert | 去皮木头 |
| pale_oak_wood | 1260 | BlockWoodDataConvert | 木头 |
| pale_oak_sapling | 1261 | BlockSaplingDataConvert | 树苗 |
| pale_oak_leaves | 1262 | BlockLeavesDataConvert | 树叶 |

## 添加步骤

### 1. 添加方块 ID 映射

在 `src/main/resources/block_ids.csv` 文件末尾添加：

```csv
1244,minecraft:pale_oak_button
1245,minecraft:pale_oak_door
1246,minecraft:pale_oak_fence
1247,minecraft:pale_oak_fence_gate
1248,minecraft:pale_oak_hanging_sign
1249,minecraft:stripped_pale_oak_log
1250,minecraft:pale_oak_log
1251,minecraft:pale_oak_planks
1252,minecraft:pale_oak_pressure_plate
1253,minecraft:pale_oak_slab
1254,minecraft:pale_oak_double_slab
1255,minecraft:pale_oak_stairs
1256,minecraft:pale_oak_standing_sign
1257,minecraft:pale_oak_trapdoor
1258,minecraft:pale_oak_wall_sign
1259,minecraft:stripped_pale_oak_wood
1260,minecraft:pale_oak_wood
1261,minecraft:pale_oak_sapling
1262,minecraft:pale_oak_leaves
```

### 2. 添加转换器映射

在 `RuntimeBlockStateConvertOld.java` 的 `convert` 方法中添加：

```java
// 苍白橡木系列 (Pale Oak) - 766+
dataConvertMap.put("minecraft:pale_oak_button", new BlockButtonDataConvert());
dataConvertMap.put("minecraft:pale_oak_door", new BlockDoorDataConvert());
dataConvertMap.put("minecraft:pale_oak_fence", new BlockEmptyStatesDataConvert());
dataConvertMap.put("minecraft:pale_oak_fence_gate", new BlockFenceGateDataConvert());
// pale_oak_hanging_sign 需要特殊处理或使用通用悬挂告示牌转换器
dataConvertMap.put("minecraft:stripped_pale_oak_log", new BlockStrippedLogDataConvert());
dataConvertMap.put("minecraft:pale_oak_log", new BlockLogDataConvert());
dataConvertMap.put("minecraft:pale_oak_planks", new BlockEmptyStatesDataConvert());
dataConvertMap.put("minecraft:pale_oak_pressure_plate", new BlockPressurePlateDataConvert());
dataConvertMap.put("minecraft:pale_oak_slab", new BlockSlabDataConvert());
dataConvertMap.put("minecraft:pale_oak_double_slab", new BlockDoubleSlabDataConvert());
dataConvertMap.put("minecraft:pale_oak_stairs", new BlockStairsDataConvert());
dataConvertMap.put("minecraft:pale_oak_standing_sign", new BlockStandingSignDataConvert());
dataConvertMap.put("minecraft:pale_oak_trapdoor", new BlockTrapdoorDataConvert());
dataConvertMap.put("minecraft:pale_oak_wall_sign", new BlockWallSignDataConvert());
dataConvertMap.put("minecraft:stripped_pale_oak_wood", new BlockStrippedLogDataConvert());
dataConvertMap.put("minecraft:pale_oak_wood", new BlockWoodDataConvert());
dataConvertMap.put("minecraft:pale_oak_sapling", new BlockSaplingDataConvert());
dataConvertMap.put("minecraft:pale_oak_leaves", new BlockLeavesDataConvert());
```

### 3. 运行转换

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvertOld"
```

### 4. 验证结果

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.utils.CheckBlockStatesCount"
```

## 注意事项

1. `pale_oak_hanging_sign` 暂未实现专用转换器，需要与开发者确认处理方式
2. 所有方块 ID 必须与 Nukkit-MOT 项目中的定义一致
3. 添加完成后记得注释掉相关代码，避免重复转换
