package net.kogasachan.kogasa_addiction_compat.compat.tacz;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * TaCZ 枪械兼容数据。
 *
 * <p>TaCZ 的 {@code GunData} 是全局 JSON 数据，不能按物品修改；这里把吸收增量存在
 * 枪械自己的 {@code KogasaTacz} NBT 中，再由 {@link TaczEventHandler} 在 TaCZ
 * 的 AttachmentCacheProperty 上叠加，避免改到同 GunId 的其它枪。
 */
public final class TaczCompat {

    private static final String ROOT = "KogasaTacz";
    private static final double RATE = 0.10D;
    private static final double AMMO_RATE = 0.50D;

    private TaczCompat() {
    }

    public static void register() {
        KogasaEquipmentHelper.registerReinforcementMatcher(TaczCompat::matchesMaterial);
        KogasaEquipmentHelper.registerAbsorptionListener(TaczCompat::absorb);
        KogasaEquipmentHelper.registerAbsorptionCompleteListener(TaczCompat::refreshCache);
        KogasaEquipmentHelper.registerStatVariantResolver(TaczCompat::variantKey);
        KogasaEquipmentHelper.registerRangedPredicate(TaczCompat::isGun);
        KogasaEquipmentHelper.registerRangedPowerProvider(TaczCompat::rangedPower);
        KogasaEquipmentHelper.registerRangedDrawProvider(TaczCompat::rangedDrawTicks);
        net.kogasachan.kogasa_addiction.util.KogasaDisplayHelper
                .registerDisplayNameResolver(TaczCompat::displayName);
        net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat
                .registerCombinablePredicate(TaczCompat::isGun);
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (IGun.getIGunOrNull(stack) != null) return true;
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains("GunId", net.minecraft.nbt.Tag.TAG_STRING);
    }

    public static boolean matchesMaterial(ItemStack left, ItemStack right) {
        return KogasaCompat.isCompatible(left) && isGun(left) && isGun(right);
    }

    public static String variantKey(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;
        String id = tag.getString("GunId");
        return id.isBlank() ? null : id;
    }

    /**
     * 吸收完成后主动让 TaCZ 重算该玩家手枪的附件缓存。
     */
    public static void refreshCache(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (player == null || !isGun(stack)) return;
        com.tacz.guns.resource.modifier.AttachmentPropertyManager.postChangeEvent(player, stack);
    }

    public static void restoreOneAmmo(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        GunData data = gunData(stack);
        if (gun == null || data == null) return;
        int max = com.tacz.guns.util.AttachmentDataUtils.getAmmoCountWithAttachment(stack, data);
        int current = gun.getCurrentAmmoCount(stack);
        if (current < max) {
            gun.setCurrentAmmoCount(stack, current + 1);
        }
    }

    public static net.minecraft.network.chat.Component displayName(String id) {
        int hash = id.indexOf('#');
        if (hash < 0) return null;
        String variant = id.substring(hash + 1);
        ResourceLocation gunId = ResourceLocation.tryParse(variant);
        if (gunId == null) return null;
        com.tacz.guns.client.resource.index.ClientGunIndex index =
                com.tacz.guns.api.TimelessAPI.getClientGunIndex(gunId).orElse(null);
        if (index != null && index.getName() != null && !index.getName().isBlank()) {
            return net.minecraft.network.chat.Component.translatable(index.getName());
        }
        return null;
    }

