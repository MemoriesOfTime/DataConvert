package cn.lanink.dataconvert.utils;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * 外部数据源解析器
 * <p>
 * 统一管理对 Nukkit-MOT 等外部仓库数据的访问，工具类统一从这里获取外部数据文件。
 * <p>
 * 解析优先级：
 * <ol>
 *   <li>本地仓库覆盖（系统属性/环境变量指定路径，适合本机有克隆仓库的开发者）</li>
 *   <li>本地缓存目录（默认 cache/，可通过 dataconvert.cache.dir / DATACONVERT_CACHE_DIR 修改）</li>
 *   <li>从远程仓库下载到缓存目录</li>
 * </ol>
 * 设置系统属性 dataconvert.refresh=true 可强制重新下载（忽略已有缓存）。
 * <p>
 * 可用的本地仓库覆盖：
 * <ul>
 *   <li>nukkitmot.dir / NUKKIT_MOT_DIR — Nukkit-MOT 仓库根目录</li>
 * </ul>
 *
 * @author LT_Name
 */
@Log4j2
public class ExternalData {

    private static final String NUKKIT_MOT_RAW_BASE =
            "https://raw.githubusercontent.com/MemoriesOfTime/Nukkit-MOT/master/src/main/resources/";

    private static final String CACHE_DIR = configValue("dataconvert.cache.dir", "DATACONVERT_CACHE_DIR", "cache");

    private static final boolean REFRESH = "true".equalsIgnoreCase(System.getProperty("dataconvert.refresh", "false"));

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private ExternalData() {
    }

    /**
     * 获取 Nukkit-MOT 的国际版 runtime_block_states_XXX.dat
     */
    public static File nukkitMotRuntimeBlockStates(int version) throws IOException {
        return resolveNukkitMot("runtime_block_states_" + version + ".dat");
    }

    /**
     * 获取 Nukkit-MOT 的网易版 runtime_block_states_netease_XXX.dat
     */
    public static File nukkitMotNeteaseRuntimeBlockStates(int version) throws IOException {
        return resolveNukkitMot("runtime_block_states_netease_" + version + ".dat");
    }

    private static File resolveNukkitMot(String fileName) throws IOException {
        String overrideDir = nukkitMotOverrideDir();
        if (overrideDir != null) {
            File file = new File(overrideDir, "src/main/resources/" + fileName);
            if (file.isFile()) {
                return file;
            }
            log.warn("已配置 Nukkit-MOT 本地仓库覆盖（{}），但缺少 {}，回退到缓存/远程下载", overrideDir, fileName);
        }
        return downloadIfNeeded(fileName, NUKKIT_MOT_RAW_BASE + fileName);
    }

    private static String nukkitMotOverrideDir() {
        return configValue("nukkitmot.dir", "NUKKIT_MOT_DIR", null);
    }

    /**
     * 系统属性优先于环境变量，均未设置时返回默认值
     */
    private static String configValue(String property, String env, String defaultValue) {
        String value = System.getProperty(property);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getenv(env);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }

    private static File downloadIfNeeded(String fileName, String url) throws IOException {
        File cacheFile = new File(CACHE_DIR, fileName);
        if (cacheFile.isFile() && !REFRESH) {
            return cacheFile;
        }

        log.info("从远程下载: {} -> {}", url, cacheFile.getPath());
        HttpResponse<Path> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "DataConvert")
                    .GET()
                    .build();
            Path tempFile = Files.createTempFile("dataconvert-", ".download");
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(tempFile);
                throw new IOException("下载失败，HTTP " + response.statusCode() + ": " + url);
            }
            cacheFile.getParentFile().mkdirs();
            Files.move(tempFile, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("下载被中断: " + url, e);
        }
        return cacheFile;
    }

}
