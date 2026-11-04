package net.kogasachan.kogasa_addiction.migration;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 把旧的 {@code patchouli:guide_book}（NBT 指向 kogasa_manual 的那本）迁移成本模组的独立手册物品。
 *
 * <p>Patchouli 的 guide_book 物品永远存在，{@code MissingMappingsEvent} 不会触发，
 * 所以改为玩家登录时扫描背包/末影箱做替换。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualItemMigration {

    private static final String BOOK_NBT_KEY = "patchouli:book";
    private static final String BOOK_ID = KogasaAddiction.MODID + ":kogasa_manual";

    private ManualItemMigration() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        migrate(player.getInventory());
        migrate(player.getEnderChestInventory());
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
    }

    private static void migrate(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !isManualBook(stack)) {
                continue;
            }
            ItemStack replacement = new ItemStack(ModItems.KOGASA_MANUAL.get(), stack.getCount());
            container.setItem(slot, replacement);
        }
    }

    private static boolean isManualBook(ItemStack stack) {
        Item guideBook = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath("patchouli", "guide_book"));
        if (guideBook == null || stack.getItem() != guideBook) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && BOOK_ID.equals(tag.getString(BOOK_NBT_KEY));
    }
}
