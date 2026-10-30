package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SakuyaAbilityHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 必须由玩家攻击触发
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        // 获取主手或副手的武器
        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof IKogasaEquipment)) {
            weapon = player.getOffhandItem();
            if (!(weapon.getItem() instanceof IKogasaEquipment)) return;
        }

        // 检查是否拥有咲夜能力
        if (!KogasaAbilityData.hasAbility(weapon, KogasaAbility.SAKUYA)) return;

        LivingEntity target = event.getEntity();
        if (target == player) return;

        // 计算效果等级：最大生命值 / 20 + 最大生命值 % 3
        int maxHealth = (int) target.getMaxHealth();
        int level = (maxHealth / 20) + (maxHealth % 3);

        // 如果等级为 0，或者超过原版 255 上限，做保护
        if (level <= 0) return;
        int amplifier = Math.min(level - 1, 254);

        // 根据生物类型（亡灵/非亡灵）决定效果
        if (target.getMobType() == MobType.UNDEAD) {
            // 亡灵：瞬间治疗（会扣血）
            target.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, amplifier));
        } else {
            // 非亡灵：瞬间伤害
            target.addEffect(new MobEffectInstance(MobEffects.HARM, 1, amplifier));
        }
    }
}