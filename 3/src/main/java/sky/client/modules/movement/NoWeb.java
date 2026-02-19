package sky.client.modules.movement;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import sky.client.events.Event;
import sky.client.events.impl.move.EventMotion;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.*;

@FunctionAnnotation(name = "NoWeb", type = Type.Move, desc = "Maximum bypass for ReallyWorld (Grim/Matrix)")
public class NoWeb extends Function {

    private final SliderSetting speed = new SliderSetting("Скорость", 0.42, 0.15, 0.6, 0.01);
    private final SliderSetting vertical = new SliderSetting("Вертикалка", 0.35, 0.1, 0.5, 0.01);
    private final BooleanSetting groundBypass = new BooleanSetting("Ground Bypass", true);

    private boolean active;
    private int tickCooldown;

    public NoWeb() {
        addSettings(speed, vertical, groundBypass);
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventMotion) {
            EventMotion em = (EventMotion) event;

            if (isInWeb()) {
                active = true;
                tickCooldown = 0;

                // 1. Расчет вектора
                double s = speed.get().doubleValue();
                double[] move = getDirection(s);

                // 2. Управление движением
                if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {
                    mc.player.setVelocity(move[0], mc.player.getVelocity().y, move[1]);
                } else {
                    mc.player.setVelocity(0, mc.player.getVelocity().y, 0);
                }

                // 3. Вертикальное движение (прыжок/спуск)
                if (mc.options.jumpKey.isPressed()) {
                    mc.player.setVelocity(mc.player.getVelocity().x, vertical.get().doubleValue(), mc.player.getVelocity().z);
                } else if (mc.options.sneakKey.isPressed()) {
                    mc.player.setVelocity(mc.player.getVelocity().x, -vertical.get().doubleValue(), mc.player.getVelocity().z);
                } else {
                    // ГАСИМ гравитацию, чтобы Grim не флагал за "FastFall"
                    mc.player.setVelocity(mc.player.getVelocity().x, -0.005, mc.player.getVelocity().z);
                }

                // 4. Главный обход: подмена состояния земли
                if (groundBypass.get()) {
                    em.setOnGround(true);
                }

            } else if (active) {
                // ЛОГИКА ВЫХОДА (Именно тут летели ошибки и флаги)

                if (tickCooldown < 1) {
                    // В первый тик выхода мы ПРИНУДИТЕЛЬНО замедляем игрока
                    // Это синхронизирует позицию клиента и сервера
                    mc.player.setVelocity(mc.player.getVelocity().x * 0.3, 0, mc.player.getVelocity().z * 0.3);
                    em.setOnGround(true);
                    tickCooldown++;
                } else {
                    active = false;
                }
            }
        }
    }

    private double[] getDirection(double speed) {
        float forward = mc.player.input.movementForward;
        float side = mc.player.input.movementSideways;
        float yaw = mc.player.getYaw();

        if (forward != 0) {
            if (side > 0) yaw += (forward > 0 ? -45 : 45);
            else if (side < 0) yaw += (forward > 0 ? 45 : -45);
            side = 0;
            forward = (forward > 0) ? 1 : -1;
        }

        double rad = Math.toRadians(yaw + 90);
        return new double[]{
                forward * speed * Math.cos(rad) + side * speed * Math.sin(rad),
                forward * speed * Math.sin(rad) - side * speed * Math.cos(rad)
        };
    }

    // Исправленный метод проверки (без ошибок компиляции)
    private boolean isInWeb() {
        if (mc.player == null || mc.world == null) return false;

        Box box = mc.player.getBoundingBox().expand(0.06); // Минимальное расширение
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.floor(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.floor(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.floor(box.maxZ); z++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.COBWEB) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        active = false;
        super.onDisable();
    }
}