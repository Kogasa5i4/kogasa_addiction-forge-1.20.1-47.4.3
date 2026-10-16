package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirstJoinRewardHandler {

    private static final String NBT_KEY = "KogasaFirstJoinReward";

    /**
     * 用 Supplier 包一层，避免在类加载时调用 RegistryObject.get()
     */
    private static final List<Supplier<Item>> EQUIPMENT_POOL = List.of(
            ModItems.KOGASA_SWORD,
            ModItems.KOGASA_PICKAXE,
            ModItems.KOGASA_AXE,
            ModItems.KOGASA_SHOVEL,
            ModItems.KOGASA_HOE,
            ModItems.KOGASA_HELMET,
            ModItems.KOGASA_CHESTPLATE,
            ModItems.KOGASA_LEGGINGS,
            ModItems.KOGASA_BOOTS,
            ModItems.AYA_BOW,
            ModItems.AYA_CROSSBOW
    );

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.ENABLE_FIRST_JOIN_REWARD.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(NBT_KEY)) return;

        // ===== 随机装备（此时注册表已完成） =====
        Supplier<Item> chosenSupplier =
                EQUIPMENT_POOL.get(player.getRandom().nextInt(EQUIPMENT_POOL.size()));
        Item chosen = chosenSupplier.get();
        ItemStack stack = new ItemStack(chosen);

        // ===== 随机能力 =====
        KogasaAbility[] abilities = KogasaAbility.values();
        KogasaAbility ability = abilities[player.getRandom().nextInt(abilities.length)];
        KogasaAbilityData.addAbility(stack, ability);

        // ===== 发放 =====
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
            ItemEntity drop = new ItemEntity(player.level(),
                    player.getX(), player.getY() + 0.5, player.getZ(), stack);
            drop.setPickUpDelay(0);
            player.level().addFreshEntity(drop);
        }

        // ===== 标记已领取 =====
        data.putBoolean(NBT_KEY, true);

        // ===== 音效 + 提示 =====
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);

        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction.first_join_reward")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                false);

        KogasaAddiction.LOGGER.info("First join reward given to {}: {} with {}",
                player.getName().getString(),
                chosen,
                ability.getId());
    }
}