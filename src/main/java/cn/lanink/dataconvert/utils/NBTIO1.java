package cn.lanink.dataconvert.utils;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.stream.PGZIPOutputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

/**
 * 精简自 Nukkit-MOT 内置 NBTIO，仅保留本项目实际用到的方法。
 * 无法完全用内置 NBTIO 替代的原因：runtime_block_states_XXX.dat 的根节点是 ListTag，
 * 而内置 writeGZIPCompressed 的所有重载只接受 CompoundTag 根。
 */
public class NBTIO1 {

    public static CompoundTag read(InputStream inputStream, ByteOrder endianness, boolean network) throws IOException {
        Tag tag = Tag.readNamedTag(new NBTInputStream(inputStream, endianness, network));
        if (tag instanceof CompoundTag) {
            return (CompoundTag) tag;
        }
        throw new IOException("Root tag must be a named compound tag");
    }

    public static void writeGZIPCompressed(Tag tag, OutputStream outputStream, ByteOrder endianness) throws IOException {
        PGZIPOutputStream gzip = new PGZIPOutputStream(outputStream);
        Tag.writeNamedTag(tag, new NBTOutputStream(gzip, endianness, false));
        gzip.close();
    }
}
