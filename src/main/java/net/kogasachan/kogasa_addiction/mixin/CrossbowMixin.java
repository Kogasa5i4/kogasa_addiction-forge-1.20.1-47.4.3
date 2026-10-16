package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.kogasachan.kogasa_addiction.event.server.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CrossbowItem.class)
public class CrossbowMixin {

    // ========== 1. 发射时触发成长 ==========
    @Inject(method = "performShooting", at = @At("HEAD"))
    private static void kogasa$onShoot(Level level, LivingEntity shooter,
                                       InteractionHand hand, ItemStack stack,
                                       float velocity, float inaccuracy,
                                       CallbackInfo ci) {
        if (level.isClientSide) return;
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;
        if (!(shooter instanceof Player player)) return;

        if (stack.getTag() != null) {
            stack.getTag().remove("KogasaNoConsume");
        }

        KogasaGrowthHandler.addGrowth(stack, player);
    }

    // ========== 2. 拦截箭矢加入世界，改穿透和弹速 ==========
    // shootProjectile 内部调用 pLevel.addFreshEntity(arrow)，在此拦截
    @Redirect(
            method = "shootProjectile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
            )
    )
    private static boolean kogasa$modifyArrow(Level level, Entity entity,
                                              Level pLevel, LivingEntity pShooter,
                                              InteractionHand pHand, ItemStack pCrossbowStack,
                                              ItemStack pProjectile, float pShootAngle,
                                              boolean pIsCreative, float pVelocity,
                                              float pInaccuracy, float pYawOffset) {
        // 1. 非箭矢实体：原版放行
        if (!(entity instanceof AbstractArrow arrow)) {
            return level.addFreshEntity(entity);
        }

        // 2. 非 Kogasa 弓弩：原版放行
        if (!(pCrossbowStack.getItem() instanceof IKogasaEquipment)) {
            return level.addFreshEntity(entity);
        }

        // 3. 穿透
        int basePierce = arrow instanceof AyaArrowEntity
                ? ((AyaArrowEntity) arrow).getPierceLevel() : 0;
        int bowPierce = KogasaEquipmentHelper.getBowPierceLevel(pCrossbowStack);
        int totalPierce = basePierce + bowPierce;
        if (totalPierce > 0) {
            arrow.setPierceLevel((byte) totalPierce);
        }

        // 4. 弹速
        double multiplier = KogasaEquipmentData.getMultiplier(pCrossbowStack);
        double speedFactor = Math.sqrt(multiplier);
        if (arrow instanceof AyaArrowEntity) {
            speedFactor *= 1.5;
        }
        if (speedFactor > 1.0) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(speedFactor));
        }

        var ammoType = KogasaAbilityData.getAmmoType(pCrossbowStack);

        // ===== 5. 烟花：替换箭矢为烟花 =====
        if (ammoType == KogasaAbilityData.AmmoType.FIREWORK) {
            ItemStack fireworkStack = KogasaAbilityData.getFireworkStack(pCrossbowStack);
            if (fireworkStack.isEmpty()) return false;

            int fireworkCount = KogasaEquipmentHelper.getBowArrowCount(pCrossbowStack);
            float spread = ModConfigCache.rangedMultishotSpread;

            // 原版箭矢已经生成一支，替换为烟花；多发时额外生成
            for (int i = 0; i < fireworkCount; i++) {
                FireworkRocketEntity rocket = new FireworkRocketEntity(
                        level, fireworkStack, pShooter,
                        arrow.getX(), arrow.getY(), arrow.getZ(), true);

                // 多发散射
                float yawOffset = fireworkCount == 1 ? 0.0F
                        : (i - (fireworkCount - 1) / 2.0F) * spread;
                float yaw = pShooter.getYRot() + yawOffset;
                float pitch = pShooter.getXRot();
                float fx = -Mth.sin(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));
                float fy = -Mth.sin(pitch * ((float)Math.PI / 180F));
                float fz = Mth.cos(yaw * ((float)Math.PI / 180F)) * Mth.cos(pitch * ((float)Math.PI / 180F));

                rocket.shoot(fx, fy, fz, pVelocity * 1.5F * (float) speedFactor, pInaccuracy);
                level.addFreshEntity(rocket);
            }

            level.playSound(null, pShooter.getX(), pShooter.getY(), pShooter.getZ(),
                    SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
            return true;  // 返回 true 表示不再添加原版箭矢
        }

        // ===== 6. 药水：给箭矢附加效果 =====
        if (ammoType == KogasaAbilityData.AmmoType.POTION) {
            ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(pCrossbowStack);
            if (potionId != null) {
                var potion = ForgeRegistries.POTIONS.getValue(potionId);
                if (potion != null) {
                    List<MobEffectInstance> effects = new java.util.ArrayList<>();
                    for (var effect : potion.getEffects()) {
                        effects.add(new MobEffectInstance(effect));
                    }
                    if (!effects.isEmpty()
                            && KogasaAbilityData.hasAbility(pCrossbowStack, KogasaAbility.CIRNO)) {
                        effects = KogasaEquipmentHelper.applyCirnoPotionBoost(effects);
                    }
                    if (arrow instanceof Arrow vanillaArrow) {
                        for (MobEffectInstance effect : effects) {
                            vanillaArrow.addEffect(new MobEffectInstance(effect));
                        }
                        if (!effects.isEmpty()) {
                            vanillaArrow.setFixedColor(PotionUtils.getColor(effects));
                        }
                    }
                }
            }
        }

        if (pCrossbowStack.getOrCreateTag().getBoolean("KogasaNoConsume")) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }

        // 7. 默认：原版放行
        return level.addFreshEntity(entity);
    }

    // ========== 3. 装填速度成长 ==========
    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void kogasa$modifyChargeDuration(ItemStack stack,
                                                    CallbackInfoReturnable<Integer> cir) {
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;
        int original = cir.getReturnValue();
        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        int modified = (int) (original / Math.sqrt(multiplier));
        cir.setReturnValue(Math.max(modified, 1));
    }

    // ========== 4. 装填时触发不消耗 ==========
    @Unique
    private static final String TAG_NO_CONSUME = "KogasaNoConsume";

    /**
     * HEAD：清除旧标记，记录装填前的箭矢数量
     */
    @Inject(method = "tryLoadProjectiles", at = @At("HEAD"))
    private static void kogasa$beforeLoad(LivingEntity shooter, ItemStack crossbow,
                                          CallbackInfoReturnable<Boolean> cir) {
        // 清除上一次的标记
        if (crossbow.getTag() != null) {
            crossbow.getTag().remove(TAG_NO_CONSUME);
        }
    }

    /**
     * RETURN：如果装填成功且触发不消耗，补回箭矢
     */
    @Inject(method = "tryLoadProjectiles", at = @At("RETURN"))
    private static void kogasa$afterLoad(LivingEntity shooter, ItemStack crossbow,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        if (!(shooter instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!crossbow.is(ModItems.AYA_CROSSBOW.get())) return;
        if (player.getAbilities().instabuild) return;

        // 计算概率
        double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(crossbow);
        if (chance <= 0.0) return;
        if (player.getRandom().nextDouble() >= chance) return;

        // 获取装填的箭矢数量
        List<ItemStack> charged = CrossbowItem.getChargedProjectiles(crossbow);
        if (charged.isEmpty()) return;

        // 补回箭矢：用装填的箭矢类型和数量
        ItemStack firstCharged = charged.get(0);
        if (firstCharged.isEmpty()) return;

        ItemStack restored = firstCharged.copy();
        restored.setCount(charged.size());

        if (!player.getInventory().add(restored)) {
            player.drop(restored, false);
        }

        // 标记弩：发射时箭矢不可拾取
        crossbow.getOrCreateTag().putBoolean(TAG_NO_CONSUME, true);
    }
}