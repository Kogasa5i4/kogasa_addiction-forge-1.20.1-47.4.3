package net.kogasachan.kogasa_addiction.entity;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.kogasachan.kogasa_addiction.entity.projectile.SakuyaKnifeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(
                    ForgeRegistries.ENTITY_TYPES,
                    KogasaAddiction.MODID
            );

    public static final RegistryObject<EntityType<AyaArrowEntity>> AYA_ARROW =
            ENTITY_TYPES.register("aya_arrow", () ->
                    EntityType.Builder.<AyaArrowEntity>of(
                                    AyaArrowEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("aya_arrow")
            );

    public static final RegistryObject<EntityType<SakuyaKnifeEntity>> SAKUYA_KNIFE_ENTITY =
            ENTITY_TYPES.register("sakuya_knife", () ->
                    EntityType.Builder.<SakuyaKnifeEntity>of(
                                    SakuyaKnifeEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("sakuya_knife")
            );

    private ModEntities() {
    }
}