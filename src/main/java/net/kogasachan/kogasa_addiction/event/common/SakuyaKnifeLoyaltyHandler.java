package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 咲夜飞刀（忠诚）的丢件保护。
 *
 * <p>原版会在这几种情况下把忠诚三叉戟永久弄丢：
 * <ul>
 *   <li>飞进虚空：{@code Entity#checkBelowWorld} 在 {@code minBuildHeight - 64} 以下直接
 *       {@code discard()}，物品不返还；</li>
 *   <li>主人死亡 / 中途变旁观：{@code ThrownTrident#isAcceptibleReturnOwner} 返回 false，
 *       物品被 {@code spawnAtLocation} 掉在原地（虚空里直接蒸发）；</li>
 *   <li>主人换维度：原版三叉戟不会跨维度追随，永远留在旧维度。</li>
 * </ul>
 *
 * <p>这里在三种情况下把飞刀塞回主人背包（背包满则掉在脚下），并移除实体避免重复拾取。
 * 纯事件实现，无 mixin，且只处理本模组飞刀，不影响原版/其它模组的三叉戟。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SakuyaKnifeLoyaltyHandler {

    private SakuyaKnifeLoyaltyHandler() {
    }

    /** 飞刀掉出世界时的回收 */
    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof ThrownTrident trident)) return;
        if (!isLoyaltyKnife(trident)) return;

        // 只在"掉出世界"时接管；区块卸载等正常离场不处理（实体会随区块存档）
        if (trident.getY() >= level.getMinBuildHeight() - 64.0D) return;
        if (!(trident.getOwner() instanceof ServerPlayer owner)) return;

        giveBack(owner, trident.tridentItem.copy());
    }

    /** 主人死亡：先收回所有在外的飞刀，再交给正常的死亡掉落流程处理 */
    @SubscribeEvent
    public static void onOwnerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        returnAllKnives(player, level);
    }

    /** 主人换维度：收回留在旧维度的飞刀 */
    @SubscribeEvent
    public static void onOwnerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var server = player.getServer();
        if (server == null) return;

        ServerLevel from = server.getLevel(event.getFrom());
        if (from != null) {
            returnAllKnives(player, from);
        }
    }

    /** 把这一维度里该玩家射出、尚未收回的忠诚飞刀全部收回 */
    private static void returnAllKnives(ServerPlayer owner, ServerLevel level) {
        // getAllEntities 只遍历已加载实体，不会触发区块加载
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ThrownTrident trident)) continue;
            if (!isLoyaltyKnife(trident)) continue;
            if (trident.getOwner() != owner) continue;

            giveBack(owner, trident.tridentItem.copy());
            trident.discard();
        }
    }

    private static boolean isLoyaltyKnife(ThrownTrident trident) {
        return trident.tridentItem.is(ModItems.SAKUYA_KNIFE.get())
                && EnchantmentHelper.getLoyalty(trident.tridentItem) > 0;
    }

    private static void giveBack(ServerPlayer owner, ItemStack knife) {
        if (knife.isEmpty()) return;
        if (!owner.getInventory().add(knife)) {
            owner.drop(knife, false);
        }
    }
}
