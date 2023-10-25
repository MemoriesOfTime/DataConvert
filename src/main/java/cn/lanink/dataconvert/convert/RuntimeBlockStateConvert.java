package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class RuntimeBlockStateConvert {
    public static void convert() throws IOException {
        //使用旧的NK数据作为基础
        ListTag<CompoundTag> oldTag;
        try (InputStream stream = new FileInputStream("src/main/resources/Nukkit_Data/runtime_block_states_618.dat")) { //TODO 有新的数据时记得更新
            //noinspection unchecked
            oldTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e) {
            throw new AssertionError("Unable to locate runtime_block_states_618.dat", e);
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
        try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states.nbt")) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                int runtimeId = 0;
                while (bis.available() > 0) {
                    CompoundTag tag = NBTIO1.read(bis, ByteOrder.BIG_ENDIAN, true);
                    tag.putInt("runtimeId", runtimeId++);
                    tag.putInt("id", persistenceNameToBlockId.getOrDefault(tag.getString("name").toLowerCase(), -1));
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        ListTag<CompoundTag> newTagList = new ListTag<>();

        int data = 0;
        String lastBlockName = null;
        for (CompoundTag block : tags) {
            //根据名称获取旧的方块数据
            List<CompoundTag> oldTagList = getBlockByName(oldTag, block.getString("name"));
            if (oldTagList.isEmpty()) {
                continue; //跳过未实现的方块
            }

            ArrayList<Tag> newBlockStates = new ArrayList<>(block.getCompound("states").getAllTags());

            CompoundTag equalsTag = null;
            for (CompoundTag tag : oldTagList) {
                if (equalsTag != null) {
                    break;
                }
                CompoundTag oldBlockStates = tag.getCompound("states");
                if (newBlockStates.isEmpty() && oldBlockStates.isEmpty()) {
                    equalsTag = tag;
                    break;
                } else {
                    for (Tag tag1 : newBlockStates) {
                        if (oldBlockStates.contains(tag1.getName()) && oldBlockStates.get(tag1.getName()).equals(tag1)) {
                            equalsTag = tag;
                            break;
                        }

                        /*Object object = tag1.parseValue();
                        if (tag1 instanceof CompoundTag compoundTag && compoundTag.getTags().isEmpty()
                                || tag1 instanceof ListTag<?> listTag && listTag.getAll().isEmpty()
                                || object instanceof Number number && number.intValue() == 0
                                || object instanceof String string && string.isBlank()
                                || object instanceof Array array && Array.getLength(array) == 0) {

                        }*/
                    }
                }
            }

            if (equalsTag != null) {
                CompoundTag copy = equalsTag.copy();
                copy.putInt("runtimeId", block.getInt("runtimeId"));
                copy.putInt("version", block.getInt("version"));
                newTagList.add(copy);
            }
        }

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Nukkit_Data/new_runtime_block_states.dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);
    }

    private static List<CompoundTag> getBlockByName(ListTag<CompoundTag> tags, String name) {
        ArrayList<CompoundTag> list = new ArrayList<>();
        for (CompoundTag block : tags.getAll()) {
            if (block.getString("name").equals(name)) {
                list.add(block);
            }
        }
        return list;
    }
}
