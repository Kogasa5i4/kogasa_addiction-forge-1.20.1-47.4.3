package net.kogasachan.kogasa_addiction.entity.projectile;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class SakuyaKnifeEntity extends ThrownTrident {

    public SakuyaKnifeEntity(EntityType<? extends SakuyaKnifeEntity> type, Level level) {
        super(type, level);
    }

    public SakuyaKnifeEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.SAKUYA_KNIFE_ENTITY.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        ItemStack single = stack.copy();
        single.setCount(1);
        this.tridentItem = single;
        this.setNoGravity(true);
        int loyaltyLevel = stack.getEnchantmentLevel(Enchantments.LOYALTY);
        if (loyaltyLevel > 0) {
            this.entityData.set(ID_LOYALTY, (byte) loyaltyLevel);
        }
    }

    /**
     * 供渲染使用的原始飞刀副本，不会掷损坏概率。
     */
    public ItemStack getRenderStack() {
        ItemStack stack = new ItemStack(ModItems.SAKUYA_KNIFE.get());
        if (this.tridentItem != null && this.tridentItem.hasTag()) {
            stack.setTag(this.tridentItem.getTag().copy());
        }
        return stack;
    }

    @Override
    @NotNull
    public ItemStack getPickupItem() {
        // 获取最开始扔出去的那把飞刀的 ItemStack
        ItemStack originStack = this.tridentItem;

        // 如果是创造模式，或者空物品，直接返回原物
        if (originStack.isEmpty()) return new ItemStack(ModItems.SAKUYA_KNIFE.get());

        // 计算当前飞刀的强化和成长值
        int re = KogasaEquipmentData.getReinforcements(originStack);
        double growth = KogasaEquipmentData.getGrowth(originStack);

        // 计算损坏概率：基础概率 - (强化次数 + 成长值) * 降低率
        double baseChance = Config.SAKUYA_KNIFE_BASE_BREAK_CHANCE.get();
        double reduction = Config.SAKUYA_KNIFE_BREAK_REDUCTION.get();
        double finalChance = Math.max(0.0, baseChance - (re * reduction) - (growth * reduction));

        // 掷骰子
        if (this.random.nextDouble() < finalChance) {
            // 触发损坏：生成损坏版飞刀，并复制原有 NBT
            ItemStack broken = new ItemStack(ModItems.BROKEN_SAKUYA_KNIFE.get());
            if (originStack.getTag() != null && originStack.hasTag()) {
                broken.setTag(originStack.getTag().copy());
            }
            broken.setCount(1);
            return broken;
        }

        // 未损坏：返回原版飞刀（保留 NBT）
        ItemStack result = originStack.copy();
        result.setCount(1);
        return result;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        // 1. 调用父类方法，处理基础的投掷物伤害、击退、着火等效果
        super.onHitEntity(result);

        if (this.getPierceLevel() <= 0) {
            this.inGround = false;
            this.setNoGravity(false);
            // 给一个很小的向下初速度，避免完全静止悬停一瞬
            this.setDeltaMovement(0, -0.05, 0);
        }

        // 2. 获取被命中的实体和射手
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity)) return;

        // 3. 获取原始飞刀的 ItemStack
        ItemStack knifeStack = this.tridentItem;
        if (knifeStack.isEmpty()) return;

        // 4. 计算来自强化/成长系统的额外伤害
        //    这里我们调用你已有的 KogasaEquipmentHelper 来计算倍率
        double multiplier = KogasaEquipmentHelper.getTotalMultiplier(knifeStack);
        if (multiplier > 1.0) {
            // 基础伤害在原版中约为 8.0（可配置或从父类获取）
            float baseDamage = 8.0F;
            float extraDamage = (float) (baseDamage * (multiplier - 1.0));

            if (extraDamage > 0) {
                // 构建一个与父类一致的伤害源（三叉戟伤害）
                DamageSource source = this.damageSources().trident(this, owner);
                // 造成额外伤害，并施加击退效果
                target.hurt(source, extraDamage);
            }
        }
    }
}