package net.kogasachan.kogasa_addiction.client.renderer;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.entity.projectile.SakuyaKnifeEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownTridentRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrownTrident;
import org.jetbrains.annotations.NotNull;

public class SakuyaKnifeRenderer extends ArrowRenderer<SakuyaKnifeEntity> {

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "textures/entity/projectiles/sakuya_knife.png");

    public SakuyaKnifeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SakuyaKnifeEntity entity) {
        return TEXTURE;
    }
}