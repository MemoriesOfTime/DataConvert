package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.convert.data.*;
import cn.lanink.dataconvert.convert.data.stairs.BlockStairsDataConvert;
import cn.lanink.dataconvert.utils.HashedPaletteComparator;
import cn.lanink.dataconvert.utils.NBTIO1;
import cn.lanink.dataconvert.utils.Utils;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.network.protocol.ProtocolInfo;
import com.google.common.io.ByteStreams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

@Log4j2
public class RuntimeBlockStateConvertOld {

    public static void main(String[] args) throws IOException {

        if (args.length > 0) {
            for (String arg : args) {
                boolean isNetEase = arg.startsWith("netease:");
                String version = isNetEase ? arg.substring("netease:".length()) : arg;
                convert(Integer.parseInt(version), isNetEase);
            }

            log.info("convert blockstate success");
            System.exit(0);
            return;
        }

//        convert(407);
//
//        convert(419);
//        convert(428);
//
//        convert(440);
//        convert(448);
//        convert(465);
//        convert(471);
//        convert(486);
//        convert(503);
//        convert(527);
//
//        convert(544);
//        convert(560);
//        convert(567);
//        convert(575);
//        convert(582);
//
//        convert(589);
//        convert(594);
//
//        convert(618);
//        convert(622);

//        convert(630);
//        convert(649);
//        convert(662);
//
//        convert(671);
//        convert(685);
//        convert(712);
//        convert(729);
//        convert(748);
//        convert(766);
//        convert(776);
//        convert(786);
//        convert(800);
//        convert(818);
//        convert(827);
//        convert(844);
//        convert(944);

//        convert(630, true);
//        convert(686, true);
//        convert(766, true);

//        convert(766);
//        convert(776);
//        convert(786);
//        convert(800);
//        convert(818);
//        convert(827);
//        convert(844);
//        convert(944);

        log.info("convert blockstate success");
        System.exit(0);
    }

    public static void convert(int oldBlockStatesVersion) throws IOException {
        convert(oldBlockStatesVersion, false);
    }

