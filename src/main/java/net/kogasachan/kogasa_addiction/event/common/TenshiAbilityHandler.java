package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.network.TenshiAbilityPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TenshiAbilityHandler {

    private static final String NBT_CHARGE = "KogasaTenshiCharge";
    private static final String NBT_READY = "KogasaTenshiBurstReady";

    // ==================== 1. 蓄力与爆发逻辑 ====================

    @SubscribeEvent
    public static void onLivingHurt$defence(LivingHurtEvent event) {
        // ===== 防御端：玩家受伤蓄力 =====
        if (event.getEntity() instanceof ServerPlayer player) {
            if (KogasaAbilityHelper.hasAbilityInArmor(player, KogasaAbility.TENSHI)) {
                double threshold = Math.max(0.0, Config.TENSHI_DOMAIN_THRESHOLD.get());
                double currentCharge = player.getPersistentData().getDouble(NBT_CHARGE);
                boolean isReady = player.getPersistentData().getBoolean(NBT_READY);

                if (!isReady && threshold > 0.0 && currentCharge < threshold) {
                    double damage = Math.max(0.0, event.getAmount());
                    currentCharge = Math.min(threshold, currentCharge + damage);

                    player.getPersistentData().putDouble(NBT_CHARGE, currentCharge);

                    if (currentCharge >= threshold) {
                        isReady = true;
                        player.getPersistentData().putBoolean(NBT_READY, true);

                        // 仅在首次蓄满时播放提示
                        player.level().playSound(
                                null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                                SoundSource.PLAYERS, 1.0F, 1.2F);

                        player.displayClientMessage(
                                Component.translatable("message.kogasa_addiction.tenshi_ability")
                                        .withStyle(ChatFormatting.GOLD),
                                true);
                    }

                    PacketHandler.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new TenshiAbilityPacket(currentCharge, isReady));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt$attack(LivingHurtEvent event) {
        // ===== 攻击端：下一次命中爆发 =====
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!KogasaAbilityHelper.hasAbilityInArmor(attacker, KogasaAbility.TENSHI)) return;
            if (!attacker.getPersistentData().getBoolean(NBT_READY)) return;

            LivingEntity target = event.getEntity();
            if (target == attacker) return;

            // 触发爆发
            float extraDamage = target.getMaxHealth() * Config.TENSHI_BURST_DAMAGE_RATIO.get().floatValue();
            event.setAmount(event.getAmount() + extraDamage);

            // 清空蓄力记录
            clearCharge(attacker);

            // 播放爆发音效和粒子
            attacker.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5F, 1.5F);
            if (attacker.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.5, 0.5, 0.5, 0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt$special(LivingHurtEvent event) {
        // ===== 攻击端：全人类的绯想天（命中特效） =====
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            // 检查攻击者是否穿有天子护甲（或者手持带有天子能力的武器）
            if (!KogasaAbilityHelper.hasAbilityInArmor(attacker, KogasaAbility.TENSHI) && !KogasaAbilityHelper.hasAbilityInMainHand(attacker, KogasaAbility.TENSHI))
                return;

            LivingEntity target = event.getEntity();
            if (target == attacker) return;

            // 1. 施加负面效果（VII 级缓慢与挖掘疲劳）
            int debuffDuration = Config.TENSHI_DEBUFF_DURATION_SECONDS.get() * 20;
            // amplifier 6 代表等级 VII（原版极高级别，近乎定身和无法挖掘）
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, debuffDuration, 6));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, debuffDuration, 6));

            // 2. 对非 Boss 飞行生物造成额外伤害
            if (isNonBossFlyingMob(target)) {
                float multiplier = Config.TENSHI_FLYING_MOB_MULTIPLIER.get().floatValue();
                event.setAmount(event.getAmount() * multiplier);
            }

            // 3. 随机获得原版增益效果（叠加机制）
            if (!attacker.level().isClientSide) {
                applyRandomBuff(attacker);
            }
        }
    }

    // ==================== 2. 动态抗性提升 ====================

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        // 每 100 tick 检查一次即可，没必要每 tick 都算
        if (player.tickCount % 100 != 0) return;

        if (!KogasaAbilityHelper.hasAbilityInArmor(player, KogasaAbility.TENSHI)) {
            clearCharge(player);
            return;
        }

        double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();
        double currentCharge = player.getPersistentData().getDouble(NBT_CHARGE);
        int desiredLevel = currentCharge > 0
                ? (int) Math.min(5, Math.floor(currentCharge / (threshold / 4.0)))
                : 0;

        if (desiredLevel <= 0) return;

        MobEffectInstance current = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        int currentAmp = current != null ? current.getAmplifier() : -1;

        // 只有等级不符，或剩余时间快不够了，才刷新
        if (currentAmp != desiredLevel - 1 || current.getDuration() < 30) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DAMAGE_RESISTANCE,
                    120,                 // 持续 6 秒，够撑到下一次检查
                    desiredLevel - 1,
                    false, false, true));
        }
    }

    // ==================== 3. 死亡清零 ====================

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            clearCharge(player);
        }
    }

    // ==================== 供外部调用的清零方法 ====================

    public static void clearCharge(Player player) {
        var data = player.getPersistentData();
        boolean wasActive = data.getDouble(NBT_CHARGE) != 0.0D || data.getBoolean(NBT_READY);
        data.putDouble(NBT_CHARGE, 0.0);
        data.putBoolean(NBT_READY, false);
        if (wasActive && player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new TenshiAbilityPacket(0, false)
            );
        }
    }

    /**
     * 判定是否为非Boss飞行生物
     * 原版中大部分飞行生物都有 FLYING_SPEED 属性，但末影龙和凋灵是Boss需排除
     */
    private static boolean isNonBossFlyingMob(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return false;
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return false;

        // 原版用 FlyingMob 表示"飞行生物"（蝙蝠 / 幻翼 / 恶魂 / 蜜蜂 / 悦灵）。
        // 不能用 getAttributes().hasAttribute(FLYING_SPEED)：该属性由
        // LivingEntity.createLivingAttributes() 给**所有**生物注册（僵尸、牛也有，值为 0），
        // 所以那个判定等价于"除凋灵与末影龙以外的所有生物"，原意被放大成了群体增伤。
        if (target instanceof net.minecraft.world.entity.FlyingMob) {
            return true;
        }

        // FlyingMob 之外、原版里确实会飞的个别实体
        net.minecraft.world.entity.EntityType<?> type = target.getType();
        return type == net.minecraft.world.entity.EntityType.VEX
                || type == net.minecraft.world.entity.EntityType.PARROT
                || type == net.minecraft.world.entity.EntityType.BLAZE;
    }

    /**
     * 随机赋予一种原版正面增益，并实现叠加逻辑
     */
    private static void applyRandomBuff(ServerPlayer player) {
        List<MobEffect> beneficialEffects = List.of(
                MobEffects.MOVEMENT_SPEED,
                MobEffects.DIG_SPEED,
                MobEffects.DAMAGE_BOOST,
                MobEffects.JUMP,
                MobEffects.REGENERATION,
                MobEffects.DAMAGE_RESISTANCE,
                MobEffects.FIRE_RESISTANCE,
                MobEffects.WATER_BREATHING,
                MobEffects.INVISIBILITY,
                MobEffects.NIGHT_VISION,
                MobEffects.HEALTH_BOOST,
                MobEffects.ABSORPTION,
                MobEffects.SATURATION,
                MobEffects.LUCK
        );

        MobEffect chosen = beneficialEffects.get(player.getRandom().nextInt(beneficialEffects.size()));
        MobEffectInstance current = player.getEffect(chosen);

        int newDuration = 200; // 默认 10 秒
        int newAmplifier = 0;  // 默认 I 级

        if (current != null) {
            // 时长叠加：每次 +10秒（200 tick），最高 60秒（1200 tick）
            newDuration = Math.min(current.getDuration() + 200, 1200);
            // 等级叠加：每次 +1 级，最高 IV 级（amplifier 3）
            newAmplifier = Math.min(current.getAmplifier() + 1, 3);
        }

        player.addEffect(new MobEffectInstance(chosen, newDuration, newAmplifier, false, true, true));
    }
}