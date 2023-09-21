package cn.lanink.dataconvert.utils;


import cn.nukkit.nbt.tag.CompoundTag;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * 检查方块状态是否存在的工具类
 */
@Log4j2
public class CheckBlockStatesCount {

    public static void main(String[] args) {
        // checkBlockStatesCount("minecraft:stone");  // 示例：检查 stone 方块
    }


    /**
     * 对比 Nukkit-MOT 当前的 netease dat 与 DataConvert 生成的 netease dat 中的 runtimeId
     */
    public static void compareNkMotFile(String blockName, int version) {
        if (!blockName.startsWith("minecraft:")) {
            blockName = "minecraft:" + blockName.toLowerCase().replace(" ", "_");
        }
        String dcFile = "src/main/resources/Target_Data/runtime_block_states_netease_" + version + ".dat";
        try {
            String nkMotFile = ExternalData.nukkitMotNeteaseRuntimeBlockStates(version).getPath();
            List<CompoundTag> nkMotBlocks = Utils.readNKBlockStates(nkMotFile).getAll();
            List<CompoundTag> dcBlocks = Utils.readNKBlockStates(dcFile).getAll();

            log.info("===== 对比 NkMOT vs DataConvert netease_{}.dat =====", version);
            log.info("NkMOT 总方块数: {}, DataConvert 总方块数: {}", nkMotBlocks.size(), dcBlocks.size());

            String finalBlockName = blockName;
            List<CompoundTag> nkMotTnt = nkMotBlocks.stream()
                    .filter(t -> t.getString("name").equals(finalBlockName)).toList();
            List<CompoundTag> dcTnt = dcBlocks.stream()
                    .filter(t -> t.getString("name").equals(finalBlockName)).toList();

            log.info("NkMOT {} ({} 条):", blockName, nkMotTnt.size());
            for (CompoundTag t : nkMotTnt) {
                log.info("  runtimeId={}, data={}, id={}, states={}", t.getInt("runtimeId"), t.getInt("data"), t.getInt("id"), t.getCompound("states").toSNBT());
            }
            log.info("DataConvert {} ({} 条):", blockName, dcTnt.size());
            for (CompoundTag t : dcTnt) {
                log.info("  runtimeId={}, data={}, id={}, states={}", t.getInt("runtimeId"), t.getInt("data"), t.getInt("id"), t.getCompound("states").toSNBT());
            }

            // 对比 runtimeId
            boolean allMatch = true;
            for (int i = 0; i < Math.min(nkMotTnt.size(), dcTnt.size()); i++) {
                int nkRid = nkMotTnt.get(i).getInt("runtimeId");
                int dcRid = dcTnt.get(i).getInt("runtimeId");
                int nkData = nkMotTnt.get(i).getInt("data");
                int dcData = dcTnt.get(i).getInt("data");
                if (nkRid != dcRid || nkData != dcData) {
                    log.error("不匹配! data={}: NkMOT runtimeId={} vs DataConvert runtimeId={}", nkData, nkRid, dcRid);
                    allMatch = false;
                }
            }
            if (allMatch && nkMotTnt.size() == dcTnt.size()) {
                log.info("runtimeId 完全一致");
            }
        } catch (Exception e) {
            log.error("对比失败", e);
        }
    }

    /**
     * 检查生成的网易数据中方块的 runtimeId 是否与 NetEase 调色板一致
     */
    public static void checkRuntimeId(String blockName, int version) {
        if (!blockName.startsWith("minecraft:")) {
            blockName = "minecraft:" + blockName.toLowerCase().replace(" ", "_");
        }
        try {
            // 读取 NetEase 调色板（权威数据源）
            List<CompoundTag> palette;
            String paletteFile = "src/main/resources/NetEase_Data/block_palette_" + version + ".nbt";
            try {
                palette = Utils.readNeteasePaletteBlocks(paletteFile);
            } catch (Exception e) {
                palette = Utils.readCBBlockStates(paletteFile);
            }

            // 读取生成的网易数据
            List<CompoundTag> generated = Utils.readNKBlockStates(
                    "src/main/resources/Target_Data/runtime_block_states_netease_" + version + ".dat").getAll();

            log.info("===== runtimeId 检查: {} (版本 {}) =====", blockName, version);
            String finalBlockName = blockName;
            List<CompoundTag> paletteBlocks = palette.stream()
                    .filter(t -> t.getString("name").equals(finalBlockName)).toList();
            List<CompoundTag> generatedBlocks = generated.stream()
                    .filter(t -> t.getString("name").equals(finalBlockName)).toList();

            log.info("NetEase 调色板中 {} 条:", paletteBlocks.size());
            for (CompoundTag t : paletteBlocks) {
                log.info("  runtimeId={}, states={}", t.getInt("runtimeId"), t.getCompound("states").toSNBT());
            }
            log.info("生成文件中 {} 条:", generatedBlocks.size());
            for (CompoundTag t : generatedBlocks) {
                log.info("  runtimeId={}, data={}, states={}", t.getInt("runtimeId"), t.getInt("data"), t.getCompound("states").toSNBT());
            }

            // 对比
            boolean allMatch = true;
            for (CompoundTag gen : generatedBlocks) {
                int genRid = gen.getInt("runtimeId");
                boolean found = paletteBlocks.stream().anyMatch(p -> p.getInt("runtimeId") == genRid);
                if (!found) {
                    log.error("生成的 runtimeId={} 在调色板中不存在! data={}, states={}",
                            genRid, gen.getInt("data"), gen.getCompound("states").toSNBT());
                    allMatch = false;
                }
            }
            if (allMatch) {
                log.info("所有 runtimeId 匹配正确");
            }
        } catch (Exception e) {
            log.error("检查失败", e);
        }
    }

