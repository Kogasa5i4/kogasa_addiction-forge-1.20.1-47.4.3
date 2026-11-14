package net.kogasachan.kogasa_addiction_compat.mixin.tacz;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import net.kogasachan.kogasa_addiction_compat.compat.tacz.TaczCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 子弹创建后按 Kogasa 吸收值降低 gravity。
 */
@Mixin(EntityKineticBullet.class)
public abstract class EntityKineticBulletMixin {

    @Shadow(remap = false)
    private float gravity;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
            at = @At("TAIL"), remap = false)
    private void kogasa$gravity(Level level, LivingEntity shooter, ItemStack gun,
                                ResourceLocation ammoId, ResourceLocation gunId,
                                boolean isTracer, GunData gunData, BulletData bulletData,
                                CallbackInfo ci) {
        this.gravity = TaczCompat.applyGravity(gun, this.gravity);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/resources/ResourceLocation;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V",
            at = @At("TAIL"), remap = false)
    private void kogasa$gravityWithDisplay(Level level, LivingEntity shooter, ItemStack gun,
                                           ResourceLocation ammoId, ResourceLocation gunId,
                                           ResourceLocation gunDisplayId, boolean isTracer,
                                           GunData gunData, BulletData bulletData,
                                           CallbackInfo ci) {
        this.gravity = TaczCompat.applyGravity(gun, this.gravity);
    }
}
