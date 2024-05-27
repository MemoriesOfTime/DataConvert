package cn.lanink.dataconvert;

import cn.lanink.dataconvert.convert.CreativeItemsJsoinConvert;
import cn.lanink.dataconvert.convert.RuntimeBlockStateConvert;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * @author LT_Name
 */
@Log4j2
public class DataConvert {

    public static void main(String[] args) {
        int sourceProtocol = 671;
        int targetProtocol = 685;

        log.info("Hello, World!");

        log.info("start convert runtime_block_states");
        try {
            RuntimeBlockStateConvert.convert(sourceProtocol, targetProtocol);
        } catch (IOException e) {
            log.error("convert runtime_block_states error", e);
        }
        log.info("convert blockstate success");

        log.info("start convert creative_items");
        try {
            CreativeItemsJsoinConvert.convert(targetProtocol);
        } catch (IOException e) {
            log.error("convert creative_items error", e);
        }
        log.info("convert creative_items success");

        //TODO other
        System.exit(0);
    }

}
