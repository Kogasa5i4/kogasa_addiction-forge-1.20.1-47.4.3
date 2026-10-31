package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 弩射出的弹射物：穿透 / 弹速 / 药水弹药 / 烟花弹药 / 概率不消耗 / 能力标记。
 *
 * <p><b>为什么用事件而不是 {@code @Redirect}</b>：
 * 旧实现 {@code @Redirect} 了 {@code CrossbowItem.shootProjectile} 内部的
 * {@code Level.addFreshEntity}。这是启动期硬冲突——别的弓弩类模组只要对同一指令再次
 * {@code @Redirect} 就会崩，而 {@code shootProjectile} 恰好是弓弩模组最常改动的地方。
 * 改订阅 Forge 的 {@code EntityJoinLevelEvent} 后，不再占用任何字节码注入点，
 * 冲突面从"字节码层"降到"逻辑层"。
 *
 * <p>取舍：事件是"实体即将加入世界"这一时机，此时原版已经
 * {@code projectile.shoot(...)} 过，所以弹速用 {@code setDeltaMovement} 缩放
 * （与旧实现一致）；穿透则是在原版按附魔算好的值上<b>叠加</b>，
 * 而不是覆盖它 —— 旧实现会把穿透 III 的附魔覆盖成成长穿透值。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CrossbowProjectileHandler {

    /** 本次射击是否已经生成过烟花批次的标记（每次 performShooting 开头清空） */
    public static final String TAG_FIREWORK_SPAWNED = "KogasaFireworkSpawned";

    /** 概率不消耗触发标记（由 CrossbowNoConsumeMixin 写入） */
    private static final String TAG_NO_CONSUME = "KogasaNoConsume";

    /** 原版弩的箭矢初速（CrossbowItem.ARROW_POWER），烟花沿用它做基准 */
    private static final float VANILLA_ARROW_POWER = 3.15F;

    private CrossbowProjectileHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer shooter)) return;

        ItemStack crossbow = findKogasaCrossbow(shooter);
        if (crossbow.isEmpty()) return;

        Level level = arrow.level();
        CompoundTag crossbowTag = crossbow.getOrCreateTag();

        // ===== 烟花弹药：拦掉原版箭矢，改成发射烟花 =====
        if (KogasaAbilityData.getAmmoType(crossbow) == KogasaAbilityData.AmmoType.FIREWORK) {
            // 阻止这支箭加入世界
            event.setCanceled(true);

            // 多重射击会多次调用 shootProjectile，这里保证一次射击只生成一批烟花
            if (!crossbowTag.getBoolean(TAG_FIREWORK_SPAWNED)) {
                crossbowTag.putBoolean(TAG_FIREWORK_SPAWNED, true);
                spawnFireworks(level, shooter, crossbow, arrow);
            }
            return;
        }

        // ===== 穿透：原版已把附魔穿透写进 arrow，这里只叠加成长穿透 =====
        int bowPierce = KogasaEquipmentHelper.getBowPierceLevel(crossbow);
        if (bowPierce > 0) {
            arrow.setPierceLevel((byte) (arrow.getPierceLevel() + bowPierce));
        }

        // ===== 弹速：总倍率开方 × 强化吸收来的发射威力 =====
        double multiplier = KogasaEquipmentData.getMultiplier(crossbow);
        double speedFactor = Math.sqrt(multiplier)
                * KogasaEquipmentHelper.getRangedPowerMultiplier(crossbow);
        if (arrow instanceof AyaArrowEntity) {
            speedFactor *= 1.5D;
        }
        if (speedFactor > 1.0D) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(speedFactor));
        }

        // ===== 药水弹药 =====
        applyPotionAmmo(crossbow, arrow);

        // ===== 概率不消耗：这一发不可拾取 =====
        if (crossbowTag.getBoolean(TAG_NO_CONSUME)) {
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        }

        // ===== 能力标记 =====
        if (KogasaAbilityData.hasAbility(crossbow, KogasaAbility.KOISHI)) {
            arrow.getPersistentData().putBoolean(KogasaAbilityData.PROJECTILE_KOISHI, true);
        }
        if (KogasaAbilityData.hasAbility(crossbow, KogasaAbility.TENSHI)) {
            arrow.getPersistentData().putBoolean(KogasaAbilityData.PROJECTILE_TENSHI, true);
        }
    }

    // ==================== 辅助 ====================

    private static ItemStack findKogasaCrossbow(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(ModItems.AYA_CROSSBOW.get())) return main;

        ItemStack off = player.getOffhandItem();
        if (off.is(ModItems.AYA_CROSSBOW.get())) return off;

        return ItemStack.EMPTY;
    }

    private static void applyPotionAmmo(ItemStack crossbow, AbstractArrow arrow) {
        if (KogasaAbilityData.getAmmoType(crossbow) != KogasaAbilityData.AmmoType.POTION) return;

        ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(crossbow);
        if (potionId == null) return;

        var potion = ForgeRegistries.POTIONS.getValue(potionId);
        if (potion == null) return;

        List<MobEffectInstance> effects = new ArrayList<>();
        for (var effect : potion.getEffects()) {
            effects.add(new MobEffectInstance(effect));
        }
        if (effects.isEmpty()) return;

        if (KogasaAbilityData.hasAbility(crossbow, KogasaAbility.CIRNO)) {
            effects = KogasaEquipmentHelper.applyCirnoPotionBoost(effects);
        }

        // AyaArrowEntity 继承自 Arrow，所以这里对原版箭与 Aya 箭都生效
        if (!(arrow instanceof Arrow vanillaArrow)) return;

        for (MobEffectInstance effect : effects) {
            vanillaArrow.addEffect(new MobEffectInstance(effect));
        }
        vanillaArrow.setFixedColor(PotionUtils.getColor(effects));
    }

    private static void spawnFireworks(Level level, ServerPlayer shooter,
                                       ItemStack crossbow, AbstractArrow reference) {
        ItemStack fireworkStack = KogasaAbilityData.getFireworkStack(crossbow);
        if (fireworkStack.isEmpty()) return;

        int count = KogasaEquipmentHelper.getBowArrowCount(crossbow);
        float spread = ModConfigCache.rangedMultishotSpread;

        double multiplier = KogasaEquipmentData.getMultiplier(crossbow);
        double speedFactor = Math.sqrt(multiplier)
                * KogasaEquipmentHelper.getRangedPowerMultiplier(crossbow);

        float fireworkVelocity = (float) (VANILLA_ARROW_POWER * 1.5D * speedFactor);

        for (int i = 0; i < count; i++) {
            FireworkRocketEntity rocket = new FireworkRocketEntity(
                    level, fireworkStack, shooter,
                    reference.getX(), reference.getY(), reference.getZ(), true);

            float yawOffset = count == 1 ? 0.0F
                    : (i - (count - 1) / 2.0F) * spread;
            float yaw = shooter.getYRot() + yawOffset;
            float pitch = shooter.getXRot();
            float fx = -Mth.sin(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
            float fy = -Mth.sin(pitch * ((float) Math.PI / 180F));
            float fz = Mth.cos(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));

            if (KogasaAbilityData.hasAbility(crossbow, KogasaAbility.KOISHI)) {
                rocket.getPersistentData().putBoolean(KogasaAbilityData.PROJECTILE_KOISHI, true);
            }
            if (KogasaAbilityData.hasAbility(crossbow, KogasaAbility.TENSHI)) {
                rocket.getPersistentData().putBoolean(KogasaAbilityData.PROJECTILE_TENSHI, true);
            }

            rocket.shoot(fx, fy, fz, fireworkVelocity, 1.0F);
            level.addFreshEntity(rocket);
        }

        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
