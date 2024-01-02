package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Log4j2
public class RuntimeBlockStateConvert {
    public static void convert() throws IOException {
        int oldBlockStatesVersion = 622;
        int targetBlockStatesVersion = 630;

        //使用PM1E的数据作为更新判断的基础（因为相比nkx比较全）
        ListTag<CompoundTag> oldBaseListTag;
        try (InputStream stream = new FileInputStream("src/main/resources/Nukkit_Data/pm1e_runtime_block_states_" + oldBlockStatesVersion + ".dat")) {
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
        try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states.nbt")) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                int runtimeId = 0;
                while (bis.available() > 0) {
                    CompoundTag tag = NBTIO1.read(bis, ByteOrder.BIG_ENDIAN, true);
                    tag.putInt("runtimeId", runtimeId++);
                    String name = tag.getString("name").toLowerCase();
                    tag.putInt("id", persistenceNameToBlockId.getOrDefault(name, -1));
                    tags.add(tag);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        //更新方块数据到新版本
        ListTag<CompoundTag> listTag = new ListTag<>();
        for (CompoundTag compoundTag : oldBaseListTag.getAll()) {
            listTag.add(nbtMap2CompoundTag(BlockStateUpdaters.updateBlockState(compoundTag2NbtMap(compoundTag),  compoundTag.getInt("version"))));
        }
        oldBaseListTag = listTag;

        ListTag<CompoundTag> newTagList = new ListTag<>();
        newTagList.setAll(nkxTag.getAll());

        //移除不需要的
        List<CompoundTag> all = newTagList.getAll();
        newTagList = new ListTag<>();
        for (CompoundTag tag : all) {
            String name = tag.getString("name");
            if (name.equals("minecraft:respawn_anchor")
                    || name.equals("minecraft:bee_nest")
                    || name.equals("minecraft:beehive")) {
                continue;
            }
            newTagList.add(tag);
        }

        int data = 0;
        String lastBlockName = null;
        for (CompoundTag block : tags) {
            //根据名称获取旧的方块数据
            String name = block.getString("name");
            ArrayList<Tag> newBlockStates = new ArrayList<>(block.getCompound("states").getAllTags());

            //minecraft:beehive  minecraft:bee_nest

            List<CompoundTag> oldTagList = getBlockByName(oldBaseListTag, name, null);
            if (oldTagList.isEmpty()) {
                /*CompoundTag copy = block.copy();
                copy.putShort("data", -1);
                newTagList.add(copy);*/
                continue;
            }

            //额外添加
            if (name.equals("minecraft:respawn_anchor")) {
                IntTag intTag = (IntTag) newBlockStates.get(0);
                CompoundTag copy = block.copy();
                copy.putShort("data", intTag.getData());
                newTagList.add(copy);
                continue;
            } else if (name.equals("minecraft:bee_nest") || name.equals("minecraft:beehive")) {
                //direction int 0
                //honey_level int 1
                CompoundTag copy = block.copy();
                IntTag directionTag = (IntTag) newBlockStates.get(0);
                IntTag honeyLevelTag = (IntTag) newBlockStates.get(1);
                copy.putShort("data", (short) (honeyLevelTag.getData() << 2 | directionTag.getData()));
                newTagList.add(copy);
                continue;
            }

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
                    boolean equals = true;
                    for (Tag tag1 : newBlockStates) {
                        if (!oldBlockStates.contains(tag1.getName())
                                || !oldBlockStates.get(tag1.getName()).parseValue().equals(tag1.parseValue())) {
                            equals = false;
                            break;
                        }
                    }
                    if (equals) {
                        equalsTag = tag;
                    }
                }
            }

            if (equalsTag != null) {
                CompoundTag copy = equalsTag.copy();
                int runtimeId = block.getInt("runtimeId");
                copy.putInt("runtimeId", runtimeId);
                copy.putInt("version", block.getInt("version"));
                boolean isDuplicate = false;
                for (CompoundTag tag : newTagList.getAll()) {
                    if (tag.getInt("runtimeId") == runtimeId
                            || (tag.getString("name").equals(copy.getString("name"))
                            && tag.getInt("id") == copy.getInt("id")
                            && tag.getShort("data") == copy.getShort("data")
                            && tag.getByte("stateOverload") == copy.getByte("stateOverload"))) {
                        isDuplicate = true;
                        log.debug("Duplicate : " + block + "\n" + copy + "\n " + tag);
                    }
                }
                if (!isDuplicate) {
                    newTagList.add(copy);
                }
            }
        }

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/runtime_block_states_" + targetBlockStatesVersion + ".dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);
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