    public static void checkBlockStatesCount(@NotNull String blockName) {
        blockName = blockName.toLowerCase().replace(" ", "_");
        if (!blockName.startsWith("minecraft:")) {
            blockName = "minecraft:" + blockName;
        }
        Map<Integer, Integer> blockStatesCountPM = new Int2ObjectOpenHashMap<>();
        Map<Integer, Integer> blockStatesCountMOT = new Int2ObjectOpenHashMap<>();

        List<String> suspectedParts = new ArrayList<>();
        List<String> properParts = new ArrayList<>();
        @NotNull String finalBlockName = blockName;
        for (File file : Objects.requireNonNull(new File("src/main/resources/Target_Data").listFiles())) {
            //TODO 388
            if (file.getName().equals("runtime_block_states_388.dat")) {
                continue;
            }
            if (!file.isFile() || !file.getName().contains("runtime_block_states_")) {
                continue;
            }

            boolean isNetEase = file.getName().contains("netease");

            try {
                int version;
                if (isNetEase) {
                    version = Integer.parseInt(file.getName().replace("runtime_block_states_netease_", "").replace(".dat", ""));
                } else {
                    version = Integer.parseInt(file.getName().replace("runtime_block_states_", "").replace(".dat", ""));
                }
                log.info("---------------- version: {}----------------", version);
                List<CompoundTag> motFind = Utils.readNKBlockStates(file.getAbsolutePath()).getAll().stream()
                        .filter(tag -> tag.getString("name").equals(finalBlockName)).toList();

                List<CompoundTag> pmFind;
                if (isNetEase) {
                    if (version >= 766) {
                        pmFind = Utils.readNeteasePaletteBlocks("src/main/resources/NetEase_Data/block_palette_" + version + ".nbt").stream()
                                .filter(tag -> tag.getString("name").equals(finalBlockName)).toList();
                    } else {
                        String neteaseFile = "src/main/resources/NetEase_Data/runtime_block_states_" + version + ".dat";
                        pmFind = Utils.readECBlockStates(neteaseFile).stream()
                                .filter(tag -> tag.getString("name").equals(finalBlockName)).toList();
                    }
                } else {
                    String pmFile = "src/main/resources/PMMP_Data/canonical_block_states_" + version + ".nbt";
                    if (version <= 407) {
                        pmFile = "src/main/resources/PMMP_Data/required_block_states_" + version + ".nbt";
                    }
                    try {
                        pmFind = Utils.readPMBlockStates(pmFile).stream()
                                .filter(tag -> tag.getString("name").equals(finalBlockName)).toList();
                    } catch (FileNotFoundException e) {
                        String cbFile = "src/main/resources/CB_Data/block_palette_" + version + ".nbt";
                        pmFind = Utils.readCBBlockStates(cbFile).stream()
                                .filter(tag -> tag.getString("name").equals(finalBlockName)).toList();
                    }
                }

                int motFindSize = motFind.size();
                int pmFindSize = pmFind.size();
                blockStatesCountMOT.put(version, motFindSize);
                blockStatesCountPM.put(version, pmFindSize);
                if (motFindSize != pmFindSize) {
                    suspectedParts.add(
                            "[版本: " + version + (isNetEase ? " (NetEase)" : "") + "] PM数量: " + pmFindSize + ", MOT数量: " + motFindSize
                    );
                } else {
                    properParts.add(
                            "[版本: " + version + (isNetEase ? " (NetEase)" : "") + "] PM数量: " + pmFindSize + ", MOT数量: " + motFindSize + ", 方块状态是否一致: " + isEqual(pmFind, getPMTags(motFind))
                    );
                }
            } catch (Exception e) {
                log.error("Error: ", e);
            }
        }
        log.info("------------ Summary ------------");
        log.info("block: {}", blockName);
        log.info("PM: {}", blockStatesCountPM);
        log.info("NK_MOT: {}", blockStatesCountMOT);
        log.warn("差异版本: ");
        for (String s : suspectedParts) {
            log.warn(s);
        }
        log.info("相同版本: ");
        for (String s : properParts) {
            log.info(s);
        }
    }

