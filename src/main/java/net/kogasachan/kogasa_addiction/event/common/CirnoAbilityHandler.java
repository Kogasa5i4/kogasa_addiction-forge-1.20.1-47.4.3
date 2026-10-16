package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CirnoAbilityHandler {

    // ========== 攻击时附加缓慢 ==========
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;

        ItemStack held = player.getMainHandItem();
        if (!isCirnoEquipment(held)) return;

        LivingEntity target = event.getEntity();

        // 99% 对目标施加缓慢 IV
        if (player.getRandom().nextFloat() < Config.CIRNO_SLOW_CHANCE.get()) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    (int) (Config.CIRNO_SLOW_DURATION.get() * 20),
                    Config.CIRNO_SLOW_LEVEL.get() - 1,
                    false, true));
        }

        // 9% 对自己施加缓慢
        if (player.getRandom().nextFloat() < Config.CIRNO_SELF_SLOW_CHANCE.get()) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    (int) (Config.CIRNO_SELF_SLOW_DURATION.get() * 20),
                    Config.CIRNO_SELF_SLOW_LEVEL.get() - 1,
                    false, true));
        }
    }

    // ========== 耐久耗尽时复活 ==========
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!Config.CIRNO_REVIVE_ENABLED.get()) return;

        // 检查主手、副手、4 个盔甲槽
        checkAndRevive(player, player.getMainHandItem());
        checkAndRevive(player, player.getOffhandItem());
        checkAndRevive(player, player.getItemBySlot(EquipmentSlot.HEAD));
        checkAndRevive(player, player.getItemBySlot(EquipmentSlot.CHEST));
        checkAndRevive(player, player.getItemBySlot(EquipmentSlot.LEGS));
        checkAndRevive(player, player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static void checkAndRevive(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;
        if (!KogasaAbilityData.hasAbility(stack, KogasaAbility.CIRNO)) return;

        // 耐久是否接近耗尽（下次损坏就会碎）
        int damage = stack.getDamageValue();
        int max = stack.getMaxDamage();
        if (damage < max - 1) return;

        // 成长值是否足够
        double growth = KogasaEquipmentData.getGrowth(stack);
        double cost = Config.CIRNO_REVIVE_GROWTH_COST.get();
        if (growth < cost) return;

        // 复活：消耗成长值，耐久恢复99%
        KogasaEquipmentData.setGrowth(stack, growth - cost);
        stack.setDamageValue((int) (max * 0.99));
        player.getInventory().setChanged();

        // 音效 + 提示
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction.cirno_revive",
                        String.format("%.2f", cost)).withStyle(net.minecraft.ChatFormatting.AQUA),
                true);
    }

    private static boolean isCirnoEquipment(ItemStack stack) {
        if (!(stack.getItem() instanceof IKogasaEquipment)) return false;
        return KogasaAbilityData.hasAbility(stack, KogasaAbility.CIRNO);
    }
}