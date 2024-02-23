package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Log4j2
public class RuntimeBlockStateConvertOld {

    public static void main(String[] args) throws IOException {
        convert(589);
        convert(594);
        convert(618);
        convert(622);
        convert(630);
        convert(649);
        System.exit(0);
    }

    public static void convert(int oldBlockStatesVersion) throws IOException {

        ListTag<CompoundTag> oldBaseListTag;
        try (InputStream stream = new FileInputStream("src/main/resources/Target_Data/runtime_block_states_" + oldBlockStatesVersion + ".dat")) {
            //noinspection unchecked
            oldBaseListTag = (ListTag<CompoundTag>) NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
        } catch (IOException e) {
            throw new AssertionError("Unable to locate runtime_block_states_" + oldBlockStatesVersion + ".dat", e);
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
        ListTag<CompoundTag> tags2 = null;
        if (oldBlockStatesVersion >= 419) {
            try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/canonical_block_states_" + oldBlockStatesVersion + ".nbt")) {
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
                throw new AssertionError(e);
            }
        } else {
            try (InputStream stream = new FileInputStream("src/main/resources/PMMP_Data/required_block_states_" + oldBlockStatesVersion + ".nbt")) {
                //noinspection unchecked
                tags2 = (ListTag<CompoundTag>) NBTIO.readTag(new ByteArrayInputStream(ByteStreams.toByteArray(stream)), ByteOrder.BIG_ENDIAN, true);
            } catch (IOException e) {
                throw new AssertionError("Unable to locate runtime_block_states_" + oldBlockStatesVersion + ".dat", e);
            }
        }

        ListTag<CompoundTag> newTagList = new ListTag<>();

        ListTag<CompoundTag> newBeeNest = new ListTag<>(); //蜂巢 蜂箱
        ListTag<CompoundTag> newBeehive = new ListTag<>();

        ListTag<CompoundTag> newDecoratedPot = new ListTag<>(); //陶罐

        ListTag<CompoundTag> newCrimsonPressurePlate = new ListTag<>(); //深红压力板
        ListTag<CompoundTag> newWarpedPressurePlate = new ListTag<>(); //扭曲木压力板

        ListTag<CompoundTag> newMangrovePlanks = new ListTag<>(); //红木板
        ListTag<CompoundTag> newBambooPlanks = new ListTag<>(); //竹板
        ListTag<CompoundTag> newCherryPlanks  = new ListTag<>(); //樱花木板

        if (tags2 != null) {
            for (CompoundTag tag : tags2.getAll()) {
                tags.add(tag.getCompound("block"));
            }
        }

        for (CompoundTag block : tags) {
            String name = block.getString("name");
            switch (name.toLowerCase()) {
                case "minecraft:crimson_pressure_plate":
                    ArrayList<Tag> blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    CompoundTag copy = block.copy();
                    IntTag directionTag = (IntTag) blockStates.get(0);
                    copy.putShort("data", directionTag.getData());
                    newCrimsonPressurePlate.add(copy);
                    break;
                case "minecraft:warped_pressure_plate":
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy = block.copy();
                    directionTag = (IntTag) blockStates.get(0);
                    copy.putShort("data", directionTag.getData());
                    newWarpedPressurePlate.add(copy);
                    break;
                case "minecraft:mangrove_planks":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    newMangrovePlanks.add(copy);
                    break;
                case "minecraft:bamboo_planks":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    newBambooPlanks.add(copy);
                    break;
                case "minecraft:cherry_planks":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    newCherryPlanks.add(copy);
                    break;
            }
            //额外添加
            if (name.equals("minecraft:bee_nest") || name.equals("minecraft:beehive")) {
                ArrayList<Tag> blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                //direction int 0
                //honey_level int 1
                CompoundTag copy = block.copy();
                IntTag directionTag = (IntTag) blockStates.get(0);
                IntTag honeyLevelTag = (IntTag) blockStates.get(1);
                copy.putShort("data", (short) (honeyLevelTag.getData() << 2 | convertFacingDirectionToDirection(directionTag.getData())));
                if (name.equals("minecraft:bee_nest")) {
                    newBeeNest.add(copy);
                } else {
                    newBeehive.add(copy);
                }
            } else if (name.equals("minecraft:decorated_pot")) {
                ArrayList<Tag> blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                CompoundTag copy = block.copy();
                IntTag directionTag = (IntTag) blockStates.get(0);
                int data = directionTag.getData();
                copy.putShort("data", (short) data);
                newDecoratedPot.add(copy);
            }
        }

        int v = -1;
        for (CompoundTag tag : oldBaseListTag.getAll()) {
            String name = tag.getString("name");
            if (v == -1) {
                v = tag.getInt("version");
            }
            /*if (name.equals("minecraft:crimson_pressure_plate")) {
                for (CompoundTag block : newCrimsonPressurePlate.getAll()) {
                    block.putInt("version", tag.getInt("version"));
                    newTagList.add(block);
                }
            } else if (name.equals("minecraft:warped_pressure_plate")) {
                for (CompoundTag block : newWarpedPressurePlate.getAll()) {
                    block.putInt("version", tag.getInt("version"));
                    newTagList.add(block);
                }
            }*/
            /*if (name.equals("minecraft:bee_nest")) {
                for (CompoundTag block : newBeeNest.getAll()) {
                    block.putInt("version", tag.getInt("version"));
                    newTagList.add(block);
                }
            } else if (name.equals("minecraft:beehive")) {
                for (CompoundTag block : newBeehive.getAll()) {
                    block.putInt("version", tag.getInt("version"));
                    newTagList.add(block);
                }
            }*/ //else {
                newTagList.add(tag);
            //}
        }
        /*for (CompoundTag block : newDecoratedPot.getAll()) {
            block.putInt("version", v);
            newTagList.add(block);
        }*/

        for (CompoundTag block : newMangrovePlanks.getAll()) {
            block.putInt("version", v);
            newTagList.add(block);
        }
        for (CompoundTag block : newBambooPlanks.getAll()) {
            block.putInt("version", v);
            newTagList.add(block);
        }
        for (CompoundTag block : newCherryPlanks.getAll()) {
            block.putInt("version", v);
            newTagList.add(block);
        }

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/n_runtime_block_states_" + oldBlockStatesVersion + ".dat"));
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
