package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.client.renderer.AyaArrowRenderer;
import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties();
            registerCrossbowProperties();
        });
    }

    // ==================== 箭 ====================

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.AYA_ARROW.get(),
                AyaArrowRenderer::new
        );
    }

    // ==================== 弓 ====================

    private static void registerBowProperties() {
        // pull：拉弓进度 0.0 ~ 1.0
        ItemProperties.register(ModItems.AYA_BOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    if (entity.getUseItem() != stack) return 0.0F;
                    return (float) (stack.getUseDuration()
                            - entity.getUseItemRemainingTicks()) / 20.0F;
                });

        // pulling：是否正在拉弓（0 或 1）
        ItemProperties.register(ModItems.AYA_BOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "pulling"),
                (stack, level, entity, seed) ->
                        entity != null
                                && entity.isUsingItem()
                                && entity.getUseItem() == stack
                                ? 1.0F : 0.0F);
    }

    // ==================== 弩 ====================

    private static void registerCrossbowProperties() {
        // pull：装填进度 0.0 ~ 1.0
        ItemProperties.register(ModItems.AYA_CROSSBOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    if (CrossbowItem.isCharged(stack)) return 0.0F;
                    return (float) (stack.getUseDuration()
                            - entity.getUseItemRemainingTicks())
                            / (float) CrossbowItem.getChargeDuration(stack);
                });

        // pulling：是否正在装填
        ItemProperties.register(ModItems.AYA_CROSSBOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "pulling"),
                (stack, level, entity, seed) ->
                        entity != null
                                && entity.isUsingItem()
                                && entity.getUseItem() == stack
                                && !CrossbowItem.isCharged(stack)
                                ? 1.0F : 0.0F);

        // charged：是否已装填
        ItemProperties.register(ModItems.AYA_CROSSBOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "charged"),
                (stack, level, entity, seed) ->
                        CrossbowItem.isCharged(stack) ? 1.0F : 0.0F);

        // firework：是否装填了烟花
        ItemProperties.register(ModItems.AYA_CROSSBOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "firework"),
                (stack, level, entity, seed) ->
                        CrossbowItem.isCharged(stack)
                                && CrossbowItem.containsChargedProjectile(stack, Items.FIREWORK_ROCKET)
                                ? 1.0F : 0.0F);
    }
}