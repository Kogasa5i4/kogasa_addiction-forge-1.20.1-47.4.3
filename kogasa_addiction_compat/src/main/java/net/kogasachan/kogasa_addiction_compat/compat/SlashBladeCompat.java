package net.kogasachan.kogasa_addiction_compat.compat;

import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.slashblade.SlashBladeDefinition;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

/**
 * SlashBlade: Resharped 兼容。
 *
 * <p>拔刀剑大多数只注册一个 {@code slashblade:slashblade}，真正区别在
 * {@code bladeState} 的 translationKey / ModelName / TextureName / baseAttackModifier。
 * 这里把 bladeState 作为变体身份和实际攻击力来源，避免所有刀都被当成“无铭刀”。
 */
public final class SlashBladeCompat {

    private static final String BLADE_STATE = "bladeState";

    private SlashBladeCompat() {
    }

    public static void register() {
        KogasaEquipmentHelper.registerForceActualStatPredicate(SlashBladeCompat::isSlashBlade);
        KogasaEquipmentHelper.registerStatVariantResolver(stack ->
                isSlashBlade(stack) ? variantKey(stack) : null);
        KogasaEquipmentHelper.registerMaterialStackExpander(SlashBladeCompat::materialStacks);
        KogasaEquipmentHelper.registerSpecialEffectApplier(SlashBladeCompat::applySpecialEffects);
        KogasaEquipmentHelper.registerBladeStateApplier(SlashBladeCompat::applyBladeState);
    }

    public static boolean isSlashBlade(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemSlashBlade) return true;
        CompoundTag root = stack.getTag();
        return root != null && root.contains(BLADE_STATE, Tag.TAG_COMPOUND);
    }

    /**
     * 把完整 bladeState 数值同步到 SlashBlade capability。
     */
    public static void applyBladeState(ItemStack stack, CompoundTag bladeState) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) return;
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            state.setRefine(bladeState.getInt("RepairCounter"));
            state.setKillCount(bladeState.getInt("killCount"));
            state.setProudSoulCount(bladeState.getInt("proudSoul"));
            ListTag list = new ListTag();
            net.minecraft.nbt.Tag raw = bladeState.get("SpecialEffects");
            if (raw instanceof ListTag effects) {
                for (Tag e : effects) {
                    if (e instanceof StringTag s) list.add(StringTag.valueOf(s.getAsString()));
                }
            }
            state.setSpecialEffects(list);
        });
    }

    /**
     * 把 SE 同步到 SlashBlade capability，Tooltip/实际效果读取的是 capability 而不是裸 NBT。
     */
    public static void applySpecialEffects(ItemStack stack, java.util.List<String> effects) {
        if (!(stack.getItem() instanceof ItemSlashBlade)) return;
        stack.getCapability(ItemSlashBlade.BLADESTATE).ifPresent(state -> {
            ListTag list = new ListTag();
            for (String raw : effects) {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id == null) continue;
                list.add(StringTag.valueOf(id.toString()));
            }
            state.setSpecialEffects(list);
        });
    }

    /**
     * 同一注册名下的稳定变体身份；优先翻译键，其次模型/贴图，最后 bladeId。
     */
    public static String variantKey(ItemStack stack) {
        if (!isSlashBlade(stack)) return null;

        CompoundTag root = stack.getTag();
        CompoundTag state = root == null ? new CompoundTag() : root.getCompound(BLADE_STATE);
        String translationKey = state.getString("translationKey");
        if (!translationKey.isBlank()) return translationKey;

        // 没有 translationKey 的旧刀/无铭刀：ItemSlashBlade#getDescriptionId
        // 已经会按 bladeState 返回可翻译的 description id，优先用它。
        String descriptionId = stack.getDescriptionId();
        if (descriptionId.startsWith("item.") || descriptionId.startsWith("block.")
                || descriptionId.startsWith("entity.")) {
            return descriptionId;
        }

        String model = state.getString("ModelName");
        String texture = state.getString("TextureName");
        if (!model.isBlank() || !texture.isBlank()) {
            return model + "|" + texture;
        }

        if (stack.getItem() instanceof ItemSlashBlade blade) {
            ResourceLocation bladeId = blade.getBladeId(stack);
            if (bladeId != null) return bladeId.toString();
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return itemId == null ? null : itemId.toString();
    }

    /**
     * 把 SlashBlade 数据包定义展开成带 NBT 的 ItemStack，供 JEI 显示真实刀名。
     * 只从客户端当前 level 的 registry 读取；拿不到 registry 时返回空，让主模组用默认堆叠。
     */
    public static List<ItemStack> materialStacks(ItemStack base) {
        if (!isSlashBlade(base) || FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.CLIENT) {
            return List.of();
        }
        if (!ModList.get().isLoaded("slashblade")) return List.of();

        Level level = Minecraft.getInstance().level;
        if (level == null && ServerLifecycleHooks.getCurrentServer() != null) {
            level = ServerLifecycleHooks.getCurrentServer().overworld();
        }
        if (level == null) return List.of();

        try {
            Registry<SlashBladeDefinition> registry =
                    SlashBlade.getSlashBladeDefinitionRegistry(level);
            List<ItemStack> result = new ArrayList<>();
            for (SlashBladeDefinition definition : registry) {
                ItemStack blade = definition.getBlade();
                if (!blade.isEmpty() && isSlashBlade(blade)) {
                    result.add(blade);
                }
            }
            return result;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

}
