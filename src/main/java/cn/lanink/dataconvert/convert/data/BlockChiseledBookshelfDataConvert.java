package cn.lanink.dataconvert.convert.data;

import cn.nukkit.math.BlockFace;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * Converts chiseled bookshelf states to Nukkit legacy data.
 */
public class BlockChiseledBookshelfDataConvert extends BlockDataConvert {

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int booksStored = blockState.getInt("books_stored") & 0x3f;
        int direction = getDirection(blockState) & 0x03;
        return booksStored | (direction << 6);
    }

    private int getDirection(CompoundTag blockState) {
        if (blockState.contains("direction")) {
            return blockState.getInt("direction");
        }
        if (blockState.contains("minecraft:cardinal_direction")) {
            BlockFace face = BlockFace.valueOf(blockState.getString("minecraft:cardinal_direction").toUpperCase());
            return face.getHorizontalIndex();
        }
        return BlockFace.SOUTH.getHorizontalIndex();
    }
}
