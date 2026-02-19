package sky.client.modules.movement;

import net.minecraft.util.math.Box;
import net.minecraft.text.Text;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.SliderSetting;

@FunctionAnnotation(name = "GrimTP", type = Type.Move, desc = "Teleport through blocks using hitbox manipulation (for GrimAC)")
public class TeleportRW extends Function {

    private Box originalHitBox;
    private boolean warningShown = false;

    // Настройки
    private final SliderSetting updateSpeed = new SliderSetting("Update Speed", 5, 1, 20, 1);
    private final SliderSetting heightUp = new SliderSetting("Height Up", 0.5, 0.1, 2.0, 0.1);
    private final SliderSetting heightDown = new SliderSetting("Height Down", 0.5, 0.1, 2.0, 0.1);

    public TeleportRW() {
        addSettings(updateSpeed, heightUp, heightDown);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate e) {
            onUpdate(e);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        warningShown = false;

        if (mc.player != null) {
            originalHitBox = mc.player.getBoundingBox();

            // Показываем предупреждение в чат
            if (!warningShown) {
                sendWarningMessage();
                warningShown = true;
            }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (mc.player != null && originalHitBox != null) {
            mc.player.setBoundingBox(originalHitBox);
        }
    }

    private void onUpdate(EventUpdate eventUpdate) {
        if (mc.player == null) return;

        handleGrimTP();
    }

    private void sendWarningMessage() {
        if (mc.player != null) {
            String warningMessage = "§f&lLightDLC: &7>> &cВнимание! §eДля правильной работы этой функции нужен §bFLY!";
            mc.player.sendMessage(Text.of(warningMessage), false);
        }
    }

    private void handleGrimTP() {
        int updateSpeedValue = updateSpeed.get().intValue();
        double heightUpValue = heightUp.get().doubleValue();
        double heightDownValue = heightDown.get().doubleValue();

        // Обновляем каждые N тиков
        if (mc.player.age % updateSpeedValue == 0) {
            expandSelfHitBox(heightUpValue, heightDownValue);
        }
    }

    private void expandSelfHitBox(double heightUp, double heightDown) {
        if (mc.player == null) return;

        Box currentBB = mc.player.getBoundingBox();

        // Расширяем хитбокс только по вертикали (Y и -Y)
        Box expandedBB = new Box(
                currentBB.minX,
                currentBB.minY - heightDown,
                currentBB.minZ,
                currentBB.maxX,
                currentBB.maxY + heightUp,
                currentBB.maxZ
        );

        mc.player.setBoundingBox(expandedBB);
    }
}