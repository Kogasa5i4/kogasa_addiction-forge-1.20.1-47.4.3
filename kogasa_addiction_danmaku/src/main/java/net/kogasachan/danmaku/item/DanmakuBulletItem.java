package net.kogasachan.danmaku.item;

import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.kogasachan.danmaku.entity.DanmakuBeamEntity;
import net.kogasachan.danmaku.entity.DanmakuBulletEntity;
import net.kogasachan.danmaku.entity.DanmakuEntities;
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
 * 弹幕物品（手持发射形式），每种 {@link DanmakuType} 注册一个。
 *
 * <p>操作：
 * <ul>
 *   <li>右键：发射本类型的弹幕（激光类改为"从发射点延伸 10 格的射线"）；</li>
 *   <li>Alt + 右键：切换颜色（Ctrl/Alt 状态复用主模组的 {@link KeyStateTracker} 网络包）。</li>
 * </ul>
 */
public class DanmakuBulletItem extends Item {

    static final String TAG_COLOR = "DanmakuColorIndex";
    private static final int COOLDOWN_TICKS = 3;
    private static final int BEAM_COOLDOWN_TICKS = 12;

    private final DanmakuType type;

    public DanmakuBulletItem(DanmakuType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public DanmakuType type() {
        return type;
    }

    /** 取当前颜色：没设置过时用"类型序号 % 16"作为默认色，方便一眼区分每种弹幕 */
    public static DanmakuColor colorOf(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        int index = 0;
        if (tag != null && tag.contains(TAG_COLOR)) {
            index = tag.getInt(TAG_COLOR);
        } else if (stack.getItem() instanceof DanmakuBulletItem bullet) {
            index = bullet.type().ordinal();
        }
        return DanmakuColor.byIndex(index);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // Alt + 右键：切换颜色
        if (KeyStateTracker.isAltDown(player)) {
            DanmakuColor next = colorOf(stack).next();
            stack.getOrCreateTag().putInt(TAG_COLOR, next.ordinal());
            player.displayClientMessage(Component.literal("Danmaku color: " + next.id())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResultHolder.success(stack);
        }

        fire(level, player, type, colorOf(stack));
        player.getCooldowns().addCooldown(this, type.isBeam() ? BEAM_COOLDOWN_TICKS : COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    public static void fire(Level level, Player player, DanmakuType type, DanmakuColor color) {
        fire(level, player, type, color, player.getLookAngle());
    }

    /** 按给定方向发射：激光类生成射线实体，其余生成弹体 */
    public static void fire(Level level, Player player, DanmakuType type, DanmakuColor color, Vec3 direction) {
        Vec3 dir = direction.normalize();
        Vec3 origin = player.getEyePosition().add(dir.scale(0.6D));

        if (type.isBeam()) {
            DanmakuBeamEntity beam = new DanmakuBeamEntity(DanmakuEntities.DANMAKU_BEAM.get(), level);
            beam.init(player, type, color, dir);
            beam.setPos(origin.x, origin.y, origin.z);
            level.addFreshEntity(beam);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
            return;
        }

        DanmakuBulletEntity bullet = new DanmakuBulletEntity(DanmakuEntities.DANMAKU_BULLET.get(), level);
        bullet.init(player, type, color, dir);
        bullet.setPos(origin.x, origin.y, origin.z);
        bullet.setDeltaMovement(dir.scale(type.speed()));
        level.addFreshEntity(bullet);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.item.type", type.id())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.item.color", colorOf(stack).id())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.kogasa_addiction_danmaku.item.hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
