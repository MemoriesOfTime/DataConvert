package cn.lanink.dataconvert.utils;

import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.nbt.*;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author LT_Name
 */
@Log4j2
public class Utils {

    public static List<CompoundTag> sort(List<CompoundTag> list) {
        Map<String, List<CompoundTag>> vanillaPaletteList = new Object2ObjectRBTreeMap<>(HashedPaletteComparator.INSTANCE);

        String lastName = null;
        List<CompoundTag> group = new ObjectArrayList<>();

        for (CompoundTag state : list) {
            //删除不属于原版的内容
            state.remove("network_id");
            state.remove("name_hash");
            state.remove("block_id");

            String name = state.getString("name");
            if (lastName != null && !name.equals(lastName)) {
                vanillaPaletteList.put(lastName, group);
                group = new ObjectArrayList<>();
            }
            group.add(state);
            lastName = name;
        }

        if (lastName != null) {
            vanillaPaletteList.put(lastName, group);
        }

        int runtimeId = 0;
        List<CompoundTag> newTagList = new ArrayList<>(list.size());
        for (List<CompoundTag> tag : vanillaPaletteList.values()) {
            for (CompoundTag compoundTag : tag) {
                compoundTag.putInt("runtimeId", runtimeId++);
                newTagList.add(compoundTag);
            }
        }

        return newTagList;
    }

