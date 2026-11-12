package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.KogasaAbilityIngotItem;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKind;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKinds;
import net.kogasachan.kogasa_addiction.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaAnvilHandler {

    /** 待写入最终取出物品的 SE（AnvilUpdateEvent 输出可能在取出时被重算）。 */
    private static final java.util.Map<java.util.UUID, java.util.List<String>> PENDING_SE =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static java.util.List<String> consumePendingSe(Player player) {
        if (player == null) return java.util.List.of();
        java.util.List<String> pending = PENDING_SE.remove(player.getUUID());
        return pending == null ? java.util.List.of() : pending;
    }

    private static void rememberPendingSe(Player player, java.util.List<String> effects) {
        if (player == null || effects.isEmpty()) return;
        PENDING_SE.put(player.getUUID(), new java.util.ArrayList<>(effects));
    }


    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!Config.ENABLE_KOGASA_EQUIPMENT.get()) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();


        // 检查左侧是否为咲夜飞刀
        if (left.getItem() instanceof SakuyaKnifeItem) {
            // 右侧材料（附魔书、三叉戟等）带有激流时禁止修复/合并
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(right);
            if (enchants.containsKey(Enchantments.RIPTIDE)) {
                event.setOutput(ItemStack.EMPTY); // 禁止输出
                event.setCost(0);
                return;
            }
        }

        if (left.is(ModItems.BROKEN_SAKUYA_KNIFE.get()) && right.is(ModItems.SAKUYA_INGOT.get())) {
            ItemStack result = new ItemStack(ModItems.SAKUYA_KNIFE.get());
            if (left.hasTag() && left.getTag() != null) {
                result.setTag(left.getTag().copy());
            }
            // 一次最多修复 3 把；超过 3 把的余量由 AnvilMenuMixin 放回左侧输入槽
            result.setCount(Math.min(left.getCount(), 3));

            event.setOutput(result);
            event.setCost(Config.SAKUYA_KNIFE_REPAIR_COST.get());
            event.setMaterialCost(1);
        }

        // 左侧必须是 Kogasa 装备
        if (!net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(left)) return;
        if (right.isEmpty()) return;

        // ========== 分支 1：桶清除（独立模块：水桶清强化 / 奶桶清能力） ==========
        if (right.is(Items.WATER_BUCKET) || right.is(Items.MILK_BUCKET)) {
            handleBucketClearing(event, left, right);
            return;
        }

        // ========== 分支 2：能力锻造（右侧是能力锭） ==========
        if (right.getItem() instanceof KogasaAbilityIngotItem ingotItem) {
            handleAbilityInfusion(event, left, ingotItem.getAbility());
            return;
        }

        // ========== 分支 3：封印（右侧是木棍） ==========
        if (right.is(Items.STICK)) {
            handleSeal(event, left);
            return;
        }

        // ========== 分支 4：弓弩弹药装载 ==========
        if (isRangedWeapon(left)) {
            if (handleAmmoInfusion(event, left, right)) return;
        }

        // 同一种可损坏装备的原版合并优先于自定义强化：
        // 若耐久修复或右侧附魔合并有意义，就让原版铁砧继续计算。
        if (shouldUseVanillaCombination(left, right)) return;

        // ========== 分支 5：强化（右侧是同类型装备） ==========
        handleReinforcement(event, left, right);
    }

    /**
     * 判断是否应交由原版铁砧处理同物品合并。
     * 当左侧需要修复、右侧带有损坏耐久，或右侧带有附魔时，优先保留原版合并行为。
     */
    private static boolean shouldUseVanillaCombination(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        if (left.getItem() != right.getItem()) return false;
        if (!left.isDamageableItem()) return false;

        // 同一注册名但不同变体（如不同 bladeState 的拔刀剑）不是同一件物品，
        // 不能走原版修理/附魔合并；交给下面的强化分支。
        String leftVariant = KogasaEquipmentHelper.getStatVariantKey(left);
        String rightVariant = KogasaEquipmentHelper.getStatVariantKey(right);
        if (leftVariant != null && rightVariant != null && !leftVariant.equals(rightVariant)) {
            return false;
        }

        return left.isDamaged() || right.isDamaged() || right.isEnchanted();
    }

    /**
     * 能力锻造：装备 + 能力锭 → 装备获得能力
     */
    private static void handleAbilityInfusion(AnvilUpdateEvent event,
                                              ItemStack left,
                                              KogasaAbility ability) {
        if (KogasaAbilityData.hasAbility(left, ability)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        int currentCount = KogasaAbilityData.getUsedSlots(left);
        int maxSlots = KogasaAbilityData.getMaxAbilitySlots(left);
        if (currentCount >= maxSlots) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        ItemStack result = left.copy();
        KogasaAbilityData.addAbility(result, ability);

        event.setOutput(result);
        event.setCost(Config.ABILITY_INFUSION_COST.get());
        event.setMaterialCost(1);
    }

    /**
     * 桶清除：装备 + 水桶/奶桶 → 清空对应数据（与强化完全独立的一个分支）。
     *
     * <ul>
     *   <li>水桶：清强化次数 / 已吸收材料 / 吸收值 / 拉弓速度，保留成长与附魔；</li>
     *   <li>奶桶：清能力侧数据（能力列表、新闻值与记录、弹药），保留强化与成长；</li>
     *   <li>取出结果时由 {@link KogasaAnvilTakeHandler} 返还空铁桶。</li>
     * </ul>
     */
    private static void handleBucketClearing(AnvilUpdateEvent event, ItemStack left, ItemStack right) {
        // 对应数据已经是空的：直接阻止，避免白白消耗水桶/奶桶
        boolean hasData;
        if (right.is(Items.WATER_BUCKET)) {
            hasData = KogasaEquipmentData.getReinforcements(left) > 0
                    || !KogasaEquipmentData.getUsedMaterials(left).isEmpty()
                    || KogasaEquipmentData.getAbsorbedValue(left) != 0.0D
                    || KogasaEquipmentData.getDrawSpeedValue(left) != 0.0D;
        } else {
            hasData = KogasaAbilityData.hasData(left);
        }
        if (!hasData) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            event.setMaterialCost(0);
            return;
        }

        ItemStack result = left.copy();

        if (right.is(Items.WATER_BUCKET)) {
            KogasaEquipmentData.clearReinforcementData(result);
        } else {
            KogasaAbilityData.clearAllData(result);
        }

        event.setOutput(result);
        event.setCost(Config.CLEAR_COST.get());
        event.setMaterialCost(1); // 消耗 1 个水桶或奶桶，空桶在取出时返还
    }

    /**
     * 封印：装备 + 木棍 → 属性 -50%、能力暂失效，成长达到阈值后自动解封。
     * 这是"绕过强化解锁成长"的捷径：封印装备无需 3 次强化即可直接积累成长。
     */
    private static void handleSeal(AnvilUpdateEvent event, ItemStack left) {
        if (KogasaEquipmentData.isLocked(left)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        ItemStack result = left.copy();
        KogasaEquipmentData.setLocked(result, true);

        event.setOutput(result);
        event.setCost(Config.SEAL_COST.get());
        event.setMaterialCost(1);
    }

    /**
     * 强化：装备 + 同类型装备 → 装备强化 1 级
     */
    private static void handleReinforcement(AnvilUpdateEvent event,
                                            ItemStack left, ItemStack right) {
        if (left.getItem() == right.getItem()
                && !KogasaCuriosHelper.sharesSlot(left, right)) {
            String leftVariant = KogasaEquipmentHelper.getStatVariantKey(left);
            String rightVariant = KogasaEquipmentHelper.getStatVariantKey(right);
            // 同一注册名但不同变体（如拔刀剑 bladeState）允许互相强化；
            // 同变体仍然禁止自喂。
            if (leftVariant == null || rightVariant == null || leftVariant.equals(rightVariant)) {
                event.setOutput(ItemStack.EMPTY);
                event.setCost(0);
                return;
            }
        }

        int re = KogasaEquipmentData.getReinforcements(left);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(left);

        // ===== 判断材料是否有效 =====
        KogasaEquipmentKind leftKind = KogasaEquipmentKinds.of(left);
        boolean isBowOrCrossbow = leftKind == KogasaEquipmentKind.BOW
                || leftKind == KogasaEquipmentKind.CROSSBOW;
        boolean isArrowMaterial = isBowOrCrossbow && right.is(ItemTags.ARROWS);

        String materialId;
        if (isBowOrCrossbow) {
            // 弓弩：本体或 16 支箭矢
            if (isArrowMaterial) {
                if (right.getCount() < 16) return;
                ResourceLocation arrowId = ForgeRegistries.ITEMS.getKey(right.getItem());
                if (arrowId == null) return;
                materialId = "arrow:" + arrowId;
            } else if (KogasaEquipmentKinds.of(right) == leftKind) {
                // 同类型才能当强化材料：弓不能用弩强化，弩也不能用弓
                ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
                if (rightId == null) return;
                materialId = KogasaEquipmentHelper.getMaterialId(right, rightId.toString());
                if (KogasaEquipmentData.hasUsedMaterial(left, materialId)) {
                    event.setOutput(ItemStack.EMPTY);
                    return;
                }
            } else {
                return;
            }
        } else {
            // 非弓弩：tag 优先，后缀 fallback
            if (!KogasaEquipmentHelper.matchesReinforcementMaterial(left, right)) return;

            ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
            if (rightId == null) return;
            materialId = KogasaEquipmentHelper.getMaterialId(right, rightId.toString());
            if (KogasaEquipmentData.hasUsedMaterial(left, materialId)) {
                event.setOutput(ItemStack.EMPTY);
                return;
            }
        }

        // ===== 强化上限检查 =====
        if (re >= maxRe) {
            event.setOutput(ItemStack.EMPTY);
            return;
        }

        // ===== 应用强化 =====
        ItemStack result = left.copy();
        KogasaEquipmentData.setReinforcements(result, re + 1);
        KogasaEquipmentData.addUsedMaterial(result, materialId);

        applyAbsorption(result, left, right, event.getPlayer());

        event.setOutput(result);
        event.setCost(Config.REINFORCEMENT_COST.get());
        event.setMaterialCost(isArrowMaterial ? 16 : 1);
    }

    /** 把一件材料装备的吸收应用到 result；铁砧与 /kogasa reinforce 共用。 */
    public static double applyAbsorption(ItemStack result, ItemStack left, ItemStack right, Player player) {
        // 两把拔刀剑：先把右侧 SE 转移进输出，再计算普通吸收。
        absorbSlashBladeSpecialEffects(result, right);

        boolean isBowOrCrossbow = isRangedWeapon(left);
        double ratio = KogasaEquipmentHelper.getAbsorbedRatio(right);

        double baseValue;
        double drawValue;
        if (isBowOrCrossbow) {
            baseValue = KogasaEquipmentHelper.getRangedBasePower(left);
            drawValue = KogasaEquipmentHelper.getRangedBaseDrawTicks(left)
                    * Config.RANGED_DRAW_SPEED_ABSORB_RATIO.get();
        } else {
            baseValue = KogasaEquipmentHelper.getEquipmentBaseValue(right);
            drawValue = 0.0D;
        }

        double absorb = baseValue * ratio;
        if (absorb > 0) {
            KogasaEquipmentData.addAbsorbedValue(result, absorb);
        }

        KogasaAttributeAbsorption.Category absorbCategory;
        if (isBowOrCrossbow) {
            absorbCategory = KogasaAttributeAbsorption.Category.RANGED;
        } else if (KogasaEquipmentHelper.getArmorEquipmentSlot(left) != null) {
            absorbCategory = KogasaAttributeAbsorption.Category.ARMOR;
        } else if (left.getItem() instanceof SwordItem
                || left.getItem() instanceof DiggerItem
                || left.getItem() instanceof TridentItem) {
            absorbCategory = KogasaAttributeAbsorption.Category.MELEE;
        } else {
            absorbCategory = KogasaAttributeAbsorption.Category.OTHER;
        }
        KogasaAttributeAbsorption.Result absorbed =
                KogasaAttributeAbsorption.collect(right, absorbCategory, player);
        double materialMultiplier = KogasaEquipmentHelper.getTotalMultiplier(right);
        if (materialMultiplier <= 0.0D) materialMultiplier = 1.0D;

        java.util.Set<String> legacyAttributes = new java.util.HashSet<>(legacyAttributesFor(absorbCategory));
        // 基础攻击已经写入 scalar baseValue；无论左侧是什么分类，都不能再吸一次 attack_damage。
        if (KogasaEquipmentHelper.usesAttackDamageBase(right)) {
            legacyAttributes.add("minecraft:generic.attack_damage");
        }
        if (KogasaEquipmentHelper.isExternalArmor(right)) {
            legacyAttributes.add("minecraft:generic.armor");
        }
        for (var entry : absorbed.additive().entrySet()) {
            String id = entry.getKey().toString();
            if (legacyAttributes.contains(id)) continue;
            double raw = entry.getValue() / materialMultiplier;
            double value = raw * ratio * KogasaAttributeAbsorption.scale(entry.getKey());
            if (value > 0.0D) {
                KogasaEquipmentData.addAbsorbedAttribute(result, entry.getKey(), value);
            }
        }

        for (var entry : absorbed.multipliers()) {
            String id = entry.attribute().toString();
            if (legacyAttributes.contains(id)) continue;
            double value = entry.amount() * ratio * KogasaAttributeAbsorption.scale(entry.attribute());
            if (value > 0.0D) {
                KogasaEquipmentData.addAbsorbedMultiplier(
                        result, entry.uuid(), entry.attribute(), value, entry.operation());
            }
        }

        double drawAbsorb = drawValue * ratio;
        if (drawAbsorb > 0) {
            KogasaEquipmentData.addDrawSpeedValue(result, drawAbsorb);
        }
        KogasaEquipmentHelper.notifyAbsorption(result, right);
        return absorb;
    }

    private static java.util.Set<String> legacyAttributesFor(KogasaAttributeAbsorption.Category category) {
        return switch (category) {
            case MELEE -> java.util.Set.of("minecraft:generic.attack_damage");
            case ARMOR -> java.util.Set.of("minecraft:generic.armor");
            case RANGED, OTHER -> java.util.Set.of();
        };
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean handleAmmoInfusion(AnvilUpdateEvent event,
                                              ItemStack left, ItemStack right) {
        return KogasaAmmoService.tryLoad(left, right, event);
    }

    /**
     * 拔刀剑 SE 专用铁砧操作：左侧兼容、左右都有 bladeState、右侧有 SE 时，
     * 输出直接复制左侧完整 NBT，只把右侧缺失的 SE 合并进来。
     * 这样不再依赖原版/Kogasa 重算出来的 output。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void mergeSlashBladeSeOnAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) return;
        if (!KogasaEquipmentHelper.isKogasaEquipment(left)) return;

        CompoundTag leftRoot = left.getTag();
        CompoundTag rightRoot = right.getTag();
        if (leftRoot == null || rightRoot == null) return;
        if (!leftRoot.contains("bladeState", Tag.TAG_COMPOUND)
                || !rightRoot.contains("bladeState", Tag.TAG_COMPOUND)) return;

        java.util.List<String> rightEffects = readSpecialEffects(
                rightRoot.getCompound("bladeState"));
        if (rightEffects.isEmpty()) return;

        // 优先沿用 Kogasa 强化分支已经生成的输出（强化数/吸收/材料记录都在里面）；
        // 只有输出为空或不是左侧物品时才退回 left.copy。
        ItemStack output = event.getOutput();
        if (output.isEmpty() || output.getItem() != left.getItem()) {
            output = left.copy();
        }
        boolean changed = absorbSlashBladeSpecialEffects(output, right);
        changed |= mergeBladeEnchantments(output, right);
        changed |= mergeBladeCounters(output, right);
        if (!changed) return;

        // 把最终 bladeState 同步给 SlashBlade capability，Tooltip/实际效果读取的是 capability。
        CompoundTag outputTag = output.getTag();
        if (outputTag != null && outputTag.contains("bladeState", Tag.TAG_COMPOUND)) {
            KogasaEquipmentHelper.applyBladeState(output, outputTag.getCompound("bladeState"));
        }

        event.setOutput(output);
        event.setCost(Math.max(1, event.getCost()));
        event.setMaterialCost(Math.max(1, event.getMaterialCost()));
    }

    /** 用原版铁砧的附魔合并规则：同级加一级，否则取较高等级；冲突的附魔跳过。 */
    private static boolean mergeBladeEnchantments(ItemStack output, ItemStack right) {
        Map<Enchantment, Integer> rightEnchants = EnchantmentHelper.getEnchantments(right);
        if (rightEnchants.isEmpty()) return false;

        Map<Enchantment, Integer> merged = new java.util.LinkedHashMap<>(
                EnchantmentHelper.getEnchantments(output));
        boolean changed = false;
        for (var entry : rightEnchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int rightLevel = entry.getValue();
            if (rightLevel <= 0) continue;

            Integer leftLevel = merged.get(enchantment);
            if (leftLevel == null) {
                if (!EnchantmentHelper.isEnchantmentCompatible(merged.keySet(), enchantment)) continue;
                merged.put(enchantment, rightLevel);
                changed = true;
            } else if (rightLevel > leftLevel) {
                merged.put(enchantment, rightLevel);
                changed = true;
            } else if (rightLevel == leftLevel
                    && leftLevel < enchantment.getMaxLevel()) {
                merged.put(enchantment, leftLevel + 1);
                changed = true;
            }
        }
        if (changed) EnchantmentHelper.setEnchantments(merged, output);
        return changed;
    }

    /** 累加拔刀剑的 RepairCounter/refine、killCount、proudSoul。 */
    private static boolean mergeBladeCounters(ItemStack output, ItemStack right) {
        CompoundTag outputRoot = output.getTag();
        CompoundTag rightRoot = right.getTag();
        if (outputRoot == null || rightRoot == null) return false;
        if (!outputRoot.contains("bladeState", Tag.TAG_COMPOUND)
                || !rightRoot.contains("bladeState", Tag.TAG_COMPOUND)) return false;

        CompoundTag outputState = outputRoot.getCompound("bladeState");
        CompoundTag rightState = rightRoot.getCompound("bladeState");
        boolean changed = false;
        for (String key : new String[]{"RepairCounter", "killCount", "proudSoul"}) {
            int rightValue = rightState.getInt(key);
            if (rightValue == 0) continue;
            outputState.putInt(key, outputState.getInt(key) + rightValue);
            changed = true;
        }
        if (changed) {
            outputRoot.put("bladeState", outputState);
            output.setTag(outputRoot);
        }
        return changed;
    }

    /**
     * 模式 1 的绝对黑名单：铁砧不允许产出带黑名单附魔的物品
     * （在其它铁砧逻辑之后执行，直接清空输出）。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockBlacklistedAnvilOutput(AnvilUpdateEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty()) return;

        for (Enchantment enchantment : EnchantmentHelper.getEnchantments(output).keySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
            if (KogasaGrowthHandler.isBlacklisted(id)) {
                event.setOutput(ItemStack.EMPTY);
                event.setCost(0);
                event.setMaterialCost(0);
                return;
            }
        }
    }

    /**
     * 兼容读取 SpecialEffects。正常是 ListTag<StringTag>，但旧数据/附属可能写成
     * 其它 ListTag 元素类型，因此按实际 Tag 解析，而不是只 getList(..., TAG_STRING)。
     */
    private static java.util.List<String> readSpecialEffects(CompoundTag state) {
        java.util.List<String> result = new java.util.ArrayList<>();
        Tag raw = state.get("SpecialEffects");
        if (raw == null) return result;
        if (raw instanceof ListTag list) {
            for (Tag element : list) {
                if (element instanceof StringTag string) {
                    String id = string.getAsString();
                    if (!id.isBlank()) result.add(id);
                } else if (element instanceof CompoundTag compound) {
                    String id = compound.getString("id");
                    if (id.isBlank()) id = compound.getString("Id");
                    if (id.isBlank()) id = compound.getString("effect");
                    if (!id.isBlank()) result.add(id);
                }
            }
        } else if (raw instanceof StringTag string) {
            for (String id : string.getAsString().split("[,\\s]+")) {
                if (!id.isBlank()) result.add(id);
            }
        }
        return result;
    }

    /** 取出时把待转移 SE 写到最终输出（防止 AnvilUpdateEvent 结果被重算覆盖）。 */
    public static boolean applyPendingSe(Player player, ItemStack output) {
        java.util.List<String> pending = consumePendingSe(player);
        if (pending.isEmpty() || output.isEmpty()) return false;
        CompoundTag root = output.getTag();
        if (root == null || !root.contains("bladeState", Tag.TAG_COMPOUND)) return false;
        CompoundTag state = root.getCompound("bladeState");
        java.util.Set<String> existing = new java.util.LinkedHashSet<>(readSpecialEffects(state));
        int added = 0;
        ListTag merged = new ListTag();
        for (String id : existing) merged.add(StringTag.valueOf(id));
        for (String id : pending) {
            if (id.isBlank() || !existing.add(id)) continue;
            merged.add(StringTag.valueOf(id));
            added++;
        }
        if (added == 0) return false;
        state.put("SpecialEffects", merged);
        root.put("bladeState", state);
        output.setTag(root);
        KogasaAddiction.LOGGER.info("[KogasaSE] take-merge output={} added={}", output, added);
        return true;
    }

    /**
     * 把右侧拔刀剑的 SpecialEffects 合并进输出（左侧）并清空右侧。
     * 纯 NBT 判断，不依赖 SlashBlade 类；由 {@link #applyAbsorption} 调用。
     */
    private static boolean absorbSlashBladeSpecialEffects(ItemStack result, ItemStack right) {
        if (result.isEmpty() || right.isEmpty()) return false;
        CompoundTag resultRoot = result.getTag();
        CompoundTag rightRoot = right.getTag();
        if (resultRoot == null || rightRoot == null) return false;
        if (!resultRoot.contains("bladeState", Tag.TAG_COMPOUND)
                || !rightRoot.contains("bladeState", Tag.TAG_COMPOUND)) return false;

        CompoundTag resultState = resultRoot.getCompound("bladeState");
        CompoundTag rightState = rightRoot.getCompound("bladeState");
        java.util.List<String> rightEffects = readSpecialEffects(rightState);
        KogasaAddiction.LOGGER.info("[KogasaSE] helper result={} hasBlade={} rightSE={}",
                result, resultRoot.contains("bladeState", Tag.TAG_COMPOUND), rightEffects);
        if (rightEffects.isEmpty()) {
            KogasaAddiction.LOGGER.info("[KogasaSE] rawSE={} state={}",
                    rightState.get("SpecialEffects"), rightState);
            return false;
        }

        java.util.Set<String> existing = new java.util.LinkedHashSet<>(readSpecialEffects(resultState));
        KogasaAddiction.LOGGER.info("[KogasaSE] helper existing={} right={}", existing, rightEffects);

        ListTag merged = new ListTag();
        for (String id : existing) merged.add(StringTag.valueOf(id));
        int added = 0;
        for (String id : rightEffects) {
            if (id.isBlank() || !existing.add(id)) continue;
            merged.add(StringTag.valueOf(id));
            added++;
        }
        if (added == 0) return false;

        resultState.put("SpecialEffects", merged);
        resultRoot.put("bladeState", resultState);
        result.setTag(resultRoot);
        java.util.List<String> mergedIds = new java.util.ArrayList<>();
        for (int i = 0; i < merged.size(); i++) mergedIds.add(merged.getString(i));
        KogasaEquipmentHelper.applySpecialEffects(result, mergedIds);
        // 不要在 AnvilUpdateEvent 中清右侧 NBT：右侧输入被修改会让铁砧重新计算，
        // 刚合并到输出的 SE 会丢失。右侧数量为 1 时取出后整把消耗，天然消失。
        KogasaAddiction.LOGGER.info("[KogasaSE] merged result={} added={} effects={}", result, added, mergedIds);
        return true;
    }

}
