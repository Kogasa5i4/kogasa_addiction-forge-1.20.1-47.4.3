package net.kogasachan.kogasa_addiction.entity.projectile;

import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AyaArrowEntity extends Arrow {

    /**
     * 发射时缓存在投掷物上的原始箭矢物品 NBT。
     * 拾取时直接还原，保证与合成出的药水箭 NBT 完全一致。
     */
    public static final String ITEM_NBT_KEY = "KogasaAyaArrowItem";

    /**
     * Aya 箭矢自带的基础穿透等级
     */
    private static final byte BASE_PIERCE_LEVEL = 1;

    public AyaArrowEntity(EntityType<? extends AyaArrowEntity> entityType, Level level) {
        super(entityType, level);
        this.setPierceLevel(BASE_PIERCE_LEVEL);
    }

    public AyaArrowEntity(Level level, LivingEntity shooter) {
        super(ModEntities.AYA_ARROW.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
        this.setPierceLevel(BASE_PIERCE_LEVEL);
    }

    public AyaArrowEntity(Level level, double x, double y, double z) {
        super(ModEntities.AYA_ARROW.get(), level);
        this.setPos(x, y, z);
        this.setPierceLevel(BASE_PIERCE_LEVEL);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        // 优先还原发射时缓存的原始物品 NBT，避免用实体药水数据重新构造导致 NBT 差异
        ItemStack stored = getStoredArrowItem();
        if (!stored.isEmpty()) return stored;

        // 兼容旧数据/命令生成的实体：没有缓存时按实体药水数据重建
        ItemStack stack = new ItemStack(ModItems.AYA_ARROW.get());

        CompoundTag arrowTag = new CompoundTag();
        this.saveWithoutId(arrowTag);
        if (!arrowTag.contains("Potion", Tag.TAG_STRING)
                && !arrowTag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
            return stack;
        }

        CompoundTag stackTag = stack.getOrCreateTag();
        if (arrowTag.contains("Potion", Tag.TAG_STRING)) {
            stackTag.putString("Potion", arrowTag.getString("Potion"));
        }
        if (arrowTag.contains("CustomPotionEffects", Tag.TAG_LIST)) {
            stackTag.put("CustomPotionEffects",
                    arrowTag.getList("CustomPotionEffects", Tag.TAG_COMPOUND).copy());
        }
        return stack;
    }

    private ItemStack getStoredArrowItem() {
        CompoundTag data = this.getPersistentData();
        if (!data.contains(ITEM_NBT_KEY, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;

        CompoundTag itemTag = data.getCompound(ITEM_NBT_KEY);
        if (itemTag.isEmpty()) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(ModItems.AYA_ARROW.get());
        stack.setTag(itemTag.copy());
        return stack;
    }
}