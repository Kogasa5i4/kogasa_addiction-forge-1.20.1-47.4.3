package net.kogasachan.kogasa_addiction.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * 战利品函数 {@code kogasa_addiction:locked_equipment}：
 * 把条目替换为一件"封印装备"——随机本模组装备 + 注入 1 个随机能力 +
 * {@code KogasaEquipment.Locked=true}。封印期间能力不生效（见
 * {@link KogasaAbilityData#hasAbility}），成长达到
 * {@code loot.lockedEquipmentUnlockGrowth} 时自动解封。
 *
 * <p>用法（宝箱战利品表条目）：
 * <pre>{@code
 * {
 *   "type": "minecraft:item",
 *   "name": "minecraft:air",
 *   "weight": 2,
 *   "functions": [ { "function": "kogasa_addiction:locked_equipment" } ]
 * }
 * }</pre>
 */
public class KogasaLockedEquipmentFunction implements LootItemFunction {

    public static final DeferredRegister<LootItemFunctionType> LOOT_FUNCTION_TYPES =
            DeferredRegister.create(Registries.LOOT_FUNCTION_TYPE, KogasaAddiction.MODID);

    public static final RegistryObject<LootItemFunctionType> TYPE =
            LOOT_FUNCTION_TYPES.register("locked_equipment",
                    () -> new LootItemFunctionType(new Serializer()));

    /**
     * 可开出的装备池（不含锭/箭/损坏飞刀等非正装）。
     * 只存 RegistryObject：若在静态初始化里调用 {@code get()}，模组构造阶段
     * 物品尚未真正注册，会抛 "Registry Object not present"。这里延迟到
     * {@link #apply}（战利品生成时，注册必然已完成）再解析。
     */
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] EQUIPMENT_POOL = new RegistryObject[]{
            ModItems.KOGASA_SWORD,
            ModItems.KOGASA_PICKAXE,
            ModItems.KOGASA_AXE,
            ModItems.KOGASA_SHOVEL,
            ModItems.KOGASA_HOE,
            ModItems.TENSHI_HELMET,
            ModItems.TENSHI_CHESTPLATE,
            ModItems.TENSHI_LEGGINGS,
            ModItems.TENSHI_BOOTS,
            ModItems.AYA_BOW,
            ModItems.AYA_CROSSBOW,
            ModItems.SAKUYA_KNIFE
    };

    @Override
    public ItemStack apply(ItemStack stack, LootContext context) {
        return createLockedEquipment(context.getRandom());
    }

    /**
     * 生成一件封印装备（随机装备 + 随机能力 + Locked）。
     * 供战利品函数与怪物装备（{@code MobEquipmentHandler}）共用。
     */
    public static ItemStack createLockedEquipment(RandomSource random) {
        Item item = EQUIPMENT_POOL[random.nextInt(EQUIPMENT_POOL.length)].get();
        ItemStack result = new ItemStack(item);

        KogasaAbility ability =
                KogasaAbility.values()[random.nextInt(KogasaAbility.values().length)];
        KogasaAbilityData.forceAddAbility(result, ability);
        KogasaEquipmentData.setLocked(result, true);

        return result;
    }

    @Override
    public @NotNull LootItemFunctionType getType() {
        return TYPE.get();
    }

    @Override
    public @NotNull Set<LootContextParam<?>> getReferencedContextParams() {
        return Collections.emptySet();
    }

    /**
     * JSON 序列化：无参数，反序列化即构造。
     */
    public static class Serializer
            implements net.minecraft.world.level.storage.loot.Serializer<KogasaLockedEquipmentFunction> {

        @Override
        public void serialize(@NotNull JsonObject json, @NotNull KogasaLockedEquipmentFunction function,
                              @NotNull JsonSerializationContext context) {
            // 无参数
        }

        @Override
        public @NotNull KogasaLockedEquipmentFunction deserialize(@NotNull JsonObject json,
                                                                  @NotNull JsonDeserializationContext context) {
            return new KogasaLockedEquipmentFunction();
        }
    }
}
