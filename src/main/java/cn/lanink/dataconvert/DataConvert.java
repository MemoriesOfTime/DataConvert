package cn.lanink.dataconvert;

import cn.lanink.dataconvert.convert.RuntimeBlockStateConvert;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * @author LT_Name
 */
@Log4j2
public class DataConvert {

    public static void main(String[] args) {
        log.info("Hello, World!");
        log.info("start convert runtime_block_states");
        try {
            RuntimeBlockStateConvert.convert();
        } catch (IOException e) {
            log.error("convert runtime_block_states error", e);
        }
        log.info("convert blockstate success");

        //TODO creative_items
        System.exit(0);
    }

}
