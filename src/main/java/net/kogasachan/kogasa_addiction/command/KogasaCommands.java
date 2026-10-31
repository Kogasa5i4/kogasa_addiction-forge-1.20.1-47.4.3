package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * {@code /kogasa} 根命令的注册入口。
 *
 * <p>本类只负责"把各命令组的节点挂到根节点上"，具体子命令分散在：
 * <ul>
 *   <li>{@link KogasaInfoCommands} —— 只读查看，**不要求 OP**</li>
 *   <li>{@link KogasaEquipmentCommands} —— 成长 / 强化 / 重置</li>
 *   <li>{@link KogasaAbilityCommands} —— 能力与新闻值</li>
 *   <li>{@link KogasaAmmoCommands} —— 弓弩弹药</li>
 *   <li>{@link KogasaEnchantCommands} —— 附魔与成长附魔</li>
 * </ul>
 *
 * <p><b>权限：</b>原先"权限等级 2"写在根节点 {@code /kogasa} 上，导致连
 * {@code /kogasa info} 这种纯读命令都只有 OP 能用。现在把谓词下放到各命令组的
 * 子命令字面量上，只读命令不再要求权限。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KogasaCommands {

    private KogasaCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kogasa");

        // 只读命令：任何人可用
        KogasaInfoCommands.register(root);

        // 以下都会修改装备 / 玩家数据，统一要求权限等级 2
        KogasaEquipmentCommands.register(root, event.getBuildContext());
        KogasaAbilityCommands.register(root);
        KogasaAmmoCommands.register(root, event.getBuildContext());
        KogasaEnchantCommands.register(root, event.getBuildContext());

        event.getDispatcher().register(root);
    }
}
