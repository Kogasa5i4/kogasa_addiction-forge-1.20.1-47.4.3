package net.kogasachan.kogasa_addiction.compat.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class KogasaPonderScenes {

    public static void kogasaIngotScene(SceneBuilder scene, SceneBuildingUtil util) {
        // 配置基础底板，参数为：底板起始X, 起始Z, 底板边长
        // 底板必须是一个正方形，且包含在.nbt结构中
        scene.configureBasePlate(0, 0, 5);
        scene.title("kogasa_ingot", "获取伞之锭");
        scene.showBasePlate();

        // 显示场景中的某个区域（例如，一个高炉）
        scene.world().showSection(util.select().fromTo(1, 1, 1, 3, 3, 3), Direction.UP);
        scene.idle(20);

        // 添加文本说明
        scene.addKeyframe();
        scene.overlay().showText(80).text("在高炉燃烧时，有几率掉落伞之锭").pointAt(util.vector().centerOf(2, 1, 2));
        scene.idle(60);

        // 模拟物品掉落的粒子效果
        scene.effects().indicateSuccess(util.grid().at(2, 2, 2));

        // 在高炉上方生成一个物品实体，让它掉落
        // 参数：位置、初速度、物品栈
        scene.world().createItemEntity(
                util.vector().topOf(2, 3, 2),      // 生成位置（高炉上方）
                new Vec3(0, 0, 0),                 // 初速度（0 = 原地掉落）
                new ItemStack(ModItems.KOGASA_INGOT.get())
        );
        scene.idle(30);

        // 物品落地后，高亮显示
        scene.overlay().showOutline(
                PonderPalette.GREEN,
                new Object(),
                util.select().position(2, 1, 2),
                30
        );
        scene.idle(20);

        scene.markAsFinished();
    }
}
