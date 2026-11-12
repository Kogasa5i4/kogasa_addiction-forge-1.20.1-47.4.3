package net.kogasachan.kogasa_addiction_compat.compat.tacz;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.event.common.AttachmentPropertyEvent;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.LinkedList;
import java.util.UUID;

/** TaCZ 运行时数值叠加：附件缓存 + 近战攻击属性。 */
public final class TaczEventHandler {

    private static final UUID MELEE_UUID = UUID.fromString("7b3e8a5e-6f3f-4d8d-9f95-6d5b1d9c2a71");

    private TaczEventHandler() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new TaczEventHandler());
    }

    @SubscribeEvent
    public static void onAttachmentProperty(AttachmentPropertyEvent event) {
        ItemStack stack = event.getGunItem();
        if (!TaczCompat.isGun(stack) || !KogasaCompat.isCompatible(stack)) return;

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
    }

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
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
