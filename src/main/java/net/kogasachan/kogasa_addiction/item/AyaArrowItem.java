package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AyaArrowItem extends ArrowItem {

    public AyaArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy()
                .withStyle(style -> style.withColor(ChatFormatting.GOLD));
    }

    @Override
    public @NotNull Rarity getRarity(@NotNull ItemStack pStack) {
        return Rarity.EPIC;
    }

    // ==================== 发射 ====================

    @Override
    public @NotNull AbstractArrow createArrow(
            @NotNull Level level,
            @NotNull ItemStack stack,
            @NotNull LivingEntity shooter
    ) {
        AyaArrowEntity arrow = new AyaArrowEntity(level, shooter);

        // 缓存原始箭矢 NBT，拾取时原样还原（药水/自定义效果/颜色等）
        CompoundTag itemTag = stack.getTag();
        if (itemTag != null && !itemTag.isEmpty()) {
            arrow.getPersistentData().put(AyaArrowEntity.ITEM_NBT_KEY, itemTag.copy());
        }

        // 手动读取 NBT（原版 setEffectsFromItem 只认 tipped_arrow）
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            // 自定义效果
            List<MobEffectInstance> custom = PotionUtils.getCustomEffects(stack);
            for (MobEffectInstance effect : custom) {
                arrow.addEffect(new MobEffectInstance(effect));
            }

            // 标准药水效果
            Potion potion = PotionUtils.getPotion(stack);
            if (potion != Potions.EMPTY) {
                for (MobEffectInstance effect : potion.getEffects()) {
                    arrow.addEffect(new MobEffectInstance(effect));
                }
            }

            // 设置粒子颜色
            if (!custom.isEmpty()) {
                arrow.setFixedColor(PotionUtils.getColor(custom));
            } else if (potion != Potions.EMPTY && !potion.getEffects().isEmpty()) {
                arrow.setFixedColor(PotionUtils.getColor(potion.getEffects()));
            }
        }

        return arrow;
    }

    // ==================== Tooltip ====================

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @Nullable Level level,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        // 药水效果（0.125F = 原版药箭的时长系数）
        PotionUtils.addPotionTooltip(stack, tooltip, 0.125F);

        super.appendHoverText(stack, level, tooltip, flag);
    }
}