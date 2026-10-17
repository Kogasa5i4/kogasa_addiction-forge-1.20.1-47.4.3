package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiRangedAbilityHandler {

    /**
     * 穿甲伤害类型
     */
    private static final ResourceKey<DamageType> KOISHI_RANGED = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "koishi_ranged"));

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!Config.ENABLE_KOISHI_RANGED.get()) return;

        // 仅处理箭矢命中实体
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = resolveWeapon(player);
        if (weapon.isEmpty() || !KogasaAbilityData.hasAbility(weapon, KogasaAbility.KOISHI)) return;

        // ===== 开始接管命中 =====
        // 1. 取消原版事件，移除箭矢
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        arrow.discard();

        // 2. 计算基础伤害
        double speed = arrow.getDeltaMovement().length();
        float damage = (float) (speed * arrow.getBaseDamage());
        if (arrow.isCritArrow()) damage *= 1.5F;

        // 3. KOISHI 的 75% 暴击判定
        float chance = Config.KOISHI_RANGED_CRIT_CHANCE.get().floatValue();
        boolean isCrit = player.getRandom().nextFloat() < chance;
        if (isCrit) {
            damage *= Config.KOISHI_RANGED_CRIT_MULTIPLIER.get().floatValue();
        }

        float multiplier = Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get().floatValue();
        if ((target instanceof WitherBoss &&
                ((WitherBoss) target).getHealth() / ((WitherBoss) target).getMaxHealth() < 0.5f) ||
                (target instanceof EnderMan)) {
            damage *= multiplier;
        }

        if (target instanceof EnderDragon) {
            damage *= multiplier * multiplier;
        }

        // 4. 构建穿甲伤害源
        DamageSource killSource = createArmorBypassingSource(player);
        livingTarget.hurt(killSource, damage);

        // 5. 手动处理原版的附加效果（击退、火矢、药水）
        // 击退（Punch）
        if (arrow.getKnockback() > 0) {
            Vec3 vec3 = arrow.getDeltaMovement().normalize().scale(arrow.getKnockback() * 0.6);
            livingTarget.push(vec3.x, 0.1, vec3.z);
        }
        // 火矢（Flame）
        if (arrow.isOnFire()) {
            livingTarget.setSecondsOnFire(5);
        }
        // 药水效果（CustomPotionEffects）
        if (arrow instanceof Arrow vanillaArrow) {
            CompoundTag tag = new CompoundTag();
            vanillaArrow.saveWithoutId(tag); // 把箭矢的数据存进 NBT

            // 1. 应用药水本体的效果（注意：原版会在 doPostHurtEffects 里将时长除以 8）
            Potion potion = PotionUtils.getPotion(tag);
            if (potion != Potions.EMPTY) {
                for (MobEffectInstance effect : potion.getEffects()) {
                    livingTarget.addEffect(new MobEffectInstance(
                            effect.getEffect(),
                            Math.max(effect.getDuration() / 8, 1), // 原版同款时长缩减
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible()
                    ));
                }
            }

            // 2. 应用自定义附加效果（如果是通过 addEffect 加上的，时长不缩减）
            for (MobEffectInstance effect : PotionUtils.getCustomEffects(tag)) {
                livingTarget.addEffect(effect);
            }
        }

        // 6. 播放命中音效
        arrow.level().playSound(null, arrow.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * 创建穿甲伤害源，伤害归因于玩家
     */
    private static DamageSource createArmorBypassingSource(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(KOISHI_RANGED),
                player,
                player
        );
    }

    /**
     * 尝试解析玩家手持的武器，优先主手，其次副手
     * @param player 玩家
     * @return 如果主手或副手持有 IKogasaEquipment，则返回对应 ItemStack，否则返回空 ItemStack
     */
    private static ItemStack resolveWeapon(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof IKogasaEquipment) return off;
        return ItemStack.EMPTY;
    }
}