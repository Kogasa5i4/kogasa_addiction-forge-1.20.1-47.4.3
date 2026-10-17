package net.kogasachan.kogasa_addiction.entity.projectile;

import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AyaArrowEntity extends Arrow {

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
        return new ItemStack(ModItems.AYA_ARROW.get());
    }
}