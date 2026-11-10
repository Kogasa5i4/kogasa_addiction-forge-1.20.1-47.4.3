package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
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

        if (isSpellAmmo(right.getItem())) {
            ItemStack result = left.copy();
            KogasaAbilityData.setSpellAmmo(result, right, requiresTarget(right));
            accept(event, result);
            return true;
        }

        KogasaAbilityData.AmmoType type = yhAmmoType(right);
        if (type == null) return false;

        ItemStack result = left.copy();
        if (type == KogasaAbilityData.AmmoType.YH_SPELL) {
            KogasaAbilityData.setSpellAmmo(result, right, requiresTarget(right));
        } else {
            KogasaAbilityData.setYhAmmo(result, right, type, requiresTarget(right));
        }
        accept(event, result);
        return true;
    }

    /** 妖归弹药分类；非妖归物品或未安装妖归时返回 null。 */
    public static KogasaAbilityData.AmmoType yhAmmoType(ItemStack stack) {
        if (stack.isEmpty() || !ModList.get().isLoaded(YH)) return null;
        String className = stack.getItem().getClass().getName();
        if (className.endsWith("DanmakuItem")) {
            return KogasaAbilityData.AmmoType.YH_DANMAKU;
        }
        if (className.endsWith("LaserItem")) {
            return KogasaAbilityData.AmmoType.YH_LASER;
        }
        if (implementsSpell(stack.getItem())) {
            return KogasaAbilityData.AmmoType.YH_SPELL;
        }
        return null;
    }

    private static boolean isIssScroll(Item item) {
        return item.getClass().getName().equals("io.redspace.ironsspellbooks.item.Scroll")
                || item.getClass().getName().endsWith(".Scroll");
    }

    private static boolean isGoetyFocus(Item item) {
        for (Class<?> c = item.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals("com.Polarice3.Goety.api.items.magic.IFocus")) return true;
            }
        }
        return false;
    }

    private static boolean isArsSpell(Item item) {
        String n = item.getClass().getName();
        return n.equals("com.hollingsworth.arsnouveau.common.items.SpellParchment")
                || n.endsWith(".SpellParchment");
    }

    public static boolean isSpellAmmo(Item item) {
        return isIssScroll(item) || isGoetyFocus(item) || isArsSpell(item);
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
                || type == KogasaAbilityData.AmmoType.YH_SPELL
                || type == KogasaAbilityData.AmmoType.SPELL;
    }

    /** 符卡固定单发；弹幕/激光按弓弩多发数。 */
    public static int shotCount(ItemStack weapon) {
        KogasaAbilityData.AmmoType type = KogasaAbilityData.getAmmoType(weapon);
        return type == KogasaAbilityData.AmmoType.YH_SPELL
                || type == KogasaAbilityData.AmmoType.SPELL
                ? 1
                : KogasaEquipmentHelper.getBowArrowCount(weapon);
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
    public enum FireResult {IMMEDIATE, PROJECTILE, FAIL}

    public static FireResult fireAmmo(ItemStack weapon, ServerPlayer player) {
        KogasaAbilityData.AmmoType type = KogasaAbilityData.getAmmoType(weapon);
        if (type == null) return FireResult.FAIL;
        ItemStack ammo = KogasaAbilityData.getYhAmmo(weapon);
        if (ammo.isEmpty()) return FireResult.FAIL;

        if (type == KogasaAbilityData.AmmoType.YH_SPELL) {
            if (KogasaAbilityData.spellRequiresTarget(weapon)) return FireResult.PROJECTILE;
            return castYhSpell(ammo, player) ? FireResult.IMMEDIATE : FireResult.FAIL;
        }
        if (type == KogasaAbilityData.AmmoType.SPELL) {
            boolean target = KogasaAbilityData.spellRequiresTarget(weapon) || requiresTarget(ammo);
            if (target) return FireResult.PROJECTILE;
            return castSpellAmmo(ammo, player) ? FireResult.IMMEDIATE : FireResult.FAIL;
        }
        if (type != KogasaAbilityData.AmmoType.YH_DANMAKU
                && type != KogasaAbilityData.AmmoType.YH_LASER) {
            return FireResult.FAIL;
        }
        if (KogasaAbilityData.spellRequiresTarget(weapon)) return FireResult.PROJECTILE;

        int count = Math.max(1, shotCount(weapon));
        float spread = 3.0F;
        for (int i = 0; i < count; i++) {
            float yawOffset = count == 1 ? 0.0F : (i - (count - 1) / 2.0F) * spread;
            if (!spawnDanmaku(player, ammo, type, yawOffset)) return FireResult.FAIL;
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.8F);
        return FireResult.IMMEDIATE;
    }

    /** 兼容旧调用：只有立即释放才算 true。 */
    public static boolean fireYh(ItemStack weapon, ServerPlayer player) {
        return fireAmmo(weapon, player) == FireResult.IMMEDIATE;
    }

    public static void markProjectile(AbstractArrow arrow, ItemStack weapon) {
        ItemStack ammo = KogasaAbilityData.getYhAmmo(weapon);
        KogasaAbilityData.AmmoType type = KogasaAbilityData.getAmmoType(weapon);
        if (ammo.isEmpty() || type == null) return;
        CompoundTag data = new CompoundTag();
        data.putString("Type", type.name());
        data.putBoolean("RequiresTarget", KogasaAbilityData.spellRequiresTarget(weapon));
        data.putBoolean("YhSpell", isYhSpellItem(ammo.getItem()));
        data.put("Ammo", ammo.copyWithCount(1).save(new CompoundTag()));
        arrow.getPersistentData().put("KogasaSpellAmmo", data);
    }

    public static boolean castOnImpact(AbstractArrow arrow, HitResult hit, ServerPlayer player) {
        if (!arrow.getPersistentData().contains("KogasaSpellAmmo", 10)) return false;
        CompoundTag payload = arrow.getPersistentData().getCompound("KogasaSpellAmmo");
        ItemStack ammo = ItemStack.of(payload.getCompound("Ammo"));
        KogasaAbilityData.AmmoType type;
        try {
            type = KogasaAbilityData.AmmoType.valueOf(payload.getString("Type"));
        } catch (RuntimeException ignored) {
            type = null;
        }
        if (ammo.isEmpty() || type == null) return false;

        // 需要瞄准的法术打在方块上时没有目标，直接保留普通箭矢作为退化。
        boolean requiresTarget = payload.getBoolean("RequiresTarget");
        boolean yhSpell = payload.getBoolean("YhSpell");
        if (type == KogasaAbilityData.AmmoType.SPELL
                && hit.getType() == HitResult.Type.BLOCK) {
            if (requiresTarget && yhSpell) showTargetRequired(player);
            arrow.getPersistentData().remove("KogasaSpellAmmo");
            return false;
        }

        // YH 的 requireTarget 会优先读 GrazeHelper 的会话目标；命中实体时补一次，
        // 避免玩家瞄准了目标但 serverGetTarget 仍未命中的情况。
        if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity target) {
            try {
                Class<?> graze = Class.forName("dev.xkmc.youkaishomecoming.content.capability.GrazeHelper");
                graze.getMethod("addSession", net.minecraft.world.entity.player.Player.class,
                        net.minecraft.world.entity.LivingEntity.class).invoke(null, player, target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        float oldYaw = player.getYRot();
        float oldPitch = player.getXRot();
        aimAt(player, hit.getLocation());
        boolean ok;
        try {
            if (type == KogasaAbilityData.AmmoType.YH_DANMAKU
                    || type == KogasaAbilityData.AmmoType.YH_LASER) {
                ok = spawnDanmaku(player, ammo, type, 0.0F);
            } else if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                    && entityHit.getEntity() instanceof net.minecraft.world.entity.LivingEntity target
                    && isYhSpellItem(ammo.getItem())) {
                // YH 的普通生物目标不能通过 Graze 校验，直接把命中实体交给 SpellContainer。
                ok = castYhAtTarget(ammo, player, target);
            } else {
                ok = castSpellAmmo(ammo, player);
            }
        } finally {
            player.setYRot(oldYaw);
            player.setXRot(oldPitch);
        }
        if (!ok && requiresTarget && yhSpell) showTargetRequired(player);
        arrow.getPersistentData().remove("KogasaSpellAmmo");
        return ok;
    }

    private static void showSoulInsufficient(ServerPlayer player) {
        // 复用 Goety 本体的提示文案（仪式与聚晶共用同一句“没有足够的灵魂能量”）。
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "info.goety.ritual.noSouls.fail")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    private static void showTargetRequired(ServerPlayer player) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "message.kogasa_addiction.spell.requires_target")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    private static void aimAt(ServerPlayer player, Vec3 target) {
        Vec3 eye = player.getEyePosition();
        Vec3 d = target.subtract(eye);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(-d.x, d.z)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(d.y, horizontal)));
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    private static boolean isYhSpellItem(Item item) {
        for (Class<?> c = item.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals("dev.xkmc.youkaishomecoming.content.item.danmaku.ISpellItem")) return true;
            }
        }
        return item.getClass().getName().endsWith(".SpellItem")
                || item.getClass().getName().endsWith(".CustomSpellItem");
    }

    private static boolean castYhAtTarget(ItemStack ammo, ServerPlayer player,
                                          net.minecraft.world.entity.LivingEntity target) {
        try {
            Object item = ammo.getItem();
            Object supplier = null;
            Class<?> type = item.getClass();
            while (type != null && supplier == null) {
                try {
                    var field = type.getDeclaredField("spell");
                    field.setAccessible(true);
                    supplier = field.get(item);
                } catch (NoSuchFieldException ignored) {
                }
                if (supplier == null) {
                    try {
                        var field = type.getDeclaredField("def");
                        field.setAccessible(true);
                        Object def = field.get(item);
                        supplier = (java.util.function.Supplier<Object>) () -> {
                            try {
                                return def.getClass().getMethod("createInstance").invoke(def);
                            } catch (ReflectiveOperationException e) {
                                throw new RuntimeException(e);
                            }
                        };
                    } catch (NoSuchFieldException ignored) {
                    }
                }
                type = type.getSuperclass();
            }
            if (supplier == null) return false;
            Class<?> container = Class.forName("dev.xkmc.youkaishomecoming.content.spell.item.SpellContainer");
            container.getMethod("castSpell", ServerPlayer.class, java.util.function.Supplier.class,
                    net.minecraft.world.entity.LivingEntity.class)
                    .invoke(null, player, supplier, target);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean castYhSpell(ItemStack ammo, ServerPlayer player) {
        try {
            Method method = ammo.getItem().getClass().getMethod("castSpell",
                    ItemStack.class, net.minecraft.world.entity.player.Player.class,
                    boolean.class, boolean.class);
            Object result = method.invoke(ammo.getItem(), ammo, player, false, false);
            boolean ok = !(result instanceof Boolean b) || b;
            if (!ok) {
                Object forced = castYhIgnoringTarget(ammo, player);
                if (forced != null) {
                    return !(forced instanceof Boolean b) || b;
                }
            }
            return ok;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static Object castYhIgnoringTarget(ItemStack ammo, ServerPlayer player) {
        try {
            Object item = ammo.getItem();
            Class<?> type = item.getClass();
            java.lang.reflect.Field field = null;
            while (type != null && field == null) {
                try {
                    field = type.getDeclaredField("requireTarget");
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                }
            }
            if (field == null) return null;
            field.setAccessible(true);
            boolean old = field.getBoolean(item);
            if (!old) return null;
            field.setBoolean(item, false);
            try {
                var method = item.getClass().getMethod("castSpell", ItemStack.class,
                        net.minecraft.world.entity.player.Player.class, boolean.class, boolean.class);
                return method.invoke(item, ammo, player, false, false);
            } finally {
                field.setBoolean(item, old);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static boolean castSpellAmmo(ItemStack ammo, ServerPlayer player) {
        try {
            if (isIssScroll(ammo.getItem())) return castIss(ammo, player);
            if (isGoetyFocus(ammo.getItem())) return castGoety(ammo, player);
            if (isArsSpell(ammo.getItem())) return castArs(ammo, player);
            return castYhSpell(ammo, player);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean castIss(ItemStack ammo, ServerPlayer player) throws ReflectiveOperationException {
        Class<?> containerType = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
        Object container = containerType.getMethod("get", ItemStack.class).invoke(null, ammo);
        Object data = container.getClass().getMethod("getSpellAtIndex", int.class).invoke(container, 0);
        Object spell = data.getClass().getMethod("getSpell").invoke(data);
        int level = (int) data.getClass().getMethod("getLevel").invoke(data);
        Class<?> sourceType = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
        Object source = Enum.valueOf((Class) sourceType, player.isCreative() ? "SCROLL" : "SPELLBOOK");

        // 必须走 attemptInitiateCast：直接 castSpell 只会进入内部状态，不会真正执行
        // onCast/onServerCastComplete，所以会出现“扣了蓝但没效果”。
        boolean ok = (boolean) spell.getClass().getMethod("attemptInitiateCast",
                ItemStack.class, int.class, Level.class,
                net.minecraft.world.entity.player.Player.class,
                sourceType, boolean.class, String.class)
                .invoke(spell, ammo, level, player.level(), player, source, false, "");
        return ok;
    }

    private static boolean castGoety(ItemStack ammo, ServerPlayer player) throws ReflectiveOperationException {
        Object spell = ammo.getItem().getClass().getMethod("getSpell").invoke(ammo.getItem());
        if (!player.isCreative()) {
            int cost = (int) spell.getClass().getMethod("soulCost",
                    net.minecraft.world.entity.LivingEntity.class, ItemStack.class)
                    .invoke(spell, player, ammo);
            Class<?> se = Class.forName("com.Polarice3.Goety.utils.SEHelper");
            int souls = (int) se.getMethod("getSESouls", net.minecraft.world.entity.player.Player.class)
                    .invoke(null, player);
            if (cost > 0) {
                if (souls < cost) {
                    showSoulInsufficient(player);
                    return false;
                }
                se.getMethod("decreaseSouls", net.minecraft.world.entity.player.Player.class, int.class)
                        .invoke(null, player, cost);
            }
        }
        Object stats = spell.getClass().getMethod("defaultStats").invoke(spell);
        Class<?> statType = Class.forName("com.Polarice3.Goety.common.magic.SpellStat");
        spell.getClass().getMethod("SpellResult", net.minecraft.server.level.ServerLevel.class,
                net.minecraft.world.entity.LivingEntity.class, ItemStack.class, statType)
                .invoke(spell, player.level(), player, ammo, stats);
        return true;
    }

    private static boolean castArs(ItemStack ammo, ServerPlayer player) throws ReflectiveOperationException {
        Object caster = ammo.getItem().getClass().getMethod("getSpellCaster", ItemStack.class)
                .invoke(ammo.getItem(), ammo);
        Object result = caster.getClass().getMethod("castSpell", Level.class,
                net.minecraft.world.entity.LivingEntity.class,
                net.minecraft.world.InteractionHand.class,
                net.minecraft.network.chat.Component.class)
                .invoke(caster, player.level(), player,
                        net.minecraft.world.InteractionHand.MAIN_HAND,
                        net.minecraft.network.chat.Component.empty());
        boolean ok = !(result instanceof net.minecraft.world.InteractionResultHolder<?> h)
                || h.getResult().consumesAction();
        return ok;
    }

    private static final TagKey<Item> REQUIRES_TARGET =
            TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("kogasa_addiction", "requires_target_spell_ammo"));

    public static boolean requiresTargetFor(ItemStack ammo) {
        return requiresTarget(ammo);
    }

    private static boolean requiresTarget(ItemStack ammo) {
        if (ammo.is(REQUIRES_TARGET)) return true;
        Item item = ammo.getItem();
        for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
            try {
                var field = type.getDeclaredField("requireTarget");
                field.setAccessible(true);
                return field.getBoolean(item);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (isGoetyFocus(item)) {
            try {
                Object spell = item.getClass().getMethod("getSpell").invoke(item);
                for (Class<?> i : spell.getClass().getInterfaces()) {
                    if (i.getName().equals("com.Polarice3.Goety.api.magic.ITouchSpell")) return true;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }


}
