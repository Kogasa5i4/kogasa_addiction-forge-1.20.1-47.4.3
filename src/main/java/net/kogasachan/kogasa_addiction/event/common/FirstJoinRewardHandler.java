package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FirstJoinRewardHandler {

    private static final String NBT_KEY = "KogasaFirstJoinReward";

    private static final List<Supplier<Item>> EQUIPMENT_POOL = List.of(
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
    );

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Config.ENABLE_FIRST_JOIN_REWARD.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(NBT_KEY)) return;

        // ===== 1. 随机装备 =====
        Supplier<Item> chosenSupplier =
                EQUIPMENT_POOL.get(player.getRandom().nextInt(EQUIPMENT_POOL.size()));
        Item chosen = chosenSupplier.get();
        ItemStack stack = new ItemStack(chosen);

        // 随机能力
        KogasaAbility[] abilities = KogasaAbility.values();
        KogasaAbility ability = abilities[player.getRandom().nextInt(abilities.length)];
        KogasaAbilityData.addAbility(stack, ability);

        giveItemToPlayer(player, stack);

        // ===== 2. 发放帕秋莉手册（软依赖） =====
        if (ModList.get().isLoaded("patchouli")) {
            Item guideBookItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book"));
            if (guideBookItem != null) {
                ItemStack bookStack = new ItemStack(guideBookItem);
                CompoundTag bookNbt = new CompoundTag();
                // 指定是哪本书，格式为：命名空间:书ID
                bookNbt.putString("patchouli:book", KogasaAddiction.MODID + ":kogasa_manual");
                bookStack.setTag(bookNbt);

                giveItemToPlayer(player, bookStack);
            }
        }

        // ===== 3. 标记已领取与反馈 =====
        data.putBoolean(NBT_KEY, true);

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

    /**
     * 发放物品，背包满则掉落在玩家脚下
     */
    private static void giveItemToPlayer(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            ItemEntity drop = new ItemEntity(player.level(),
                    player.getX(), player.getY() + 0.5, player.getZ(), stack);
            drop.setPickUpDelay(0);
            player.level().addFreshEntity(drop);
        }
    }
}