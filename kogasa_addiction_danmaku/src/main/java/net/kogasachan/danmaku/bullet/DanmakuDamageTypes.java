package net.kogasachan.danmaku.bullet;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 弹幕伤害类型。
 *
 * <p>数据文件：{@code data/kogasa_addiction_danmaku/damage_type/danmaku.json}，
 * 死亡消息键 {@code death.attack.danmaku}。
 *
 * <p>tag（见 {@code data/minecraft/tags/damage_type/}）参照主模组的 {@code koishi_ranged}：
 * 穿甲 / 穿抗性 / 穿附魔 / 穿药水 / 穿盾 / 始终伤害末影龙；
 * 另外额外挂了 <b>{@code bypasses_cooldown}</b> —— 让高速弹幕每次命中都结算，
 * 不被受击冷却（{@code invulnerableTime}）吃掉伤害。
 * 刻意不挂 {@code bypasses_invulnerability}：避免打到创造模式玩家/无敌实体。
 */
public final class DanmakuDamageTypes {

    public static final ResourceKey<DamageType> DANMAKU = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KogasaDanmaku.MODID, "danmaku"));

    private DanmakuDamageTypes() {
    }

    /** 构建带发射者归属的弹幕伤害源 */
    public static DamageSource danmaku(Entity source, @Nullable Entity owner) {
        var level = source.level();
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(DANMAKU);
        return new DamageSource(holder, source, owner);
    }
}
