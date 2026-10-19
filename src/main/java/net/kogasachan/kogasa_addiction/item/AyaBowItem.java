package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AyaBowItem extends BowItem implements IKogasaEquipment {

    public AyaBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return KogasaEquipmentHelper.getMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level,
                             @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;

        boolean infinity = player.getAbilities().instabuild
                || stack.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        ItemStack ammo = player.getProjectile(stack);

        int charge = this.getUseDuration(stack) - timeLeft;
        charge = ForgeEventFactory.onArrowLoose(stack, level, player, charge, !ammo.isEmpty() || infinity);
        if (charge < 0) return;

        if (ammo.isEmpty() && !infinity) return;
        if (ammo.isEmpty()) ammo = new ItemStack(Items.ARROW);

        // ===== 自定义 power 计算：拉弓速度成长 =====
        float power = getCustomPowerForTime(stack, charge);
        if (power < 0.1F) return;

        if (!level.isClientSide) {
            boolean ammoInfinite = player.getAbilities().instabuild
                    || (ammo.getItem() instanceof ArrowItem arrowItem
                    && arrowItem.isInfinite(ammo, stack, player));
            // ===== 概率不消耗箭矢 =====
            boolean noConsume = false;
            if (!ammoInfinite && !player.getAbilities().instabuild) {
                double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(stack);
                if (chance > 0.0 && player.getRandom().nextDouble() < chance) {
                    noConsume = true;
                }
            }

            // ===== 烟花分支：消耗箭矢，发射烟花 =====
            var ammoType = KogasaAbilityData.getAmmoType(stack);
            // 烟花多发分支
            if (ammoType == KogasaAbilityData.AmmoType.FIREWORK) {
                ItemStack fireworkStack = KogasaAbilityData.getFireworkStack(stack);
                if (fireworkStack.isEmpty()) return;

                int fireworkCount = KogasaEquipmentHelper.getBowArrowCount(stack);

                for (int i = 0; i < fireworkCount; i++) {
                    // 消耗一支箭（每发烟花消耗一支箭）
                    if (!ammoInfinite && !player.getAbilities().instabuild) {
                        ammo.shrink(1);
                    }

                    FireworkRocketEntity rocket = new FireworkRocketEntity(
                            level, fireworkStack, player,
                            player.getX(), player.getEyeY() - 0.15, player.getZ(), true);

                    // 散射角度，和箭矢多发一致
                    float yawOffset = fireworkCount == 1 ? 0.0F
                            : (i - (fireworkCount - 1) / 2.0F) * 8.0F;

                    // 用 lookAngle 计算带散射的发射方向
                    float yaw = player.getYRot() + yawOffset;
                    float pitch = player.getXRot();
                    float f = -Mth.sin(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
                    float f1 = -Mth.sin(pitch * ((float) Math.PI / 180F));
                    float f2 = Mth.cos(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));

                    if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOISHI)) {
                        rocket.getPersistentData().putBoolean("KogasaKoishiArrow", true);
                    }

                    if (KogasaAbilityData.hasAbility(stack, KogasaAbility.TENSHI)) {
                        rocket.getPersistentData().putBoolean("KogasaTenshiArrow", true);
                    }

                    rocket.shoot(f, f1, f2, power * 1.5F, 1.0F);
                    level.addFreshEntity(rocket);
                }

                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);

                stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
                KogasaGrowthHandler.addGrowth(stack, player);
                return;
            }

            // ===== 药水 / 普通分支：生成箭矢 =====
            ArrowItem arrowItem = ammo.getItem() instanceof ArrowItem ai
                    ? ai : (ArrowItem) Items.ARROW;

            double multiplier = KogasaEquipmentData.getMultiplier(stack);
            int arrowCount = KogasaEquipmentHelper.getBowArrowCount(stack);
            int pierceLevel = KogasaEquipmentHelper.getBowPierceLevel(stack);
            float velocityMultiplier = (float) Math.sqrt(multiplier);

            // 药水效果：如果是药水弹药，缓存效果列表
            List<MobEffectInstance> potionEffects = new java.util.ArrayList<>();
            if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(stack);
                if (potionId != null) {
                    var potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        for (var effect : potion.getEffects()) {
                            potionEffects.add(new MobEffectInstance(effect));
                        }
                    }
                }

                // Cirno 能力：增强药水效果
                if (!potionEffects.isEmpty()
                        && KogasaAbilityData.hasAbility(stack, KogasaAbility.CIRNO)) {
                    potionEffects = KogasaEquipmentHelper.applyCirnoPotionBoost(potionEffects);
                }
            }

            for (int i = 0; i < arrowCount; i++) {
                AbstractArrow arrow = arrowItem.createArrow(level, ammo, player);
                arrow = customArrow(arrow);

                // ===== 穿透叠加：Aya 箭矢自带 1 + 弓弩成长穿透 =====
                int basePierce = arrow instanceof AyaArrowEntity
                        ? arrow.getPierceLevel() : 0;
                int bowPierce = KogasaEquipmentHelper.getBowPierceLevel(stack);
                int totalPierce = basePierce + bowPierce;
                if (totalPierce > 0) {
                    arrow.setPierceLevel((byte) totalPierce);
                }

                // 原版附魔
                int powerEnch = stack.getEnchantmentLevel(Enchantments.POWER_ARROWS);
                if (powerEnch > 0) arrow.setBaseDamage(arrow.getBaseDamage() + powerEnch * 0.5 + 0.5);
                int punch = stack.getEnchantmentLevel(Enchantments.PUNCH_ARROWS);
                if (punch > 0) arrow.setKnockback(punch);
                if (stack.getEnchantmentLevel(Enchantments.FLAMING_ARROWS) > 0) {
                    arrow.setSecondsOnFire(100);
                }

                // 药水弹药：给箭矢附加效果
                if (arrow instanceof Arrow vanillaArrow && !potionEffects.isEmpty()) {
                    for (MobEffectInstance effect : potionEffects) {
                        vanillaArrow.addEffect(new MobEffectInstance(effect));
                    }
                    // 设置粒子颜色让箭矢看起来像药箭
                    if (!potionEffects.isEmpty()) {
                        vanillaArrow.setFixedColor(PotionUtils.getColor(potionEffects));
                    }
                }

                // ===== 弹速：Aya 箭矢额外 +50% =====
                float ayaSpeedBonus = arrow instanceof AyaArrowEntity ? 1.5F : 1.0F;
                float finalVelocity = power * 3.0F * velocityMultiplier * ayaSpeedBonus;

                // 多发散射
                float yawOffset = arrowCount == 1 ? 0.0F
                        : (i - (arrowCount - 1) / 2.0F) * ModConfigCache.rangedMultishotSpread;
                arrow.shootFromRotation(player,
                        player.getXRot(), player.getYRot() + yawOffset,
                        0.0F, finalVelocity, 1.0F);

                if (power == 1.0F) arrow.setCritArrow(true);

                if (noConsume) {
                    arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                }

                if (ammoInfinite) arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

                if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOISHI)) {
                    arrow.getPersistentData().putBoolean("KogasaKoishiArrow", true);
                }

                if (KogasaAbilityData.hasAbility(stack, KogasaAbility.TENSHI)) {
                    arrow.getPersistentData().putBoolean("KogasaTenshiArrow", true);
                }

                level.addFreshEntity(arrow);
            }

            // 音效
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                    1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + power * 0.5F);

            // 耐久
            stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));

            // 箭矢消耗（多发也只消耗 1 支）
            if (!noConsume && !ammoInfinite && !player.getAbilities().instabuild) {
                ammo.shrink(1);
            }

            // 成长
            KogasaGrowthHandler.addGrowth(stack, player);
        }
    }

    /**
     * 自定义 power 计算：
     * 拉满所需 charge = 20 / sqrt(multiplier)
     * multiplier = 1.0 → 20 tick（原版）
     * multiplier = 4.0 → 10 tick（快一倍）
     */
    public static float getCustomPowerForTime(ItemStack stack, int charge) {
        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        float maxCharge = (float) (20.0 / Math.sqrt(multiplier));
        float f = charge / maxCharge;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(f, 1.0F);
    }
}