package cn.lanink.dataconvert.convert;

import com.google.gson.*;
import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 将 CloudburstMC/Data 的 recipes_944.json 转换为 Nukkit-MOT 可用的 recipes_944.json
 * 核心逻辑：通过物品名称作桥梁，将 CB 的 runtime item ID 映射到 NK 的 runtime item ID
 *
 * @author LT_Name
 */
@Log4j2
public class RecipeConvert {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
//        convert(924);
    }

    public static void convert(int targetVersion) {
        // 1. 加载 CB runtime_item_states: id -> name
        Map<Integer, String> cbIdToName = new HashMap<>();
        try (InputStream stream = new FileInputStream("src/main/resources/CB_Data/runtime_item_states_" + targetVersion + ".json")) {
            JsonArray cbItems = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : cbItems) {
                JsonObject item = element.getAsJsonObject();
                cbIdToName.put(item.get("id").getAsInt(), item.get("name").getAsString());
            }
        } catch (Exception e) {
            log.error("加载 CB runtime_item_states_944.json 失败", e);
            return;
        }
        log.info("加载 CB runtime_item_states: {} 条", cbIdToName.size());

        // 2. 加载 NK runtime_item_states: name -> id
        Map<String, Integer> nkNameToId = new HashMap<>();
        try (InputStream stream = new FileInputStream("src/main/resources/Target_Data/runtime_item_states_" + targetVersion + ".json")) {
            JsonArray nkItems = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement element : nkItems) {
                JsonObject item = element.getAsJsonObject();
                nkNameToId.put(item.get("name").getAsString(), item.get("id").getAsInt());
            }
        } catch (Exception e) {
            log.error("加载 NK runtime_item_states_{}.json 失败", targetVersion, e);
            return;
        }
        log.info("加载 NK runtime_item_states_{}: {} 条", targetVersion, nkNameToId.size());

        // 3. 读取 CB recipes_944.json
        JsonObject recipesRoot;
        try (InputStream stream = new FileInputStream("src/main/resources/CB_Data/recipes_944.json")) {
            recipesRoot = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            log.error("加载 CB recipes_944.json 失败", e);
            return;
        }

        // 4. 遍历并转换配方
        JsonArray recipes = recipesRoot.getAsJsonArray("recipes");
        JsonArray convertedRecipes = new JsonArray();
        int skippedCount = 0;
        Set<String> unmappedItems = new HashSet<>();

        for (JsonElement element : recipes) {
            JsonObject recipe = element.getAsJsonObject();
            int type = recipe.get("type").getAsInt();

            try {
                switch (type) {
                    case 0: // 无序配方 (shapeless)
                    case 5: // 无序配方 (shulker box)
                        if (!convertShapelessRecipe(recipe, cbIdToName, nkNameToId, unmappedItems)) {
                            skippedCount++;
                            continue;
                        }
                        break;
                    case 1: // 有序配方 (shaped)
                        if (!convertShapedRecipe(recipe, cbIdToName, nkNameToId, unmappedItems)) {
                            skippedCount++;
                            continue;
                        }
                        break;
                    case 3: // 冶炼配方 (furnace)
                        if (!convertFurnaceRecipe(recipe, cbIdToName, nkNameToId, unmappedItems)) {
                            skippedCount++;
                            continue;
                        }
                        break;
                    case 4: // multi recipe
                    case 8: // smithing_trim
                    case 9: // smithing_transform
                        // 直接保留
                        break;
                    default:
                        log.warn("未知配方类型: {}", type);
                        break;
                }
                convertedRecipes.add(recipe);
            } catch (Exception e) {
                log.error("转换配方失败: {}", recipe, e);
                skippedCount++;
            }
        }

        // 5. 构建输出
        JsonObject output = new JsonObject();
        output.addProperty("version", targetVersion);
        output.add("recipes", convertedRecipes);

        // potionMixes 和 containerMixes 直接保留
        if (recipesRoot.has("potionMixes")) {
            output.add("potionMixes", recipesRoot.get("potionMixes"));
        }
        if (recipesRoot.has("containerMixes")) {
            output.add("containerMixes", recipesRoot.get("containerMixes"));
        }

        // 6. 写入文件
        File outputFile = new File("src/main/resources/Target_Data/recipes_" + targetVersion + ".json");
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            GSON.toJson(output, writer);
        } catch (Exception e) {
            log.error("写入 recipes_944.json 失败", e);
            return;
        }

        log.info("转换完成! 总配方: {}, 成功: {}, 跳过: {}", recipes.size(), convertedRecipes.size(), skippedCount);
        if (!unmappedItems.isEmpty()) {
            log.warn("无法映射的物品 ({} 个): {}", unmappedItems.size(), unmappedItems);
        }
    }

    /**
     * 转换无序配方 (type 0/5)
     */
    private static boolean convertShapelessRecipe(JsonObject recipe, Map<Integer, String> cbIdToName, Map<String, Integer> nkNameToId, Set<String> unmappedItems) {
        // 转换 input
        JsonArray input = recipe.getAsJsonArray("input");
        for (JsonElement inputElement : input) {
            JsonObject inputItem = inputElement.getAsJsonObject();
            if ("default".equals(inputItem.get("type").getAsString())) {
                if (!convertItemId(inputItem, "itemId", cbIdToName, nkNameToId, unmappedItems)) {
                    return false;
                }
            }
        }
        // 转换 output
        JsonArray output = recipe.getAsJsonArray("output");
        for (JsonElement outputElement : output) {
            JsonObject outputItem = outputElement.getAsJsonObject();
            if (!convertItemId(outputItem, "legacyId", cbIdToName, nkNameToId, unmappedItems)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 转换有序配方 (type 1)
     */
    private static boolean convertShapedRecipe(JsonObject recipe, Map<Integer, String> cbIdToName, Map<String, Integer> nkNameToId, Set<String> unmappedItems) {
        // 转换 input (map)
        JsonObject input = recipe.getAsJsonObject("input");
        for (Map.Entry<String, JsonElement> entry : input.entrySet()) {
            JsonObject inputItem = entry.getValue().getAsJsonObject();
            if ("default".equals(inputItem.get("type").getAsString())) {
                if (!convertItemId(inputItem, "itemId", cbIdToName, nkNameToId, unmappedItems)) {
                    return false;
                }
            }
        }
        // 转换 output
        JsonArray output = recipe.getAsJsonArray("output");
        for (JsonElement outputElement : output) {
            JsonObject outputItem = outputElement.getAsJsonObject();
            if (!convertItemId(outputItem, "legacyId", cbIdToName, nkNameToId, unmappedItems)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 转换冶炼配方 (type 3)
     */
    private static boolean convertFurnaceRecipe(JsonObject recipe, Map<Integer, String> cbIdToName, Map<String, Integer> nkNameToId, Set<String> unmappedItems) {
        JsonObject input = recipe.getAsJsonObject("input");
        if (!convertItemId(input, "legacyId", cbIdToName, nkNameToId, unmappedItems)) {
            return false;
        }
        JsonObject output = recipe.getAsJsonObject("output");
        if (!convertItemId(output, "legacyId", cbIdToName, nkNameToId, unmappedItems)) {
            return false;
        }
        return true;
    }

    /**
     * 将 CB 的 runtime item ID 转换为 NK 的 runtime item ID
     *
     * @return 是否转换成功
     */
    private static boolean convertItemId(JsonObject item, String idField, Map<Integer, String> cbIdToName, Map<String, Integer> nkNameToId, Set<String> unmappedItems) {
        int cbId = item.get(idField).getAsInt();
        String name = cbIdToName.get(cbId);
        if (name == null) {
            unmappedItems.add("cb_id:" + cbId);
            log.warn("CB runtime_item_states 中找不到 ID: {}", cbId);
            return false;
        }
        Integer nkId = nkNameToId.get(name);
        if (nkId == null) {
            unmappedItems.add(name);
            log.warn("NK runtime_item_states 中找不到物品: {} (CB ID: {})", name, cbId);
            return false;
        }
        item.addProperty(idField, nkId);
        return true;
    }
}
