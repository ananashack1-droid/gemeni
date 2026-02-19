package sky.client.modules.combat;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Vector2f;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.input.EventKey;
import sky.client.events.impl.input.EventKeyBoard;
import sky.client.events.impl.move.EventMotion;
import sky.client.events.impl.world.EventObsidianPlace;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.BindSetting;
import sky.client.modules.setting.BooleanSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.util.move.MoveUtil;

import java.util.concurrent.*;

@FunctionAnnotation(name = "AutoExplosion", type = Type.Combat, desc = "Автоматически ставит кристал, при ставке обсидиана")
public class AutoExplosion extends Function {

    private final BooleanSetting correction = new BooleanSetting("Коррекция движения", true);
    private final SliderSetting delay = new SliderSetting("Задержка", 100f, 50f, 300f, 1f);
    private final BooleanSetting sanya = new BooleanSetting("Ставить по бинду", false);
    private final BindSetting bind = new BindSetting("Кнопка", 0, () -> sanya.get());

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private BlockPos crystalPos;
    private Entity crystalEntity;
    private int prevSlot = -1;
    public Vector2f serverRot;

    public AutoExplosion() {
        addSettings(correction, delay, sanya, bind);
    }

    public boolean check() {
        return state && correction.get() && crystalEntity != null && crystalPos != null && serverRot != null;
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventKey e && sanya.get() && e.key == bind.getKey()) {
            placeSequence();
        }

        if (event instanceof EventKeyBoard e && check()) {
            MoveUtil.fixMovement(e, serverRot.x);
        }

        if (event instanceof EventMotion e && crystalEntity != null) {
            Vector2f target = rotationTo(crystalEntity);
            if (serverRot == null) serverRot = target;
            serverRot.x += MathHelper.clamp(target.x - serverRot.x, -10, 10);
            serverRot.y += MathHelper.clamp(target.y - serverRot.y, -10, 10);
            e.setYaw(serverRot.x);
            e.setPitch(serverRot.y);
        }

        if (event instanceof EventObsidianPlace e) {
            scheduleCrystal(e.getPos());
        }

        if (event instanceof EventUpdate && crystalPos != null) {
            if (mc.player.getPos().distanceTo(Vec3d.ofCenter(crystalPos)) > 6) {
                reset();
                return;
            }

            mc.world.getOtherEntities(null, new Box(crystalPos).expand(1)).stream()
                    .filter(e -> e instanceof EndCrystalEntity)
                    .findFirst()
                    .ifPresent(e -> {
                        crystalEntity = e;
                        if (mc.player.getAttackCooldownProgress(0) >= 1) {
                            mc.interactionManager.attackEntity(mc.player, e);
                            mc.player.swingHand(Hand.MAIN_HAND);
                            reset();
                        }
                    });
        }
    }

    private void placeSequence() {
        BlockPos pos = getLookingBlock();
        int obsSlot = findSlot(Items.OBSIDIAN);
        if (pos == null || obsSlot == -1) return;

        prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = obsSlot;

        scheduler.schedule(() -> mc.execute(() -> {
            placeAt(pos);
            mc.player.getInventory().selectedSlot = prevSlot;
            scheduleCrystal(pos);
        }), 50, TimeUnit.MILLISECONDS);
    }

    private void scheduleCrystal(BlockPos pos) {
        int slot = findSlot(Items.END_CRYSTAL);
        if (slot == -1 || !mc.world.isAir(pos.up())) return;

        scheduler.schedule(() -> mc.execute(() -> {
            prevSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slot;

            if (placeAt(pos)) crystalPos = pos;

            mc.player.getInventory().selectedSlot = prevSlot;
        }), delay.get().longValue(), TimeUnit.MILLISECONDS);
    }

    private boolean placeAt(BlockPos pos) {
        var hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        if (mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit) == ActionResult.SUCCESS) {
            mc.player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private int findSlot(Item item) {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(item)) return i;
        return -1;
    }

    private BlockPos getLookingBlock() {
        Vec3d eye = mc.player.getCameraPosVec(1f);
        var hit = mc.world.raycast(new RaycastContext(eye, eye.add(mc.player.getRotationVec(1f).multiply(4)),
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
        return hit != null ? hit.getBlockPos() : null;
    }

    private Vector2f rotationTo(Entity e) {
        Vec3d d = e.getPos().subtract(mc.player.getPos());
        return new Vector2f((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f,
                (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
    }

    private void reset() {
        crystalEntity = null;
        crystalPos = null;
        serverRot = null;
        prevSlot = -1;
    }

    @Override
    protected void onDisable() {
        reset();
    }
}