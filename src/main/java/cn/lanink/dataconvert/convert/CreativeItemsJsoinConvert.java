package cn.lanink.dataconvert.convert;

import cn.lanink.dataconvert.utils.Utils;
import cn.nukkit.Server;
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
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * @author LT_Name
 */
@Log4j2
public class CreativeItemsJsoinConvert {

    public static void main(String[] args) throws IOException {
        convert(685);
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

        JsonArray itemsArray;
        try (InputStream stream =  new FileInputStream("src/main/resources/CB_Data/CreativeItems/creative_items_" + protocol + ".json")) {
            itemsArray = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("items");
        } catch (Exception e) {
            throw new AssertionError("Error loading required creative_items.json!", e);
        }

        for (JsonElement element : itemsArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            JsonElement block_state_b64 = jsonObject.get("block_state_b64");
            if (block_state_b64 != null) {
                jsonObject.remove("block_state_b64");
                NbtMap nbt = Utils.base64ToNbt(block_state_b64.getAsString());
                NbtMapBuilder builder = nbt.toBuilder();
                builder.remove("block_id");
                builder.remove("name_hash");
                builder.remove("network_id");
                nbt = builder.build();
                int runtimeId = fine(tags, nbt);
                if (runtimeId == -1) {
                    log.warn("未找到对应的方块数据: {}", nbt);
                    continue;
                }
                jsonObject.addProperty("runtime_id", runtimeId);
            }
        }

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("items", itemsArray);
        cn.nukkit.utils.Utils.writeFile(new File("src/main/resources/Target_Data/CreativeItems/creative_items_" + protocol + ".json"), jsonObject.toString());
    }

    public static int fine(List<NbtMap> tags, NbtMap nbtMap) {
        for (int i = 0; i < tags.size(); i++) {
            if (tags.get(i).equals(nbtMap)) {
                return i;
            }
        }
        return -1;
    }

}
