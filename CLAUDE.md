# DataConvert 项目说明

## 项目简介

DataConvert 是一个用于转换 Minecraft 基岩版方块状态数据的工具项目。主要用于将 PMMP/CloudBurst 格式的方块调色板数据转换为 Nukkit-MOT 格式。

## 方块调色板添加流程

当需要为新版本的方块调色板添加新方块时，按照以下步骤操作：

### 1. 检查方块状态差异

使用 `CheckBlockStatesCount` 工具检查哪些版本缺少方块：

```java
// 修改 CheckBlockStatesCount.java 的 main 方法
public static void main(String[] args) {
    checkBlockStatesCount("方块名称"); // 例如: pale_oak_planks
}
```

运行命令：
```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.utils.CheckBlockStatesCount"
```

输出会显示：
- **差异版本**: PM数量与MOT数量不一致的版本（需要添加）
- **相同版本**: PM数量与MOT数量一致的版本（已正确）

该方法对网易版本文件（`runtime_block_states_netease_XXX.dat`）同样有效，会自动识别并改用网易数据源对比。

### 2. 添加方块 ID 映射

在 `src/main/resources/block_ids.csv` 文件末尾添加新方块的 ID 映射：

```csv
方块ID,minecraft:方块名称
```

### 3. 创建或选择转换器

根据方块类型选择合适的转换器。详细的转换器类型和使用方法请参考 [转换器参考文档](docs/converter-reference.md)。

如需创建新转换器，请参考 [转换器开发指南](docs/converter-development.md)。

### 4. 添加转换器映射

在 `RuntimeBlockStateConvertOld.java` 的 `convert` 方法中添加方块转换器映射。

