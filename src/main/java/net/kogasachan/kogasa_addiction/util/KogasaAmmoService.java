package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** 弓弩共用弹药：装载、判定、发射。 */
public final class KogasaAmmoService {

    private static final String YH = "youkaishomecoming";

    private KogasaAmmoService() {
    }

    /** 铁砧装载：药水、烟花、妖归弹幕/激光/符卡。 */
    public static boolean tryLoad(ItemStack left, ItemStack right, AnvilUpdateEvent event) {
        if (right.isEmpty()) return false;

        if (right.getItem() instanceof PotionItem) {
            ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(PotionUtils.getPotion(right));
            if (potionId == null) return false;
            KogasaAbilityData.PotionKind kind = right.getItem() instanceof net.minecraft.world.item.LingeringPotionItem
                    ? KogasaAbilityData.PotionKind.LINGERING
                    : right.getItem() instanceof net.minecraft.world.item.SplashPotionItem
                    ? KogasaAbilityData.PotionKind.SPLASH
                    : KogasaAbilityData.PotionKind.REGULAR;
            ItemStack result = left.copy();
            KogasaAbilityData.setPotionAmmo(result, potionId, kind);
            accept(event, result);
            return true;
        }

        if (right.is(Items.FIREWORK_ROCKET)) {
            CompoundTag nbt = right.getTag();
            if (nbt == null || !nbt.contains("Fireworks")) return false;
            ItemStack result = left.copy();
            KogasaAbilityData.setFireworkAmmo(result, nbt.copy());
            accept(event, result);
            return true;
        }

        if (!ModList.get().isLoaded(YH)) return false;
        String className = right.getItem().getClass().getName();
        KogasaAbilityData.AmmoType type;
        if (className.endsWith("DanmakuItem")) {
            type = KogasaAbilityData.AmmoType.YH_DANMAKU;
        } else if (className.endsWith("LaserItem")) {
            type = KogasaAbilityData.AmmoType.YH_LASER;
        } else if (implementsSpell(right.getItem())) {
            type = KogasaAbilityData.AmmoType.YH_SPELL;
        } else {
            return false;
        }

        ItemStack result = left.copy();
        KogasaAbilityData.setYhAmmo(result, right, type);
        accept(event, result);
        return true;
    }

    private static void accept(AnvilUpdateEvent event, ItemStack result) {
        event.setOutput(result);
        event.setCost(Config.AMMO_INFUSION_COST.get());
        event.setMaterialCost(1);
    }

    public static boolean hasYhAmmo(ItemStack stack) {
        KogasaAbilityData.AmmoType type = KogasaAbilityData.getAmmoType(stack);
        return type == KogasaAbilityData.AmmoType.YH_DANMAKU
                || type == KogasaAbilityData.AmmoType.YH_LASER
                || type == KogasaAbilityData.AmmoType.YH_SPELL;
    }

    /** 符卡固定单发；弹幕/激光按弓弩多发数。 */
    public static int shotCount(ItemStack weapon) {
        return KogasaAbilityData.getAmmoType(weapon) == KogasaAbilityData.AmmoType.YH_SPELL
                ? 1
                : KogasaEquipmentHelper.getBowArrowCount(weapon);
    }

    /** 返回 true 表示已生成妖归投射物/施放符卡，调用方不要再走原版箭矢。 */
    public static boolean fireYh(ItemStack weapon, ServerPlayer player) {
        KogasaAbilityData.AmmoType type = KogasaAbilityData.getAmmoType(weapon);
        if (type == null) return false;
        ItemStack ammo = KogasaAbilityData.getYhAmmo(weapon);
        if (ammo.isEmpty()) return false;

        if (type == KogasaAbilityData.AmmoType.YH_SPELL) {
            return castSpell(ammo, player);
        }
        if (type != KogasaAbilityData.AmmoType.YH_DANMAKU && type != KogasaAbilityData.AmmoType.YH_LASER) {
            return false;
        }

        int count = Math.max(1, shotCount(weapon));
        float spread = 3.0F;
        for (int i = 0; i < count; i++) {
            float yawOffset = count == 1 ? 0.0F : (i - (count - 1) / 2.0F) * spread;
            if (!spawnDanmaku(player, ammo, type, yawOffset)) return false;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
        return true;
    }

    private static boolean castSpell(ItemStack ammo, ServerPlayer player) {
        try {
            Method method = ammo.getItem().getClass().getMethod("castSpell",
                    ItemStack.class, net.minecraft.world.entity.player.Player.class,
                    boolean.class, boolean.class);
            Object result = method.invoke(ammo.getItem(), ammo, player, false, false);
            return !(result instanceof Boolean b) || b;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean spawnDanmaku(ServerPlayer player, ItemStack ammo,
                                        KogasaAbilityData.AmmoType type, float yawOffset) {
        try {
            String path = type == KogasaAbilityData.AmmoType.YH_LASER ? "item_laser" : "item_danmaku";
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(
                    ResourceLocation.fromNamespaceAndPath(YH, path));
            if (entityType == null) return false;

            Class<?> clazz = Class.forName(type == KogasaAbilityData.AmmoType.YH_LASER
                    ? "dev.xkmc.youkaishomecoming.content.entity.danmaku.ItemLaserEntity"
                    : "dev.xkmc.youkaishomecoming.content.entity.danmaku.ItemDanmakuEntity");
            Constructor<?> ctor = clazz.getConstructor(EntityType.class, LivingEntity.class, Level.class);
            Object entity = ctor.newInstance(entityType, player, player.level());
            clazz.getMethod("setItem", ItemStack.class).invoke(entity, ammo.copyWithCount(1));

            float damage = readDamage(ammo.getItem());
            Vec3 direction = lookDirection(player, yawOffset);
            if (type == KogasaAbilityData.AmmoType.YH_LASER) {
                clazz.getMethod("setup", float.class, int.class, float.class, boolean.class, Vec3.class)
                        .invoke(entity, damage, 40, 40.0F, false, direction);
            } else {
                clazz.getMethod("setup", float.class, int.class, boolean.class, boolean.class, Vec3.class)
                        .invoke(entity, damage, 40, false, false, direction);
            }

            Entity spawned = (Entity) entity;
            spawned.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            player.level().addFreshEntity(spawned);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static float readDamage(Item item) {
        try {
            Object bullet = item.getClass().getField("type").get(item);
            Object damage = bullet.getClass().getMethod("damage").invoke(bullet);
            return ((Number) damage).floatValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 4.0F;
        }
    }

    private static Vec3 lookDirection(ServerPlayer player, float yawOffset) {
        float yaw = player.getYRot() + yawOffset;
        float pitch = player.getXRot();
        float fx = -Mth.sin(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
        float fy = -Mth.sin(pitch * ((float) Math.PI / 180F));
        float fz = Mth.cos(yaw * ((float) Math.PI / 180F)) * Mth.cos(pitch * ((float) Math.PI / 180F));
        return new Vec3(fx, fy, fz);
    }

    private static boolean implementsSpell(Item item) {
        for (Class<?> type : item.getClass().getInterfaces()) {
            if ("ISpellItem".equals(type.getSimpleName())) return true;
        }
        // 某些符卡可能通过父类实现，方法存在即可。
        try {
            item.getClass().getMethod("castSpell", ItemStack.class,
                    net.minecraft.world.entity.player.Player.class, boolean.class, boolean.class);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
