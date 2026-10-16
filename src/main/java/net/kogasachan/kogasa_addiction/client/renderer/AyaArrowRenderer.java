package net.kogasachan.kogasa_addiction.client.renderer;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AyaArrowRenderer extends ArrowRenderer<AyaArrowEntity> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "textures/entity/projectiles/aya_arrow.png");

    public AyaArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AyaArrowEntity entity) {
        return TEXTURE;
    }
}