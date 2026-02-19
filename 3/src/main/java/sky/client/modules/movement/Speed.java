package sky.client.modules.movement;

import sky.client.modules.setting.ModeSetting;
import sky.client.events.Event;
import sky.client.events.impl.move.EventMotion;
import sky.client.manager.ClientManager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.move.MoveUtil;
import sky.client.util.player.TimerUtil;
import net.minecraft.util.math.Vec3d;

@FunctionAnnotation(name = "Speed", desc = "Улучшенная скорость с обходами", type = Type.Move)
public class Speed extends Function {

    private final ModeSetting mode = new ModeSetting("Режим", "Watchdog",
            "Watchdog", "Matrix", "Verus", "NCP", "Grim", "Vulcan", "Spartan", "Vanilla");

    private final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 3.0f, 0.05f);
    private final SliderSetting timer = new SliderSetting("Таймер", 1.0f, 0.8f, 1.5f, 0.01f);
    private final SliderSetting height = new SliderSetting("Высота прыжка", 0.42f, 0.3f, 0.5f, 0.01f);
    private final TimerUtil timerUtil = new TimerUtil();

    private int ticks;
    private double moveSpeed;
    private double lastDist;
    private boolean wasOnGround;
    private double lastPosX, lastPosZ;

    public Speed() {
        addSettings(mode, speed, timer, height);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion eventMotion) {
            if (mc.player == null || mc.world == null) return;

            // Получаем текущую дистанцию через getPos()
            double currentX = mc.player.getPos().x;
            double currentZ = mc.player.getPos().z;
            double xDist = currentX - lastPosX;
            double zDist = currentZ - lastPosZ;
            lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

            // Сохраняем текущую позицию для следующего тика
            lastPosX = currentX;
            lastPosZ = currentZ;

            // Вызываем соответствующий режим
            String currentMode = mode.get();
            switch (currentMode) {
                case "Watchdog":
                    handleWatchdog();
                    break;
                case "Matrix":
                    handleMatrix();
                    break;
                case "Verus":
                    handleVerus();
                    break;
                case "NCP":
                    handleNCP();
                    break;
                case "Grim":
                    handleGrim();
                    break;
                case "Vulcan":
                    handleVulcan();
                    break;
                case "Spartan":
                    handleSpartan();
                    break;
                case "Vanilla":
                    handleVanilla();
                    break;
            }

            ticks++;
            wasOnGround = mc.player.isOnGround();
        }
    }

    private void handleWatchdog() {
        // Обход Hypixel Watchdog - используем правильные проверки
        if (!MoveUtil.isMoving() || mc.player.isSwimming() || mc.player.isInLava()) return;

        float baseSpeed = speed.get().floatValue() * 0.28f;
        float timerSpeed = timer.get().floatValue();

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            // На земле - прыжок
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, height.get().floatValue(), velocity.z);
            moveSpeed = baseSpeed * 1.6;
            ClientManager.TICK_TIMER = timerSpeed;
        } else {
            // В воздухе - плавное замедление
            moveSpeed = lastDist * 0.91;
            if (ticks % 3 == 0) {
                ClientManager.TICK_TIMER = 1.0f;
            }
        }

        MoveUtil.setSpeed((float) Math.max(moveSpeed, baseSpeed));
    }

    private void handleMatrix() {
        // Обход Matrix 6.x.x
        if (!MoveUtil.isMoving()) return;

        float baseSpeed = speed.get().floatValue() * 0.25f;

        if (mc.player.isOnGround()) {
            if (MoveUtil.isMoving()) {
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(velocity.x, 0.42f, velocity.z);
                moveSpeed = baseSpeed * 2.0;
            }
        } else {
            // Matrix проверяет ускорение в воздухе
            moveSpeed = lastDist * 0.98;
            if (moveSpeed < baseSpeed) {
                moveSpeed = baseSpeed;
            }
        }

        MoveUtil.setSpeed((float) Math.min(moveSpeed, 0.48f));
    }

    private void handleVerus() {
        // Обход Verus (Public Bypass)
        if (!MoveUtil.isMoving()) return;

        float baseSpeed = speed.get().floatValue() * 0.2f;

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, 0.4f, velocity.z);
            moveSpeed = baseSpeed * 1.8;
        } else {
            // Verus строгий к воздушному ускорению
            moveSpeed = lastDist * 0.85;
        }

        // Verus требует постепенного ускорения
        if (ticks < 20) {
            moveSpeed *= 0.9 + (ticks * 0.005);
        }

        MoveUtil.setSpeed((float) moveSpeed);
    }

    private void handleNCP() {
        // Обход NoCheatPlus
        if (!MoveUtil.isMoving()) return;

        float baseSpeed = 0.29f * speed.get().floatValue();

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, 0.42f, velocity.z);
            moveSpeed = baseSpeed * 1.6;
        } else if (!MoveUtil.isMoving()) {
            moveSpeed = 0;
        } else {
            moveSpeed = lastDist * 0.91;
        }

        MoveUtil.setSpeed((float) moveSpeed);
    }

    private void handleGrim() {
        // Обход GrimAC (очень строгий - минимальные значения)
        // Проверяем воду через isTouchingWater()
        if (!MoveUtil.isMoving() || mc.player.isTouchingWater() || mc.player.isInLava()) return;

        float baseSpeed = speed.get().floatValue() * 0.15f;

        if (mc.player.isOnGround()) {
            if (MoveUtil.isMoving() && ticks % 2 == 0) {
                // Grim детектит частые прыжки
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(velocity.x, 0.4f, velocity.z);
                moveSpeed = baseSpeed * 1.4;
            }
        } else {
            // Grim не позволяет ускоряться в воздухе
            moveSpeed = lastDist * 0.98;
        }

        MoveUtil.setSpeed((float) Math.min(moveSpeed, 0.35f));
    }

    private void handleVulcan() {
        // Обход Vulcan (требует минимальных значений)
        if (!MoveUtil.isMoving()) return;

        float baseSpeed = 0.2873f * Math.min(speed.get().floatValue(), 1.2f);

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            if (ticks % 3 == 0) {
                // Vulcan детектит паттерны
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(velocity.x, 0.42f, velocity.z);
                moveSpeed = baseSpeed * 1.3;
            }
        } else {
            moveSpeed = lastDist * 0.99;
            if (moveSpeed < baseSpeed) {
                moveSpeed = baseSpeed;
            }
        }

        MoveUtil.setSpeed((float) moveSpeed);
    }

    private void handleSpartan() {
        // Обход Spartan
        if (!MoveUtil.isMoving()) return;

        float baseSpeed = speed.get().floatValue() * 0.22f;

        if (mc.player.isOnGround() && MoveUtil.isMoving()) {
            Vec3d velocity = mc.player.getVelocity();
            mc.player.setVelocity(velocity.x, 0.42f, velocity.z);
            moveSpeed = baseSpeed * 1.7;
            ClientManager.TICK_TIMER = 1.08f;
        } else {
            moveSpeed = lastDist * 0.91;
            if (ticks % 5 == 0) {
                ClientManager.TICK_TIMER = 1.0f;
            }
        }

        MoveUtil.setSpeed((float) moveSpeed);
    }

    private void handleVanilla() {
        // Простая скорость без обходов
        if (MoveUtil.isMoving()) {
            MoveUtil.setSpeed(speed.get().floatValue());
        }
    }

    @Override
    protected void onEnable() {
        timerUtil.reset();
        ticks = 0;
        moveSpeed = 0;
        lastDist = 0;
        wasOnGround = false;

        // Инициализируем последние позиции
        if (mc.player != null) {
            lastPosX = mc.player.getPos().x;
            lastPosZ = mc.player.getPos().z;
        }

        super.onEnable();
    }

    @Override
    public void onDisable() {
        ClientManager.TICK_TIMER = 1.0f;
        super.onDisable();
    }
}