### 5. 运行转换

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvertOld"
```

`main` 也支持命令行参数，无需改代码即可指定版本（可空格分隔多个）：

```bash
# 网易版加 netease: 前缀
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvertOld" -Dexec.args="netease:766"
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvertOld" -Dexec.args="766 776"
```

无参数时执行 main 内写死的调用。

### 6. 复制生成文件

将生成的文件从 `Target_Data/new/` 复制到 `Target_Data/`：

```bash
cp src/main/resources/Target_Data/new/*.dat src/main/resources/Target_Data/
```

注意：旧流程在生成 dat 的同时，还会把 `vanilla_palette_XXX.nbt`（网易版为 `vanilla_palette_netease_XXX.nbt`）**直接写入 `Target_Data/`（原地覆盖，不经过 `new/`）**。

### 7. 验证结果

再次运行 `CheckBlockStatesCount` 验证转换结果，确保所有版本的 PM 数量与 MOT 数量一致。

## 新版本更新流程

当 Minecraft 基岩版发布新版本时，按照以下步骤更新方块调色板数据：

### 1. 获取源数据文件

**CloudBurst 数据**:
- 从 https://github.com/CloudburstMC/Data 仓库获取
- 查看提交信息中的协议版本号（如 827, 844 等）
- 文件命名: `block_palette_XXX.nbt`（XXX 为协议版本号）
- 放置到 `src/main/resources/CB_Data/` 目录

**PMMP 数据** (参考):
- 从 https://github.com/pmmp/BedrockData 仓库获取
- 查看 git tag 中的协议版本号
- 文件命名: `canonical_block_states_XXX.nbt`
- 放置到 `src/main/resources/PMMP_Data/` 目录

### 2. 更新旧的 Nukkit-MOT 基础数据

- 从 https://github.com/MemoriesOfTime/Nukkit-MOT/tree/master/src/main/resources 拉取
- 文件命名: `runtime_block_states_XXX.dat`
- 放置到 `src/main/resources/Target_Data/` 目录

### 3. 运行转换

使用 `RuntimeBlockStateConvert` 类进行转换：

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvert"
```

修改 `main` 方法中的版本号：
```java
// 示例：从版本 827 升级到 844
convert(827, 844, false);
```

`convert()` 在生成 `runtime_block_states_XXX.dat` 的同时，会自动调用 `generateVanillaPalette()` 生成 `vanilla_palette_XXX.nbt`（国际版原始调色板，`{blocks: [...]}` 格式），两者均输出到 `Target_Data/new/`。网易版（`isNetEase = true`）不触发此自动生成，其流程见下方「网易版数据流程」。

### 4. 验证结果

使用 `CheckBlockStatesCount` 工具验证转换结果：

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.utils.CheckBlockStatesCount"
```

### 5. 复制生成文件

将生成的文件从 `Target_Data/new/` 复制到 `Target_Data/`：

```bash
cp src/main/resources/Target_Data/new/*.dat src/main/resources/Target_Data/new/*.nbt src/main/resources/Target_Data/
```

### 6. 处理异常

- 转换运行日志中的 `Not found : ...` 错误表示旧数据中的方块状态未能匹配到新数据，通常需要更新 `block_ids.csv` 文件
- 与开发者确认后再添加新的方块 ID 映射

### 与旧版本流程的区别

| 方面 | 旧版本流程 (RuntimeBlockStateConvertOld) | 新版本流程 (RuntimeBlockStateConvert) |
|------|----------------------------------------|--------------------------------------|
| 数据来源 | 手动添加转换器映射 | 从外部仓库获取数据文件 |
| 转换方式 | 逐个方块添加转换器 | 自动匹配状态更新 |
| 适用场景 | 在已有版本中添加新方块 | 版本升级/全新版本 |

## 网易版（NetEase）数据流程

网易版调色板与国际版不同：保留旧式状态格式、不含 `chalkboard`、额外包含 `micro_block`（id=9990）等，需使用 `RuntimeBlockStateConvert` 中的专用方法单独生成。

### 1. 获取网易版调色板

`NetEase_Data/block_palette_XXX.nbt` 为网易版调色板（766+ 版本使用，`{blocks: [...]}` 格式，按 runtimeId 顺序排列）。

可调用 `generateNetEasePaletteDirect(国际版基础版本号, 网易目标版本号)` 从国际版 CB 全量数据构建（基于 EaseCation/SynapseAPI 的 toNetEase 逻辑）：
- 移除 `minecraft:chalkboard`
- 添加 `minecraft:micro_block`（id=9990）
- 按 hash 排序分配 runtimeId（与 Nukkit-MOT 一致）
- **直接写入 `NetEase_Data/block_palette_<目标版本>.nbt`**，不经过 `new/`

旧版本（<766）使用 `NetEase_Data/runtime_block_states_XXX.dat` 作为源数据（现有 630、686）。

### 2. 生成网易版 runtime_block_states

调用 `generateNetEaseData(基础版本号, 目标版本号[, 上一网易版本号])`（两参数重载默认上一网易版本为 686），修改 main 后运行：

```bash
mvn compile exec:java -Dexec.mainClass="cn.lanink.dataconvert.convert.RuntimeBlockStateConvert"
```

流程分四个阶段：
1. **Phase 1**: 以国际版 `runtime_block_states_<基础版本>.dat` 为基础（不应用 BlockStateUpdaters，网易版保留旧式状态格式），按名称 + 状态匹配网易调色板赋值 runtimeId
2. **Phase 2**: 从上一版网易数据补充匹配（优先同名，其次同 ID 不同名，处理 tnt→underwater_tnt 这类重命名方块）
3. **Phase 3**: 剩余新方块按 data 从 0 递增添加
4. **Phase 4**: 补齐调色板中尚未覆盖的 runtimeId

结束后自动执行 Phase 5/5b 校验（基础数据遗漏检查、权威调色板覆盖率、TNT 重命名断言），出现 `log.error` 需排查后重跑。

**输出直接写入 `Target_Data/runtime_block_states_netease_<目标版本>.dat`**，不经过 `new/`，无需复制。

另有 `generateNetEaseDataDirect(国际版基础版本号, 网易目标版本号)`：跳过匹配，直接按构建的调色板生成（文件内按 name + data 排序），同样直接写入 `Target_Data/`。

如需网易版原始调色板文件，调用 `generateNetEaseVanillaPalette(版本号)`，输出到 `Target_Data/new/vanilla_palette_netease_XXX.nbt`（需手动复制到 `Target_Data/`）。

### 3. 验证结果

`CheckBlockStatesCount` 中的网易专用校验方法（修改 main 调用后运行）：

| 方法 | 用途 |
|------|------|
| `checkBlockStatesCount("方块名")` | 通用检查，自动识别 netease 版本文件 |
| `checkRuntimeId("方块名", 版本号)` | 校验生成数据的 runtimeId 与网易调色板一致 |
| `compareNkMotFile("方块名", 版本号)` | 对比 Nukkit-MOT 仓库现有数据与本地生成数据（经 `ExternalData` 拉取） |
| `checkTntInPalettes(版本号)` | tnt/underwater_tnt 重命名专项检查 |

### 注意

`convert(旧版本, 新版本, true)` 也支持网易版，但其数据源读取逻辑特殊：调色板读取的是**旧版本号**的 `NetEase_Data/block_palette_<旧版本>.nbt`（目标版本 >= 766 时）。网易版新流程优先使用 `generateNetEaseData`。

## 外部数据访问（AI 必读）

本项目是供 AI 使用的转换工具。需要访问 Nukkit-MOT 等外部仓库的数据文件时，统一通过 `ExternalData`（`cn.lanink.dataconvert.utils.ExternalData`）获取：

| 方法 | 数据 |
|------|------|
| `nukkitMotRuntimeBlockStates(version)` | Nukkit-MOT 国际版 `runtime_block_states_XXX.dat` |
| `nukkitMotNeteaseRuntimeBlockStates(version)` | Nukkit-MOT 网易版 `runtime_block_states_netease_XXX.dat` |

解析顺序：本地仓库覆盖 → 本地缓存 → 远程下载。

- 本地仓库覆盖：系统属性 `nukkitmot.dir` 或环境变量 `NUKKIT_MOT_DIR`，指向对应仓库根目录
- 缓存目录：默认 `cache/`（已被 .gitignore 忽略），可用系统属性 `dataconvert.cache.dir` 或环境变量 `DATACONVERT_CACHE_DIR` 修改
- 强制刷新缓存：`-Ddataconvert.refresh=true`

新增工具需要外部数据时，扩展 `ExternalData` 添加对应数据源，文件路径一律由它解析，项目内数据（`src/main/resources/`）仍使用项目相对路径。

## 目录结构

```
src/main/
├── java/cn/lanink/dataconvert/
│   ├── DataConvert.java                       # 组合入口（连续多版本转换）
│   ├── convert/
│   │   ├── RuntimeBlockStateConvertOld.java   # 旧版本转换逻辑（手动添加转换器）
│   │   ├── RuntimeBlockStateConvert.java      # 新版本转换逻辑 + 网易版生成方法
│   │   ├── RecipeConvert.java                 # CB 配方 JSON 转换
│   │   ├── data/                              # 方块数据转换器
│   │   │   └── stairs/
│   │   └── item/creative/
│   │       └── CreativeItemsJsonConvert.java  # 创造模式物品 JSON 转换
│   └── utils/
│       ├── CheckBlockStatesCount.java         # 方块状态检查/对比工具
│       ├── ExternalData.java                  # 外部仓库数据访问
│       ├── HashedPaletteComparator.java       # 调色板 hash 排序器
│       ├── NBTIO1.java                        # NBT 读写
│       └── Utils.java
└── resources/
    ├── block_ids.csv                          # 方块ID映射
    ├── PMMP_Data/                             # PMMP 源数据
    ├── CB_Data/                               # CloudBurst 源数据
    ├── NetEase_Data/                          # 网易版源数据
    │   ├── block_palette_XXX.nbt              # 网易版调色板（766+）
    │   └── runtime_block_states_XXX.dat       # 旧版网易源数据（630/686）
    └── Target_Data/                           # 转换后的目标数据
        ├── runtime_block_states_XXX.dat       # 国际版
        ├── runtime_block_states_netease_XXX.dat  # 网易版
        ├── vanilla_palette_XXX.nbt            # 原始调色板
        └── new/                               # 新生成的文件
```

## 相关文档

- [转换器参考文档](docs/converter-reference.md) - 转换器类型和工作原理
- [转换器开发指南](docs/converter-development.md) - 如何创建新的转换器
- [苍白橡木系列方块示例](docs/pale-oak-example.md) - 完整的方块添加示例

## 注意事项

1. 方块 ID 需要与 Nukkit-MOT 项目中的定义保持一致
2. 转换器的 `calculateData` 方法返回的 data 值需要与 Nukkit-MOT 中方块的 meta 计算逻辑一致
3. 添加完转换后记得注释掉相关代码，避免重复转换
4. 网易版（NetEase）的方块调色板需要单独处理，参见「网易版（NetEase）数据流程」章节
5. 国际版流程产物（dat/nbt）统一输出到 `Target_Data/new/` 需手动复制；网易版方法大多直接写入目标目录，注意区分，避免照搬复制命令