    public static float displayArmorIgnore(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null || data.getBulletData() == null) return 0.0F;
        ExtraDamage extra = data.getBulletData().getExtraDamage();
        float base = extra == null ? 0.0F : extra.getArmorIgnore();
        return (float) increase(stack, base, armorIgnoreBonus(stack));
    }

    public static float displayHeadshot(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null || data.getBulletData() == null) return 1.0F;
        ExtraDamage extra = data.getBulletData().getExtraDamage();
        float base = extra == null ? 1.0F : extra.getHeadShotMultiplier();
        return (float) increase(stack, base, headshotBonus(stack));
    }

    public static float displayWeight(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null) return 0.0F;
        return (float) decrease(stack, data.getWeight(), weightBonus(stack));
    }

    public static int displayRpm(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null) return 0;
        return (int) Math.round(increase(stack, data.getRoundsPerMinute(), rpmBonus(stack)));
    }

    public static int displayAmmo(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null) return 0;
        int base = com.tacz.guns.util.AttachmentDataUtils.getAmmoCountWithAttachment(stack, data);
        return base + (int) Math.round(ammoBonus(stack, base,
                com.tacz.guns.util.AttachmentDataUtils.getMagExtendLevel(stack, data)));
    }

    public static float displayDamage(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null || data.getBulletData() == null) return 0.0F;
        return (float) (data.getBulletData().getDamageAmount() * damageFactor(stack));
    }

    public static double rangedPower(ItemStack stack) {
        return baseDamage(stack);
    }

    public static double rangedDrawTicks(ItemStack stack) {
        GunData data = gunData(stack);
        if (data == null) return 0.0D;
        return Math.max(1.0D, data.getDrawTime() * 20.0D);
    }

    public static boolean hasData(ItemStack stack) {
        return !read(stack).isEmpty();
    }

    public static GunData gunData(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return null;
        ResourceLocation gunId = gun.getGunId(stack);
        if (gunId == null) return null;
        CommonGunIndex index = TimelessAPI.getCommonGunIndex(gunId).orElse(null);
        return index == null ? null : index.getGunData();
    }

    /**
     * Anvil 吸收后把右侧枪械基础值的比例累加到左侧枪。
     */
    public static void absorb(ItemStack result, ItemStack right) {
        if (!isGun(result) || !isGun(right)) return;
        if (!KogasaCompat.isCompatible(result)) return;

        GunData data = gunData(right);
        if (data == null) return;
        BulletData bullet = data.getBulletData();
        if (bullet == null) return;

        TaczData current = read(result);
        current.damage += bullet.getDamageAmount();
        current.rpm += data.getRoundsPerMinute() * RATE;
        ExtraDamage extra = bullet.getExtraDamage();
        if (extra != null) {
            current.armorIgnore += extra.getArmorIgnore() * RATE;
            current.headshot += extra.getHeadShotMultiplier() * RATE;
        }
        current.speed += bullet.getSpeed() * RATE;
        current.pierce += bullet.getPierce() * RATE;
        current.gravity -= bullet.getGravity() * RATE;
        current.weight -= data.getWeight() * RATE;
        current.ammo += data.getAmmoAmount() * AMMO_RATE;
        int[] extended = data.getExtendedMagAmmoAmount();
        if (extended != null) {
            for (int i = 0; i < Math.min(3, extended.length); i++) {
                current.magBonus[i] += extended[i] * AMMO_RATE;
            }
        }
        write(result, current);
    }

    public static double growth(ItemStack stack) {
        return Math.max(0.0D, KogasaEquipmentData.getGrowth(stack));
    }

    public static double growthProgress(ItemStack stack) {
        double max = net.kogasachan.kogasa_addiction.Config.MAX_GROWTH.get();
        if (max <= 0.0D) return 0.0D;
        return Math.max(0.0D, Math.min(1.0D, growth(stack) / max));
    }

    /**
     * 增加类属性的成长：满成长总量为当前累计值的 2x。
     */
    public static double increase(ItemStack stack, double base, double bonus) {
        // 小伞的“综合值”包含强化数、成长和文文新闻值；不能只乘 growth。
        return (base + bonus) * KogasaEquipmentHelper.getTotalMultiplier(stack);
    }

    /**
     * 降低类属性的成长：满成长总量为当前累计值的 0.5x。
     */
    public static double decrease(ItemStack stack, double base, double bonus) {
        return Math.max(0.0D, (base + bonus) * (1.0D - 0.5D * growthProgress(stack)));
    }

    /**
     * 弹匣：满成长总量为当前累计值的 10x。
     */
    public static double ammo(ItemStack stack, double base, double bonus) {
        return Math.max(0.0D, (base + bonus) * (1.0D + 9.0D * growthProgress(stack)));
    }

    public static double rpmBonus(ItemStack stack) {
        return read(stack).rpm;
    }

    public static double armorIgnoreBonus(ItemStack stack) {
        return read(stack).armorIgnore;
    }

    public static double headshotBonus(ItemStack stack) {
        return read(stack).headshot;
    }

    public static double speedBonus(ItemStack stack) {
        return read(stack).speed;
    }

    public static double pierceBonus(ItemStack stack) {
        return read(stack).pierce;
    }

    public static double gravityBonus(ItemStack stack) {
        return read(stack).gravity;
    }

    public static double weightBonus(ItemStack stack) {
        return read(stack).weight;
    }

    /**
     * 后坐力按幅值降低：正数减、负数加，避免负值被减成更大。
     */
    public static float reduceRecoil(float value, double amount) {
        if (value >= 0.0F) return Math.max(0.0F, (float) (value - amount));
        return Math.min(0.0F, (float) (value + amount));
    }

    /**
     * 枪械基础伤害（不含附件）。
     */
    public static double baseDamage(ItemStack stack) {
        GunData data = gunData(stack);
        return data == null || data.getBulletData() == null ? 0.0D : data.getBulletData().getDamageAmount();
    }

    public static double meleeDamage(ItemStack stack) {
        TaczData data = read(stack);
        GunData gun = gunData(stack);
        double base = 0.0D;
        if (gun != null && gun.getMeleeData() != null
                && gun.getMeleeData().getDefaultMeleeData() != null) {
            base = gun.getMeleeData().getDefaultMeleeData().getDamage();
        }
        return increase(stack, base + baseDamage(stack), data.damage);
    }

    public static double damageFactor(ItemStack stack) {
        double base = baseDamage(stack);
        if (base <= 0.0D) return 1.0D;
        return meleeDamage(stack) / base;
    }

    /**
     * 在 TaCZ 已算出的容量基础上，叠加吸收值并应用成长。
     */
    public static double ammoBonus(ItemStack stack, int baseCapacity, int magLevel) {
        TaczData data = read(stack);
        if (data.isEmpty()) return 0.0D;
        double accumulated = baseCapacity + data.ammo;
        if (magLevel > 0) accumulated += data.magBonus[Math.min(2, magLevel - 1)];
        double target = ammo(stack, accumulated, 0.0D);
        return Math.max(0.0D, target - baseCapacity);
    }

    /**
     * 子弹重力：越低越强，满成长最多 /2。
     */
    public static float applyGravity(ItemStack stack, float base) {
        TaczData data = read(stack);
        if (data.isEmpty()) return base;
        return (float) decrease(stack, base, data.gravity);
    }

    private static TaczData read(ItemStack stack) {
        TaczData data = new TaczData();
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) return data;
        CompoundTag tag = root.getCompound(ROOT);
        data.damage = tag.getDouble("Damage");
        data.rpm = tag.getDouble("Rpm");
        data.armorIgnore = tag.getDouble("ArmorIgnore");
        data.headshot = tag.getDouble("Headshot");
        data.speed = tag.getDouble("Speed");
        data.pierce = tag.getDouble("Pierce");
        data.gravity = tag.getDouble("Gravity");
        data.weight = tag.getDouble("Weight");
        data.ammo = tag.getDouble("Ammo");
        data.magBonus[0] = tag.getDouble("Mag0");
        data.magBonus[1] = tag.getDouble("Mag1");
        data.magBonus[2] = tag.getDouble("Mag2");
        return data;
    }

    private static void write(ItemStack stack, TaczData data) {
        CompoundTag root = stack.getOrCreateTag();
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Damage", data.damage);
        tag.putDouble("Rpm", data.rpm);
        tag.putDouble("ArmorIgnore", data.armorIgnore);
        tag.putDouble("Headshot", data.headshot);
        tag.putDouble("Speed", data.speed);
        tag.putDouble("Pierce", data.pierce);
        tag.putDouble("Gravity", data.gravity);
        tag.putDouble("Weight", data.weight);
        tag.putDouble("Ammo", data.ammo);
        tag.putDouble("Mag0", data.magBonus[0]);
        tag.putDouble("Mag1", data.magBonus[1]);
        tag.putDouble("Mag2", data.magBonus[2]);
        root.put(ROOT, tag);
        stack.setTag(root);
    }

    private static final class TaczData {
        private double damage;
        private double rpm;
        private double armorIgnore;
        private double headshot;
        private double speed;
        private double pierce;
        private double gravity;
        private double weight;
        private double ammo;
        private final double[] magBonus = new double[3];

        private boolean isEmpty() {
            return Math.abs(damage) < 1.0E-9 && Math.abs(rpm) < 1.0E-9
                    && Math.abs(armorIgnore) < 1.0E-9 && Math.abs(headshot) < 1.0E-9
                    && Math.abs(speed) < 1.0E-9 && Math.abs(pierce) < 1.0E-9
                    && Math.abs(gravity) < 1.0E-9 && Math.abs(weight) < 1.0E-9
                    && Math.abs(ammo) < 1.0E-9
                    && Math.abs(magBonus[0]) < 1.0E-9
                    && Math.abs(magBonus[1]) < 1.0E-9
                    && Math.abs(magBonus[2]) < 1.0E-9;
        }
    }
}
