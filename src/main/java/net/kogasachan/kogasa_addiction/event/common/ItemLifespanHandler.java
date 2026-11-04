package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 本模组（含兼容附属）的物品掉落后不因时间流逝而消失。
 *
 * <p>原版 {@link ItemEntity} 的 {@code age} 到达 {@code lifespan}（默认 6000 tick / 5 分钟）就会 discard；
 * 抗火的物品虽然不会在熔岩里烧掉，但照样会被这个计时刷掉。这里把所有本模组物品的 lifespan
 * 设为 {@link Integer#MAX_VALUE}，保留正常的翻滚/拾取逻辑，只是不再自然消失。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemLifespanHandler {

    private static final String COMPAT_MODID = "kogasa_addiction_compat";

    private ItemLifespanHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (isModItem(itemEntity.getItem())) {
            itemEntity.lifespan = Integer.MAX_VALUE;
        }
    }

    private static boolean isModItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        return KogasaAddiction.MODID.equals(id.getNamespace())
                || COMPAT_MODID.equals(id.getNamespace());
    }
}
