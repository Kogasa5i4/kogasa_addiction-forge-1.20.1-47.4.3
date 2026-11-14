package net.kogasachan.kogasa_addiction_compat.compat;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * 彩虹水晶的熔岩转化：丢进熔岩（或熔岩炼药锅）后，按配置概率转化为随机一种能力锭。
 *
 * <p>由 {@code ItemEntityMixin} 在 {@code ItemEntity#tick} 头部调用；只在服务端执行。
 * 为了避免每个 tick 都抽一次，失败后隔 {@value #RETRY_TICKS} tick 再试（用实体持久 NBT 记录）。
 */
public final class RainbowLavaHandler {

    private static final String NEXT_TRY_KEY = "KogasaLavaTry";
    private static final int RETRY_TICKS = 10;

    /**
     * 可转化的能力锭（与主模组一致的六种）
     */
    private static final List<RegistryObject<Item>> ABILITY_INGOTS = List.of(
            ModItems.KOGASA_INGOT,
            ModItems.AYA_INGOT,
            ModItems.CIRNO_INGOT,
            ModItems.KOISHI_INGOT,
            ModItems.TENSHI_INGOT,
            ModItems.SAKUYA_INGOT
    );

    private RainbowLavaHandler() {
    }

    public static void tick(ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return;
        }
        if (!CompatConfig.RAINBOW_LAVA_ENABLED.get()) {
            return;
        }

        ItemStack stack = entity.getItem();
        if (stack.isEmpty() || !stack.is(CompatRegistry.RAINBOW_CRYSTAL.get())) {
            return;
        }
        if (entity.isRemoved() || !isInLavaOrCauldron(entity)) {
            return;
        }

        long now = level.getGameTime();
        CompoundTag data = entity.getPersistentData();
        if (now < data.getLong(NEXT_TRY_KEY)) {
            return;
        }
        data.putLong(NEXT_TRY_KEY, now + RETRY_TICKS);

        if (level.random.nextDouble() >= CompatConfig.RAINBOW_LAVA_CHANCE.get()) {
            return;
        }

        convert(entity, level, stack);
    }

    private static boolean isInLavaOrCauldron(ItemEntity entity) {
        if (entity.isInLava()) {
            return true;
        }
        BlockPos pos = entity.blockPosition();
        Level level = entity.level();
        BlockState below = level.getBlockState(pos.below());
        BlockState here = level.getBlockState(pos);
        return here.is(Blocks.LAVA_CAULDRON) || below.is(Blocks.LAVA_CAULDRON);
    }

    private static void convert(ItemEntity entity, Level level, ItemStack crystal) {
        ItemStack ingot = new ItemStack(randomAbilityIngot(level.random));

        ItemStack remain = crystal.copy();
        remain.shrink(1);
        if (remain.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(remain);
        }

        ItemEntity drop = new ItemEntity(level, entity.getX(), entity.getY() + 0.2D, entity.getZ(), ingot);
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);

        level.playSound(null, entity.blockPosition(), SoundEvents.LAVA_EXTINGUISH,
                SoundSource.BLOCKS, 0.8F, 1.2F);
        level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 1.4F);
    }

    private static Item randomAbilityIngot(RandomSource random) {
        return ABILITY_INGOTS.get(random.nextInt(ABILITY_INGOTS.size())).get();
    }
}
