package net.kogasachan.kogasa_addiction.entity.projectile;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SakuyaKnifeEntity extends ThrownTrident {

    // 只同步渲染真正需要的两个值。
    // 不同步整个 ItemStack：既浪费带宽，又等于把完整物品数据发给客户端；
    // 而 tridentItem 是普通字段（AT 暴露），ClientboundAddEntityPacket 根本不带它，
    // 所以客户端的 tridentItem 永远是空的 —— 这就是附魔光效/渲染信息全丢的根因。
    private static final EntityDataAccessor<Boolean> DATA_FOIL =
            SynchedEntityData.defineId(SakuyaKnifeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Integer> DATA_REINFORCEMENT =
            SynchedEntityData.defineId(SakuyaKnifeEntity.class, EntityDataSerializers.INT);

    /** getPickupItem 的结果缓存；保证同一实体只掷一次损坏骰 */
    private ItemStack pickupCache = ItemStack.EMPTY;
    private boolean pickupResolved = false;

    public SakuyaKnifeEntity(EntityType<? extends SakuyaKnifeEntity> type, Level level) {
        super(type, level);
    }

    public SakuyaKnifeEntity(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.SAKUYA_KNIFE_ENTITY.get(), level);
        this.setOwner(shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());

        ItemStack single = stack.copy();
        single.setCount(1);
        this.tridentItem = single;
        this.setNoGravity(true);

        int loyaltyLevel = stack.getEnchantmentLevel(Enchantments.LOYALTY);
        if (loyaltyLevel > 0) {
            this.entityData.set(ID_LOYALTY, (byte) loyaltyLevel);
        }

        // 渲染所需信息写进 synched data，客户端才拿得到
        this.entityData.set(DATA_FOIL, stack.isEnchanted());
        this.entityData.set(DATA_REINFORCEMENT, KogasaEquipmentData.getReinforcements(stack));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FOIL, false);
        this.entityData.define(DATA_REINFORCEMENT, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("KogasaKnifeFoil", this.entityData.get(DATA_FOIL));
        tag.putInt("KogasaKnifeReinforcement", this.entityData.get(DATA_REINFORCEMENT));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_FOIL, tag.getBoolean("KogasaKnifeFoil"));
        this.entityData.set(DATA_REINFORCEMENT, tag.getInt("KogasaKnifeReinforcement"));
    }

    public boolean hasSyncedFoil() {
        return this.entityData.get(DATA_FOIL);
    }

    public int getSyncedReinforcement() {
        return this.entityData.get(DATA_REINFORCEMENT);
    }

    /**
     * 供渲染使用的飞刀副本。完全由 synched data 拼装，因此客户端也能正确显示附魔光效。
     */
    public ItemStack getRenderStack() {
        ItemStack stack = new ItemStack(ModItems.SAKUYA_KNIFE.get());

        if (hasSyncedFoil()) {
            // 目的仅是让 ItemStack.hasFoil() 返回 true 以渲染出附魔光效。
            // 这是纯粹的客户端渲染栈，不会回写到任何物品。
            EnchantmentHelper.setEnchantments(Map.of(Enchantments.UNBREAKING, 1), stack);
        }
        return stack;
    }

    @Override
    @NotNull
    public ItemStack getPickupItem() {
        // 幂等：原实现每次调用都重掷损坏骰，拾取重试或其它模组读取时会得到不同结果
        if (!pickupResolved) {
            // 拾取只发生在服务端；客户端不给掷骰结果，避免两端分歧
            pickupCache = level().isClientSide
                    ? new ItemStack(ModItems.SAKUYA_KNIFE.get())
                    : rollPickupItem();
            pickupResolved = true;
        }
        return pickupCache.copy();
    }

    /**
     * 真正掷损坏骰的逻辑，只在服务端首次调用时执行一次。
     */
    private ItemStack rollPickupItem() {
        // 获取最开始扔出去的那把飞刀的 ItemStack
        ItemStack originStack = this.tridentItem;

        // 原始物品为空时只能返回一把新的普通飞刀
        if (originStack.isEmpty()) return new ItemStack(ModItems.SAKUYA_KNIFE.get());

        // 计算当前飞刀的强化和成长值
        int re = KogasaEquipmentData.getReinforcements(originStack);
        double growth = KogasaEquipmentData.getGrowth(originStack);

        // 计算损坏概率：基础概率 - (强化次数 + 成长值) * 降低率
        double baseChance = Config.SAKUYA_KNIFE_BASE_BREAK_CHANCE.get();
        double reduction = Config.SAKUYA_KNIFE_BREAK_REDUCTION.get();
        double finalChance = Math.max(0.0, baseChance - (re * reduction) - (growth * reduction));

        // 掷骰子
        if (this.random.nextDouble() < finalChance) {
            // 触发损坏：生成损坏版飞刀，并复制原有 NBT
            ItemStack broken = new ItemStack(ModItems.BROKEN_SAKUYA_KNIFE.get());
            if (originStack.hasTag()) {
                broken.setTag(originStack.getTag().copy());
            }
            broken.setCount(1);
            return broken;
        }

        // 未损坏：返回正常飞刀（保留 NBT）
        ItemStack result = originStack.copy();
        result.setCount(1);
        return result;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        // 基础投掷物伤害、击退、着火等交给父类处理。
        //
        // 强化/成长带来的伤害倍率**不在这里补刀**：父类已经造成过伤害并把 invulnerableTime
        // 置为 20、lastHurt 记为本次伤害，此时再调一次 target.hurt(...)，
        // 会命中 LivingEntity 里 "invulnerableTime > 10 && amount <= lastHurt → return false"
        // 的分支，导致倍率 ≤ 2 时额外伤害恒为 0、> 2 时只补差值。
        // 倍率改由 SakuyaKnifeDamageHandler 在 LivingHurtEvent 中并入同一次结算。
        super.onHitEntity(result);

        if (this.getPierceLevel() <= 0) {
            this.inGround = false;
            this.setNoGravity(false);
            // 给一个很小的向下初速度，避免完全静止悬停一瞬
            this.setDeltaMovement(0, -0.05, 0);
        }
    }
}
