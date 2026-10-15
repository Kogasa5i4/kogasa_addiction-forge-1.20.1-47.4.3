package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiRangedAbilityHandler {
    private static final ThreadLocal<Boolean> KOGASA_KOISHI_REPROCESSING =
            ThreadLocal.withInitial(() -> false);

    /** 原版穿甲伤害类型：generic_kill（/kill 使用，100% 穿甲） */
    private static final ResourceKey<DamageType> GENERIC_KILL = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("minecraft", "generic_kill"));

    @SubscribeEvent
    public static void onProjectileHit(LivingHurtEvent event) {
        if (!Config.ENABLE_KOISHI_RANGED.get()) return;
        if (KOGASA_KOISHI_REPROCESSING.get()) return;

        Entity direct = event.getSource().getDirectEntity();
        if (!(direct instanceof Arrow) && !(direct instanceof FireworkRocketEntity)) return;

        Entity owner = direct instanceof Arrow arrow ? arrow.getOwner()
                : ((FireworkRocketEntity) direct).getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = resolveWeapon(player);
        if (weapon.isEmpty()) return;
        if (!KogasaAbilityData.hasAbility(weapon, KogasaAbility.KOISHI)) return;

        LivingEntity target = event.getEntity();
        float originalDamage = event.getAmount();

        // 弹射物免疫：直接 3 倍（可配置）
        if (isProjectileImmune(target)) {
            event.setCanceled(true);
            DamageSource killSource = createArmorBypassingSource(player);
            float multiplier = Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get().floatValue();
            target.hurt(killSource, originalDamage * multiplier);
            return;
        }

        // 正常暴击
        float chance = Config.KOISHI_RANGED_CRIT_CHANCE.get().floatValue();
        if (player.getRandom().nextFloat() < chance) {
            event.setCanceled(true);
            DamageSource killSource = createArmorBypassingSource(player);
            float multiplier = Config.KOISHI_RANGED_CRIT_MULTIPLIER.get().floatValue();
            KOGASA_KOISHI_REPROCESSING.set(true);
            try {
                target.hurt(killSource, originalDamage * multiplier);
            } finally {
                KOGASA_KOISHI_REPROCESSING.set(false);
            }
        }
    }

    /**
     * 创建穿甲伤害源：使用 generic_kill 类型，伤害归因于玩家
     */
    private static DamageSource createArmorBypassingSource(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        DamageType type = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(GENERIC_KILL)
                .value();
        return new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(GENERIC_KILL),
                player,
                player
        );
    }

    /**
     * 检查目标是否免疫弹射物伤害
     */
    private static boolean isProjectileImmune(LivingEntity target) {
        // 末影人：免疫所有弹射物
        if (target instanceof EnderMan) return true;

        // 凋灵：半血以下进入“凋灵护甲”状态，免疫箭矢和药水
        if (target instanceof WitherBoss wither) {
            return wither.getHealth() <= wither.getMaxHealth() / 2.0F;
        }

        return false;
    }

    private static ItemStack resolveWeapon(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof IKogasaEquipment) return off;
        return ItemStack.EMPTY;
    }
}