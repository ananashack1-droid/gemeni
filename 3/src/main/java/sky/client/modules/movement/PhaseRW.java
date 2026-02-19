package sky.client.modules.movement;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.EventPacket;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.*;
import java.util.concurrent.CopyOnWriteArrayList;

@FunctionAnnotation(name = "PhaseHard", type = Type.Move, desc = "DESTRUCTIVE Grim/Polar Bypass")
public class PhaseRW extends Function {
    private final SliderSetting range = new SliderSetting("Range", 0.3, 0.1, 2.0, 0.05);
    private final BooleanSetting blink = new BooleanSetting("Blink", true);

    private final CopyOnWriteArrayList<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private int teleportId = -1;

    public PhaseRW() {
        addSettings(range, blink);
    }

    @Override
    public void onEvent(Event event) {
        if (!this.isState()) return;

        // Блокируем пакеты подтверждения (Grim не поймет где мы)
        if (event instanceof EventPacket ep) {
            Packet<?> p = ep.getPacket();
            if (p instanceof PlayerMoveC2SPacket || p instanceof TeleportConfirmC2SPacket || p instanceof PlayerActionC2SPacket) {
                if (blink.get()) {
                    packets.add(p);
                    ep.setCancel(true);
                }
            }
        }

        if (event instanceof EventUpdate) {
            onUpdate();
        }
    }

    private void onUpdate() {
        if (mc.player == null) return;

        // Если перед нами стена
        if (mc.player.horizontalCollision) {
            double yaw = Math.toRadians(mc.player.getYaw());
            double r = range.get().doubleValue();

            // РАССЧЕТ КЛИПА (Пробиваем стену)
            double x = -Math.sin(yaw) * r;
            double z = Math.cos(yaw) * r;

            // 1. Пакет на микро-смещение (обход Raytrace)
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX() + x, mc.player.getY() + 0.0001, mc.player.getZ() + z, false, false
            ));

            // 2. Пакет на "проваливание" вниз (Grim Bypass)
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX() + x, mc.player.getY() - 1.0, mc.player.getZ() + z, false, false
            ));

            // 3. Перемещаем визуально
            mc.player.setPosition(mc.player.getX() + x, mc.player.getY(), mc.player.getZ() + z);

            // Если прошли — выплевываем пакеты и выключаем
            if (!mc.world.getBlockState(mc.player.getBlockPos()).isSolid()) {
                finish();
            }
        }

        // Ограничитель, чтобы не кикнуло за слишком долгий блинк
        if (packets.size() > 25) finish();
    }

    private void finish() {
        if (!packets.isEmpty()) {
            packets.forEach(p -> mc.player.networkHandler.sendPacket(p));
            packets.clear();
        }
        // Финальный синхронизирующий пакет
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround(), false
        ));
        if (this.isState()) this.toggle();
    }

    @Override
    protected void onDisable() {
        finish();
        super.onDisable();
    }
}