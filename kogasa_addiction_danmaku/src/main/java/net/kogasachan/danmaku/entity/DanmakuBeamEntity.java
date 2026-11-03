package net.kogasachan.danmaku.entity;

import net.kogasachan.danmaku.bullet.DanmakuColor;
import net.kogasachan.danmaku.bullet.DanmakuDamageTypes;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 激光束实体：不飞行的"即时射线"攻击。
 *
 * <p>发射时从发射点向飞行方向延伸 {@link DanmakuType#beamLength()} 格：
 * <ul>
 *   <li>{@link DanmakuType#LASER}：直线；</li>
 *   <li>{@link DanmakuType#CURVY_LASER}：沿垂直于飞行方向的上/下轴做余弦式起伏。</li>
 * </ul>
 *
 * <p>路径会被方块遮挡截断；诞生后的第一 tick 对路径上的生物结算一次伤害
 * （每个生物只结算一次），存活 {@link #MAX_LIFE_TICKS} tick 用于渲染淡出。
 */
public class DanmakuBeamEntity extends Entity {

    public static final int MAX_LIFE_TICKS = 12;

    /** 光束半宽（格）：渲染与伤害判定共用，与弹幕尺寸同步加粗 */
    public static final float BEAM_HALF_WIDTH = 0.32F;

    /** 伤害判定额外容差（格），让贴近光束边缘的生物也能被命中 */
    public static final float HIT_PADDING = 0.35F;

    private static final EntityDataAccessor<Byte> DATA_TYPE =
            SynchedEntityData.defineId(DanmakuBeamEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_COLOR =
            SynchedEntityData.defineId(DanmakuBeamEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_YAW =
            SynchedEntityData.defineId(DanmakuBeamEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(DanmakuBeamEntity.class, EntityDataSerializers.FLOAT);

    private static final String TAG_TYPE = "BeamType";
    private static final String TAG_COLOR = "BeamColor";
    private static final String TAG_YAW = "BeamYaw";
    private static final String TAG_PITCH = "BeamPitch";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_LIFE = "Life";

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private Entity ownerCache;

    private int life = MAX_LIFE_TICKS;
    private boolean damageApplied;

    public DanmakuBeamEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public DanmakuBeamEntity init(Entity owner, DanmakuType type, DanmakuColor color, Vec3 direction) {
        this.entityData.set(DATA_TYPE, (byte) type.ordinal());
        this.entityData.set(DATA_COLOR, (byte) color.ordinal());
        this.setDirection(direction);

        this.ownerCache = owner;
        this.ownerUuid = owner == null ? null : owner.getUUID();
        this.life = MAX_LIFE_TICKS;
        this.damageApplied = false;
        return this;
    }

    public void setDirection(Vec3 direction) {
        Vec3 n = direction.normalize();
        if (n.lengthSqr() < 1.0E-8D) {
            n = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.entityData.set(DATA_YAW, (float) Math.toDegrees(Math.atan2(-n.x, n.z)));
        this.entityData.set(DATA_PITCH,
                (float) Math.toDegrees(-Math.asin(Mth.clamp(n.y, -1.0D, 1.0D))));
    }

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

    /** 曲线激光用的"上"方向：与飞行方向垂直 */
    public static Vec3 beamUp(Vec3 dir) {
        Vec3 reference = Math.abs(dir.y) > 0.98D ? new Vec3(0.0D, 0.0D, 1.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 side = reference.cross(dir).normalize();
        return dir.cross(side).normalize();
    }

    /** 曲线激光用的"横向"轴（水平）：曲线沿这个方向起伏 */
    public static Vec3 beamSide(Vec3 dir) {
        return dir.cross(beamUp(dir)).normalize();
    }

    /** 计算射线路径（含曲线起伏与方块遮挡截断），客户端渲染与服务端伤害共用 */
    public List<Vec3> pathPoints() {
        DanmakuType type = danmakuType();
        Vec3 origin = position();
        Vec3 dir = direction();
        Vec3 curveAxis = beamSide(dir);
        float length = type.beamLength();
        float amplitude = type.beamAmplitude();

        int samples = Math.max(8, (int) (length / 0.25F));
        List<Vec3> points = new ArrayList<>(samples + 1);
        points.add(origin);

        Vec3 previous = origin;
        for (int i = 1; i <= samples; i++) {
            double s = length * i / samples;
            Vec3 point = origin.add(dir.scale(s));
            if (amplitude > 0.0F) {
                // 沿水平横向轴做余弦式起伏（横放，而不是上下起伏）
                point = point.add(curveAxis.scale(amplitude * Math.sin(s / length * Math.PI * 2.0D)));
            }

            BlockHitResult hit = level().clip(new ClipContext(
                    previous, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                return points;
            }

            points.add(point);
            previous = point;
        }
        return points;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            return;
        }

        if (--life <= 0) {
            discard();
            return;
        }

        if (!damageApplied) {
            damageApplied = true;
            applyDamage();
        }
    }

    private void applyDamage() {
        List<Vec3> points = pathPoints();
        Set<UUID> hit = new HashSet<>();

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i);
            Vec3 b = points.get(i + 1);
            // 取线段两端与中点做范围查询，覆盖整条射线（判定半径 = 光束半宽 + 容差）
            for (Vec3 sample : List.of(a, a.add(b).scale(0.5D), b)) {
                for (LivingEntity target : level().getEntitiesOfClass(
                        LivingEntity.class,
                        new net.minecraft.world.phys.AABB(sample, sample)
                                .inflate(BEAM_HALF_WIDTH + HIT_PADDING),
                        this::canHit)) {
                    if (hit.add(target.getUUID())) {
                        target.hurt(DanmakuDamageTypes.danmaku(this, getOwner()), danmakuType().damage());
                    }
                }
            }
        }
    }

    private boolean canHit(LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && entity != getOwner();
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_TYPE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        return EntityDimensions.scalable(0.2F, 0.2F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, (byte) DanmakuType.LASER.ordinal());
        this.entityData.define(DATA_COLOR, (byte) DanmakuColor.WHITE.ordinal());
        this.entityData.define(DATA_YAW, 0.0F);
        this.entityData.define(DATA_PITCH, 0.0F);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
        DanmakuType type = DanmakuType.byId(tag.getString(TAG_TYPE));
        this.entityData.set(DATA_TYPE, (byte) (type == null ? DanmakuType.LASER.ordinal() : type.ordinal()));
        DanmakuColor color = DanmakuColor.byId(tag.getString(TAG_COLOR));
        this.entityData.set(DATA_COLOR,
                (byte) (color == null ? DanmakuColor.WHITE.ordinal() : color.ordinal()));
        this.entityData.set(DATA_YAW, tag.getFloat(TAG_YAW));
        this.entityData.set(DATA_PITCH, tag.getFloat(TAG_PITCH));

        this.ownerUuid = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        this.ownerCache = null;
        this.life = tag.getInt(TAG_LIFE);
        this.damageApplied = true; // 读档的射线不再重复结算
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
