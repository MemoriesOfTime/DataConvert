package cn.lanink.dataconvert;

import cn.lanink.dataconvert.convert.CreativeItemsJsoinConvert;
import cn.lanink.dataconvert.convert.RuntimeBlockStateConvert;
import cn.lanink.dataconvert.convert.ayear.RuntimeBlockStatusGenerator;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * @author LT_Name
 */
@Log4j2
public class DataConvert {

    public static void main(String[] args) {
        new RuntimeBlockStatusGenerator("582").start(); // 1.19.80
        new RuntimeBlockStatusGenerator("589").start(); // 1.20.0
        new RuntimeBlockStatusGenerator("594").start(); // 1.20.10
        new RuntimeBlockStatusGenerator("618").start(); // 1.20.30
        new RuntimeBlockStatusGenerator("622").start(); // 1.20.40
        new RuntimeBlockStatusGenerator("630").start(); // 1.20.50
        new RuntimeBlockStatusGenerator("649").start(); // 1.20.60
        new RuntimeBlockStatusGenerator("662").start(); // 1.20.70
        new RuntimeBlockStatusGenerator("671").start(); // 1.20.80
        new RuntimeBlockStatusGenerator("685").start(); // 1.21.0
        new RuntimeBlockStatusGenerator("712").start(); // 1.21.20
        new RuntimeBlockStatusGenerator("729").start(); // 1.21.30

//        new RuntimeBlockStatusGenerator("671").compare();
    }

    /*
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
     */

}
