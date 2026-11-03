package net.kogasachan.danmaku.entity;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class DanmakuEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KogasaDanmaku.MODID);

    /** 通用弹幕实体：具体弹型与颜色由同步数据决定 */
    public static final RegistryObject<EntityType<DanmakuBulletEntity>> DANMAKU_BULLET =
            ENTITY_TYPES.register("danmaku_bullet", () -> EntityType.Builder
                    .of(DanmakuBulletEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("danmaku_bullet"));

    /** 激光束实体：激光/曲线激光的即时射线 */
    public static final RegistryObject<EntityType<DanmakuBeamEntity>> DANMAKU_BEAM =
            ENTITY_TYPES.register("danmaku_beam", () -> EntityType.Builder
                    .of(DanmakuBeamEntity::new, MobCategory.MISC)
                    .sized(0.2F, 0.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build("danmaku_beam"));

    private DanmakuEntities() {
    }
}
