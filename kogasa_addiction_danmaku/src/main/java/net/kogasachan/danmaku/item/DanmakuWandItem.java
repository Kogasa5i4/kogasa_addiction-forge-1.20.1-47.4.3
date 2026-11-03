package net.kogasachan.danmaku.item;

import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.kogasachan.kogasa_addiction.network.KeyStateTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 弹幕发射器（测试用）。
 *
 * <p>修饰键复用主模组的 Ctrl/Alt 网络包（{@link KeyStateTracker}）：
 * <ul>
 *   <li>右键：发射当前弹型 / 颜色；</li>
 *   <li>Ctrl + 右键：切换弹型；</li>
 *   <li>Alt + 右键：切换颜色；</li>
 *   <li>Ctrl + Alt + 右键：一次扇形打全 29 种弹型（点检贴图用）。</li>
 * </ul>
 */
public class DanmakuWandItem extends Item {

    private static final String TAG_TYPE = "DanmakuTypeIndex";
    private static final String TAG_COLOR = "DanmakuColorIndex";
    private static final int COOLDOWN_TICKS = 4;

    public DanmakuWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        CompoundTag tag = stack.getOrCreateTag();
        int typeIndex = tag.getInt(TAG_TYPE);
        int colorIndex = tag.getInt(TAG_COLOR);

        boolean ctrl = KeyStateTracker.isCtrlDown(player);
        boolean alt = KeyStateTracker.isAltDown(player);

        if (ctrl && alt) {
            fireCatalog(level, player);
        } else if (ctrl) {
            int next = Math.floorMod(typeIndex + 1, DanmakuType.values().length);
            tag.putInt(TAG_TYPE, next);
            player.displayClientMessage(Component.literal("Danmaku type: " + DanmakuType.byIndex(next).id())
                    .withStyle(ChatFormatting.AQUA), true);
        } else if (alt) {
            int next = Math.floorMod(colorIndex + 1, DanmakuColor.values().length);
            tag.putInt(TAG_COLOR, next);
            player.displayClientMessage(Component.literal("Danmaku color: " + DanmakuColor.byIndex(next).id())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        } else {
            DanmakuBulletItem.fire(level, player, DanmakuType.byIndex(typeIndex), DanmakuColor.byIndex(colorIndex));
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    /** 扇形打全 29 种弹型，颜色依次轮换（激光类自动走射线） */
    private static void fireCatalog(Level level, Player player) {
        DanmakuType[] types = DanmakuType.values();
        for (int i = 0; i < types.length; i++) {
            float offset = -75.0F + 150.0F * i / Math.max(1, types.length - 1);
            Vec3 direction = player.getLookAngle().yRot((float) Math.toRadians(offset));
            DanmakuBulletItem.fire(level, player, types[i], DanmakuColor.byIndex(i), direction);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        int typeIndex = tag == null ? 0 : tag.getInt(TAG_TYPE);
        int colorIndex = tag == null ? 0 : tag.getInt(TAG_COLOR);

        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.wand.type",
                        DanmakuType.byIndex(typeIndex).id())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.wand.color",
                        DanmakuColor.byIndex(colorIndex).id())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.wand.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
