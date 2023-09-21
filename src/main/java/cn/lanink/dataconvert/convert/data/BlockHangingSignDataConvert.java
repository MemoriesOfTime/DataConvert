package cn.lanink.dataconvert.convert.data;

import cn.nukkit.nbt.tag.CompoundTag;

/**
 * 悬挂告示牌方块状态转换器
 *
 * Meta 结构:
 * - bits 0-2: facing_direction (0-7)
 * - bits 3-6: ground_sign_direction (0-15)
 * - bit 7: hanging
 * - bit 8: attached_bit
 *
 * @author LT_Name
 */
public class BlockHangingSignDataConvert extends BlockDataConvert {

    public static final int FACING_DIRECTION_MASK = 0b111;
    public static final int ATTACHED_DIRECTION_MASK = 0b1111_000;
    public static final int ATTACHED_DIRECTION_START = 3;
    public static final int HANGING_BIT = 0b1_0000_000;    // 128
    public static final int ATTACHED_BIT = 0b10_0000_000;  // 256

    @Override
    public int calculateData(int version, CompoundTag blockState) {
        int meta = 0;

        // 获取 facing_direction (0-5)
        int facingDirection = blockState.getInt("facing_direction");

        // 获取 ground_sign_direction (0-15)
        int groundSignDirection = blockState.getInt("ground_sign_direction");

        // 获取 hanging (bool)
        int hanging = blockState.getByte("hanging");

        // 获取 attached_bit (bool)
        int attachedBit = blockState.getByte("attached_bit");

        // 组合 meta 值
        // 根据 attached_bit 决定使用 facing_direction 还是 ground_sign_direction
        if (attachedBit == 1) {
            // 附着模式: 使用 ground_sign_direction
            meta = (groundSignDirection << ATTACHED_DIRECTION_START) & ATTACHED_DIRECTION_MASK;
        } else {
            // 非附着模式: 使用 facing_direction
            meta = facingDirection & FACING_DIRECTION_MASK;
        }

        // 设置 hanging bit
        if (hanging == 1) {
            meta |= HANGING_BIT;
        }

        // 设置 attached_bit
        if (attachedBit == 1) {
            meta |= ATTACHED_BIT;
        }

        return meta;
    }

}
