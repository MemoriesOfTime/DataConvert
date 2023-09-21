package cn.lanink.dataconvert.convert.item.creative;

import cn.lanink.dataconvert.utils.Utils;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.nbt.*;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author LT_Name
 */
@Log4j2
public class CreativeItemsJsonConvert {

    public static void main(String[] args) throws IOException {
        convert(685);
        convert(712);
        convert(729);
        convert(748);
    }

    public static void convert(int protocol) throws IOException {
        //加载cb数据
        List<NbtMap> tags = new ArrayList<>();
        try (InputStream stream = new FileInputStream("src/main/resources/CB_Data/block_palette_" + protocol + ".nbt")) {
            int runtimeId = 0;
            Tag tag1 = NBTIO.readTag(new BufferedInputStream(new GZIPInputStream(stream)), ByteOrder.BIG_ENDIAN, false);
            ListTag<CompoundTag> blocks = ((CompoundTag) tag1).getList("blocks", CompoundTag.class);
            for (CompoundTag compoundTag : blocks.getAll()) {
                compoundTag.remove("network_id");
                compoundTag.remove("name_hash");
                compoundTag.remove("block_id");
                //compoundTag.putInt("runtimeId", runtimeId++);
                tags.add(Utils.compoundTag2NbtMap(compoundTag.copy()));
            }
        } catch (IOException e1) {
            throw new AssertionError(e1);
        }

        JsonArray groupsArray = null;
        JsonArray itemsArray;
        try (InputStream stream =  new FileInputStream("src/main/resources/CB_Data/CreativeItems/creative_items_" + protocol + ".json")) {
            JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (jsonObject.get("groups") != null) {
                groupsArray = jsonObject.getAsJsonArray("groups");
            }
            itemsArray = jsonObject.getAsJsonArray("items");
        } catch (Exception e) {
            throw new AssertionError("Error loading required creative_items.json!", e);
        }

        if (groupsArray != null) {
            int id = 0;
            for (JsonElement element : groupsArray) {
                JsonObject jsonObject = element.getAsJsonObject();

                JsonObject icon = jsonObject.getAsJsonObject("icon");
                JsonElement block_state_b64 = icon.get("block_state_b64");
                if (block_state_b64 != null) {
                    icon.remove("block_state_b64");
                    int runtimeId = convertBlockStateToBlockRuntimeId(icon.get("id").getAsString(), block_state_b64, tags);
                    if (runtimeId != -1) {
                        icon.addProperty("blockRuntimeId", runtimeId);
                    }
                }
            }
        }

        for (JsonElement element : itemsArray) {
            JsonObject jsonObject = element.getAsJsonObject();

            // 转换 block_state_b64 -> blockRuntimeId
            JsonElement block_state_b64 = jsonObject.get("block_state_b64");
            if (block_state_b64 != null) {
                jsonObject.remove("block_state_b64");
                int runtimeId = convertBlockStateToBlockRuntimeId(jsonObject.get("id").getAsString(), block_state_b64, tags);
                if (runtimeId != -1) {
                    jsonObject.addProperty("blockRuntimeId", runtimeId);
                }
            }
        }

        JsonObject jsonObject = new JsonObject();
        if (groupsArray != null) {
            jsonObject.add("groups", groupsArray);
        }
        jsonObject.add("items", itemsArray);
        cn.nukkit.utils.Utils.writeFile(new File("src/main/resources/Target_Data/CreativeItems/creative_items_" + protocol + ".json"), jsonObject.toString());
    }

    public static int find(List<NbtMap> tags, NbtMap nbtMap) {
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equals(nbtMap)) {
                return i;
            }
        }
        return -1;
    }

    private static int convertBlockStateToBlockRuntimeId(String name, JsonElement block_state_b64, List<NbtMap> tags) {
        if (block_state_b64 == null) {
            return -1;
        }

        NbtMap nbt = Utils.base64ToNbt(block_state_b64.getAsString());
        NbtMapBuilder builder = nbt.toBuilder();
        builder.remove("block_id");
        builder.remove("name_hash");
        builder.remove("network_id");

        //TODO 优化这个替换
        NbtMap states = nbt.getCompound("states");
        switch (name) {
            case "minecraft:piston":
                int facingDirection = states.getInt("facing_direction");
                if (facingDirection == 1) {
                    NbtMapBuilder builder1 = states.toBuilder();
                    builder1.put("facing_direction", 0);
                    states = builder1.build();
                }
                builder.putCompound("states", states);
                break;
            case "minecraft:furnace":
                String string = states.getString("minecraft:cardinal_direction");
                if (string.equalsIgnoreCase("south")) {
                    NbtMapBuilder builder1 = states.toBuilder();
                    builder1.putString("minecraft:cardinal_direction", "north");
                    states = builder1.build();
                }
                builder.putCompound("states", states);
                break;
        }

        nbt = builder.build();
        int runtimeId = find(tags, nbt);
        if (runtimeId == -1) {
            log.warn("未找到对应的方块数据: {}", nbt);
        }
        return runtimeId;
    }

}