    /**
     * 读取EC的方块状态文件
     */
    public static List<CompoundTag> readECBlockStates(String file) throws IOException {
        List<CompoundTag> tags = new ArrayList<>();
        try (InputStream stream = new FileInputStream(file)) {
            //noinspection unchecked
            ListTag<CompoundTag> tag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);

            Map<String, Integer> persistenceNameToBlockId = Utils.readBlockName2IdMap();
            int runtimeId = 0;
            for (CompoundTag t : tag.getAll()) {
                CompoundTag block = t.getCompound("block").copy();
                block.putInt("runtimeId", runtimeId++);
                Integer id = persistenceNameToBlockId.getOrDefault(block.getString("name"), -1);
                block.putInt("id", id);
                if (id != -1) {
                    //
                }
                //TODO auto version
                if (file.contains("630")) {
                    block.putInt("version", 18100737);
                } else {
                    block.putInt("version", 18153475); //636
                }
                tags.add(block);
            }
        }
        return tags;
    }

    public static ListTag<CompoundTag> readNKBlockStates(String file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            //noinspection unchecked
            return (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        }
    }

    public static List<CompoundTag> readCBBlockStates(String file) throws IOException {
        List<CompoundTag> tags = new ArrayList<>();
        Map<String, Integer> name2IdMap = readBlockName2IdMap();
        try (InputStream stream = new FileInputStream(file)) {
            int runtimeId = 0;
            Tag tag1 = NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
            ListTag<CompoundTag> blocks = ((CompoundTag) tag1).getList("blocks", CompoundTag.class);
            for (CompoundTag compoundTag : blocks.getAll()) {
                compoundTag.putInt("runtimeId", runtimeId++);
                String name = compoundTag.getString("name").toLowerCase();
                Integer id = name2IdMap.getOrDefault(name, -1);
                compoundTag.putInt("id", id);
                if (id == -1) {
                    //log.error("Unable to find block id for {}", name);
                }
                compoundTag.remove("network_id");
                compoundTag.remove("name_hash");
                compoundTag.remove("block_id");
                tags.add(compoundTag.copy());
            }
            return tags;
        }
    }

    /**
     * 读取网易 palette NBT 文件
     * 兼容 gzip/非 gzip 以及大小端的 {blocks:[...]} NBT 文件
     */
    public static List<CompoundTag> readNeteasePaletteBlocks(String file) throws IOException {
        ReadAttempt[] attempts = new ReadAttempt[]{
                new ReadAttempt(true, ByteOrder.BIG_ENDIAN),
                new ReadAttempt(false, ByteOrder.BIG_ENDIAN),
                new ReadAttempt(true, ByteOrder.LITTLE_ENDIAN),
                new ReadAttempt(false, ByteOrder.LITTLE_ENDIAN)
        };
        for (ReadAttempt attempt : attempts) {
            List<CompoundTag> tags = tryReadNeteasePaletteBlocks(file, attempt.compressed(), attempt.byteOrder());
            if (tags != null) {
                log.info("成功读取网易 palette，压缩={}, 字节序={}, 方块数量: {}", attempt.compressed(), attempt.byteOrder(), tags.size());
                return tags;
            }
        }
        throw new IOException("无法读取网易 palette 文件");
    }

    private record ReadAttempt(boolean compressed, ByteOrder byteOrder) {
    }

    private static List<CompoundTag> tryReadNeteasePaletteBlocks(String file, boolean compressed, ByteOrder byteOrder) {
        Map<String, Integer> name2IdMap = readBlockName2IdMap();
        List<CompoundTag> tags = new ArrayList<>();

        try (InputStream fileStream = new FileInputStream(file);
             InputStream decodedStream = compressed ? new GZIPInputStream(fileStream) : fileStream;
             BufferedInputStream bufferedInputStream = new BufferedInputStream(decodedStream)) {
            Tag tag1 = NBTIO.readTag(bufferedInputStream, byteOrder, false);
            if (!(tag1 instanceof CompoundTag compound) || !compound.contains("blocks")) {
                return null;
            }

            int runtimeId = 0;
            ListTag<CompoundTag> blocks = compound.getList("blocks", CompoundTag.class);
            for (CompoundTag compoundTag : blocks.getAll()) {
                compoundTag.putInt("runtimeId", runtimeId++);
                String name = compoundTag.getString("name").toLowerCase();
                Integer id = name2IdMap.getOrDefault(name, -1);
                compoundTag.putInt("id", id);
                compoundTag.remove("network_id");
                compoundTag.remove("name_hash");
                compoundTag.remove("block_id");
                tags.add(compoundTag.copy());
            }
            return tags;
        } catch (IOException | OutOfMemoryError e) {
            return null;
        }
    }

    public static List<CompoundTag> readPMBlockStates(String file) throws IOException {
        if (file.contains("required_")) {
            return readPMBlockStatesOld(file);
        }
        Map<String, Integer> name2IdMap = readBlockName2IdMap();
        List<CompoundTag> tags = new ArrayList<>();
        try (InputStream stream = new FileInputStream(file)) {
            try (BufferedInputStream bis = new BufferedInputStream(stream)) {
                int runtimeId = 0;
                while (bis.available() > 0) {
                    CompoundTag tag = NBTIO1.read(bis, ByteOrder.BIG_ENDIAN, true);
                    tag.putInt("runtimeId", runtimeId++);
                    String name = tag.getString("name").toLowerCase();
                    Integer id = name2IdMap.getOrDefault(name, -1);
                    tag.putInt("id", id);
                    if (id == -1) {
                        //log.error("Unable to find block id for {}", name);
                    }
                    tags.add(tag);
                }
            }
            return tags;
        }
    }

    private static List<CompoundTag> readPMBlockStatesOld(String file) {
        ListTag<CompoundTag> oldTags;
        try (InputStream stream = new FileInputStream(file)) {
            //noinspection unchecked
            oldTags = (ListTag<CompoundTag>) NBTIO.readTag(new ByteArrayInputStream(ByteStreams.toByteArray(stream)), ByteOrder.BIG_ENDIAN, true);
        } catch (IOException e) {
            throw new AssertionError(file, e);
        }

        List<CompoundTag> tags = new ArrayList<>();
        for (CompoundTag tag : oldTags.getAll()) {
            tags.add(tag.getCompound("block"));
        }
        return tags;
    }

    private static Map<String, Integer> blockName2IdMapCache;

    public static Map<String, Integer> readBlockName2IdMap() {
        if (blockName2IdMapCache != null) {
            return blockName2IdMapCache;
        }
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
                        persistenceNameToBlockId.put(parts[1], id);
                    }
                }
            } catch (Exception e) {
                throw new IOException("Error reading the line "+count+" of the block_ids.csv", e);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        blockName2IdMapCache = persistenceNameToBlockId;
        return persistenceNameToBlockId;
    }

    public static NbtMap base64ToNbt(String base64) {
        byte[] nbtBytes = Base64.getDecoder().decode(base64);
        try (NBTInputStream stream = NbtUtils.createReaderLE(new ByteArrayInputStream(nbtBytes))) {
            return (NbtMap) stream.readTag();
        } catch (Exception e) {
            throw new AssertionError("Unable to decode NBT value", e);
        }
    }

    public static NbtMap compoundTag2NbtMap(CompoundTag compoundTag) {
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

    public static CompoundTag nbtMap2CompoundTag(NbtMap nbtMap) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (stream; NBTOutputStream nbtOutputStream = NbtUtils.createWriter(stream)) {
                nbtOutputStream.writeTag(nbtMap);
            }
            return NBTIO.read(stream.toByteArray(), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert NbtMap: " + nbtMap, e);
        }
    }

    public static NbtList listTag2NbtList(ListTag<Tag> listTag) {
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

    public static ListTag<Tag> nbtList2ListTag(NbtList nbtList) {
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

    public static NbtType<?> tagType2NbtType(int tagType) {
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

    public static int nbtType2TagType(NbtType<?> nbtType) {
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
