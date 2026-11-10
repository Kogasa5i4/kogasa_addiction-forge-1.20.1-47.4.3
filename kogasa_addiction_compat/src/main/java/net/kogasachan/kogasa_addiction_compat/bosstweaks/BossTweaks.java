package net.kogasachan.kogasa_addiction_compat.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.compat.CompatConfig;

/** Boss 行为修正的运行时开关；配置尚未加载时按默认全开处理。 */
public final class BossTweaks {

    private BossTweaks() {
    }

    private static boolean enabled() {
        try {
            return CompatConfig.ENABLE_BOSS_TWEAKS.get();
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private static boolean value(boolean fallback, java.util.function.BooleanSupplier supplier) {
        if (!enabled()) return false;
        try {
            return supplier.getAsBoolean();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    public static boolean preventApollyonItemTheft() {
        return value(true, CompatConfig.APOLLYON_PREVENT_ITEM_THEFT::get);
    }

    public static boolean preventApollyonAttributeReset() {
        return value(true, CompatConfig.APOLLYON_PREVENT_ATTRIBUTE_RESET::get);
    }

    public static boolean preventApollyonCooldown() {
        return value(true, CompatConfig.APOLLYON_PREVENT_COOLDOWN::get);
    }

    public static boolean preventYhDamageImmunity() {
        return value(true, CompatConfig.YH_PREVENT_DAMAGE_IMMUNITY::get);
    }

    public static boolean preventYhSetHealthState() {
        return value(true, CompatConfig.YH_PREVENT_SET_HEALTH_STATE::get);
    }

    public static double yhNormalDamageCap() {
        try {
            return CompatConfig.YH_NORMAL_DAMAGE_CAP.get();
        } catch (IllegalStateException ignored) {
            return 0.05D;
        }
    }

    /** 反射判断亚波伦是否处于 Doom 尾杀阶段；读不到时按“普通阶段”处理。 */
    public static boolean isApollyonDoom(Object apostle) {
        if (apostle == null) return false;
        for (String name : new String[]{"getDoom", "isDoomNow", "allTitlesApostle_1_20_1$getDoom"}) {
            try {
                Object value = apostle.getClass().getMethod(name).invoke(apostle);
                if (value instanceof Boolean b) return b;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        // RevelationFix 的真实判定入口：SafeClass.isDoom(Apostle)。
        try {
            Class<?> safe = Class.forName("com.mega.revelationfix.common.compat.SafeClass");
            for (java.lang.reflect.Method method : safe.getMethods()) {
                if (!method.getName().equals("isDoom") || method.getParameterCount() != 1) continue;
                if (!method.getParameterTypes()[0].isInstance(apostle)) continue;
                Object value = method.invoke(null, apostle);
                if (value instanceof Boolean b) return b;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        return false;
    }

    /** 让 YH 实体在 setHealth<=0 时真正进入死亡，而不是停在 0 血。 */
    public static void killYhEntity(net.minecraft.world.entity.LivingEntity entity) {
        // 这里不能用 isDeadOrDying(): setHealth(0) 后它已经是 true，
        // 但 YH 的 die() 仍需要被调用才会真正进入死亡流程。
        if (entity == null || entity.isRemoved()) return;
        net.minecraft.world.damagesource.DamageSource source =
                entity.level().damageSources().genericKill();
        for (String name : new String[]{"die", "m_6667_"}) {
            try {
                var method = entity.getClass().getMethod(name,
                        net.minecraft.world.damagesource.DamageSource.class);
                method.invoke(entity, source);
                if (entity.isDeadOrDying()) return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        entity.hurt(source, Float.MAX_VALUE);
    }

    public static boolean preventApollyonFlightBan() {
        return value(true, CompatConfig.APOLLYON_PREVENT_FLIGHT_BAN::get);
    }

    public static boolean preventApollyonInvulnerability() {
        return value(true, CompatConfig.APOLLYON_PREVENT_INVULNERABILITY::get);
    }

    public static boolean preventUomInvulnerability() {
        return value(true, CompatConfig.UOM_PREVENT_INVULNERABILITY::get);
    }

    public static boolean replaceYhDanmakuSetHealth() {
        return value(true, CompatConfig.YH_REPLACE_DANMAKU_SET_HEALTH::get);
    }

    public static boolean preventYhEntityInvulnerability() {
        return value(true, CompatConfig.YH_PREVENT_ENTITY_INVULNERABILITY::get);
    }

    public static double yhDanmakuDamageCap() {
        try {
            return CompatConfig.YH_DANMAKU_DAMAGE_CAP.get();
        } catch (IllegalStateException ignored) {
            return 0.20D;
        }
    }
}
