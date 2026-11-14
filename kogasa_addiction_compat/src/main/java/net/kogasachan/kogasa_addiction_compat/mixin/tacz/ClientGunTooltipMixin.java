package net.kogasachan.kogasa_addiction_compat.mixin.tacz;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.util.AttachmentDataUtils;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.kogasachan.kogasa_addiction_compat.compat.tacz.TaczCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 覆盖 TACZ 原生 Tooltip 的静态数值行。
 */
@Mixin(ClientGunTooltip.class)
public abstract class ClientGunTooltipMixin {

    @Final
    @Shadow(remap = false)
    private ItemStack gun;
    @Final
    @Shadow(remap = false)
    private IGun iGun;
    @Shadow(remap = false)
    private net.minecraft.network.chat.MutableComponent damage;
    @Shadow(remap = false)
    private net.minecraft.network.chat.MutableComponent armorIgnore;
    @Shadow(remap = false)
    private net.minecraft.network.chat.MutableComponent headShotMultiplier;
    @Shadow(remap = false)
    private net.minecraft.network.chat.MutableComponent weight;
    @Shadow(remap = false)
    private net.minecraft.network.chat.MutableComponent ammoCountText;

    @Inject(method = "getText", at = @At("TAIL"), remap = false)
    private void kogasa$overrideTooltip(CallbackInfo ci) {
        if (!TaczCompat.isGun(gun)) return;
        if (!TaczCompat.hasData(gun)) return;
        var data = TaczCompat.gunData(gun);
        if (data == null) return;

        damage = Component.translatable("tooltip.tacz.gun.damage")
                .append(Component.literal(KogasaNumberFormatHelper.formatTrimmed(
                        TaczCompat.displayDamage(gun), 2)).withStyle(ChatFormatting.AQUA));
        armorIgnore = Component.translatable("tooltip.tacz.gun.armor_ignore",
                String.format("%.0f%%", TaczCompat.displayArmorIgnore(gun) * 100.0F));
        headShotMultiplier = Component.translatable("tooltip.tacz.gun.head_shot_multiplier",
                String.format("%.0f%%", TaczCompat.displayHeadshot(gun) * 100.0F));
        double movement = -TaczCompat.displayWeight(gun) * 100.0D;
        String movementText = (movement >= 0.0D ? "+" : "-")
                + KogasaNumberFormatHelper.formatTrimmed(Math.abs(movement), 2) + "%";
        weight = Component.translatable("tooltip.tacz.gun.movement_speed",
                movementText).withStyle(
                movement >= 0.0D ? ChatFormatting.BLUE : ChatFormatting.RED);

        int current = iGun.getCurrentAmmoCount(gun);
        int max = AttachmentDataUtils.getAmmoCountWithAttachment(gun, data);
        if (iGun.hasBulletInBarrel(gun)) {
            current++;
            max++;
        }
        ammoCountText = Component.literal(current + "/" + max);
    }
}
