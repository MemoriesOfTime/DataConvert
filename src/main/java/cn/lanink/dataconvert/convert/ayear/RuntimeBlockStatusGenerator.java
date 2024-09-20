package cn.lanink.dataconvert.convert.ayear;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

@Log4j2
public class RuntimeBlockStatusGenerator {

    private String version;

    private final HashMap<String, Integer> legacyBlockIdMap = new HashMap<>();

    private final HashMap<String, Integer> counterMap = new HashMap<>();

    public RuntimeBlockStatusGenerator(String version){
        this.version = version;
        readLegacyBlockIds();
    }

    public void start() {

        /*
        ArrayList<BlockStatusStructure> blockArray;
        try {
            // 读取 JSON 文件
            FileReader reader = new FileReader("src/main/resources/ayear/block_status/block_status.json");

            // 使用 GSON 解析 JSON
            Gson gson = new Gson();
            Type blockListType = new TypeToken<ArrayList<BlockStatusStructure>>(){}.getType();

            // 将 JSON 文件解析为 ArrayList<BlockStatusStructure>
            blockArray = gson.fromJson(reader, blockListType);
        } catch (IOException e) {
            e.printStackTrace();
        }
         */


        counterMap.clear();
        ListTag<CompoundTag> outputTagList = new ListTag<>();
        try (InputStream stream = new FileInputStream("src/main/resources/ayear/block_palette/block_palette_"+version+".nbt")) {
            Tag tag1 = NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
            ListTag<CompoundTag> blocks = ((CompoundTag) tag1).getList("blocks", CompoundTag.class);
            for (int i = 0; i < blocks.getAll().size(); i++) {
                CompoundTag compoundTag = blocks.getAll().get(i);

                String name = compoundTag.getString("name");
                CompoundTag newTag = new CompoundTag();
                newTag.putByte("stateOverload", 0);
                newTag.putShort("data", compStateData(name, compoundTag.getCompound("states")));
                newTag.putString("name", name);
                newTag.putInt("id", compBlockId(name, compoundTag));
                newTag.putInt("version", compoundTag.getInt("version"));
                newTag.putInt("runtimeId", i);
                newTag.putCompound("states", compoundTag.getCompound("states"));
                outputTagList.add(newTag);
            }
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/ayear/output/runtime_block_states_"+version+".dat"));
            NBTIO1.writeGZIPCompressed(outputTagList, outputStream, ByteOrder.BIG_ENDIAN);
        } catch (IOException e1) {
            throw new AssertionError(e1);
        }
    }

    private int compBlockId(String name, CompoundTag fullTag) {
        if (fullTag.containsInt("block_id")) {
            return fullTag.getInt("block_id");
        }
        if (!legacyBlockIdMap.isEmpty() && legacyBlockIdMap.containsKey(name)) {
            return legacyBlockIdMap.get(name);
        }
        log.error("not found block_id: {}", name);
        return -1;
    }

    private int compStateData(String name, CompoundTag compoundTag) {
        // 获取所有标签
//        Collection<Tag> allTags = compoundTag.getAllTags();

//        // 按照指定规则进行排序
//        List<Tag> sortedTags = allTags.stream()
//                .sorted(Comparator
//                        // 按照类型排序，顺序为：String(8)、Int(3)、Byte(1)
//                        .comparingInt(tag -> getTypePriority((Tag) tag))
//                        // 同类型数据按名字长度排序
//                        .thenComparing(tag -> ((Tag) tag).getName().length(), Comparator.reverseOrder()))
//                .toList();
//
//        log.info(sortedTags.toString());

        if (counterMap.containsKey(name)) {
            int count = counterMap.get(name) + 1;
            counterMap.put(name, count);
            return count;
        }
        counterMap.put(name, 0);
        return 0;  // 返回1作为示例值，具体返回值取决于你的需求
    }

    // 获取类型的优先级，用于排序
//    private static int getTypePriority(Tag tag) {
//        switch (tag.getId()) {
//            case Tag.TAG_String: return 1;  // String 类型优先级最高
//            case Tag.TAG_Int: return 2;     // Int 类型第二
//            case Tag.TAG_Byte: return 3;    // Byte 类型第三
//            default: return 4;               // 其他类型优先级最低
//        }
//    }

    /**
     * 读取旧方块ID
     */
    public void readLegacyBlockIds() {
        // 创建文件对象
        File file = new File("src/main/resources/ayear/legacy_block_ids/legacy_block_ids_"+version+".json");

        // 检查文件是否存在
        if (!file.exists() || !file.isFile()) {
            log.info("[{}] 不存在 legacy_block_ids.json 文件", version);
            return;
        }

        // 创建Gson对象
        Gson gson = new Gson();

        // 定义HashMap的类型
        Type hashMapType = new TypeToken<HashMap<String, Integer>>() {}.getType();

        // 定义一个HashMap对象
        HashMap<String, Integer> blockIdMap = null;

        try (FileReader reader = new FileReader(file)) {
            // 使用Gson读取JSON文件并转换为HashMap
            blockIdMap = gson.fromJson(reader, hashMapType);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.legacyBlockIdMap.putAll(blockIdMap);
    }

    /**
     * 比较与旧类的差异
     */
    public void compare() {
        int errorCount = 0;
        ListTag<CompoundTag> oldBaseListTag;
        ListTag<CompoundTag> outputListTag;

        // 读取 oldBaseListTag
        try (InputStream stream = new FileInputStream("src/main/resources/ayear/old/runtime_block_states_" + version + ".dat")) {
            oldBaseListTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e1) {
            throw new AssertionError(e1);
        }

        // 读取 outputListTag
        try (InputStream stream = new FileInputStream("src/main/resources/ayear/output/runtime_block_states_" + version + ".dat")) {
            outputListTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e1) {
            throw new AssertionError(e1);
        }

        // 开始对比
        for (int i = 0; i < oldBaseListTag.size(); i++) {
            CompoundTag oldTag = oldBaseListTag.get(i);
            if (oldTag.getInt("runtimeId") != 13260) continue;
            boolean found = false;

            for (int j = 0; j < outputListTag.size(); j++) {
                CompoundTag outputTag = outputListTag.get(j);
                if (outputTag.getInt("runtimeId") != 13260) continue;
                if (compareTags(oldTag, outputTag)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.warn("Tag not found in output: " + oldTag.toSNBT());
                errorCount++;
            }
        }
        log.info("对比了 "+oldBaseListTag.size()+" 条数据，共发现 "+errorCount+" 个错误（" + ((double) errorCount / oldBaseListTag.size()) * 100 + "%）。");
    }

    // 比较两个 CompoundTag 是否相同
    private boolean compareTags(CompoundTag tag1, CompoundTag tag2) {
        // 如果两个 tag 的键值对数量不同，则直接返回 false
//        if (tag1.getTags().size() != tag2.getTags().size()) {
//            return false;
//        }

        if (tag1.getInt("runtimeId") != tag2.getInt("runtimeId")) {
            return false;
        }
        if (!Objects.equals(tag1.getString("name"), tag2.getString("name"))) {
            return false;
        }
        if (tag1.getShort("data") != tag2.getShort("data")) {
            return false;
        }
        CompoundTag nbt1 = tag1.getCompound("states");
        CompoundTag nbt2 = tag2.getCompound("states");
        for (Tag tag : nbt1.getAllTags()) {
            if (!nbt2.get(tag.getName()).equals(tag)) {
                return false;
            }
        }
        return true;
    }
}
