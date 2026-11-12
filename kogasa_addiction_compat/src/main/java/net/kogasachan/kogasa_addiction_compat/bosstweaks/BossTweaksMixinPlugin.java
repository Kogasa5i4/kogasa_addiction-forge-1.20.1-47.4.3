package net.kogasachan.kogasa_addiction_compat.bosstweaks;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** 只在 RevelationFix 的 PlayerTickrateExecutor 真实存在时应用对应 mixin。 */
public final class BossTweaksMixinPlugin implements IMixinConfigPlugin {

    private boolean revelationFixPresent;
    private boolean youkaiHomecomingPresent;
    private boolean goetyPresent;
    private boolean uomPresent;
    private boolean taczPresent;

    @Override
    public void onLoad(String mixinPackage) {
        // 只用资源探测，不能 Class.forName：那会在 mixin prepare 之前把目标类加载，
        // 导致 MixinTargetAlreadyLoadedException。
        revelationFixPresent = BossTweaksMixinPlugin.class.getClassLoader().getResource(
                "com/mega/revelationfix/common/apollyon/common/PlayerTickrateExecutor.class") != null;
        youkaiHomecomingPresent = BossTweaksMixinPlugin.class.getClassLoader().getResource(
                "dev/xkmc/youkaishomecoming/content/entity/boss/BossYoukaiEntity.class") != null;
        goetyPresent = BossTweaksMixinPlugin.class.getClassLoader().getResource(
                "com/Polarice3/Goety/common/entities/boss/Apostle.class") != null;
        uomPresent = BossTweaksMixinPlugin.class.getClassLoader().getResource(
                "com/mega/uom/common/entity/boss/uom/BaseEntity.class") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ApollyonDoomMixin")) {
            return revelationFixPresent;
        }
        if (mixinClassName.endsWith("YhBossTweakMixin")
                || mixinClassName.endsWith("YhYoukaiSetHealthMixin")
                || mixinClassName.endsWith("YhEntityInvulMixin")
                || mixinClassName.endsWith("YhDanmakuSetHealthMixin")) {
            return youkaiHomecomingPresent;
        }
        if (mixinClassName.endsWith("ApostleInvulMixin")) {
            return goetyPresent;
        }
        if (mixinClassName.endsWith("UomBaseInvulMixin")
                || mixinClassName.endsWith("UomWitherInvulMixin")) {
            return uomPresent;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
