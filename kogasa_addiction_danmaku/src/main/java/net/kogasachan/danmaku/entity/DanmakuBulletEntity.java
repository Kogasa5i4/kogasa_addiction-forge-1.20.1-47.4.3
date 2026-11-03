package net.kogasachan.danmaku.entity;

import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuDamageTypes;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 通用弹幕实体（无重力，但**保留方块碰撞**）。
 *
 * <p>只注册这一个实体类型，具体弹型 / 颜色 / 朝向通过同步数据区分：
 * <ul>
 *   <li>{@link #DATA_TYPE}：{@link DanmakuType} 序号；</li>
 *   <li>{@link #DATA_COLOR}：{@link DanmakuColor} 序号；</li>
 *   <li>{@link #DATA_YAW} / {@link #DATA_PITCH}：飞行朝向（渲染时让弹幕长轴平行于发射方向）。</li>
 * </ul>
 *
 * <p>碰撞用原版 {@link Entity#move(MoverType, Vec3)} 实现（{@code noPhysics} 保持 false），
 * 撞到方块/实体就结算并消失，不会再穿过墙壁。
 */
public class DanmakuBulletEntity extends Entity {

    private static final EntityDataAccessor<Byte> DATA_TYPE =
            SynchedEntityData.defineId(DanmakuBulletEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_COLOR =
            SynchedEntityData.defineId(DanmakuBulletEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(DanmakuBulletEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(DanmakuBulletEntity.class, EntityDataSerializers.FLOAT);

    private static final String TAG_TYPE = "DanmakuType";
    private static final String TAG_COLOR = "DanmakuColor";
    private static final String TAG_YAW = "DanmakuYaw";
    private static final String TAG_PITCH = "DanmakuPitch";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_LIFE = "Life";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Entity ownerCache;

    private int life = 100;

    public DanmakuBulletEntity(EntityType<?> type, Level level) {
        super(type, level);
        // 无重力，但保留方块碰撞（noPhysics 保持 false，交给 move() 处理）
        this.setNoGravity(true);
    }

    /** 生成时初始化：弹型 / 颜色 / 朝向 / 归属 / 寿命（速度由调用方 setDeltaMovement 指定） */
    public DanmakuBulletEntity init(Entity owner, DanmakuType type, DanmakuColor color, Vec3 direction) {
        this.entityData.set(DATA_TYPE, (byte) type.ordinal());
        this.entityData.set(DATA_COLOR, (byte) color.ordinal());
        this.setDirection(direction);

        this.ownerCache = owner;
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.life = type.life();
        refreshDimensions();
        return this;
    }

    /** 记录飞行朝向（MC 的 yaw/pitch 约定：yaw=0 指向 +Z，pitch 向下为正） */
    public void setDirection(Vec3 direction) {
        Vec3 n = direction.normalize();
        if (n.lengthSqr() < 1.0E-8D) {
            n = new Vec3(0.0D, 0.0D, 1.0D);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-n.x, n.z));
        float pitch = (float) Math.toDegrees(-Math.asin(Mth.clamp(n.y, -1.0D, 1.0D)));
        this.entityData.set(DATA_YAW, yaw);
        this.entityData.set(DATA_PITCH, pitch);
    }

    /** 飞行朝向单位向量（客户端渲染用） */
    public Vec3 direction() {
        double yaw = Math.toRadians(this.entityData.get(DATA_YAW));
        double pitch = Math.toRadians(this.entityData.get(DATA_PITCH));
        double cosPitch = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
    }

    public DanmakuType danmakuType() {
        return DanmakuType.byIndex(this.entityData.get(DATA_TYPE));
    }

    public DanmakuColor danmakuColor() {
        return DanmakuColor.byIndex(this.entityData.get(DATA_COLOR));
    }

    @Nullable
    public Entity getOwner() {
        if (ownerCache == null && ownerUuid != null && level() instanceof ServerLevel serverLevel) {
            ownerCache = serverLevel.getEntity(ownerUuid);
        }
        return ownerCache;
    }

    @Override
    public void tick() {
        super.tick();

        // 客户端只等位置同步；运动与伤害都在服务端结算
        if (level().isClientSide) {
            return;
        }

        if (--life <= 0) {
            discard();
            return;
        }

        // 交给原版移动：会正确处理方块碰撞（不再穿墙）
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.horizontalCollision || this.verticalCollision) {
            impact(position());
            return;
        }

        List<LivingEntity> targets = level().getEntitiesOfClass(
                LivingEntity.class, getBoundingBox().inflate(0.05D), this::canHit);
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0);
            target.hurt(damageSource(), danmakuType().damage());
            impact(target.getEyePosition());
        }
    }

    private boolean canHit(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && entity != getOwner();
    }

    private DamageSource damageSource() {
        // 自定义伤害类型：带发射者归属、穿甲/穿抗性，并靠 bypasses_cooldown 忽略受击冷却
        return DanmakuDamageTypes.danmaku(this, getOwner());
    }

    private void impact(Vec3 at) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    at.x, at.y, at.z, 3, 0.05D, 0.05D, 0.05D, 0.01D);
        }
        discard();
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_TYPE.equals(key) || DATA_COLOR.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        DanmakuType type = danmakuType();
        return EntityDimensions.scalable(Math.max(0.05F, type.width()), Math.max(0.05F, type.height()));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, (byte) DanmakuType.PELLET.ordinal());
        this.entityData.define(DATA_COLOR, (byte) DanmakuColor.WHITE.ordinal());
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        String typeId = tag.getString(TAG_TYPE);
        DanmakuType type = DanmakuType.byId(typeId);
        this.entityData.set(DATA_TYPE, (byte) (type == null ? DanmakuType.PELLET.ordinal() : type.ordinal()));

        String colorId = tag.getString(TAG_COLOR);
        DanmakuColor color = DanmakuColor.byId(colorId);
        this.entityData.set(DATA_COLOR,
                (byte) (color == null ? DanmakuColor.WHITE.ordinal() : color.ordinal()));

        this.entityData.set(DATA_YAW, tag.getFloat(TAG_YAW));
        this.entityData.set(DATA_PITCH, tag.getFloat(TAG_PITCH));

        this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        this.ownerCache = null;
        this.life = tag.getInt(TAG_LIFE);
        refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
        tag.putString(TAG_TYPE, danmakuType().id());
        tag.putString(TAG_COLOR, danmakuColor().id());
        tag.putFloat(TAG_YAW, this.entityData.get(DATA_YAW));
        tag.putFloat(TAG_PITCH, this.entityData.get(DATA_PITCH));
        if (ownerUuid != null) {
            tag.putUUID(TAG_OWNER, ownerUuid);
        }
        tag.putInt(TAG_LIFE, life);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