    private static NbtMap compoundTag2NbtMap(CompoundTag compoundTag) {
        NbtMapBuilder builder = NbtMap.builder();
        for (Tag tag : compoundTag.getAllTags()) {
            switch (tag.getId()) {
                case 1:
                    builder.putByte(tag.getName(), (byte) ((Integer) tag.parseValue()).intValue());
                    break;
                case 2:
                    builder.putShort(tag.getName(), (short) ((Integer) tag.parseValue()).intValue());
                    break;
                case 3:
                    builder.putInt(tag.getName(), (int) tag.parseValue());
                    break;
                case 4:
                    builder.putLong(tag.getName(), (long) tag.parseValue());
                    break;
                case 5:
                    builder.putFloat(tag.getName(), (float) tag.parseValue());
                    break;
                case 6:
                    builder.putDouble(tag.getName(), (double) tag.parseValue());
                    break;
                case 7:
                    builder.putByteArray(tag.getName(), (byte[]) tag.parseValue());
                    break;
                case 8:
                    builder.putString(tag.getName(), (String) tag.parseValue());
                    break;
                case 9:
                    ListTag listTag = (ListTag) tag;
                    builder.putList(tag.getName(), tagType2NbtType(listTag.type), listTag2NbtList(listTag));
                    break;
                case 10:
                    builder.putCompound(tag.getName(), compoundTag2NbtMap((CompoundTag) tag));
                    break;
                case 11:
                    builder.putIntArray(tag.getName(), (int[]) tag.parseValue());
                    break;
            }
        }
        return builder.build();
    }

    private static CompoundTag nbtMap2CompoundTag(NbtMap nbtMap) {
        CompoundTag compoundTag = new CompoundTag();
        for (String key : nbtMap.keySet()) {
            Object value = nbtMap.get(key);
            if (value instanceof Byte) {
                compoundTag.putByte(key, (byte) value);
            } else if (value instanceof Short) {
                compoundTag.putShort(key, (short) value);
            } else if (value instanceof Integer) {
                compoundTag.putInt(key, (int) value);
            } else if (value instanceof Long) {
                compoundTag.putLong(key, (long) value);
            } else if (value instanceof Float) {
                compoundTag.putFloat(key, (float) value);
            } else if (value instanceof Double) {
                compoundTag.putDouble(key, (double) value);
            } else if (value instanceof byte[]) {
                compoundTag.putByteArray(key, (byte[]) value);
            } else if (value instanceof String) {
                compoundTag.putString(key, (String) value);
            } else if (value instanceof NbtList nbtList) {
                compoundTag.put(key, nbtList2ListTag(nbtList));
            } else if (value instanceof NbtMap) {
                compoundTag.putCompound(key, nbtMap2CompoundTag((NbtMap) value));
            } else if (value instanceof int[]) {
                compoundTag.putIntArray(key, (int[]) value);
            }
        }
        return compoundTag;
    }

