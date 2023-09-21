package cn.lanink.dataconvert;

import cn.lanink.dataconvert.convert.item.creative.CreativeItemsJsonConvert;
import cn.lanink.dataconvert.convert.RuntimeBlockStateConvert;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * @author LT_Name
 */
@Log4j2
public class DataConvert {

    public static void main(String[] args) {
        int sourceProtocol = 818;
        int targetProtocol = 827;

        log.info("Hello, World!");

        log.info("start convert runtime_block_states");
        try {
            RuntimeBlockStateConvert.convert(sourceProtocol, targetProtocol);
        } catch (IOException e) {
            log.error("convert runtime_block_states error", e);
        }
        log.info("convert blockstate success");

        // 生成网易版本数据
        log.info("start generate netease runtime_block_states");
        try {
            RuntimeBlockStateConvert.generateNetEaseData(sourceProtocol, targetProtocol);
        } catch (IOException e) {
            log.error("generate netease runtime_block_states error", e);
        }
        log.info("generate netease runtime_block_states success");

        log.info("start convert creative_items");
        try {
            CreativeItemsJsonConvert.convert(targetProtocol);
        } catch (IOException e) {
            log.error("convert creative_items error", e);
        }
        log.info("convert creative_items success");

        //TODO other
        System.exit(0);
    }

}
