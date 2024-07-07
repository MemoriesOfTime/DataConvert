package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.NBTIO1;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.*;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Log4j2
public class RuntimeBlockStateConvertOld {

    public static void main(String[] args) throws IOException {
        convert(419);
        convert(428);
        convert(440);
        convert(448);
        convert(465);
        convert(471);
        convert(486);
        convert(503);
        convert(527);
        convert(544);
        convert(560);
        convert(567);
        convert(575);
        convert(582);
        /*convert(589);
        convert(594);
        convert(618);
        convert(622);
        convert(630);
        convert(649);
        convert(662);
        convert(671);
        convert(685);*/


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

        //加载PMMP的数据
        List<CompoundTag> originTags = new ArrayList<>();
        List<CompoundTag> tags = new ArrayList<>();
        ListTag<CompoundTag> tags2 = null;
        if (oldBlockStatesVersion >= 419) {
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

        if (tags2 != null) {
            for (CompoundTag tag : tags2.getAll()) {
                tags.add(tag.getCompound("block"));
            }
        }

        for (CompoundTag compoundTag : tags) {
            //log.info(compoundTag.toSNBT());
        }

        ListTag<CompoundTag> newTagList = new ListTag<>();

        ListTag<CompoundTag> newBeeNest = new ListTag<>(); //蜂巢 蜂箱
        ListTag<CompoundTag> newBeehive = new ListTag<>();

        ListTag<CompoundTag> newDecoratedPot = new ListTag<>(); //陶罐

        ListTag<CompoundTag> newCrimsonPressurePlate = new ListTag<>(); //绯红木压力板
        ListTag<CompoundTag> newWarpedPressurePlate = new ListTag<>(); //诡异木压力板

        ListTag<CompoundTag> newMangrovePlanks = new ListTag<>(); //红木板
        ListTag<CompoundTag> newBambooPlanks = new ListTag<>(); //竹板
        ListTag<CompoundTag> newCherryPlanks  = new ListTag<>(); //樱花木板

        ListTag<CompoundTag> newCrimsonStairs = new ListTag<>(); //绯红木楼梯
        ListTag<CompoundTag> newWarpedStairs = new ListTag<>(); //诡异木楼梯

        ListTag<CompoundTag> newStrippedCherryLog = new ListTag<>(); //去皮樱花原木
        ListTag<CompoundTag> newCherryLog = new ListTag<>(); //樱花原木
        ListTag<CompoundTag> newStrippedCherryWood = new ListTag<>(); //去皮樱花木
        ListTag<CompoundTag> newCherryWood = new ListTag<>(); //樱花木
        ListTag<CompoundTag> newCherrySapling = new ListTag<>(); //樱花树苗
        ListTag<CompoundTag> newCherryLeaves = new ListTag<>(); //樱花树叶

        ListTag<CompoundTag> newChain = new ListTag<>(); //锁链

        ListTag<CompoundTag> newCrimsonFenceGate = new ListTag<>(); //绯红木栅栏门
        ListTag<CompoundTag> newWarpedFenceGate = new ListTag<>(); //诡异木栅栏门

        ListTag<CompoundTag> newwarped_button = new ListTag<>(); //诡异木按钮
        ListTag<CompoundTag> newcrimson_button = new ListTag<>(); //绯红木按钮

        ListTag<CompoundTag> new_crimson_slab = new ListTag<>(); //绯红木台阶
        ListTag<CompoundTag> new_warped_slab = new ListTag<>(); //诡异木台阶

        ListTag<CompoundTag> new_crimson_double_slab = new ListTag<>();
        ListTag<CompoundTag> new_warped_double_slab = new ListTag<>();

        ListTag<CompoundTag> new_polished_blackstone_brick_stairs = new ListTag<>();
        ListTag<CompoundTag> new_blackstone_stairs = new ListTag<>();

        ListTag<CompoundTag> new_blackstone_slab = new ListTag<>();
        ListTag<CompoundTag> new_blackstone_double_slab = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_brick_slab = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_brick_double_slab = new ListTag<>();
        ListTag<CompoundTag> new_twisting_vines = new ListTag<>();

        ListTag<CompoundTag> new_polished_blackstone_stairs = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_slab = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_double_slab = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_pressure_plate = new ListTag<>();

        ListTag<CompoundTag> new_polished_blackstone_button = new ListTag<>();
        ListTag<CompoundTag> new_polished_blackstone_wall = new ListTag<>();

        ListTag<CompoundTag> new_warped_hyphae = new ListTag<>();
        ListTag<CompoundTag> new_crimson_hyphae = new ListTag<>();

        ListTag<CompoundTag> new_stripped_crimson_hyphae = new ListTag<>();
        ListTag<CompoundTag> new_stripped_warped_hyphae = new ListTag<>();

        ListTag<CompoundTag> new_flower = new ListTag<>();

        for (CompoundTag block : tags) {
            String name = block.getString("name");
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

            ArrayList<Tag> blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
            CompoundTag copy = block.copy();
            switch (name.toLowerCase()) {
                case "minecraft:poppy":
                    copy.putInt("id", 38);
                    copy.putShort("data", 0);
                    new_flower.add(copy);

                    for (int i=11; i<=15; i++) {
                        CompoundTag copy1 = copy.copy();
                        copy1.putInt("id", 38);
                        copy1.putShort("data", i);
                        new_flower.add(copy1);
                    }
                    break;
                case "minecraft:blue_orchid":
                    copy.putInt("id", 38);
                    copy.putShort("data", 1);
                    new_flower.add(copy);
                    break;
                case "minecraft:allium":
                    copy.putInt("id", 38);
                    copy.putShort("data", 2);
                    new_flower.add(copy);
                    break;
                case "minecraft:azure_bluet":
                    copy.putInt("id", 38);
                    copy.putShort("data", 3);
                    new_flower.add(copy);
                    break;
                case "minecraft:red_tulip":
                    copy.putInt("id", 38);
                    copy.putShort("data", 4);
                    new_flower.add(copy);
                    break;
                case "minecraft:orange_tulip":
                    copy.putInt("id", 38);
                    copy.putShort("data", 5);
                    new_flower.add(copy);
                    break;
                case "minecraft:white_tulip":
                    copy.putInt("id", 38);
                    copy.putShort("data", 6);
                    new_flower.add(copy);
                    break;
                case "minecraft:pink_tulip":
                    copy.putInt("id", 38);
                    copy.putShort("data", 7);
                    new_flower.add(copy);
                    break;
                case "minecraft:oxeye_daisy":
                    copy.putInt("id", 38);
                    copy.putShort("data", 8);
                    new_flower.add(copy);
                    break;
                case "minecraft:cornflower":
                    copy.putInt("id", 38);
                    copy.putShort("data", 9);
                    new_flower.add(copy);
                    break;
                case "minecraft:lily_of_the_valley":
                    copy.putInt("id", 38);
                    copy.putShort("data", 10);
                    new_flower.add(copy);
                    break;
                case "minecraft:crimson_pressure_plate":
                    IntTag directionTag = (IntTag) blockStates.get(0);
                    copy.putShort("data", directionTag.getData());
                    newCrimsonPressurePlate.add(copy);
                    break;
                case "minecraft:warped_pressure_plate":
                    copy = block.copy();
                    directionTag = (IntTag) blockStates.get(0);
                    copy.putShort("data", directionTag.getData());
                    newWarpedPressurePlate.add(copy);
                    break;
                case "minecraft:polished_blackstone_pressure_plate":
                    copy = block.copy();
                    copy.putShort("data", ((IntTag) blockStates.get(0)).getData());
                    new_polished_blackstone_pressure_plate.add(copy);
                    break;
                /*case "minecraft:mangrove_planks":
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
                case "minecraft:crimson_stairs":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int upsideDownBit = ((ByteTag) blockStates.get(0)).getData();
                    int weirdoDirection = ((IntTag) blockStates.get(1)).getData();
                    int data = weirdoDirection & 0x3 | upsideDownBit << 2;
                    copy.putShort("data", data);
                    newCrimsonStairs.add(copy);
                    break;
                case "minecraft:warped_stairs":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    upsideDownBit = ((ByteTag) blockStates.get(0)).getData();
                    weirdoDirection = ((IntTag) blockStates.get(1)).getData();
                    data = weirdoDirection & 0x3 | upsideDownBit << 2;
                    copy.putShort("data", data);
                    newWarpedStairs.add(copy);
                    break;
                case "minecraft:polished_blackstone_brick_stairs":
                    copy = block.copy();
                    upsideDownBit = ((ByteTag) blockStates.get(0)).getData();
                    weirdoDirection = ((IntTag) blockStates.get(1)).getData();
                    data = weirdoDirection & 0x3 | upsideDownBit << 2;
                    copy.putShort("data", data);
                    new_polished_blackstone_brick_stairs.add(copy);
                    break;
                case "minecraft:blackstone_stairs":
                    copy = block.copy();
                    upsideDownBit = ((ByteTag) blockStates.get(0)).getData();
                    weirdoDirection = ((IntTag) blockStates.get(1)).getData();
                    data = weirdoDirection & 0x3 | upsideDownBit << 2;
                    copy.putShort("data", data);
                    new_blackstone_stairs.add(copy);
                    break;
                case "minecraft:polished_blackstone_stairs":
                    copy = block.copy();
                    upsideDownBit = ((ByteTag) blockStates.get(0)).getData();
                    weirdoDirection = ((IntTag) blockStates.get(1)).getData();
                    data = weirdoDirection & 0x3 | upsideDownBit << 2;
                    copy.putShort("data", data);
                    new_polished_blackstone_stairs.add(copy);
                    break;
                case "minecraft:stripped_cherry_log":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    String pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                    data = switch (pillar_axis) {
                        case "x" -> 4;
                        case "z" -> 8;
                        default -> 0; //y
                    };
                    copy.putShort("data", data);
                    newStrippedCherryLog.add(copy);
                    break;
                case "minecraft:cherry_log":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                    data = switch (pillar_axis) {
                        case "x" -> 4;
                        case "z" -> 8;
                        default -> 0; //y
                    };
                    copy.putShort("data", data);
                    newCherryLog.add(copy);
                    break;
                case "minecraft:stripped_cherry_wood":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                    data = switch (pillar_axis) {
                        case "x" -> 0x10;
                        case "z" -> 0x20;
                        default -> 0; //y
                    };
                    copy.putShort("data", data);
                    newStrippedCherryWood.add(copy);
                    break;
                case "minecraft:cherry_wood":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int stripped_bit = ((ByteTag) blockStates.get(0)).getData();
                    pillar_axis = ((StringTag) blockStates.get(1)).parseValue();
                    data = stripped_bit << 3 | switch (pillar_axis) {
                        case "x" -> 0x10;
                        case "z" -> 0x20;
                        default -> 0; //y
                    };
                    copy.putShort("data", data);
                    newCherryWood.add(copy);
                    break;
                case "minecraft:cherry_sapling":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int age_bit = ((ByteTag) blockStates.get(0)).getData();
                    copy.putShort("data", age_bit << 3);
                    newCherrySapling.add(copy);
                    break;
                case "minecraft:cherry_leaves":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int persistent = ((ByteTag) blockStates.get(0)).getData();
                    int update = ((ByteTag) blockStates.get(0)).getData();
                    copy.putShort("data", persistent << 1 | update);
                    newCherryLeaves.add(copy);
                    break;
                case "minecraft:chain":
                    copy = block.copy();
                    try {
                        blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                        pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                        copy.putShort("data", (short) switch (pillar_axis) {
                            case "x" -> 1;
                            case "z" -> 2;
                            default -> 0; //y
                        });
                    } catch (Exception e) {
                        copy.putShort("data", (short) 0);
                    }
                    newChain.add(copy);
                    break;
                    */
                case "minecraft:warped_hyphae":
                    copy = block.copy();
                    try {
                        String pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                        copy.putShort("data", (short) switch (pillar_axis) {
                            case "x" -> 1;
                            case "z" -> 2;
                            default -> 0; //y
                        });
                    } catch (Exception e) {
                        copy.putShort("data", (short) 0);
                        log.error("Unable to find pillar_axis for warped_hyphae");
                    }
                    new_warped_hyphae.add(copy);
                    break;
                case "minecraft:crimson_hyphae":
                    copy = block.copy();
                    try {
                        String pillar_axis = ((StringTag) blockStates.get(0)).parseValue();
                        copy.putShort("data", (short) switch (pillar_axis) {
                            case "x" -> 1;
                            case "z" -> 2;
                            default -> 0; //y
                        });
                    } catch (Exception e) {
                        copy.putShort("data", (short) 0);
                        log.error("Unable to find pillar_axis for crimson_hyphae");
                    }
                    new_crimson_hyphae.add(copy);
                    break;
                case "minecraft:stripped_crimson_hyphae":
                    copy = block.copy();
                    try {
                        StringTag stringTag = null;
                        if (blockStates.get(0) instanceof StringTag) {
                            stringTag = (StringTag) blockStates.get(0);
                        } else if (blockStates.get(1) instanceof StringTag) {
                            if (((IntTag)blockStates.get(0)).parseValue() > 0) {
                                continue;
                            }
                            stringTag = (StringTag) blockStates.get(1);
                        }
                        String pillar_axis = stringTag.parseValue();
                        copy.putShort("data", (short) switch (pillar_axis) {
                            case "x" -> 1;
                            case "z" -> 2;
                            default -> 0; //y
                        });
                    } catch (Exception e) {
                        copy.putShort("data", (short) 0);
                        log.error("Unable to find pillar_axis for stripped_crimson_hyphae");
                    }
                    new_stripped_crimson_hyphae.add(copy);
                    break;
                case "minecraft:stripped_warped_hyphae":
                    copy = block.copy();
                    try {
                        StringTag stringTag = null;
                        if (blockStates.get(0) instanceof StringTag) {
                            stringTag = (StringTag) blockStates.get(0);
                        } else if (blockStates.get(1) instanceof StringTag) {
                            if (((IntTag)blockStates.get(0)).parseValue() > 0) {
                                continue;
                            }
                            stringTag = (StringTag) blockStates.get(1);
                        }
                        String pillar_axis = stringTag.parseValue();
                        copy.putShort("data", (short) switch (pillar_axis) {
                            case "x" -> 1;
                            case "z" -> 2;
                            default -> 0; //y
                        });
                    } catch (Exception e) {
                        copy.putShort("data", (short) 0);
                        log.error("Unable to find pillar_axis for stripped_warped_hyphae");
                    }
                    new_stripped_warped_hyphae.add(copy);
                    break;
                /*
                case "minecraft:crimson_fence_gate":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int in_wall_bit = ((ByteTag) blockStates.get(0)).parseValue();
                    int open_bit = ((ByteTag) blockStates.get(1)).parseValue();
                    int direction = ((IntTag) blockStates.get(2)).parseValue();
                    copy.putShort("data", (short) (in_wall_bit << 3 | open_bit << 2 | direction & 0x3));
                    newCrimsonFenceGate.add(copy);
                    break;
                case "minecraft:warped_fence_gate":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    in_wall_bit = ((ByteTag) blockStates.get(0)).parseValue();
                    open_bit = ((ByteTag) blockStates.get(1)).parseValue();
                    direction = ((IntTag) blockStates.get(2)).parseValue();
                    copy.putShort("data", (short) (in_wall_bit << 3 | open_bit << 2 | direction & 0x3));
                    newWarpedFenceGate.add(copy);
                    break;
                case "minecraft:warped_button":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    int button_pressed_bit = ((ByteTag) blockStates.get(0)).parseValue();
                    int facing_direction = ((IntTag) blockStates.get(1)).parseValue();
                    copy.putShort("data", (short) (button_pressed_bit << 3 | facing_direction & 0x7));
                    newwarped_button.add(copy);
                    break;
                case "minecraft:crimson_button":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    button_pressed_bit = ((ByteTag) blockStates.get(0)).parseValue();
                    facing_direction = ((IntTag) blockStates.get(1)).parseValue();
                    copy.putShort("data", (short) (button_pressed_bit << 3 | facing_direction & 0x7));
                    newcrimson_button.add(copy);
                    break;
                case "minecraft:polished_blackstone_button":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    button_pressed_bit = ((ByteTag) blockStates.get(0)).parseValue();
                    facing_direction = ((IntTag) blockStates.get(1)).parseValue();
                    copy.putShort("data", (short) (button_pressed_bit << 3 | facing_direction & 0x7));
                    new_polished_blackstone_button.add(copy);
                    break;
                case "minecraft:crimson_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_crimson_slab.add(copy);
                    break;
                case "minecraft:warped_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_warped_slab.add(copy);
                    break;
                case "minecraft:crimson_double_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_crimson_double_slab.add(copy);
                    break;
                case "minecraft:warped_double_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_warped_double_slab.add(copy);
                    break;
                case "minecraft:blackstone_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_blackstone_slab.add(copy);
                    break;
                case "minecraft:blackstone_double_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_blackstone_double_slab.add(copy);
                    break;
                case "minecraft:polished_blackstone_brick_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_polished_blackstone_brick_slab.add(copy);
                    break;
                case "minecraft:polished_blackstone_brick_double_slab":
                    copy = block.copy();
                    blockStates = new ArrayList<>(block.getCompound("states").getAllTags());
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_polished_blackstone_brick_double_slab.add(copy);
                    break;*/
                case "minecraft:twisting_vines":
                    copy = block.copy();
                    copy.putShort("data", ((IntTag) blockStates.get(0)).parseValue());
                    new_twisting_vines.add(copy);
                    break;
                /*case "minecraft:polished_blackstone_slab":
                    copy = block.copy();
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_polished_blackstone_slab.add(copy);
                    break;
                case "minecraft:polished_blackstone_double_slab":
                    copy = block.copy();
                    copy.putShort("data", ((ByteTag) blockStates.get(0)).parseValue());
                    new_polished_blackstone_double_slab.add(copy);
                    break;*/
                case "minecraft:polished_blackstone_wall":
                    copy = block.copy();
                    copy.putShort("data", 0);
                    new_polished_blackstone_wall.add(copy);
                    break;
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

            /*if (name.equals("minecraft:crimson_stairs")) {
                for (CompoundTag block : newCrimsonStairs.getAll()) {
                    CompoundTag copy;
                    if (oldBlockStatesVersion < 419) {
                        copy = tag.copy();
                        int data = block.getShort("data");
                        block.remove("data");
                        copy.putShort("data", data);
                        copy.put("block", block);
                    } else {
                        copy = block;
                        block.putInt("version", tag.getInt("version"));
                    }
                    newTagList.add(copy);
                }
            } else if (name.equals("minecraft:warped_stairs")) {
                for (CompoundTag block : newWarpedStairs.getAll()) {
                    CompoundTag copy;
                    if (oldBlockStatesVersion < 419) {
                        copy = tag.copy();
                        int data = block.getShort("data");
                        block.remove("data");
                        copy.putShort("data", data);
                        copy.put("block", block);
                    } else {
                        copy = block;
                        block.putInt("version", tag.getInt("version"));
                    }
                    newTagList.add(copy);
                }
            }*/

            /*if (name.equals("minecraft:stripped_crimson_hyphae")) {
                for (CompoundTag block : new_stripped_crimson_hyphae.getAll()) {
                    block.putInt("version", v);
                    newTagList.add(block);
                }
            } else if (name.equals("minecraft:stripped_warped_hyphae")) {
                for (CompoundTag block : new_stripped_warped_hyphae.getAll()) {
                    block.putInt("version", v);
                    newTagList.add(block);
                }
            } else {*/
                if (tag.getInt("id") == 38) {
                    continue;
                }

                //重复检查
                int runtimeId = tag.getInt("runtimeId");
                boolean isDuplicate = false;
                for (CompoundTag tag1 : newTagList.getAll()) {
                    if (tag1.getInt("runtimeId") == runtimeId
                            && tag1.getString("name").equals(tag.getString("name"))
                            && tag1.getInt("id") == tag.getInt("id")
                            && tag1.getInt("data") == tag.getInt("data")) {
                        isDuplicate = true;
                        log.warn("Duplicate : old:{}\nnew:{}", tag1, tag);
                    }
                }
                if (!isDuplicate) {
                    newTagList.add(tag);
                }
            //}
        }

        for (CompoundTag block : new_flower.getAll()) {
            block.putInt("version", v);
            newTagList.add(block);
        }

        //按照runtimeId排序
        newTagList.getAllUnsafe().sort(Comparator.comparingInt(o -> o.getInt("runtimeId")));

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/new/runtime_block_states_" + oldBlockStatesVersion + ".dat"));
        NBTIO1.writeGZIPCompressed(newTagList, outputStream, ByteOrder.BIG_ENDIAN);

        ListTag<CompoundTag> originTagList = new ListTag<>();
        for (CompoundTag tag : originTags) {
            originTagList.add(tag);
        }
        outputStream = new BufferedOutputStream(new FileOutputStream("src/main/resources/Target_Data/vanilla_palette_" + oldBlockStatesVersion + ".nbt"));
        NBTIO1.writeGZIPCompressed(new CompoundTag().put("blocks", originTagList), outputStream, ByteOrder.BIG_ENDIAN);
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