    private static NbtList listTag2NbtList(ListTag<Tag> listTag) {
        NbtList nbtList = new NbtList(tagType2NbtType(listTag.type));
        for (Tag tag : listTag.getAll()) {
            if (tag instanceof ByteTag) {
                nbtList.add(((ByteTag) tag).parseValue());
            } else if (tag instanceof ShortTag) {
                nbtList.add(((ShortTag) tag).parseValue());
            } else if (tag instanceof IntTag) {
                nbtList.add(((IntTag) tag).parseValue());
            } else if (tag instanceof LongTag) {
                nbtList.add(((LongTag) tag).parseValue());
            } else if (tag instanceof FloatTag) {
                nbtList.add(((FloatTag) tag).parseValue());
            } else if (tag instanceof DoubleTag) {
                nbtList.add(((DoubleTag) tag).parseValue());
            } else if (tag instanceof ByteArrayTag) {
                nbtList.add(((ByteArrayTag) tag).parseValue());
            } else if (tag instanceof StringTag) {
                nbtList.add(((StringTag) tag).parseValue());
            } else if (tag instanceof ListTag listTag1) {
                nbtList.add(listTag2NbtList(listTag1));
            } else if (tag instanceof CompoundTag compoundTag) {
                nbtList.add(compoundTag2NbtMap(compoundTag));
            } else if (tag instanceof IntArrayTag) {
                nbtList.add(((IntArrayTag) tag).parseValue());
            }
        }
        return nbtList;
    }

    private static ListTag<Tag> nbtList2ListTag(NbtList nbtList) {
        ListTag<Tag> listTag = new ListTag<>();
        for (Object value : nbtList) {
            if (value instanceof Byte) {
                listTag.add(new ByteTag("", (byte) value));
            } else if (value instanceof Short) {
                listTag.add(new ShortTag("", (short) value));
            } else if (value instanceof Integer) {
                listTag.add(new IntTag("", (int) value));
            } else if (value instanceof Long) {
                listTag.add(new LongTag("", (long) value));
            } else if (value instanceof Float) {
                listTag.add(new FloatTag("", (float) value));
            } else if (value instanceof Double) {
                listTag.add(new DoubleTag("", (double) value));
            } else if (value instanceof byte[]) {
                listTag.add(new ByteArrayTag("", (byte[]) value));
            } else if (value instanceof String) {
                listTag.add(new StringTag("", (String) value));
            } else if (value instanceof NbtList nbtList1) {
                listTag.add(nbtList2ListTag(nbtList1));
            } else if (value instanceof NbtMap) {
                listTag.add(nbtMap2CompoundTag((NbtMap) value));
            } else if (value instanceof int[]) {
                listTag.add(new IntArrayTag("", (int[]) value));
            }
        }
        return listTag;
    }

    private static NbtType<?> tagType2NbtType(int tagType) {
        return switch (tagType) {
            case 0 -> NbtType.END;
            case 1 -> NbtType.BYTE;
            case 2 -> NbtType.SHORT;
            case 3 -> NbtType.INT;
            case 4 -> NbtType.LONG;
            case 5 -> NbtType.FLOAT;
            case 6 -> NbtType.DOUBLE;
            case 7 -> NbtType.BYTE_ARRAY;
            case 8 -> NbtType.STRING;
            case 9 -> NbtType.LIST;
            case 10 -> NbtType.COMPOUND;
            case 11 -> NbtType.INT_ARRAY;
            default -> throw new IllegalArgumentException("Unknown tag type " + tagType);
        };
    }

    private static int nbtType2TagType(NbtType<?> nbtType) {
        return switch (nbtType.getEnum()) {
            case END -> 0;
            case BYTE -> 1;
            case SHORT -> 2;
            case INT -> 3;
            case LONG -> 4;
            case FLOAT -> 5;
            case DOUBLE -> 6;
            case BYTE_ARRAY -> 7;
            case STRING -> 8;
            case LIST -> 9;
            case COMPOUND -> 10;
            case INT_ARRAY -> 11;
            default -> throw new IllegalArgumentException("Unknown tag type " + nbtType);
        };
    }
}
