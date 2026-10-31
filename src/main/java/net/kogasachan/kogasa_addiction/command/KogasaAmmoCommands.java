package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.*;

/** /kogasa setammo|clearammo —— 弓弩弹药命令组。 */
public final class KogasaAmmoCommands {

    private KogasaAmmoCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext buildContext) {
        root.then(Commands.literal("setammo")
                .requires(ADMIN)
                .then(Commands.literal("potion")
                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                .executes(ctx -> setAmmoPotion(ctx,
                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                        KogasaAbilityData.PotionKind.REGULAR))))
                .then(Commands.literal("splash")
                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                .executes(ctx -> setAmmoPotion(ctx,
                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                        KogasaAbilityData.PotionKind.SPLASH))))
                .then(Commands.literal("lingering")
                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                .executes(ctx -> setAmmoPotion(ctx,
                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                        KogasaAbilityData.PotionKind.LINGERING))))
                .then(Commands.literal("firework")
                        .executes(KogasaAmmoCommands::setAmmoFirework)));
        root.then(Commands.literal("clearammo")
                .requires(ADMIN)
                .executes(KogasaAmmoCommands::clearAmmo));
    }

    // ==================== setammo ====================

    private static int setAmmoPotion(CommandContext<CommandSourceStack> ctx,
                                     ResourceLocation potionId,
                                     KogasaAbilityData.PotionKind kind)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }

        // 特殊弹药不占用能力槽位
        KogasaAbilityData.setPotionAmmo(held, potionId, kind);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.potion.success",
                potionId.toString(),
                kind.name().toLowerCase()), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

    private static int setAmmoFirework(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }

        // 从副手取烟花 NBT
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.FIREWORK_ROCKET)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.firework_required"));
            return 0;
        }
        CompoundTag nbt = offhand.getTag();
        if (nbt == null || !nbt.contains("Fireworks")) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.firework_invalid"));
            return 0;
        }

        // 特殊弹药不占用能力槽位
        KogasaAbilityData.setFireworkAmmo(held, nbt.copy());
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.firework.success"), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }


    // ==================== clearammo ====================

    private static int clearAmmo(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!KogasaAbilityData.hasAmmo(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.no_ammo"));
            return 0;
        }

        KogasaAbilityData.clearAmmo(held);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.clearammo.success"), true);
        return 1;
    }

}
