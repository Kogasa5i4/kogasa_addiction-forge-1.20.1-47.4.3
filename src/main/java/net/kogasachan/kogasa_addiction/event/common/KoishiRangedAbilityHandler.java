package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
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

    private static final ResourceKey<DamageType> KOISHI_RANGED = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "koishi_ranged"));

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = KogasaAbilityHelper.getHeldKogasaEquipment(player);
        if (weapon.isEmpty()) return;

        boolean hasKoishi = Config.ENABLE_KOISHI_RANGED.get()
                && KogasaAbilityData.hasAbility(weapon, KogasaAbility.KOISHI);
        boolean hasTenshi = KogasaAbilityData.hasAbility(weapon, KogasaAbility.TENSHI);
        if (!hasKoishi && !hasTenshi) return;

        // ===== 开始接管命中，取消原版事件 =====
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        arrow.discard();

        // 计算基础伤害
        double speed = arrow.getDeltaMovement().length();
        float damage = (float) (speed * arrow.getBaseDamage());
        if (arrow.isCritArrow()) damage *= 1.5F;

        // ===== 分支 1：只有天子，没有恋恋 =====
        if (!hasKoishi) {
            // 使用魔法伤害源（或自定义 tenshi_ranged），确保不被末影人/凋零免疫，从而触发 LivingHurtEvent
            DamageSource tenshiSource = player.damageSources().indirectMagic(arrow, player);
            livingTarget.hurt(tenshiSource, damage);

            arrow.level().playSound(null, arrow.blockPosition(), SoundEvents.ARROW_HIT,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            return; // 结束，交由 TenshiAbilityHandler 处理特效
        }

        // ===== 分支 2：拥有恋恋能力 =====
        // 3. KOISHI 的暴击判定
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
        if (arrow.getKnockback() > 0) {
            Vec3 vec3 = arrow.getDeltaMovement().normalize().scale(arrow.getKnockback() * 0.6);
            livingTarget.push(vec3.x, 0.1, vec3.z);
        }
        if (arrow.isOnFire()) {
            livingTarget.setSecondsOnFire(5);
        }
        if (arrow instanceof Arrow vanillaArrow) {
            CompoundTag tag = new CompoundTag();
            vanillaArrow.saveWithoutId(tag);

            Potion potion = PotionUtils.getPotion(tag);
            if (potion != Potions.EMPTY) {
                for (MobEffectInstance effect : potion.getEffects()) {
                    livingTarget.addEffect(new MobEffectInstance(
                            effect.getEffect(),
                            Math.max(effect.getDuration() / 8, 1),
                            effect.getAmplifier(),
                            effect.isAmbient(),
                            effect.isVisible()
                    ));
                }
            }
            for (MobEffectInstance effect : PotionUtils.getCustomEffects(tag)) {
                livingTarget.addEffect(effect);
            }
        }

        arrow.level().playSound(null, arrow.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    private static DamageSource createArmorBypassingSource(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KOISHI_RANGED),
                player, player
        );
    }
}