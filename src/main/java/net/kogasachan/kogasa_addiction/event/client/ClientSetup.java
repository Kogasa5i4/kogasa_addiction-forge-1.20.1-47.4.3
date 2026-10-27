package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.client.renderer.AyaArrowRenderer;
import net.kogasachan.kogasa_addiction.client.renderer.SakuyaKnifeRenderer;
import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.AyaBowItem;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.Set;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    private static final String SPECIAL_KEY = "item.kogasa_addiction.daiyousei_bomb";

    @SubscribeEvent
    public static void onClientSetup$register(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties();
            registerCrossbowProperties();
        });
    }

    @SubscribeEvent
    public static void onClientSetup$totem(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                Items.TOTEM_OF_UNDYING,
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "custom_name_check"),
                (stack, level, entity, seed) -> {
                    if (!stack.hasCustomHoverName()) return 0.0F;

                    Component name = stack.getHoverName();
                    if (name instanceof TranslatableContents tc
                            && SPECIAL_KEY.equals(tc.getKey())) {
                        return 1.0F;
                    } else if (name.getString().equals("大妖精的Bomb")
                            || name.getString().equals("Daiyousei's Bomb")) {
                        return 1.0F;
                    }
                    return 0.0F;
                });
        });
    }

    // ==================== 箭 ====================

    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                ModEntities.AYA_ARROW.get(),
                AyaArrowRenderer::new
        );

        event.registerEntityRenderer(
                ModEntities.SAKUYA_KNIFE_ENTITY.get(),
                SakuyaKnifeRenderer::new);
    }

    // ==================== 弓 ====================

    private static void registerBowProperties() {
        // pull：拉弓进度 0.0 ~ 1.0（跟随成长/强化缩短拉满时间）
        ItemProperties.register(ModItems.AYA_BOW.get(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0F;
                    if (entity.getUseItem() != stack) return 0.0F;
                    int charge = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                    return AyaBowItem.getCustomPowerForTime(stack, charge);
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