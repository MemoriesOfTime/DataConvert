package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.lanink.dataconvert.utils.Utils;
import cn.nukkit.nbt.tag.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

/**
 * 利用pmmp的方块数据更新nk-mot的方块数据
 */
@Log4j2
public class RuntimeBlockStateConvert {

    public static void main(String[] args) throws IOException {
        // 生成国际版 2192 方块调色板数据（基于 2168 升级） / Generate international 2192 block palette (upgrade from 2168)
        convert(2168, 2192);

        System.exit(0);
    }

    private static final String CHALKBOARD_NAME = "minecraft:chalkboard";
    private static final String MICRO_BLOCK_NAME = "minecraft:micro_block";
    private static final int MICRO_BLOCK_ID = 9990;

    /**
     * 转换结果的统一输出目录（生成后需手动复制到 Target_Data/）。
     * runtime_block_states 与 vanilla_palette 均输出到此处，保持流程一致。
     */
    private static final String OUTPUT_DIR = "src/main/resources/Target_Data/new/";

    /**
     * 基于 EaseCation/SynapseAPI 的 V1_21_80.toNetEase(false) 逻辑构建 1.21.93 使用的网易 palette 条目：
     * - 以国际版 CB 全量 block_palette 为基础
     * - 移除 chalkboard
     * - 添加 micro_block
     * - 使用与现有工具一致的 hash 排序分配 runtimeId
     */
    private static List<CompoundTag> buildNetEasePaletteEntries(int baseVersion) throws IOException {
        String basePath = "src/main/resources/CB_Data/block_palette_" + baseVersion + ".nbt";
        List<CompoundTag> baseData = Utils.readCBBlockStates(basePath);
        log.info("基础版本 {} 全量方块数量: {}", baseVersion, baseData.size());

        List<CompoundTag> blocks = new ArrayList<>(baseData.size());
        for (CompoundTag block : baseData) {
            blocks.add(block.copy());
        }

        int removed = 0;
        Iterator<CompoundTag> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            CompoundTag block = iterator.next();
            if (CHALKBOARD_NAME.equals(block.getString("name"))) {
                iterator.remove();
                removed++;
            }
        }
        log.info("移除 chalkboard 方块: {} 个", removed);

        boolean hasMicroBlock = blocks.stream().anyMatch(block -> MICRO_BLOCK_NAME.equals(block.getString("name")));
        if (!hasMicroBlock) {
            int paletteVersion = baseData.isEmpty() ? 0 : baseData.get(0).getInt("version");
            CompoundTag microBlock = new CompoundTag();
            microBlock.putString("name", MICRO_BLOCK_NAME);
            microBlock.putInt("id", MICRO_BLOCK_ID);
            microBlock.putShort("data", (short) 0);
            microBlock.putInt("version", paletteVersion);
            microBlock.putCompound("states", new CompoundTag());
            blocks.add(microBlock);
            log.info("添加 {} (id={})", MICRO_BLOCK_NAME, MICRO_BLOCK_ID);
        }

