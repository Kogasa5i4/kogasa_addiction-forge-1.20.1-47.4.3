package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.network.TenshiSyncPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TenshiAbilityHandler {

    private static final String NBT_CHARGE = "KogasaTenshiCharge";
    private static final String NBT_READY = "KogasaTenshiBurstReady";

    /**
     * 检查玩家是否穿有天子护甲
     */
    private static boolean hasTenshiArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof IKogasaEquipment &&
                    KogasaAbilityData.hasAbility(armor, KogasaAbility.TENSHI)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 1. 蓄力与爆发逻辑 ====================

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // ===== 防御端：玩家受伤蓄力 =====
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!hasTenshiArmor(player)) return;

            double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();
            double currentCharge = player.getPersistentData().getDouble(NBT_CHARGE);
            boolean isReady = player.getPersistentData().getBoolean(NBT_READY);

            // 如果已经蓄满，不再继续积累伤害
            if (!isReady && currentCharge < threshold) {
                double damage = event.getAmount();
                double remaining = threshold - currentCharge;

                // 单次溢出不计入：只取剩余所需的部分
                double toAdd = Math.min(damage, remaining);
                currentCharge += toAdd;

                player.getPersistentData().putDouble(NBT_CHARGE, currentCharge);

                // 如果达到阈值，标记为“蓄满”
                if (currentCharge >= threshold) {
                    player.getPersistentData().putBoolean(NBT_READY, true);
                    PacketHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new TenshiSyncPacket(currentCharge, true)
                    );
                    // 播放提示音效和文字
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 1.2F);
                    player.displayClientMessage(
                            Component.translatable("message.kogasa_addiction.tenshi_ability")
                                    .withStyle(ChatFormatting.GOLD), true);
                }
            }

            PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new TenshiSyncPacket(currentCharge, false)
            );
        }

        // ===== 攻击端：下一次命中爆发 =====
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            if (!hasTenshiArmor(attacker)) return;
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

        // ===== 攻击端：全人类的绯想天（命中特效） =====
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            // 检查攻击者是否穿有天子护甲（或者手持带有天子能力的武器）
            if (!hasTenshiArmor(attacker) && !hasTenshiWeapon(attacker)) return;

            LivingEntity target = event.getEntity();
            if (target == attacker) return;

            // 1. 施加负面效果（最高等级缓慢和挖掘疲劳）
            int debuffDuration = Config.TENSHI_DEBUFF_DURATION_SECONDS.get() * 20;
            // amplifier 6 代表等级 VII（原版极高级别，近乎定身和无法挖掘）
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, debuffDuration, 6));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, debuffDuration, 6));

            // 2. 对非 Boss 飞行生物造成额外伤害
            if (isNonBossFlyingMob(target)) {
                float multiplier = Config.TENSHI_FLYING_MOB_MULTIPLIER.get().floatValue();
                event.setAmount(event.getAmount() * multiplier);
                // 可选：播放命中特效
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

        // 如果没穿天子护甲，清空蓄力并移除自身加的抗性
        if (!hasTenshiArmor(player)) {
            clearCharge(player);
            return;
        }

        double currentCharge = player.getPersistentData().getDouble(NBT_CHARGE);
        if (currentCharge > 0) {
            double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();
            // 每达到四分之一阈值获得1级，最高5级(如果阈值20则每5点1级)
            int resistanceLevel = (int) Math.min(5, Math.floor(currentCharge / (threshold / 4.0)));

            if (resistanceLevel > 0) {
                // 持续时间设置短一点（40 tick），每 tick 刷新，确保不会常驻
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
                        40, resistanceLevel - 1, false, false, true));
            }
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
        player.getPersistentData().putDouble(NBT_CHARGE, 0.0);
        player.getPersistentData().putBoolean(NBT_READY, false);
        if (player instanceof ServerPlayer sp) {
            PacketHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new TenshiSyncPacket(0, false)
            );
        }
    }

    /**
     * 检查主手武器是否带有天子能力
     */
    private static boolean hasTenshiWeapon(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof IKogasaEquipment &&
                KogasaAbilityData.hasAbility(main, KogasaAbility.TENSHI);
    }

    /**
     * 判定是否为非Boss飞行生物
     * 原版中大部分飞行生物都有 FLYING_SPEED 属性，但末影龙和凋灵是Boss需排除
     */
    private static boolean isNonBossFlyingMob(LivingEntity target) {
        if (target instanceof net.minecraft.world.entity.boss.wither.WitherBoss) return false;
        if (target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return false;

        // 通过属性判断是否能飞行（蝙蝠、幻翼、恶魂、蜜蜂等都有飞行速度属性）
        if (target.getAttributes().hasAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FLYING_SPEED)) {
            return true;
        }
        // 部分特殊原版飞行生物没有飞行属性，枚举补充
        return target.getType() == net.minecraft.world.entity.EntityType.GHAST
                || target.getType() == net.minecraft.world.entity.EntityType.PHANTOM
                || target.getType() == net.minecraft.world.entity.EntityType.ALLAY;
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