package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiIngotHandler {

    private static final String NBT_KEY = "KoishiGhastKills";

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 1. 只处理恶魂
        if (!(event.getEntity() instanceof Ghast)) return;

        DamageSource source = event.getSource();

        // 2. 击杀者必须是玩家
        Entity attacker = source.getEntity();
        if (!(attacker instanceof ServerPlayer player)) return;

        if (!player.isInvisible()) return;

        // 3. 击杀手段必须是恶魂火球
        // 玩家反弹的火球，owner 会变成玩家，getDirectEntity 返回火球本身
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LargeFireball)) return;

        // 4. 计数 +1
        CompoundTag data = player.getPersistentData();
        int current = data.getInt(NBT_KEY) + 1;
        int required = Config.KOISHI_GHAST_KILLS_REQUIRED.get();

        if (current >= required) {
            current -= required;
            data.putInt(NBT_KEY, current);

            // 发放锭
            ItemStack ingot = new ItemStack(ModItems.KOISHI_INGOT.get());
            if (!player.getInventory().add(ingot)) {
                // 背包满，掉在玩家脚下
                ItemEntity drop = new ItemEntity(
                        player.level(),
                        player.getX(), player.getY(), player.getZ(),
                        ingot);
                player.level().addFreshEntity(drop);
            }

            // 音效 + 提示
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.5F);
            player.displayClientMessage(
                    Component.translatable("message.kogasa_addiction.koishi_ingot.earned")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        } else {
            data.putInt(NBT_KEY, current);
            // 进度提示
            player.displayClientMessage(
                    Component.translatable("message.kogasa_addiction.koishi_ingot.progress",
                                    current, required)
                            .withStyle(ChatFormatting.GRAY),
                    true);
        }
    }
}