    public static void convert(int oldBlockStatesVersion, boolean isNetEase) throws IOException {
        HashMap<String, BlockDataConvert> dataConvertMap = new HashMap<>();

        // P0 missing blocks
        dataConvertMap.put("minecraft:bamboo_door", new BlockDoorDataConvert());
        dataConvertMap.put("minecraft:chiseled_bookshelf", new BlockChiseledBookshelfDataConvert());
        dataConvertMap.put("minecraft:crafter", new BlockCrafterDataConvert());
        registerLightningRodDataConverters(dataConvertMap);
        registerShelfDataConverters(dataConvertMap);
        registerCopperChestDataConverters(dataConvertMap);
        registerCopperGolemStatueDataConverters(dataConvertMap);
        registerCalibratedSculkSensorDataConverters(dataConvertMap);

        // Pale Garden & Resin 系列 - 766+
        registerPaleGardenDataConverters(dataConvertMap);


        // 竹栅栏 (Bamboo Fence) - 560+
//        dataConvertMap.put("minecraft:bamboo_fence", new BlockEmptyStatesDataConvert());

        // 凝灰岩系列 (Tuff) - 630+
        // 基础方块 - 无状态
//        dataConvertMap.put("minecraft:polished_tuff", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:chiseled_tuff", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:tuff_bricks", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:chiseled_tuff_bricks", new BlockEmptyStatesDataConvert());
//        // 台阶
//        dataConvertMap.put("minecraft:tuff_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:tuff_double_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:polished_tuff_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:polished_tuff_double_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:tuff_brick_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:tuff_brick_double_slab", new BlockDoubleSlabDataConvert());
//        // 楼梯
//        dataConvertMap.put("minecraft:tuff_stairs", new BlockStairsDataConvert());
//        dataConvertMap.put("minecraft:polished_tuff_stairs", new BlockStairsDataConvert());
//        dataConvertMap.put("minecraft:tuff_brick_stairs", new BlockStairsDataConvert());
//        // 墙
//        dataConvertMap.put("minecraft:tuff_wall", new BlockWallDataConvert());
//        dataConvertMap.put("minecraft:polished_tuff_wall", new BlockWallDataConvert());
//        dataConvertMap.put("minecraft:tuff_brick_wall", new BlockWallDataConvert());

        // 苍白橡木系列 (Pale Oak) - 766+
//        dataConvertMap.put("minecraft:pale_oak_button", new BlockButtonDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_fence", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_fence_gate", new BlockFenceGateDataConvert());
//        dataConvertMap.put("minecraft:stripped_pale_oak_log", new BlockStrippedLogDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_log", new BlockLogDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_planks", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_pressure_plate", new BlockPressurePlateDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_double_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_stairs", new BlockStairsDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_standing_sign", new BlockStandingSignDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_wall_sign", new BlockWallSignDataConvert());
//        dataConvertMap.put("minecraft:stripped_pale_oak_wood", new BlockStrippedLogDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_wood", new BlockWoodDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_sapling", new BlockSaplingDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_leaves", new BlockLeavesDataConvert());

        // 悬挂告示牌系列 (Hanging Sign) - 从 560+ 版本开始支持
        // 橡木、云杉、白桦、丛林、金合欢、深色橡木 - 560+
        // 绯红、诡异、红树 - 560+
        // 竹子 - 575+
        // 樱花 - 594+
        // 苍白橡木 - 766+
//        dataConvertMap.put("minecraft:oak_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:spruce_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:birch_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:jungle_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:acacia_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:dark_oak_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:crimson_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:warped_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:mangrove_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:bamboo_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:cherry_hanging_sign", new BlockHangingSignDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_hanging_sign", new BlockHangingSignDataConvert());

//        // 雕纹铜块系列 (Chiseled Copper) - 无状态方块
//        dataConvertMap.put("minecraft:chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:exposed_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:weathered_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:oxidized_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_oxidized_chiseled_copper", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_chiseled_copper", new BlockEmptyStatesDataConvert());
//
//        // 铜格栅系列 (Copper Grate) - 无状态方块
//        dataConvertMap.put("minecraft:copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:exposed_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:weathered_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:oxidized_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_copper_grate", new BlockEmptyStatesDataConvert());
//        dataConvertMap.put("minecraft:waxed_oxidized_copper_grate", new BlockEmptyStatesDataConvert());
//
//        // 铜活板门系列 (Copper Trapdoor)
//        dataConvertMap.put("minecraft:copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:exposed_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:weathered_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:oxidized_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_copper_trapdoor", new BlockTrapdoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_oxidized_copper_trapdoor", new BlockTrapdoorDataConvert());

//        dataConvertMap.put("minecraft:waxed_exposed_cut_copper_stairs", new BlockStairsDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_cut_copper_stairs", new BlockStairsDataConvert());
//        dataConvertMap.put("minecraft:cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:double_cut_copper_slab", new BlockDoubleSlabDataConvert());

        //440-527
//        dataConvertMap.put("minecraft:exposed_cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:weathered_cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:oxidized_cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_cut_copper_slab", new BlockSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_cut_copper_slab", new BlockSlabDataConvert());
//
//        dataConvertMap.put("minecraft:exposed_double_cut_copper_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:weathered_double_cut_copper_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:oxidized_double_cut_copper_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_double_cut_copper_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_double_cut_copper_slab", new BlockDoubleSlabDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_double_cut_copper_slab", new BlockDoubleSlabDataConvert());

//        dataConvertMap.put("minecraft:acacia_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:spruce_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:bamboo_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:birch_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:cherry_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:crimson_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:dark_oak_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:exposed_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:jungle_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:mangrove_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:oxidized_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:pale_oak_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:warped_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_exposed_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_oxidized_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:waxed_weathered_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:weathered_copper_door", new BlockDoorDataConvert());
//        dataConvertMap.put("minecraft:wooden_door", new BlockDoorDataConvert());

//        dataConvertMap.put("minecraft:coral", new BlockCoralDataConvert());
//
//        dataConvertMap.put("minecraft:fire_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:brain_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:tube_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:horn_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:bubble_coral", new BlockCoralDataConvert());
//
//        dataConvertMap.put("minecraft:dead_fire_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:dead_brain_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:dead_tube_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:dead_horn_coral", new BlockCoralDataConvert());
//        dataConvertMap.put("minecraft:dead_bubble_coral", new BlockCoralDataConvert());



        String file = "src/main/resources/Target_Data/runtime_block_states_" + oldBlockStatesVersion + ".dat";
        if (isNetEase) {
            file = "src/main/resources/Target_Data/runtime_block_states_netease_" + oldBlockStatesVersion + ".dat";
        }
        ListTag<CompoundTag> oldBaseListTag = Utils.readNKBlockStates(file);

        Map<String, Integer> persistenceNameToBlockId = Utils.readBlockName2IdMap();

        //加载PMMP的数据
        List<CompoundTag> originTags = new ArrayList<>();
        List<CompoundTag> tags = new ArrayList<>();
        ListTag<CompoundTag> tags2 = null;
        if (oldBlockStatesVersion >= 419) {
            if (isNetEase) {
                // 网易方块数据
                if (oldBlockStatesVersion >= 766) {
                    tags = Utils.readNeteasePaletteBlocks("src/main/resources/NetEase_Data/block_palette_" + oldBlockStatesVersion + ".nbt");
                } else {
                    tags = Utils.readECBlockStates("src/main/resources/NetEase_Data/runtime_block_states_" + oldBlockStatesVersion + ".dat");
                }
                // 保存原始数据副本用于 vanilla_palette 输出
                for (CompoundTag tag : tags) {
                    CompoundTag originTag = tag.copy();
                    // 移除处理过程中添加的字段，保留原始数据
                    originTag.remove("runtimeId");
                    originTag.remove("id");
                    originTags.add(originTag);
                }
            } else {
                // PMMP 844 数据不包含 shelf 系列方块，使用 CB 数据
                if (oldBlockStatesVersion == 844) {
                    tags = Utils.readCBBlockStates("src/main/resources/CB_Data/block_palette_" + oldBlockStatesVersion + ".nbt");
                    for (CompoundTag tag : tags) {
                        originTags.add(tag.copy());
                    }
                } else {
                    try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states_" + oldBlockStatesVersion + ".nbt")) {
                        try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                            int runtimeId = 0;
                            while (bis.available() > 0) {
                                CompoundTag tag = NBTIO1.read(bis, ByteOrder.BIG_ENDIAN, true);
                                originTags.add(tag.copy());
                                tag.putInt("runtimeId", runtimeId++);
                                String name = tag.getString("name").toLowerCase();
                                Integer id = persistenceNameToBlockId.getOrDefault(name, -1);
                                tag.putInt("id", id);
                                if (id == -1) {
                                    //log.error(oldBlockStatesVersion + " Unable to find block id for " + name);
                                }
                                tags.add(tag);
                            }
                        }
                    } catch (IOException e) {
                        tags = Utils.readCBBlockStates("src/main/resources/CB_Data/block_palette_" + oldBlockStatesVersion + ".nbt");
                        for (CompoundTag tag : tags) {
                            originTags.add(tag.copy());
                        }
                    }
                }
            }
        } else {
            try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/required_block_states_" + oldBlockStatesVersion + ".nbt")) {
                //noinspection unchecked
                tags2 = (ListTag<CompoundTag>) NBTIO.readTag(new ByteArrayInputStream(ByteStreams.toByteArray(stream)), ByteOrder.BIG_ENDIAN, true);
            } catch (IOException e) {
                throw new AssertionError("Unable to locate runtime_block_states_" + oldBlockStatesVersion + ".dat", e);
            }
        }

        if (tags2 != null) {
            int runtimeId = 0;
            for (CompoundTag tag : tags2.getAll()) {
                CompoundTag block = tag.getCompound("block");
                block.putInt("runtimeId", runtimeId++);
                String name = block.getString("name").toLowerCase();
                Integer id = persistenceNameToBlockId.getOrDefault(name, -1);
                block.putInt("id", id);
                tags.add(block);
            }
        }

        for (CompoundTag compoundTag : tags) {
            //log.info(compoundTag.toSNBT());
        }

        ListTag<CompoundTag> newTagList = new ListTag<>();

        for (CompoundTag block : tags) {
            String name = block.getString("name");
            if (dataConvertMap.containsKey(name)) {
                newTagList.add(dataConvertMap.get(name).convert(oldBlockStatesVersion, block));
            }
        }

        int v = -1;
        for (CompoundTag tag : oldBaseListTag.getAll()) {
            String name = tag.getString("name");
            if (v == -1) {
                v = tag.getInt("version");
            }
            if (oldBlockStatesVersion < 419) {
                CompoundTag tag1 = tag.getCompound("block");
                name = tag1.getString("name");
                if (v == -1) {
                    v = tag1.getInt("version");
                }
            }

            if (dataConvertMap.containsKey(name)) {
                continue;
            }

            //重复检查
            int runtimeId = tag.getInt("runtimeId");
            boolean isDuplicate = false;
            for (CompoundTag tag1 : newTagList.getAll()) {
                if (oldBlockStatesVersion >= 419) {
                    if (tag1.getInt("runtimeId") == runtimeId
                            && tag1.getString("name").equals(tag.getString("name"))
                            && tag1.getInt("id") == tag.getInt("id")
                            && tag1.getInt("data") == tag.getInt("data")) {
                        isDuplicate = true;
                        log.warn("Duplicate : old:{}\nnew:{}", tag1, tag);
                    }
                } else {
                    CompoundTag block1 = tag1.getCompound("block");
                    CompoundTag block = tag.getCompound("block");
                    if (tag1.getInt("id") == tag.getInt("id")
                            && tag1.getShort("data") == tag.getShort("data")
                            && block1.getString("name").equals(block.getString("name"))) {
                        isDuplicate = true;
                        log.warn("Duplicate : old:{}\nnew:{}", tag1, tag);
                    }

                }
            }
            if (!isDuplicate) {
                newTagList.add(tag);
            }
        }

        //按照名称和data排序
        if (oldBlockStatesVersion >= 419) {
            //Comparator<CompoundTag> c1 = Comparator.comparingInt(o -> o.getInt("runtimeId"));
            Comparator<CompoundTag> c1 = Comparator.comparing(o -> o.getString("name"));
            Comparator<CompoundTag> comparator = c1.thenComparingInt(o -> o.getInt("data"));
            newTagList.getAllUnsafe().sort(comparator);
        } else {
            newTagList.getAllUnsafe().sort((o1, o2) -> {
                if (!o1.contains("block") || !o2.contains("block")) {
                    return 0;
                }
                String o1name = o1.getCompound("block").getString("name");
                String o2name = o2.getCompound("block").getString("name");
                if (o1name.isBlank() || o2name.isBlank()) {
                    throw new RuntimeException();
                }
                return o1name.compareToIgnoreCase(o2name);
            });
        }

        //保存文件
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/new/runtime_block_states_" + (isNetEase ? "netease_" : "") + oldBlockStatesVersion + ".dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);

        ListTag<CompoundTag> originTagList = new ListTag<>();
        for (CompoundTag tag : originTags) {
            originTagList.add(tag);
        }
        outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/vanilla_palette_" + (isNetEase ? "netease_" : "") + oldBlockStatesVersion + ".nbt"));
        NBTIO1.writeGZIPCompressed(new CompoundTag().put("blocks", originTagList), outputStream, ByteOrder.BIG_ENDIAN);

    }
    private static void registerLightningRodDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        String[] lightningRods = {
                "minecraft:lightning_rod",
                "minecraft:exposed_lightning_rod",
                "minecraft:weathered_lightning_rod",
                "minecraft:oxidized_lightning_rod",
                "minecraft:waxed_lightning_rod",
                "minecraft:waxed_exposed_lightning_rod",
                "minecraft:waxed_weathered_lightning_rod",
                "minecraft:waxed_oxidized_lightning_rod"
        };
        for (String lightningRod : lightningRods) {
            dataConvertMap.put(lightningRod, new BlockLightningRodDataConvert());
        }
    }

    private static void registerShelfDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        String[] shelves = {
                "minecraft:oak_shelf",
                "minecraft:spruce_shelf",
                "minecraft:birch_shelf",
                "minecraft:jungle_shelf",
                "minecraft:acacia_shelf",
                "minecraft:dark_oak_shelf",
                "minecraft:mangrove_shelf",
                "minecraft:cherry_shelf",
                "minecraft:pale_oak_shelf",
                "minecraft:bamboo_shelf",
                "minecraft:crimson_shelf",
                "minecraft:warped_shelf"
        };
        for (String shelf : shelves) {
            dataConvertMap.put(shelf, new BlockShelfDataConvert());
        }
    }

    private static void registerCopperChestDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        String[] copperChests = {
                "minecraft:copper_chest",
                "minecraft:exposed_copper_chest",
                "minecraft:weathered_copper_chest",
                "minecraft:oxidized_copper_chest",
                "minecraft:waxed_copper_chest",
                "minecraft:waxed_exposed_copper_chest",
                "minecraft:waxed_weathered_copper_chest",
                "minecraft:waxed_oxidized_copper_chest"
        };
        for (String chest : copperChests) {
            dataConvertMap.put(chest, new BlockChestCopperDataConvert());
        }
    }

    private static void registerCopperGolemStatueDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        String[] statues = {
                "minecraft:copper_golem_statue",
                "minecraft:exposed_copper_golem_statue",
                "minecraft:weathered_copper_golem_statue",
                "minecraft:oxidized_copper_golem_statue",
                "minecraft:waxed_copper_golem_statue",
                "minecraft:waxed_exposed_copper_golem_statue",
                "minecraft:waxed_weathered_copper_golem_statue",
                "minecraft:waxed_oxidized_copper_golem_statue"
        };
        for (String statue : statues) {
            dataConvertMap.put(statue, new BlockCopperGolemStatueDataConvert());
        }
    }

    private static void registerCalibratedSculkSensorDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        dataConvertMap.put("minecraft:calibrated_sculk_sensor", new BlockCalibratedSculkSensorDataConvert());
    }

    private static void registerPaleGardenDataConverters(HashMap<String, BlockDataConvert> dataConvertMap) {
        dataConvertMap.put("minecraft:pale_moss_block", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:pale_moss_carpet", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:pale_hanging_moss", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:creaking_heart", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:resin_bricks", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:resin_brick_slab", new BlockSlabDataConvert());
        dataConvertMap.put("minecraft:resin_brick_double_slab", new BlockDoubleSlabDataConvert());
        dataConvertMap.put("minecraft:resin_brick_stairs", new BlockStairsDataConvert());
        dataConvertMap.put("minecraft:resin_brick_wall", new BlockWallDataConvert());
        dataConvertMap.put("minecraft:open_eyeblossom", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:closed_eyeblossom", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:chiseled_resin_bricks", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:resin_block", new BlockEmptyStatesDataConvert());
        dataConvertMap.put("minecraft:resin_clump", new BlockEmptyStatesDataConvert());
    }


    private static List<CompoundTag> getBlockByName(ListTag<CompoundTag> tags, String name, Integer meta) {
        ArrayList<CompoundTag> list = new ArrayList<>();
        for (CompoundTag block : tags.getAll()) {
            if (block.getString("name").equals(name)
                    && (meta == null || block.getShort("data") == meta)) {
                list.add(block);
            }
        }
        return list;
    }

    private static int convertFacingDirectionToDirection(int facingDirection) {
        switch (facingDirection) {
            case 2:
                return 2;
            case 3:
            default:
                return 0;
            case 4:
                return 1;
            case 5:
                return 3;
        }
    }
}
