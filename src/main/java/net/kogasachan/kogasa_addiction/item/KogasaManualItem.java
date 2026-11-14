package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

/**
 * 本模组的帕秋莉手册物品（独立物品，不再借用 {@code patchouli:guide_book}）。
 *
 * <p>右键打开 {@code kogasa_addiction:kogasa_manual}；未安装 Patchouli 时给一条动作栏提示，
 * 物品本身仍然可以合成/持有。
 */
public class KogasaManualItem extends Item {

    /**
     * 主模组自己的手册
     */
    public static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "kogasa_manual");

    /**
     * 兼容附属手册分类的可见性 flag（只有装了附属才打开）
     */
    public static final String COMPAT_ENTRIES_FLAG = "kogasa_addiction:compat_entries";

    public KogasaManualItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (ModList.get().isLoaded("patchouli")) {
                // PatchouliAPI 只在安装了 Patchouli 时才会被解析（类加载是惰性的）
                vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(serverPlayer, BOOK_ID);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.kogasa_addiction.manual.no_patchouli"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy()
                .withStyle(style -> style.withColor(ChatFormatting.GOLD));
    }
}
