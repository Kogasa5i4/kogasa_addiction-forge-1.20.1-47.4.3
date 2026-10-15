package net.kogasachan.kogasa_addiction.client.renderer;

import net.kogasachan.kogasa_addiction.entity.projectile.AyaArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AyaArrowRenderer extends ArrowRenderer<AyaArrowEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "minecraft",
                    "textures/entity/projectiles/arrow.png"
            );

    public AyaArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AyaArrowEntity entity) {
        return TEXTURE;
    }
}