        List<CompoundTag> sorted = Utils.sort(blocks);
        log.info("排序后方块数量: {}", sorted.size());
        return sorted;
    }

    /**
     * 直接生成网易版 block_palette_xxx.nbt。
     * 文件内容按 runtimeId 顺序写出，便于后续读取时顺序分配 runtimeId。
     */
    public static void generateNetEasePaletteDirect(int baseVersion, int targetVersion) throws IOException {
        log.info("Generating NetEase palette directly: base={} -> target={}", baseVersion, targetVersion);

        List<CompoundTag> sorted = buildNetEasePaletteEntries(baseVersion);

        ListTag<CompoundTag> blocksList = new ListTag<>();
        for (CompoundTag block : sorted) {
            CompoundTag clean = block.copy();
            clean.remove("runtimeId");
            clean.remove("id");
            clean.remove("data");
            clean.remove("network_id");
            clean.remove("name_hash");
            clean.remove("block_id");
            blocksList.add(clean);
        }

        CompoundTag paletteTag = new CompoundTag().put("blocks", blocksList);

        String neteaseOutputPath = "src/main/resources/NetEase_Data/block_palette_" + targetVersion + ".nbt";
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(neteaseOutputPath))) {
            NBTIO1.writeGZIPCompressed(paletteTag, outputStream, ByteOrder.BIG_ENDIAN);
        }
        log.info("NetEase block_palette 已保存: {}, 方块数量: {}", neteaseOutputPath, blocksList.size());
    }

    /**
     * 直接生成网易版 runtime_block_states_xxx.dat。
     * runtimeId 按网易 palette 顺序分配，文件内部再按 name + data 排序，保持项目现有 dat 风格。
     */
    public static void generateNetEaseDataDirect(int baseVersion, int targetVersion) throws IOException {
        log.info("Generating NetEase data directly: base={} -> target={}", baseVersion, targetVersion);

        List<CompoundTag> sorted = buildNetEasePaletteEntries(baseVersion);

        List<CompoundTag> outputBlocks = new ArrayList<>(sorted.size());
        for (CompoundTag block : sorted) {
            outputBlocks.add(block.copy());
        }

        Comparator<CompoundTag> c1 = Comparator.comparing(o -> o.getString("name"));
        Comparator<CompoundTag> comparator = c1.thenComparingInt(o -> o.getInt("data"));
        outputBlocks.sort(comparator);

        ListTag<CompoundTag> resultList = new ListTag<>();
        resultList.setAll(outputBlocks);
        String outputPath = "src/main/resources/Target_Data/runtime_block_states_netease_" + targetVersion + ".dat";
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            NBTIO1.writeGZIPCompressed(resultList, outputStream, ByteOrder.BIG_ENDIAN);
        }
        log.info("NetEase 数据已保存: {}, 方块数量: {}", outputPath, resultList.size());
    }

    public static void convert(int oldBlockStatesVersion, int targetBlockStatesVersion) throws IOException {
        convert(oldBlockStatesVersion, targetBlockStatesVersion, false);
    }

    public static void convert(int oldBlockStatesVersion, int targetBlockStatesVersion, boolean isNetEase) throws IOException {
        //更新判断的基础数据
        ListTag<CompoundTag> oldBaseListTag = Utils.readNKBlockStates("src/main/resources/Target_Data/runtime_block_states_" + oldBlockStatesVersion + ".dat");

        Map<String, Integer> persistenceNameToBlockId = Utils.readBlockName2IdMap();

        //加载PMMP的数据
        List<CompoundTag> tags;
        String pmmpFileName = "src/main/resources/PMMP_Data/canonical_block_states_" + targetBlockStatesVersion + ".nbt";
        if (targetBlockStatesVersion <= 407) {
            pmmpFileName = "src/main/resources/PMMP_Data/required_block_states_" + targetBlockStatesVersion + ".nbt";
        }
        try {
            if (isNetEase) {
                // 网易方块数据
                if (targetBlockStatesVersion >= 766) {
                    tags = Utils.readNeteasePaletteBlocks("src/main/resources/NetEase_Data/block_palette_" + oldBlockStatesVersion + ".nbt");
                } else {
                    tags = Utils.readECBlockStates("src/main/resources/NetEase_Data/runtime_block_states_" + targetBlockStatesVersion + ".dat");
                }
            } else {
                tags = Utils.readPMBlockStates(pmmpFileName);
            }
        } catch (Exception e) {
            log.error("无法读取PMMP_Data/canonical_block_states_{}.nbt，尝试读取CB_Data/block_palette_{}.nbt", targetBlockStatesVersion, targetBlockStatesVersion, e);
            //加载cb数据
            tags = Utils.readCBBlockStates("src/main/resources/CB_Data/block_palette_" + targetBlockStatesVersion + ".nbt");
        }

//        if (isNetEase) {
//            // 网易方块数据
//            tags.add(new CompoundTag().putString("name", "minecraft:mod_ore")
//                    .putShort("data", (short) 0)
//                    .putInt("runtimeId", -1)
//                    .putInt("id", 230)
//                    .putInt("version", 18153475)
//                    .putCompound("states", new CompoundTag()));
//            tags.add(new CompoundTag().putString("name", "minecraft:micro_block")
//                    .putShort("data", (short) 0)
//                    .putInt("runtimeId", -1)
//                    .putInt("id", 9990)
//                    .putInt("version", 18153475)
//                    .putCompound("states", new CompoundTag()));
//            tags = Utils.sort(tags);
//        }

        //更新方块数据到新版本
        if (!isNetEase) {
            ListTag<CompoundTag> cache = new ListTag<>();
            for (CompoundTag compoundTag : oldBaseListTag.getAll()) {
                cache.add(Utils.nbtMap2CompoundTag(BlockStateUpdaters.updateBlockState(Utils.compoundTag2NbtMap(compoundTag), compoundTag.getInt("version"))));
            }
            oldBaseListTag = cache;
        }

        ListTag<CompoundTag> newTagList = new ListTag<>();

        //移除不需要的（nkx中的）
        /*List<CompoundTag> all = newTagList.getAll();
        newTagList = new ListTag<>();
        for (CompoundTag tag : all) {
            String name = tag.getString("name");
            if (name.equals("minecraft:respawn_anchor")
                    || name.equals("minecraft:bee_nest")
                    || name.equals("minecraft:beehive")) {
                continue;
            }
            newTagList.add(tag);
        }*/

        //更新方块runtimeId
        for (CompoundTag block : oldBaseListTag.getAll()) {
            //根据名称获取新的方块数据
            String name = block.getString("name");
            ArrayList<Tag> oldBlockStates = new ArrayList<>(block.getCompound("states").getAllTags());

            List<CompoundTag> newBlockList = getBlockByName(tags, name, null);
            if (newBlockList.isEmpty()) {
                continue;
            }

            CompoundTag equalsTag = null;
            for (CompoundTag newBlock : newBlockList) {
                if (equalsTag != null) {
                    break;
                }
                CompoundTag newBlockStates = newBlock.getCompound("states");
                if (newBlockStates.equals(block.getCompound("states"))) {
                    equalsTag = newBlock;
                    break;
                }

                if (newBlockStates.isEmpty() && oldBlockStates.isEmpty()) {
                    equalsTag = newBlock;
                    break;
                } else {
                    boolean equals = true;
                    for (Tag tag1 : oldBlockStates) {
                        if (!newBlockStates.contains(tag1.getName())) {
                            equals = false;
                            break;
                        }
                        Object o1 = newBlockStates.get(tag1.getName()).parseValue();
                        Object o2 = tag1.parseValue();
                        if (o1 instanceof Number) {
                            if (!compare(o1, o2)) {
                                equals = false;
                                break;
                            }
                        } else if (!o1.equals(o2)) {
                            equals = false;
                            break;
                        }
                    }
                    if (equals) {
                        equalsTag = newBlock;
                    }
                }
            }

            if (equalsTag != null) {
                CompoundTag copy = block.copy();
                int runtimeId = equalsTag.getInt("runtimeId");
                copy.putInt("runtimeId", runtimeId);
                copy.putInt("version", equalsTag.getInt("version"));
                boolean isDuplicate = false;
                for (CompoundTag tag : newTagList.getAll()) {
                    if (tag.getInt("runtimeId") == runtimeId
                            && tag.getString("name").equals(copy.getString("name"))
                            && tag.getInt("id") == copy.getInt("id")
                            && tag.getInt("data") == copy.getInt("data")) {
                        isDuplicate = true;
                        log.warn("Duplicate : old:{}\nnew:{}\n originalNew:{}", block, copy, tag);
                    }
                }
                if (!isDuplicate) {
                    newTagList.add(copy);
                }
            }
        }

        //使用旧的数据中的名称和特殊值检查是否有遗漏
        for (CompoundTag tag : oldBaseListTag.getAll()) {
            String name = tag.getString("name");
            int data = tag.getInt("data");
            boolean has = false;
            for (CompoundTag block : newTagList.getAll()) {
                if (block.getString("name").equals(name) && block.getInt("data") == data) {
                    has = true;
                    break;
                }
            }
            if (!has) {
                log.error("Not found : {}", tag.toSNBT());
            }
        }

        //新方块
        /*HashSet<String> set = new HashSet<>();
        for (CompoundTag tag : tags) {
            String name = tag.getString("name");
            boolean has = false;
            for (CompoundTag block : newTagList.getAll()) {
                if (block.getString("name").equals(name)) {
                    has = true;
                    break;
                }
            }
            if (!has && !set.contains(name)) {
                set.add(name);
                CompoundTag copy = tag.copy();
                copy.putShort("data", (short) 0);
            }
        }*/

        //按照名称和data排序
        if (oldBlockStatesVersion >= 419) {
            //Comparator<CompoundTag> c1 = Comparator.comparingInt(o -> o.getInt("runtimeId"));
            Comparator<CompoundTag> c1 = Comparator.comparing(o -> o.getString("name"));
            Comparator<CompoundTag> comparator = c1.thenComparingInt(o -> o.getInt("data"));
            newTagList.getAllUnsafe().sort(comparator);
        }

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(OUTPUT_DIR + "runtime_block_states_" + targetBlockStatesVersion + ".dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);

        // 同步生成国际版 vanilla_palette，避免每次手动单独调用
        if (!isNetEase) {
            generateVanillaPalette(targetBlockStatesVersion);
        }
    }

    private static List<CompoundTag> getBlockByName(ListTag<CompoundTag> tags, String name, Integer meta) {
        return getBlockByName(tags.getAll(), name, meta);
    }

    private static List<CompoundTag> getBlockByName(List<CompoundTag> tags, String name, Integer meta) {
        ArrayList<CompoundTag> list = new ArrayList<>();
        for (CompoundTag block : tags) {
            if (block.getString("name").equals(name)
                    && (meta == null || block.getShort("data") == meta)) {
                list.add(block);
            }
        }
        return list;
    }

    /**
     * 检查 newStates 是否为 oldStates 的子集：
     * newStates 的每一个 key 都存在于 oldStates 中，且值相等
     */
    private static boolean isSubsetMatch(CompoundTag newStates, CompoundTag oldStates) {
        if (newStates.isEmpty() && oldStates.isEmpty()) {
            return true;
        }
        for (Tag tag : newStates.getAllTags()) {
            if (!oldStates.contains(tag.getName())) {
                return false;
            }
            Object newVal = tag.parseValue();
            Object oldVal = oldStates.get(tag.getName()).parseValue();
            if (newVal instanceof Number) {
                if (!compare(newVal, oldVal)) {
                    return false;
                }
            } else if (!newVal.equals(oldVal)) {
                return false;
            }
        }
        return true;
    }

    public static boolean compare(Object num1, Object num2) {
        // 如果两个数值都是 null，则认为它们相等
        if (num1 == null && num2 == null) {
            return true;
        }

        // 如果其中一个数值为 null，则认为它们不相等
        if (num1 == null || num2 == null) {
            return false;
        }

        // 将传入的数值转换为 double 类型进行比较
        double value1 = toDouble(num1);
        double value2 = toDouble(num2);

        // 判断两个数值是否相等
        return Double.compare(value1, value2) == 0;
    }

    private static double toDouble(Object num) {
        // 如果是 Byte 或者 Integer 类型且数值为 0，则转换为 double 类型的 0
        if (num instanceof Byte || num instanceof Integer) {
            if ((int)num == 0) {
                return 0.0;
            }
        }

        // 其他情况将数值转换为 double 类型
        return ((Number)num).doubleValue();
    }

    /**
     * 生成网易版 vanilla_palette
     * 读取 NetEase_Data/block_palette_xxx.nbt，保留原始 name + states + version，
     * 输出为 {blocks: [...]} 格式的 GZIP 压缩 NBT 文件
     */
    public static void generateNetEaseVanillaPalette(int blockStatesVersion) throws IOException {
        log.info("Generating NetEase vanilla_palette for version {}", blockStatesVersion);

        String neteasePaletteFile = "src/main/resources/NetEase_Data/block_palette_" + blockStatesVersion + ".nbt";
        List<CompoundTag> neteasePalette;
        try {
            neteasePalette = Utils.readNeteasePaletteBlocks(neteasePaletteFile);
        } catch (Exception e) {
            log.warn("readNeteasePaletteBlocks 失败，回退到 readCBBlockStates: {}", e.getMessage());
            neteasePalette = Utils.readCBBlockStates(neteasePaletteFile);
        }
        log.info("NetEase 调色板方块数量: {}", neteasePalette.size());

        ListTag<CompoundTag> blocksList = new ListTag<>();
        for (CompoundTag block : neteasePalette) {
            CompoundTag clean = block.copy();
            // 移除处理过程中添加的字段，保留原始数据（name, states, version）
            clean.remove("runtimeId");
            clean.remove("id");
            clean.remove("network_id");
            clean.remove("name_hash");
            clean.remove("block_id");
            clean.remove("data");
            blocksList.add(clean);
        }

        String outputPath = OUTPUT_DIR + "vanilla_palette_netease_" + blockStatesVersion + ".nbt";
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            NBTIO1.writeGZIPCompressed(new CompoundTag().put("blocks", blocksList), outputStream, ByteOrder.BIG_ENDIAN);
        }

        log.info("NetEase vanilla_palette 已保存: {}, 方块数量: {}", outputPath, blocksList.size());
    }

    /**
     * 生成国际版 vanilla_palette
     * 读取 PMMP_Data 或 CB_Data 的方块调色板数据，保留原始 name + states + version，
     * 输出为 {blocks: [...]} 格式的 GZIP 压缩 NBT 文件
     */
    public static void generateVanillaPalette(int blockStatesVersion) throws IOException {
        log.info("Generating vanilla_palette for version {}", blockStatesVersion);

        // 尝试读取 PMMP 数据
        List<CompoundTag> palette;
        String pmmpFile = "src/main/resources/PMMP_Data/canonical_block_states_" + blockStatesVersion + ".nbt";
        if (blockStatesVersion <= 407) {
            pmmpFile = "src/main/resources/PMMP_Data/required_block_states_" + blockStatesVersion + ".nbt";
        }
        try {
            palette = Utils.readPMBlockStates(pmmpFile);
            log.info("从 PMMP_Data 读取数据成功");
        } catch (Exception e) {
            log.warn("无法读取 PMMP_Data，尝试读取 CB_Data: {}", e.getMessage());
            // 回退到 CB 数据
            String cbFile = "src/main/resources/CB_Data/block_palette_" + blockStatesVersion + ".nbt";
            palette = Utils.readCBBlockStates(cbFile);
            log.info("从 CB_Data 读取数据成功");
        }
        log.info("调色板方块数量: {}", palette.size());

        ListTag<CompoundTag> blocksList = new ListTag<>();
        for (CompoundTag block : palette) {
            CompoundTag clean = block.copy();
            // 移除处理过程中添加的字段，保留原始数据（name, states, version）
            clean.remove("runtimeId");
            clean.remove("id");
            clean.remove("network_id");
            clean.remove("name_hash");
            clean.remove("block_id");
            clean.remove("data");
            blocksList.add(clean);
        }

        String outputPath = OUTPUT_DIR + "vanilla_palette_" + blockStatesVersion + ".nbt";
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            NBTIO1.writeGZIPCompressed(new CompoundTag().put("blocks", blocksList), outputStream, ByteOrder.BIG_ENDIAN);
        }

        log.info("vanilla_palette 已保存: {}, 方块数量: {}", outputPath, blocksList.size());
    }

    /**
     * 生成网易版数据
     * 1. 读取国际版 baseVersion 的数据作为基础
     * 2. 使用 BlockStateUpdaters 将方块状态更新到 targetVersion
     * 3. 读取 NetEase targetVersion 调色板，按名称 + 状态匹配赋值 runtimeId
     * 4. 按名称 + data 排序并保存
     *
     * @param baseVersion   基础版本号（如 748）
     * @param targetVersion 目标网易版本号（如 766）
     */
    public static void generateNetEaseData(int baseVersion, int targetVersion) throws IOException {
        generateNetEaseData(baseVersion, targetVersion, 686);
    }

    public static void generateNetEaseData(int baseVersion, int targetVersion, int prevNeVersion) throws IOException {
        log.info("Generating NetEase data: base={} -> target={} prevNe={}", baseVersion, targetVersion, prevNeVersion);

        // 1. 读取国际版基础数据
        ListTag<CompoundTag> baseData = Utils.readNKBlockStates("src/main/resources/Target_Data/runtime_block_states_" + baseVersion + ".dat");
        log.info("基础版本 {} 方块数量: {}", baseVersion, baseData.size());

        // 2. 直接使用基础数据（不使用 BlockStateUpdaters，因为网易版保留旧式状态格式）

        // 3. 读取 NetEase 调色板文件
        String neteasePaletteFile = "src/main/resources/NetEase_Data/block_palette_" + targetVersion + ".nbt";
        List<CompoundTag> neteasePalette;
        try {
            neteasePalette = Utils.readNeteasePaletteBlocks(neteasePaletteFile);
        } catch (Exception e) {
            log.warn("readNeteasePaletteBlocks 失败，回退到 readCBBlockStates: {}", e.getMessage());
            neteasePalette = Utils.readCBBlockStates(neteasePaletteFile);
        }
        log.info("NetEase 调色板方块数量: {}", neteasePalette.size());

        // 按名称建立索引
        Map<String, List<CompoundTag>> neteaseByName = new HashMap<>();
        for (CompoundTag block : neteasePalette) {
            neteaseByName.computeIfAbsent(block.getString("name"), k -> new ArrayList<>()).add(block);
        }

        // 4. 匹配并赋值 runtimeId
        ListTag<CompoundTag> result = new ListTag<>();
        int matchCount = 0;
        int unmatchCount = 0;

        for (CompoundTag block : baseData.getAll()) {
            String name = block.getString("name");
            ArrayList<Tag> blockStates = new ArrayList<>(block.getCompound("states").getAllTags());

            List<CompoundTag> candidates = neteaseByName.getOrDefault(name, Collections.emptyList());
            if (candidates.isEmpty()) {
                log.warn("NetEase 调色板中找不到方块: {} data={}", name, block.getInt("data"));
                unmatchCount++;
                continue;
            }

            CompoundTag matched = null;
            for (CompoundTag candidate : candidates) {
                CompoundTag candidateStates = candidate.getCompound("states");
                if (candidateStates.equals(block.getCompound("states"))) {
                    matched = candidate;
                    break;
                }
                if (candidateStates.isEmpty() && blockStates.isEmpty()) {
                    matched = candidate;
                    break;
                }
                boolean equals = true;
                for (Tag tag : blockStates) {
                    if (!candidateStates.contains(tag.getName())) {
                        equals = false;
                        break;
                    }
                    Object o1 = candidateStates.get(tag.getName()).parseValue();
                    Object o2 = tag.parseValue();
                    if (o1 instanceof Number) {
                        if (!compare(o1, o2)) {
                            equals = false;
                            break;
                        }
                    } else if (!o1.equals(o2)) {
                        equals = false;
                        break;
                    }
                }
                if (equals) {
                    matched = candidate;
                    break;
                }
            }

            if (matched != null) {
                CompoundTag copy = block.copy();
                copy.putInt("runtimeId", matched.getInt("runtimeId"));
                copy.putInt("version", matched.getInt("version"));
                if (!copy.contains("states")) {
                    copy.putCompound("states", matched.getCompound("states").copy());
                }
                // 去重检查
                boolean isDuplicate = false;
                int runtimeId = matched.getInt("runtimeId");
                for (CompoundTag existing : result.getAll()) {
                    if (existing.getInt("runtimeId") == runtimeId
                            && existing.getString("name").equals(name)
                            && existing.getInt("id") == copy.getInt("id")
                            && existing.getInt("data") == copy.getInt("data")) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (!isDuplicate) {
                    result.add(copy);
                    matchCount++;
                }
            } else {
                log.warn("未匹配到 NetEase runtimeId: {} data={} states={}", name, block.getInt("data"), block.getCompound("states").toSNBT());
                unmatchCount++;
            }
        }

        log.info("Phase 1 匹配结果: 成功={}, 失败={}", matchCount, unmatchCount);

        // Phase 1 结果中已有的方块名集合
        Set<String> phase1Names = new HashSet<>();
        for (CompoundTag r : result.getAll()) {
            phase1Names.add(r.getString("name"));
        }

        // 找出 NE 766 调色板中存在但 Phase 1 结果中缺失的方块名
        Set<String> missingNames = new LinkedHashSet<>();
        for (String name : neteaseByName.keySet()) {
            if (!phase1Names.contains(name)) {
                missingNames.add(name);
            }
        }
        log.info("Phase 1 后缺失的方块名数量: {}", missingNames.size());
        for (String name : missingNames) {
            log.info("  缺失: {} ({}个状态)", name, neteaseByName.get(name).size());
        }

        // ===================== Phase 2: 从上一版网易数据补充匹配 =====================
        if (!missingNames.isEmpty()) {
            String prevNePath = "src/main/resources/Target_Data/runtime_block_states_netease_" + prevNeVersion + ".dat";
            ListTag<CompoundTag> prevNeData;
            try {
                prevNeData = Utils.readNKBlockStates(prevNePath);
            } catch (IOException e) {
                log.warn("无法加载上一版网易数据 {}: {}", prevNePath, e.getMessage());
                prevNeData = new ListTag<>();
            }
            log.info("Phase 2: 加载 NE {} 数据，方块数量: {}", prevNeVersion, prevNeData.size());

            // 按名称索引 NE 686 数据
            Map<String, List<CompoundTag>> prevNeByName = new LinkedHashMap<>();
            for (CompoundTag block : prevNeData.getAll()) {
                prevNeByName.computeIfAbsent(block.getString("name"), k -> new ArrayList<>()).add(block);
            }

            // 按 block ID 索引 NE 686 数据
            Map<Integer, List<CompoundTag>> prevNeById = new LinkedHashMap<>();
            for (CompoundTag block : prevNeData.getAll()) {
                prevNeById.computeIfAbsent(block.getInt("id"), k -> new ArrayList<>()).add(block);
            }

            Map<String, Integer> blockIdMap = Utils.readBlockName2IdMap();
            Set<String> usedPrevNeKeys = new HashSet<>(); // 标记已使用的 NE 686 条目 "name:data"
            int phase2Count = 0;

            for (String missingName : new ArrayList<>(missingNames)) {
                List<CompoundTag> neEntries = neteaseByName.get(missingName);
                if (neEntries == null || neEntries.isEmpty()) continue;

                // 获取该方块的 block ID
                Integer blockId = blockIdMap.get(missingName);
                if (blockId == null) continue;

                boolean anyMatched = false;

                for (CompoundTag neEntry : neEntries) {
                    CompoundTag neStates = neEntry.getCompound("states");

                    // 优先尝试同名匹配
                    CompoundTag matchedPrev = null;
                    List<CompoundTag> sameNamePrevList = prevNeByName.getOrDefault(missingName, Collections.emptyList());
                    for (CompoundTag prev : sameNamePrevList) {
                        String key = prev.getString("name") + ":" + prev.getInt("data");
                        if (usedPrevNeKeys.contains(key)) continue;
                        if (isSubsetMatch(neStates, prev.getCompound("states"))) {
                            matchedPrev = prev;
                            break;
                        }
                    }

                    // 若同名未匹配，尝试同 ID 不同名匹配（处理重命名如 tnt→underwater_tnt）
                    if (matchedPrev == null) {
                        List<CompoundTag> sameIdPrevList = prevNeById.getOrDefault(blockId, Collections.emptyList());
                        for (CompoundTag prev : sameIdPrevList) {
                            if (prev.getString("name").equals(missingName)) continue; // 已经尝试过同名
                            String key = prev.getString("name") + ":" + prev.getInt("data");
                            if (usedPrevNeKeys.contains(key)) continue;
                            if (isSubsetMatch(neStates, prev.getCompound("states"))) {
                                matchedPrev = prev;
                                break;
                            }
                        }
                    }

                    if (matchedPrev != null) {
                        String usedKey = matchedPrev.getString("name") + ":" + matchedPrev.getInt("data");
                        usedPrevNeKeys.add(usedKey);

                        int prevId = matchedPrev.getInt("id");
                        int prevData = matchedPrev.getInt("data");

                        // 检查 id+data 是否与 Phase 1 结果冲突，如冲突则移除
                        List<CompoundTag> toRemove = new ArrayList<>();
                        for (CompoundTag existing : result.getAllUnsafe()) {
                            if (existing.getInt("id") == prevId && existing.getInt("data") == prevData) {
                                toRemove.add(existing);
                            }
                        }
                        for (CompoundTag rem : toRemove) {
                            result.getAllUnsafe().remove(rem);
                            log.info("Phase 2: 移除冲突条目 {} id={} data={}", rem.getString("name"), rem.getInt("id"), rem.getInt("data"));
                        }

                        CompoundTag copy = new CompoundTag();
                        copy.putString("name", missingName);
                        copy.putInt("id", prevId);
                        copy.putInt("data", prevData);
                        copy.putInt("runtimeId", neEntry.getInt("runtimeId"));
                        copy.putInt("version", neEntry.getInt("version"));
                        copy.putCompound("states", neStates.copy());
                        result.add(copy);
                        anyMatched = true;
                        phase2Count++;
                        log.info("Phase 2: 匹配 {} data={} runtimeId={} (来源: {} data={})",
                                missingName, prevData, neEntry.getInt("runtimeId"),
                                matchedPrev.getString("name"), matchedPrev.getInt("data"));
                    }
                }
                if (anyMatched) {
                    missingNames.remove(missingName);
                }
            }
            log.info("Phase 2 补充匹配: {}", phase2Count);
        }

        // ===================== Phase 3: 新方块处理 =====================
        if (!missingNames.isEmpty()) {
            Map<String, Integer> blockIdMap = Utils.readBlockName2IdMap();
            int phase3Count = 0;

            for (String missingName : new ArrayList<>(missingNames)) {
                Integer blockId = blockIdMap.get(missingName);
                if (blockId == null) {
                    log.warn("Phase 3: 方块 {} 在 block_ids.csv 中未找到 ID，跳过", missingName);
                    continue;
                }

                List<CompoundTag> neEntries = neteaseByName.get(missingName);
                if (neEntries == null || neEntries.isEmpty()) continue;

                int dataValue = 0;
                for (CompoundTag neEntry : neEntries) {
                    CompoundTag copy = new CompoundTag();
                    copy.putString("name", missingName);
                    copy.putInt("id", blockId);
                    copy.putInt("data", dataValue);
                    copy.putInt("runtimeId", neEntry.getInt("runtimeId"));
                    copy.putInt("version", neEntry.getInt("version"));
                    copy.putCompound("states", neEntry.getCompound("states").copy());
                    result.add(copy);
                    phase3Count++;
                    log.info("Phase 3: 新增 {} id={} data={} runtimeId={}", missingName, blockId, dataValue, neEntry.getInt("runtimeId"));
                    dataValue++;
                }
                missingNames.remove(missingName);
            }
            log.info("Phase 3 新方块添加: {}", phase3Count);
        }

        // ===================== Phase 4: 补齐尚未覆盖的 runtimeId =====================
        Map<String, Integer> blockIdMap = Utils.readBlockName2IdMap();
        Set<Integer> coveredRuntimeIds = new HashSet<>();
        Map<String, Integer> nextDataByName = new HashMap<>();
        for (CompoundTag block : result.getAll()) {
            coveredRuntimeIds.add(block.getInt("runtimeId"));
            nextDataByName.merge(block.getString("name"), block.getInt("data"), Math::max);
        }
        for (Map.Entry<String, Integer> entry : new ArrayList<>(nextDataByName.entrySet())) {
            nextDataByName.put(entry.getKey(), entry.getValue() + 1);
        }

        int phase4Count = 0;
        for (CompoundTag neEntry : neteasePalette) {
            int runtimeId = neEntry.getInt("runtimeId");
            if (coveredRuntimeIds.contains(runtimeId)) {
                continue;
            }

            String name = neEntry.getString("name");
            Integer blockId = blockIdMap.get(name);
            if (blockId == null) {
                log.warn("Phase 4: 方块 {} 在 block_ids.csv 中未找到 ID，无法补齐 runtimeId={}", name, runtimeId);
                continue;
            }

            int dataValue = nextDataByName.getOrDefault(name, 0);
            CompoundTag copy = new CompoundTag();
            copy.putString("name", name);
            copy.putInt("id", blockId);
            copy.putInt("data", dataValue);
            copy.putInt("runtimeId", runtimeId);
            copy.putInt("version", neEntry.getInt("version"));
            copy.putCompound("states", neEntry.getCompound("states").copy());
            result.add(copy);

            coveredRuntimeIds.add(runtimeId);
            nextDataByName.put(name, dataValue + 1);
            phase4Count++;
            log.info("Phase 4: 补齐 {} id={} data={} runtimeId={}", name, blockId, dataValue, runtimeId);
        }
        log.info("Phase 4 runtimeId 补齐: {}", phase4Count);

        if (!missingNames.isEmpty()) {
            log.warn("仍有 {} 个方块未处理:", missingNames.size());
            for (String name : missingNames) {
                log.warn("  未处理: {}", name);
            }
        }

        // 5. 遗漏检查
        for (CompoundTag block : baseData.getAll()) {
            String name = block.getString("name");
            int data = block.getInt("data");
            boolean found = false;
            for (CompoundTag r : result.getAll()) {
                if (r.getString("name").equals(name) && r.getInt("data") == data) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.error("遗漏: {} data={} states={}", name, data, block.getCompound("states").toSNBT());
            }
        }

        // 5b. 权威 NetEase palette 覆盖率校验
        // 与 Phase 5（只查 baseData→result）不同，这里查权威源 neteasePalette→result，
        // 确保网易 palette 中所有 runtimeId 都被生成结果覆盖。
        // 防止 TNT 这类「同 id 不同名」重命名方块因 Phase 1 同名匹配失误 + Phase 2 参数错误而静默丢失。
        Set<Integer> resultRuntimeIds = new HashSet<>();
        for (CompoundTag r : result.getAll()) {
            resultRuntimeIds.add(r.getInt("runtimeId"));
        }
        Map<String, List<Integer>> uncovered = new LinkedHashMap<>();
        for (CompoundTag neEntry : neteasePalette) {
            int rid = neEntry.getInt("runtimeId");
            if (!resultRuntimeIds.contains(rid)) {
                uncovered.computeIfAbsent(neEntry.getString("name"), k -> new ArrayList<>()).add(rid);
            }
        }
        if (uncovered.isEmpty()) {
            log.info("Phase 5b: 权威 palette 覆盖完整，{} 个 runtimeId 全部覆盖", neteasePalette.size());
        } else {
            int totalUncovered = 0;
            for (Map.Entry<String, List<Integer>> e : uncovered.entrySet()) {
                for (int rid : e.getValue()) {
                    // 找到对应的 states 用于诊断
                    String statesSnbt = neteasePalette.stream()
                            .filter(b -> b.getInt("runtimeId") == rid)
                            .map(b -> b.getCompound("states").toSNBT())
                            .findFirst().orElse("{}");
                    log.error("Phase 5b: 未覆盖 {} runtimeId={} states={}", e.getKey(), rid, statesSnbt);
                    totalUncovered++;
                }
            }
            log.error("Phase 5b: 权威 palette 覆盖不全！{} 个 runtimeId 未覆盖（共 {} 个）", totalUncovered, neteasePalette.size());
        }

        // TNT 重命名专项断言：国际版把 tnt 合并到 underwater_tnt（data 0/1=tnt, 2/3=underwater_tnt），
        // 但网易版区分两个方块。Phase 1 同名匹配容易把 data=0/1 的 TNT 错配到 underwater_tnt 的 rid。
        // 这里显式断言两个方块的 runtimeId 集合不交集，且都存在。
        Set<Integer> tntRids = new HashSet<>();
        Set<Integer> underwaterTntRids = new HashSet<>();
        for (CompoundTag r : result.getAll()) {
            String n = r.getString("name");
            if ("minecraft:tnt".equals(n)) {
                tntRids.add(r.getInt("runtimeId"));
            } else if ("minecraft:underwater_tnt".equals(n)) {
                underwaterTntRids.add(r.getInt("runtimeId"));
            }
        }
        if (tntRids.isEmpty()) {
            log.error("Phase 5b: minecraft:tnt 缺失！国际版 underwater_tnt 重命名可能未被 Phase 2 修复");
        } else if (underwaterTntRids.isEmpty()) {
            log.error("Phase 5b: minecraft:underwater_tnt 缺失！");
        } else {
            Set<Integer> intersection = new HashSet<>(tntRids);
            intersection.retainAll(underwaterTntRids);
            if (!intersection.isEmpty()) {
                log.error("Phase 5b: minecraft:tnt 与 minecraft:underwater_tnt 的 runtimeId 重复！{}",
                        intersection);
            } else {
                log.info("Phase 5b: TNT 重命名断言通过 (tnt rids={}, underwater_tnt rids={})",
                        tntRids, underwaterTntRids);
            }
        }

        // 6. 按名称 + data 排序
        Comparator<CompoundTag> c1 = Comparator.comparing(o -> o.getString("name"));
        Comparator<CompoundTag> comparator = c1.thenComparingInt(o -> o.getInt("data"));
        result.getAllUnsafe().sort(comparator);

        // 保存
        String outputPath = "src/main/resources/Target_Data/runtime_block_states_netease_" + targetVersion + ".dat";
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            NBTIO1.writeGZIPCompressed(result, outputStream, ByteOrder.BIG_ENDIAN);
        }

        log.info("NetEase 数据已保存: {}, 方块数量: {}", outputPath, result.size());
    }

}