    public static boolean isEqual(List<CompoundTag> pmTags, List<CompoundTag> motTags) {
        pmTags = getOrderedCompoundTagList(pmTags);
        motTags = getOrderedCompoundTagList(motTags);
        if (pmTags.size() != motTags.size()) {
            return false;
        }

        int index = 0;
        for (CompoundTag pmTag : pmTags) {
            CompoundTag motTag = motTags.get(index);
            if (!motTag.equals(pmTag)) {
                log.warn("compare failed");
                log.warn("== PM ==");
                log.warn(pmTag.toSNBT());
                log.warn("== MOT ==");
                log.warn(motTag.toSNBT());
                return false;
            }
            index++;
        }
        return true;
    }

    public static List<CompoundTag> getOrderedCompoundTagList(List<CompoundTag> originalList) {
        return originalList.stream().sorted(Comparator.comparingInt(o -> o.getInt("runtimeId"))).toList();
    }

    public static List<CompoundTag> getPMTags(List<CompoundTag> motTags) {
        List<CompoundTag> outputs = new ArrayList<>();
        for (CompoundTag pmTag : motTags) {
            CompoundTag newTag = pmTag.clone();
            newTag.remove("stateOverload");
            newTag.remove("data");
            outputs.add(newTag);
        }
        return outputs;
    }

    /**
     * 检查 tnt 相关方块在各调色板中的情况
     */
    public static void checkTntInPalettes(int version) {
        try {
            // NetEase 调色板
            String neteaseFile = "src/main/resources/NetEase_Data/block_palette_" + version + ".nbt";
            List<CompoundTag> neteasePalette;
            try {
                neteasePalette = Utils.readNeteasePaletteBlocks(neteaseFile);
            } catch (Exception e) {
                neteasePalette = Utils.readCBBlockStates(neteaseFile);
            }

            // 国际版 PMMP 调色板
            List<CompoundTag> pmPalette = Utils.readPMBlockStates("src/main/resources/PMMP_Data/canonical_block_states_" + version + ".nbt");

            // 生成的网易数据
            List<CompoundTag> generatedNetease = Utils.readNKBlockStates(
                    "src/main/resources/Target_Data/runtime_block_states_netease_" + version + ".dat").getAll();

            // 国际版生成数据
            List<CompoundTag> generatedIntl = Utils.readNKBlockStates(
                    "src/main/resources/Target_Data/runtime_block_states_" + version + ".dat").getAll();

            log.info("===== tnt/underwater_tnt 在各数据源中的情况 (版本 {}) =====", version);

            log.info("--- NetEase 调色板 ({} 方块) ---", neteasePalette.size());
            for (CompoundTag b : neteasePalette) {
                String name = b.getString("name");
                if (name.contains("tnt")) {
                    log.info("  name={}, runtimeId={}, states={}", name, b.getInt("runtimeId"), b.getCompound("states").toSNBT());
                }
            }

            log.info("--- PMMP 国际版调色板 ({} 方块) ---", pmPalette.size());
            for (CompoundTag b : pmPalette) {
                String name = b.getString("name");
                if (name.contains("tnt")) {
                    log.info("  name={}, runtimeId={}, states={}", name, b.getInt("runtimeId"), b.getCompound("states").toSNBT());
                }
            }

            log.info("--- 生成的网易数据 ({} 方块) ---", generatedNetease.size());
            for (CompoundTag b : generatedNetease) {
                String name = b.getString("name");
                if (name.contains("tnt")) {
                    log.info("  name={}, runtimeId={}, data={}, id={}, states={}", name, b.getInt("runtimeId"), b.getInt("data"), b.getInt("id"), b.getCompound("states").toSNBT());
                }
            }

            log.info("--- 生成的国际版数据 ({} 方块) ---", generatedIntl.size());
            for (CompoundTag b : generatedIntl) {
                String name = b.getString("name");
                if (name.contains("tnt")) {
                    log.info("  name={}, runtimeId={}, data={}, id={}, states={}", name, b.getInt("runtimeId"), b.getInt("data"), b.getInt("id"), b.getCompound("states").toSNBT());
                }
            }
        } catch (Exception e) {
            log.error("检查失败", e);
        }
    }

}
