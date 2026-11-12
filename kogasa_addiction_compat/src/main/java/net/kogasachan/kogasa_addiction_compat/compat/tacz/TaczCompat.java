package net.kogasachan.kogasa_addiction_compat.compat.tacz;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
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
        KogasaEquipmentHelper.registerStatVariantResolver(TaczCompat::variantKey);
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

    /** Anvil 吸收后把右侧枪械基础值的比例累加到左侧枪。 */
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
        return Math.max(0.0D, Math.min(1.0D, KogasaEquipmentData.getGrowth(stack)));
    }

    /** 增加类属性的成长：满成长总量为当前累计值的 2x。 */
    public static double increase(ItemStack stack, double base, double bonus) {
        return (base + bonus) * (1.0D + growth(stack));
    }

    /** 降低类属性的成长：满成长总量为当前累计值的 0.5x。 */
    public static double decrease(ItemStack stack, double base, double bonus) {
        return Math.max(0.0D, (base + bonus) * (1.0D - 0.5D * growth(stack)));
    }

    /** 弹匣：满成长总量为当前累计值的 10x。 */
    public static double ammo(ItemStack stack, double base, double bonus) {
        return Math.max(0.0D, (base + bonus) * (1.0D + 9.0D * growth(stack)));
    }

    public static double rpmBonus(ItemStack stack) { return read(stack).rpm; }
    public static double armorIgnoreBonus(ItemStack stack) { return read(stack).armorIgnore; }
    public static double headshotBonus(ItemStack stack) { return read(stack).headshot; }
    public static double speedBonus(ItemStack stack) { return read(stack).speed; }
    public static double pierceBonus(ItemStack stack) { return read(stack).pierce; }
    public static double gravityBonus(ItemStack stack) { return read(stack).gravity; }
    public static double weightBonus(ItemStack stack) { return read(stack).weight; }

    /** 枪械基础伤害（不含附件）。 */
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

    /** 在 TaCZ 已算出的容量基础上，叠加吸收值并应用成长。 */
    public static double ammoBonus(ItemStack stack, int baseCapacity, int magLevel) {
        TaczData data = read(stack);
        if (data.isEmpty()) return 0.0D;
        double accumulated = baseCapacity + data.ammo;
        if (magLevel > 0) accumulated += data.magBonus[Math.min(2, magLevel - 1)];
        double target = ammo(stack, accumulated, 0.0D);
        return Math.max(0.0D, target - baseCapacity);
    }

    /** 子弹重力：越低越强，满成长最多 /2。 */
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
