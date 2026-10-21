package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AyaAbilityHandler {

    // ========== 首次击杀生物 ==========
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        ServerPlayer player = null;

        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer sp) {
            player = sp;
        } else if (event.getSource().getDirectEntity() instanceof Projectile proj
                && proj.getOwner() instanceof ServerPlayer owner) {
            player = owner;
        }

        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!KogasaAbilityHelper.isEquipmentWithAbility(held, KogasaAbility.AYA)) return;

        ResourceLocation mobId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (mobId == null) return;

        if (KogasaAbilityData.tryAddKilledMob(held, mobId.toString())) {
            onNewRecord(player, held, 0.1);
        }
    }

    // ========== 首次挖掘方块 ==========
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        ItemStack held = player.getMainHandItem();
        if (!KogasaAbilityHelper.isEquipmentWithAbility(held, KogasaAbility.AYA)) return;

        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (blockId == null) return;

        if (KogasaAbilityData.tryAddMinedBlock(held, blockId.toString())) {
            onNewRecord(serverPlayer, held, 1);
        }
    }

    // ========== 首次受到某种伤害 ==========
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack held = player.getMainHandItem();
        if (!KogasaAbilityHelper.isEquipmentWithAbility(held, KogasaAbility.AYA)) return;

        // 获取伤害类型的注册名
        ResourceKey<DamageType> damageKey = event.getSource()
                .typeHolder()
                .unwrapKey()
                .orElse(null);
        if (damageKey == null) return;

        String damageId = damageKey.location().toString();

        if (KogasaAbilityData.tryAddDamageTaken(held, damageId)) {
            onNewRecord(player, held, 1);
        }
    }

    // ========== 首次获得记录 ==========
    private static void onNewRecord(ServerPlayer player, ItemStack held, double multiplier) {
        KogasaAbilityData.addNewsValue(held, Config.NEWS_VALUE_PER_RECORD.get() * multiplier);
        player.getInventory().setChanged();
        KogasaAdvancementHelper.checkAndGrant(player, held);
    }
}