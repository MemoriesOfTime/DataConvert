package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

public class RuntimeBlockStateConvert {
    public static void convert() throws IOException {
        Int2ObjectMap<String> blockIdToPersistenceName = new Int2ObjectOpenHashMap<>();
        Map<String, Integer> persistenceNameToBlockId = new LinkedHashMap<>();
        try (InputStream stream = new FileInputStream("src/main/resources/block_ids.csv")) {
            if (stream == null) {
                throw new AssertionError("Unable to locate block_ids.csv");
            }

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


        List<CompoundTag> tags = new ArrayList<>();
        try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states.nbt")) {
            if (stream == null) {
                throw new AssertionError("Unable to locate block state nbt");
            }

            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                int runtimeId = 0;
                while (bis.available() > 0) {
                    CompoundTag tag = NBTIO.read(bis, ByteOrder.BIG_ENDIAN, true);
                    tag.putInt("runtimeId", runtimeId++);
                    tag.putInt("blockId", persistenceNameToBlockId.getOrDefault(tag.getString("name").toLowerCase(), -1));
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        ListTag<CompoundTag> statesList = new ListTag<>();

        int data = 0;
        String lastBlockName = null;
        for (CompoundTag block : tags) {
            int blockId = block.getInt("blockId");
            block.remove("blockId");
            block.putInt("id", blockId);

            //TODO fix data
            String name = block.getString("name");
            if (name.equalsIgnoreCase(lastBlockName)) {
                data++;
            } else {
                data = 0;
            }
            lastBlockName = name;
            block.putShort("data", (short) data);

            CompoundTag statesCompound = block.getCompound("states");
            if (statesCompound.isEmpty()) {
                block.remove("states");
            }
            statesList.add(block);
        }

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Nukkit_Data/runtime_block_states.dat"));
        NBTIO.writeGZIPCompressed(statesList, outputStream, ByteOrder.BIG_ENDIAN);
    }
}
