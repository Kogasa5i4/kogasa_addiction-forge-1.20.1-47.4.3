package net.kogasachan.kogasa_addiction.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Curios 可选的轻量封装；未安装 Curios 时所有方法安全返回空/false。 */
public final class KogasaCuriosHelper {

    private KogasaCuriosHelper() {
    }

    public static boolean isCurio(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModList.get().isLoaded("curios")) return false;
        return CuriosApi.getCurio(stack).isPresent();
    }

    /** 两个 Curios 是否至少共享一个槽位类型（法术书只能配法术书，戒指只配戒指）。 */
    public static boolean sharesSlot(ItemStack a, ItemStack b) {
        if (!isCurio(a) || !isCurio(b)) return false;
        Set<String> left = new LinkedHashSet<>(CuriosApi.getItemStackSlots(a, false).keySet());
        if (left.isEmpty()) return false;
        for (String slot : CuriosApi.getItemStackSlots(b, false).keySet()) {
            if (left.contains(slot)) return true;
        }
        return false;
    }

    /** 遍历玩家当前装备的 Curios（不含 cosmetic），用于成长触发。 */
    public static void forEachEquipped(LivingEntity entity, Consumer<ItemStack> consumer) {
        if (entity == null || !ModList.get().isLoaded("curios")) return;
        CuriosApi.getCuriosInventory(entity).ifPresent(handler -> {
            IItemHandlerModifiable equipped = handler.getEquippedCurios();
            for (int i = 0; i < equipped.getSlots(); i++) {
                ItemStack stack = equipped.getStackInSlot(i);
                if (!stack.isEmpty()) consumer.accept(stack);
            }
        });
    }
}
