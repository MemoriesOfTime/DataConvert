package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.lanink.dataconvert.utils.Utils;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * 利用pmmp的方块数据更新nk-mot的方块数据
 */
@Log4j2
public class RuntimeBlockStateConvert {

    public static void convert(int oldBlockStatesVersion, int targetBlockStatesVersion) throws IOException {
        //更新判断的基础数据
        ListTag<CompoundTag> oldBaseListTag;
        try (InputStream stream = new FileInputStream("src/main/resources/Target_Data/runtime_block_states_" + oldBlockStatesVersion + ".dat")) {
            //noinspection unchecked
            oldBaseListTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e) {
            throw new AssertionError("Unable to locate pm1e_runtime_block_states_" + oldBlockStatesVersion + ".dat", e);
        }

        //加载NKX的数据，对于NKX存在的数据我们不需要重复生成
        ListTag<CompoundTag> nkxTag;
        try (InputStream stream = new FileInputStream("src/main/resources/Nukkit_Data/nkx_runtime_block_states_" + targetBlockStatesVersion + ".dat")) {
            //noinspection unchecked
            nkxTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e) {
            //throw new AssertionError("Unable to locate nkx_runtime_block_states_" + targetBlockStatesVersion + ".dat", e);
            log.warn("Unable to locate nkx_runtime_block_states_" + targetBlockStatesVersion + ".dat", e);
            nkxTag = new ListTag<>();
        }

        Int2ObjectMap<String> blockIdToPersistenceName = new Int2ObjectOpenHashMap<>();
        Map<String, Integer> persistenceNameToBlockId = new LinkedHashMap<>();
        try (InputStream stream = new FileInputStream("src/main/resources/block_ids.csv")) {
            int count = 0;
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    count++;
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                    String[] parts = line.split(",");
                    Preconditions.checkArgument(parts.length == 2 || parts[0].matches("^[0-9]+$"));
                    if (parts.length > 1 && parts[1].startsWith("minecraft:")) {
                        int id = Integer.parseInt(parts[0]);
                        blockIdToPersistenceName.put(id, parts[1]);
                        persistenceNameToBlockId.put(parts[1], id);
                    }
                }
            } catch (Exception e) {
                throw new IOException("Error reading the line "+count+" of the block_ids.csv", e);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        //加载PMMP的数据
        List<CompoundTag> tags = new ArrayList<>();
        try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states_" + targetBlockStatesVersion + ".nbt")) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                int runtimeId = 0;
                while (bis.available() > 0) {
                    CompoundTag tag = NBTIO1.read(bis, ByteOrder.BIG_ENDIAN, true);
                    tag.putInt("runtimeId", runtimeId++);
                    String name = tag.getString("name").toLowerCase();
                    Integer id = persistenceNameToBlockId.getOrDefault(name, -1);
                    tag.putInt("id", id);
                    if (id == -1) {
                        log.error("Unable to find block id for " + name);
                    }
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            log.error("无法读取" + "PMMP_Data/canonical_block_states_" + targetBlockStatesVersion + ".nbt" + "，尝试读取CB_Data/block_palette_" + targetBlockStatesVersion + ".nbt");
            //加载cb数据
            try (InputStream stream = new FileInputStream("src/main/resources/CB_Data/block_palette_" + targetBlockStatesVersion + ".nbt")) {
                int runtimeId = 0;
                Tag tag1 = NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
                ListTag<CompoundTag> blocks = ((CompoundTag) tag1).getList("blocks", CompoundTag.class);
                for (CompoundTag compoundTag : blocks.getAll()) {
                    compoundTag.remove("network_id");
                    compoundTag.remove("name_hash");
                    compoundTag.remove("block_id");
                    compoundTag.putInt("runtimeId", runtimeId++);
                    tags.add(compoundTag.copy());
                }
            } catch (IOException e1) {
                throw new AssertionError(e1);
            }
        }

        //更新方块数据到新版本
        ListTag<CompoundTag> listTag = new ListTag<>();
        for (CompoundTag compoundTag : oldBaseListTag.getAll()) {
            listTag.add(Utils.nbtMap2CompoundTag(BlockStateUpdaters.updateBlockState(Utils.compoundTag2NbtMap(compoundTag),  compoundTag.getInt("version"))));
        }
        oldBaseListTag = listTag;

        ListTag<CompoundTag> newTagList = new ListTag<>();
        newTagList.setAll(nkxTag.getAll());

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

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/runtime_block_states_" + targetBlockStatesVersion + ".dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);
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
}
