package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.entity.projectile.SakuyaKnifeEntity;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class SakuyaKnifeItem extends TridentItem implements IKogasaEquipment {

    public SakuyaKnifeItem(Properties properties) {
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

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return KogasaEquipmentHelper.getAttributeModifiers(this, slot, stack);
    }

    // 耐久仅为兼容属性/附魔逻辑保留；isDamageable 返回 false，实际不会消耗耐久
    @Override
    public int getMaxDamage(ItemStack stack) {
        return 250;
    }

    // 投掷不消耗耐久
    @Override
    public boolean isDamageable(ItemStack stack) {
        return false;
    }

    // 完全接管投掷逻辑
    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        int useTime = this.getUseDuration(stack) - timeLeft;
        if (useTime < 10) return; // 必须蓄力满 10 tick

        if (!level.isClientSide) {
            // 1. 生成实体
            SakuyaKnifeEntity knife = new SakuyaKnifeEntity(level, player, stack);

            // 2. 设置投掷速度
            float velocity = Config.SAKUYA_KNIFE_THROW_VELOCITY.get().floatValue();
            knife.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, 1.0F);

            // 3. 播放音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);

            // 4. 加入世界
            level.addFreshEntity(knife);

            // 5. 消耗堆叠（如果不是创造模式）
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        // 明确拒绝激流
        if (enchantment == Enchantments.RIPTIDE) {
            return false;
        }
        // 其他附魔交由父类判断
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
}