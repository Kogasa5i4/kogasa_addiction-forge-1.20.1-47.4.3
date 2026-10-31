package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.event.common.TimestopHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 时停期间**真正冻结**实体。
 *
 * <p>旧实现只对非生物实体每 tick 做 {@code setDeltaMovement(ZERO)} + {@code setNoGravity(true)} +
 * {@code setOldPosAndRot()}，但它们的 {@code tick()} 仍在跑：箭矢自己会重新施加重力、矿车/掉落物
 * 仍会继续走，所以只是"看起来慢"，不是冻结。
 *
 * <p>在 {@code ServerLevel#tickNonPassenger} 的 HEAD 取消才是真正的冻结：该方法负责
 * {@code setOldPosAndRot()} / {@code tickCount++} / {@code entity.tick()} 以及整个载具乘客链，
 * 取消后该实体这一 tick 完全不推进。
 *
 * <p>选择 {@code tickNonPassenger} 而不是 {@code Entity#tick()}：
 * <ul>
 *   <li>它只被服务端主线程调用，天然没有客户端侧问题；</li>
 *   <li>玩家通过 {@code tickPassenger}/网络路径处理，不会被这里连带冻结；</li>
 *   <li>取消点更靠外，兼容性比改 {@code Entity.tick()} 好。</li>
 * </ul>
 */
@Mixin(ServerLevel.class)
public class ServerLevelEntityFreezeMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void kogasa$freezeTimestoppedEntity(Entity entity, CallbackInfo ci) {
        if (TimestopHandler.shouldFreeze(entity)) {
            ci.cancel();
        }
    }
}
