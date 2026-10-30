package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.minecraft.advancements.CriteriaTriggers;
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
import net.minecraft.world.entity.EntityType;
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

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiRangedAbilityHandler {

    public static final ResourceKey<DamageType> KOISHI_RANGED = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "koishi_ranged"));

    /**
     * 已穿透实体数量，用于在自定义命中结算下模拟原版穿透上限。
     */
    private static final String PIERCE_HITS = "KogasaRangedPierceHits";

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult hitResult)) return;

        Entity target = hitResult.getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = KogasaAbilityHelper.getHeldKogasaEquipment(player);

        // 优先读取发射时写入投掷物的标记，其次回退到当前手持装备判定的旧行为。
        // 这样射完后切换武器不会丢失能力，也不会把能力错误地施加到别的箭上。
        boolean hasKoishi = Config.ENABLE_KOISHI_RANGED.get()
                && (arrow.getPersistentData().getBoolean(KogasaAbilityData.PROJECTILE_KOISHI)
                || (!weapon.isEmpty() && KogasaAbilityData.hasAbility(weapon, KogasaAbility.KOISHI)));
        boolean hasTenshi = arrow.getPersistentData().getBoolean(KogasaAbilityData.PROJECTILE_TENSHI)
                || (!weapon.isEmpty() && KogasaAbilityData.hasAbility(weapon, KogasaAbility.TENSHI));
        if (!hasKoishi && !hasTenshi) return;

        // ===== 接管命中，伤害由本方法结算 =====
        // SKIP_ENTITY 会让箭矢把该实体加入忽略列表并继续飞行，从而保留穿透；
        // 是否继续由 finishImpact 按原版穿透上限判断。
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);

        // 计算基础伤害
        double speed = arrow.getDeltaMovement().length();
        float damage = (float) (speed * arrow.getBaseDamage());
        if (arrow.isCritArrow()) damage *= 1.5F;

        // ===== 分支 1：只有天子，没有恋恋 =====
        if (!hasKoishi) {
            // 使用魔法伤害源（或自定义 tenshi_ranged），确保不被末影人/凋零免疫，从而触发 LivingHurtEvent
            DamageSource tenshiSource = player.damageSources().indirectMagic(arrow, player);
            boolean damaged = livingTarget.hurt(tenshiSource, damage);
            recordVanillaBookkeeping(arrow, livingTarget, player, damaged);

            arrow.level().playSound(null, arrow.blockPosition(), SoundEvents.ARROW_HIT,
                    SoundSource.PLAYERS, 1.0F, 1.2F);
            finishImpact(arrow);
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
        boolean damaged = livingTarget.hurt(killSource, damage);

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
        recordVanillaBookkeeping(arrow, livingTarget, player, damaged);
        finishImpact(arrow);
    }

    /**
     * 自定义命中绕过了 AbstractArrow.onHitEntity，这里补回原版相关的记录：
     * 箭矢插在实体上的计数，以及弩的 KILLED_BY_CROSSBOW 进度。
     */
    private static void recordVanillaBookkeeping(AbstractArrow arrow, LivingEntity target,
                                                 ServerPlayer player, boolean damaged) {
        if (!damaged || arrow.level().isClientSide) return;
        // 原版对末影人在 hurt 后直接返回，不记录箭矢计数与击杀
        if (target.getType() == EntityType.ENDERMAN) return;

        if (arrow.getPierceLevel() <= 0) {
            target.setArrowCount(target.getArrowCount() + 1);
        }

        if (!arrow.shotFromCrossbow() || target.isAlive()) return;

        if (arrow.getPierceLevel() > 0) {
            List<Entity> killed = arrow.piercedAndKilledEntities;
            if (killed == null) {
                killed = new ArrayList<>();
                arrow.piercedAndKilledEntities = killed;
            }
            killed.add(target);
            CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(player, killed);
        } else {
            CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(player, List.of(target));
        }
    }

    /**
     * 按原版穿透规则结束本次命中：
     * 穿透等级 N 最多命中 N+1 个实体，超过后丢弃箭矢。
     */
    private static void finishImpact(AbstractArrow arrow) {
        int pierceLevel = arrow.getPierceLevel();
        if (pierceLevel <= 0) {
            arrow.discard();
            return;
        }

        CompoundTag data = arrow.getPersistentData();
        int hits = data.getInt(PIERCE_HITS) + 1;
        data.putInt(PIERCE_HITS, hits);
        if (hits > pierceLevel) {
            arrow.discard();
        }
    }

    private static DamageSource createArmorBypassingSource(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KOISHI_RANGED),
                player, player
        );
    }
}