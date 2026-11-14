package net.kogasachan.kogasa_addiction_compat.compat.tacz;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

/**
 * TaCZ 运行时数值叠加：附件缓存 + 近战攻击属性。
 */
public final class TaczEventHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("KogasaTacz");

    private static final UUID MELEE_UUID = UUID.fromString("7b3e8a5e-6f3f-4d8d-9f95-6d5b1d9c2a71");

    private TaczEventHandler() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new TaczEventHandler());
    }

    /**
     * TaCZ 子弹命中前复用 Kogasa 连击 / Koishi 暴击。
     */
    @SubscribeEvent
    public void onGunHurtPre(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Pre event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getAttacker() instanceof ServerPlayer player)) return;
        if (!(event.getHurtEntity() instanceof net.minecraft.world.entity.LivingEntity target)) return;
        ItemStack stack = player.getMainHandItem();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;

        float base = event.getBaseAmount();
        if (net.kogasachan.kogasa_addiction.Config.ENABLE_KOGASA_RANGED.get()
                && net.kogasachan.kogasa_addiction.ability.KogasaAbilityData
                .hasAbility(stack, net.kogasachan.kogasa_addiction.ability.KogasaAbility.KOGASA)) {
            net.minecraft.nbt.CompoundTag data = player.getPersistentData();
            long now = player.level().getGameTime();
            long last = data.getLong("KogasaLastRangedHitTime");
            int reset = net.kogasachan.kogasa_addiction.Config.KOGASA_STREAK_RESET_SECONDS.get() * 20;
            int streak = now - last <= reset ? data.getInt("KogasaRangedHitStreak") + 1 : 0;
            int max = net.kogasachan.kogasa_addiction.Config.KOGASA_STREAK_MAX.get();
            if (max > 0 && streak > max) streak = max;
            data.putInt("KogasaRangedHitStreak", streak);
            data.putLong("KogasaLastRangedHitTime", now);
            double bonus = net.kogasachan.kogasa_addiction.Config.KOGASA_STREAK_BONUS.get();
            base *= (float) (1.0D + streak * bonus);
        }

        if (net.kogasachan.kogasa_addiction.Config.ENABLE_KOISHI_RANGED.get()
                && net.kogasachan.kogasa_addiction.ability.KogasaAbilityData
                .hasAbility(stack, net.kogasachan.kogasa_addiction.ability.KogasaAbility.KOISHI)
                && player.getRandom().nextDouble()
                < net.kogasachan.kogasa_addiction.Config.KOISHI_RANGED_CRIT_CHANCE.get()) {
            base *= net.kogasachan.kogasa_addiction.Config.KOISHI_RANGED_CRIT_MULTIPLIER.get();
        }
        event.setBaseAmount(base);
    }

    /**
     * TaCZ 子弹命中时复用 Aya 远程能力：速度 + 挖掘加速。
     */
    @SubscribeEvent
    public void onGunHurt(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getAttacker() instanceof ServerPlayer player)) return;
        if (!(event.getHurtEntity() instanceof net.minecraft.world.entity.LivingEntity)) return;
        ItemStack stack = player.getMainHandItem();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        if (!net.kogasachan.kogasa_addiction.Config.ENABLE_AYA_RANGED.get()) return;
        if (!net.kogasachan.kogasa_addiction.ability.KogasaAbilityData
                .hasAbility(stack, net.kogasachan.kogasa_addiction.ability.KogasaAbility.AYA)) return;

        int speedDuration = net.kogasachan.kogasa_addiction.Config.AYA_SPEED_DURATION_SECONDS.get() * 20;
        int speedAmplifier = net.kogasachan.kogasa_addiction.Config.AYA_SPEED_LEVEL.get() - 1;
        int hasteDuration = net.kogasachan.kogasa_addiction.Config.AYA_HASTE_DURATION_SECONDS.get() * 20;
        int hasteAmplifier = net.kogasachan.kogasa_addiction.Config.AYA_HASTE_LEVEL.get() - 1;
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,
                speedDuration, speedAmplifier, false, true));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DIG_SPEED,
                hasteDuration, hasteAmplifier, false, true));
    }

    /**
     * Cirno：TACZ 子弹命中时按概率给目标缓慢，并可能反噬自己。
     */
    @SubscribeEvent
    public void onGunHurtCirno(com.tacz.guns.api.event.common.EntityHurtByGunEvent.Post event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getAttacker() instanceof ServerPlayer player)) return;
        if (!(event.getHurtEntity() instanceof net.minecraft.world.entity.LivingEntity target)) return;
        ItemStack stack = player.getMainHandItem();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        if (!net.kogasachan.kogasa_addiction.Config.ENABLE_CIRNO_RANGED.get()) return;
        if (!net.kogasachan.kogasa_addiction.ability.KogasaAbilityData
                .hasAbility(stack, net.kogasachan.kogasa_addiction.ability.KogasaAbility.CIRNO)) return;

        if (player.getRandom().nextFloat() < net.kogasachan.kogasa_addiction.Config.CIRNO_SLOW_CHANCE.get()) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    (int) (net.kogasachan.kogasa_addiction.Config.CIRNO_SLOW_DURATION.get() * 20),
                    net.kogasachan.kogasa_addiction.Config.CIRNO_SLOW_LEVEL.get() - 1));
        }
    }

    @SubscribeEvent
    public void onGunFire(GunFireEvent event) {
        growFromGun(event.getShooter(), event.getGunItemStack(), event.getLogicalSide());
    }

    @SubscribeEvent
    public void onGunMelee(GunMeleeEvent event) {
        growFromGun(event.getShooter(), event.getGunItemStack(), event.getLogicalSide());
    }

    @SubscribeEvent
    public void onGunFirePost(com.tacz.guns.api.event.common.GunFirePostEvent event) {
        if (event.getLogicalSide() != LogicalSide.SERVER) return;
        if (!(event.getShooter() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getGunItemStack();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(stack);
        if (chance <= 0.0D || player.getRandom().nextDouble() >= chance) return;
        TaczCompat.restoreOneAmmo(stack);
    }

    private void growFromGun(net.minecraft.world.entity.LivingEntity shooter,
                             ItemStack stack, LogicalSide side) {
        if (side != LogicalSide.SERVER) return;
        if (!(shooter instanceof ServerPlayer player)) return;
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        KogasaGrowthHandler.addGrowth(stack, player);
    }

    @SubscribeEvent
    public void onAttachmentProperty(AttachmentPropertyEvent event) {
        ItemStack stack = event.getGunItem();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        if (!TaczCompat.hasData(stack)) return;

        GunData data = TaczCompat.gunData(stack);
        if (data == null) return;
        AttachmentCacheProperty cache = event.getCacheProperty();
        if (cache == null) return;

        int baseRpm = data.getRoundsPerMinute();
        int currentRpm = cache.getCache(GunProperties.ROUNDS_PER_MINUTE) != null
                ? cache.getCache(GunProperties.ROUNDS_PER_MINUTE) : baseRpm;
        cache.setCache(GunProperties.ROUNDS_PER_MINUTE,
                (int) Math.round(TaczCompat.increase(stack, currentRpm, TaczCompat.rpmBonus(stack))));

        float baseSpeed = data.getBulletData() == null ? 0.0F : data.getBulletData().getSpeed();
        Float currentSpeed = cache.getCache(GunProperties.AMMO_SPEED);
        if (currentSpeed == null) currentSpeed = baseSpeed;
        cache.setCache(GunProperties.AMMO_SPEED,
                (float) TaczCompat.increase(stack, currentSpeed, TaczCompat.speedBonus(stack)));

        ExtraDamage extra = data.getBulletData() == null ? null : data.getBulletData().getExtraDamage();
        float baseArmor = extra == null ? 0.0F : extra.getArmorIgnore();
        Float currentArmor = cache.getCache(GunProperties.ARMOR_IGNORE);
        if (currentArmor == null) currentArmor = baseArmor;
        cache.setCache(GunProperties.ARMOR_IGNORE,
                (float) TaczCompat.increase(stack, currentArmor, TaczCompat.armorIgnoreBonus(stack)));

        float baseHead = extra == null ? 1.0F : extra.getHeadShotMultiplier();
        Float currentHead = cache.getCache(GunProperties.HEADSHOT_MULTIPLIER);
        if (currentHead == null) currentHead = baseHead;
        cache.setCache(GunProperties.HEADSHOT_MULTIPLIER,
                (float) TaczCompat.increase(stack, currentHead, TaczCompat.headshotBonus(stack)));

        int basePierce = data.getBulletData() == null ? 0 : data.getBulletData().getPierce();
        Integer currentPierce = cache.getCache(GunProperties.PIERCE);
        if (currentPierce == null) currentPierce = basePierce;
        cache.setCache(GunProperties.PIERCE,
                (int) Math.round(TaczCompat.increase(stack, currentPierce, TaczCompat.pierceBonus(stack))));

        double baseWeight = data.getWeight();
        Float currentWeight = cache.getCache(GunProperties.WEIGHT);
        if (currentWeight == null) currentWeight = (float) baseWeight;
        cache.setCache(GunProperties.WEIGHT,
                (float) TaczCompat.decrease(stack, currentWeight, TaczCompat.weightBonus(stack)));

        LinkedList<ExtraDamage.DistanceDamagePair> damageList = cache.getCache(GunProperties.DAMAGE);
        if (damageList == null) {
            damageList = extra == null ? new LinkedList<>() : extra.getDamageAdjust();
        }
        if (damageList != null && !damageList.isEmpty()) {
            double factor = TaczCompat.damageFactor(stack);
            LinkedList<ExtraDamage.DistanceDamagePair> scaled = new LinkedList<>();
            for (ExtraDamage.DistanceDamagePair pair : damageList) {
                scaled.add(new ExtraDamage.DistanceDamagePair(
                        pair.getDistance(), (float) (pair.getDamage() * factor)));
            }
            cache.setCache(GunProperties.DAMAGE, scaled);
        }

        double growth = TaczCompat.growth(stack);
        if (growth > 0.0D) {
            float aimTime = cache.getCache(GunProperties.ADS_TIME) == null
                    ? data.getAimTime() : cache.getCache(GunProperties.ADS_TIME);
            cache.setCache(GunProperties.ADS_TIME,
                    Math.max(0.0F, (float) (aimTime - growth)));

            float range = cache.getCache(GunProperties.EFFECTIVE_RANGE) == null
                    ? 0.0F : cache.getCache(GunProperties.EFFECTIVE_RANGE);
            // 与成长值 1:1：成长 1 = +100% 优势射程。
            cache.setCache(GunProperties.EFFECTIVE_RANGE, (float) (range * (1.0D + growth)));

            Map<InaccuracyType, Float> inaccuracy = cache.getCache(GunProperties.INACCURACY);
            if (inaccuracy != null) {
                Map<InaccuracyType, Float> adjusted = new java.util.EnumMap<>(InaccuracyType.class);
                adjusted.putAll(inaccuracy);
                for (InaccuracyType type : new InaccuracyType[]{
                        InaccuracyType.STAND, InaccuracyType.SNEAK, InaccuracyType.LIE}) {
                    Float value = adjusted.get(type);
                    if (value != null) adjusted.put(type, Math.max(0.0F, (float) (value - growth)));
                }
                Float aim = adjusted.get(InaccuracyType.AIM);
                if (aim != null) adjusted.put(InaccuracyType.AIM,
                        Math.max(0.0F, (float) (aim * (1.0D - 0.1D * growth))));
                cache.setCache(GunProperties.INACCURACY, adjusted);
            }

            Map<InaccuracyType, Float> aimInaccuracy = cache.getCache(GunProperties.AIM_INACCURACY);
            if (aimInaccuracy != null) {
                Map<InaccuracyType, Float> adjusted = new java.util.EnumMap<>(InaccuracyType.class);
                for (var entry : aimInaccuracy.entrySet()) {
                    adjusted.put(entry.getKey(),
                            Math.max(0.0F, (float) (entry.getValue() * (1.0D - 0.1D * growth))));
                }
                cache.setCache(GunProperties.AIM_INACCURACY, adjusted);
            }

        }
    }


    @SubscribeEvent
    public void onAttributeModifiers(ItemAttributeModifierEvent event) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) return;
        ItemStack stack = event.getItemStack();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;
        if (!TaczCompat.hasData(stack)) return;

        double damage = TaczCompat.meleeDamage(stack);
        if (damage <= 0.0D) return;
        event.addModifier(Attributes.ATTACK_DAMAGE,
                new AttributeModifier(MELEE_UUID, "Kogasa TaCZ melee", damage,
                        AttributeModifier.Operation.ADDITION));
    }
}
