package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaDisplayHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Predicate;

/**
 * 命令层公共设施：权限谓词、补全建议、以及被 19 处命令方法逐字重复过的前置校验。
 *
 * <p>拆分的意义不只是"文件变小"：原来每个子命令都要自己写一遍
 * "取玩家 → 取主手 → 判断是不是本模组装备 → 不是就发失败消息并 return 0"，
 * 19 份拷贝一字不差。现在统一走 {@link #requireKogasaEquipment(CommandContext)}，
 * 改一处即全局生效。
 */
public final class KogasaCommandSupport {

    /**
     * 会修改装备/玩家数据的命令统一要求权限等级 2。
     *
     * <p>只读命令（{@code /kogasa info}）不挂这个谓词，普通玩家即可使用。
     */
    public static final Predicate<CommandSourceStack> ADMIN = source -> source.hasPermission(2);

    /** 能力 id 补全 */
    public static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (ctx, builder) -> {
                for (KogasaAbility ability : KogasaAbility.values()) {
                    builder.suggest(ability.getId());
                }
                return builder.buildFuture();
            };

    /** 主手装备上"已有附魔"的 id 补全 */
    public static final SuggestionProvider<CommandSourceStack> EXISTING_ENCHANT_SUGGESTIONS =
            (ctx, builder) -> {
                try {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ItemStack held = player.getMainHandItem();
                    for (Enchantment ench : EnchantmentHelper.getEnchantments(held).keySet()) {
                        ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
                        if (id != null) builder.suggest(id.toString());
                    }
                } catch (CommandSyntaxException ignored) {
                    // 非玩家执行时不给建议
                }
                return builder.buildFuture();
            };

    private KogasaCommandSupport() {
    }

    /**
     * 统一的"主手必须是本模组装备"前置校验。
     *
     * <p>不满足时已经发好了 {@code error.not_kogasa} 失败消息，调用方直接
     * {@code if (held.isEmpty()) return 0;} 即可 —— 与原实现的行为完全一致。
     *
     * @return 主手上的装备；不是本模组装备时返回 {@link ItemStack#EMPTY}
     */
    public static ItemStack requireKogasaEquipment(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ItemStack held = ctx.getSource().getPlayerOrException().getMainHandItem();
        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return ItemStack.EMPTY;
        }
        return held;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    public static Component getMaterialDisplayName(String id) {
        return KogasaDisplayHelper.getItemDisplayName(id);
    }
}
