package cn.lanink.dataconvert;

import cn.lanink.dataconvert.convert.RuntimeBlockStateConvert;

import java.io.IOException;

/**
 * @author LT_Name
 */
public class DataConvert {

    public static void main(String[] args) {
        System.out.println("Hello, World!");
        System.out.println("start convert runtime_block_states");
        try {
            RuntimeBlockStateConvert.convert();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("convert blockstate success");

        //TODO creative_items
        System.exit(0);
    }

}
