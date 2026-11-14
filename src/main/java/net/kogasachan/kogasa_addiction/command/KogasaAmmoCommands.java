package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaAmmoService;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;

import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.ADMIN;
import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.isRangedWeapon;

/**
 * /kogasa setammo|clearammo —— 弓弩弹药命令组。
 */
public final class KogasaAmmoCommands {

    private KogasaAmmoCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext buildContext) {
        root.then(Commands.literal("setammo")
                .requires(ADMIN)
                .then(Commands.literal("potion")
                        .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.REGULAR))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.REGULAR))))
                .then(Commands.literal("splash")
                        .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.SPLASH))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.SPLASH))))
                .then(Commands.literal("lingering")
                        .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.LINGERING))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoPotion(ctx, KogasaAbilityData.PotionKind.LINGERING))))
                .then(Commands.literal("firework")
                        .executes(KogasaAmmoCommands::setAmmoFirework)
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(KogasaAmmoCommands::setAmmoFirework)))
                .then(Commands.literal("danmaku")
                        .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_DANMAKU))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_DANMAKU))))
                .then(Commands.literal("laser")
                        .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_LASER))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_LASER))))
                .then(Commands.literal("spellcard")
                        .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_SPELL))
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(ctx -> setAmmoYh(ctx, KogasaAbilityData.AmmoType.YH_SPELL))))
                .then(Commands.literal("spell")
                        .executes(KogasaAmmoCommands::setAmmoSpell)
                        .then(Commands.argument("item", ItemArgument.item(buildContext))
                                .executes(KogasaAmmoCommands::setAmmoSpell))));
        root.then(Commands.literal("clearammo")
                .requires(ADMIN)
                .executes(KogasaAmmoCommands::clearAmmo));
    }

    // ==================== setammo ====================

    private static int setAmmoPotion(CommandContext<CommandSourceStack> ctx,
                                     KogasaAbilityData.PotionKind requestedKind)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }

        ItemStack input = inputStack(ctx, player);
        if (input.isEmpty() || !(input.getItem() instanceof PotionItem)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.potion_required"));
            return 0;
        }
        ResourceLocation potionId = net.minecraftforge.registries.ForgeRegistries.POTIONS.getKey(
                PotionUtils.getPotion(input));
        if (potionId == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.potion_required"));
            return 0;
        }
        KogasaAbilityData.PotionKind kind = input.getItem() instanceof net.minecraft.world.item.LingeringPotionItem
                ? KogasaAbilityData.PotionKind.LINGERING
                : input.getItem() instanceof net.minecraft.world.item.SplashPotionItem
                ? KogasaAbilityData.PotionKind.SPLASH
                : KogasaAbilityData.PotionKind.REGULAR;
        KogasaAbilityData.setPotionAmmo(held, potionId, kind);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.potion.success",
                potionId.toString(),
                kind.name().toLowerCase()), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

    private static ItemStack inputStack(CommandContext<CommandSourceStack> ctx, ServerPlayer player)
            throws CommandSyntaxException {
        try {
            return ItemArgument.getItem(ctx, "item").createItemStack(1, true);
        } catch (IllegalArgumentException ignored) {
            return player.getOffhandItem().copy();
        }
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


    private static int setAmmoYh(CommandContext<CommandSourceStack> ctx,
                                 KogasaAbilityData.AmmoType expected)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }
        ItemStack ammo = inputStack(ctx, player);
        if (KogasaAmmoService.yhAmmoType(ammo) != expected) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.yh_ammo_required"));
            return 0;
        }
        if (expected == KogasaAbilityData.AmmoType.YH_SPELL) {
            KogasaAbilityData.setSpellAmmo(held, ammo, KogasaAmmoService.requiresTargetFor(ammo));
        } else {
            KogasaAbilityData.setYhAmmo(held, ammo, expected, KogasaAmmoService.requiresTargetFor(ammo));
        }
        player.getInventory().setChanged();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.yh.success", ammo.getHoverName()), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

    private static int setAmmoSpell(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }
        ItemStack offhand = inputStack(ctx, player);
        if (offhand.isEmpty() || !net.kogasachan.kogasa_addiction.util.KogasaAmmoService.isSpellAmmo(offhand.getItem())) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.spell_required"));
            return 0;
        }
        KogasaAbilityData.setSpellAmmo(held, offhand, net.kogasachan.kogasa_addiction.util.KogasaAmmoService.requiresTargetFor(offhand));
        player.getInventory().setChanged();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.spell.success", offhand.getHoverName()), true);
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
