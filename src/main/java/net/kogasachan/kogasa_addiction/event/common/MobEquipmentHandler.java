package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.loot.KogasaLockedEquipmentFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 怪物概率持有本模组装备。
 *
 * <p>注入点选 {@code EntityJoinLevelEvent} 而不是 FinalizeSpawn：僵尸等子类会在
 * super.finalizeSpawn（Forge 事件触发点）之后才执行 {@code populateDefaultEquipmentSlots}，
 * 事件里塞的装备可能被原版填充覆盖。进世界事件发生在全部填充之后，覆盖即生效。
 *
 * <p>属性链路天然支持（伤害/攻速/护甲都从 ItemStack NBT 计算），能力特效全部挂在
 * 玩家事件上、怪物持有不会触发；成长也只对玩家结算——怪物拿装备纯数值、零风险。
 *
 * <p>概率抽中"封印装备"时从全装备池随机（可穿可持），掉落即玩家可捡的封印装备，
 * 与战利品循环打通。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobEquipmentHandler {

    private MobEquipmentHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!Config.MOB_EQUIPMENT_ENABLED.get()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(mob instanceof Zombie || mob instanceof Skeleton
                || mob instanceof Pillager || mob instanceof Vindicator)) return;
        if (mob.getRandom().nextFloat() >= Config.MOB_EQUIPMENT_CHANCE.get()) return;

        RandomSource random = mob.getRandom();

        ItemStack equipment;
        if (random.nextFloat() < Config.MOB_EQUIPMENT_SEALED_CHANCE.get()) {
            // 封印装备：全池随机（武器/工具/护甲/弓弩/飞刀都可能）
            equipment = KogasaLockedEquipmentFunction.createLockedEquipment(random);
        } else {
            Item item;
            if (mob instanceof Zombie) {
                // 僵尸系（含尸壳/溺尸）拿小伞剑或铲
                item = random.nextBoolean()
                        ? ModItems.KOGASA_SWORD.get()
                        : ModItems.KOGASA_SHOVEL.get();
            } else if (mob instanceof Skeleton) {
                // 骷髅拿文文弓
                item = ModItems.AYA_BOW.get();
            } else if (mob instanceof Pillager) {
                // 掠夺者拿文文弩
                item = ModItems.AYA_CROSSBOW.get();
            } else if (mob instanceof Vindicator) {
                // 卫道士拿小伞斧
                item = ModItems.KOGASA_AXE.get();
            } else {
                item = null;
            }
            equipment = item == null ? ItemStack.EMPTY : new ItemStack(item);
        }

        if (equipment.isEmpty()) return;

        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(equipment);
        mob.setItemSlot(slot, equipment);
        mob.setDropChance(slot, Config.MOB_EQUIPMENT_DROP_CHANCE.get().floatValue());
    }
}
