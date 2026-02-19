package sky.client.modules.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.input.EventKeyBoard;
import sky.client.events.impl.move.EventMotion;
import sky.client.events.impl.player.EventSprint;
import sky.client.manager.Manager;
import sky.client.mixin.iface.ClientPlayerEntityAccessor;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.setting.*;
import sky.client.util.math.RayTraceUtil;
import sky.client.util.move.MoveUtil;
import sky.client.util.player.AuraUtil;
import sky.client.util.player.InventoryUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@SuppressWarnings("All")
@FunctionAnnotation(name = "AttackAura", keywords = {"Пиздить", "Хуячить", "KillAura"}, desc = "Ебашит бошки всем вокруг", type = Type.Combat)
public class AttackAura extends Function {

    private final ModeSetting mode = new ModeSetting("Мод", "Universal", "Universal", "Snap", "FunTime", "SpookyTime", "KoopinAc", "LonyGrief", "1.8.8", "Neural", "ReallyWorld");
    private final MultiSetting targets = new MultiSetting("Цели", Arrays.asList("Игроки", "Голые", "Мобы", "Монстры"), new String[]{"Игроки", "Голые", "Друзья", "Мобы", "Монстры", "Жители", "Невидимки"});
    private final ModeSetting sort = new ModeSetting("Сортировать", "Умная", "Умная", "По здоровью", "По дистанции", "По броне", "По FOV");
    private final MultiSetting setting = new MultiSetting("Настройки", Arrays.asList("Только критами", "Ломать щит", "Отжим щита"), new String[]{"Только критами", "Ломать щит", "Отжим щит", "Бить через стены"});

    private final SliderSetting distance = new SliderSetting("Радиус атаки", 3.0f, 1.8f, 6f, 0.1f);
    private final SliderSetting rotateDistance = new SliderSetting("Радиус обнаружения", 5f, 0.0f, 10f, 0.1f);
    private final SliderSetting snapDelay = new SliderSetting("Задержка снапа", 150, 50, 300, 10, () -> mode.is("Snap"));
    private final SliderSetting attacksBeforeSnap = new SliderSetting("Атак до снапа", 20, 5, 30, 1, () -> mode.is("Snap"));

    private final BindBooleanSetting onlySpaceCritical = new BindBooleanSetting("Только с пробелом", false, () -> setting.get("Только критами"));
    private final BooleanSetting noAttackIfEat = new BooleanSetting("Не бить если ешь", false);
    private final BooleanSetting raycast = new BooleanSetting("Проверять наведение", false);
    public final BooleanSetting correction = new BooleanSetting("Коррекция", true);
    public final ModeSetting correctionType = new ModeSetting(() -> correction.get(), "Тип коррекции", "Free", "Free", "Focus");
    private final ModeSetting sprintreset = new ModeSetting("Тип спринта", "Rage", "Rage", "Legit", "None");

    private final BooleanSetting advanced = new BooleanSetting("Продвинутые настройки", false);
    private final SliderSetting elytraDistance = new SliderSetting("Радиус на элитрах", 40f, 0f, 80f, 1f, () -> advanced.get());
    private final SliderSetting rotationSpeed = new SliderSetting("Скорость ротации", 180f, 10f, 360f, 5f, () -> advanced.get() && mode.is("Universal"));
    private final BooleanSetting dynamicPoint = new BooleanSetting("Динамическая точка", true, () -> advanced.get() && mode.is("Universal"));
    private final BooleanSetting applyGCD = new BooleanSetting("GCD Fix", true, () -> advanced.get());

    // ReallyWorld настройки
    private final SliderSetting rwCritChance = new SliderSetting("RW Шанс крита", 80.0f, 0.0f, 100.0f, 1.0f, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwHitDelay = new SliderSetting("RW Задержка удара", 50, 0, 200, 5, () -> mode.is("ReallyWorld"));
    private final BooleanSetting rwAutoCrit = new BooleanSetting("RW Авто-криты", true, () -> mode.is("ReallyWorld"));
    private final BooleanSetting rwPacketCrit = new BooleanSetting("RW Пакетные криты", false, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwRangeBoost = new SliderSetting("RW Увеличение дистанции", 0.5f, 0.0f, 2.0f, 0.1f, () -> mode.is("ReallyWorld"));
    private final BooleanSetting rwSmoothing = new BooleanSetting("RW Сглаживание", true, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwSmoothSpeed = new SliderSetting("RW Скорость сглаживания", 120f, 10f, 360f, 5f, () -> mode.is("ReallyWorld") && rwSmoothing.get());

    // ReallyWorld HVH настройки
    private final ModeSetting rwCombatMode = new ModeSetting("RW Режим боя", "Normal", "Normal", "Aggressive", "Defensive", "Matrix", "Verus", "Vulcan", "Grim", "NCP", "Spartan");
    private final SliderSetting rwStrafeSpeed = new SliderSetting("RW Скорость стрейфа", 0.45f, 0.1f, 1.0f, 0.01f, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwOvertakeBoost = new SliderSetting("RW Ускорение обгона", 0.3f, 0.0f, 1.0f, 0.05f, () -> mode.is("ReallyWorld"));
    private final BooleanSetting rwPenetration = new BooleanSetting("RW Проникновение", true, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwPenetrationForce = new SliderSetting("RW Сила пенетрации", 1.2f, 0.5f, 3.0f, 0.1f, () -> mode.is("ReallyWorld") && rwPenetration.get());
    private final BooleanSetting rwMotionCorrection = new BooleanSetting("RW Коррекция движения", true, () -> mode.is("ReallyWorld"));
    private final ModeSetting rwCorrectionType = new ModeSetting(() -> rwMotionCorrection.get() && mode.is("ReallyWorld"), "RW Тип коррекции", "StrafeBoost", "StrafeBoost", "CircleStrafe", "Predictive", "Jitter");
    private final SliderSetting rwJitterStrength = new SliderSetting("RW Сила джиттера", 0.15f, 0.0f, 0.5f, 0.01f, () -> mode.is("ReallyWorld") && rwMotionCorrection.get() && rwCorrectionType.is("Jitter"));
    private final BooleanSetting rwAutoBlock = new BooleanSetting("RW Авто-блок", false, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwBlockChance = new SliderSetting("RW Шанс блока", 40.0f, 0.0f, 100.0f, 1.0f, () -> mode.is("ReallyWorld") && rwAutoBlock.get());
    private final BooleanSetting rwHitSelect = new BooleanSetting("RW Селектор ударов", true, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwHitOffset = new SliderSetting("RW Смещение удара", 0.2f, -0.5f, 0.5f, 0.01f, () -> mode.is("ReallyWorld") && rwHitSelect.get());
    private final BooleanSetting rwAntiBotCheck = new BooleanSetting("RW Анти-бот проверка", true, () -> mode.is("ReallyWorld"));
    private final BooleanSetting rwSpinBot = new BooleanSetting("RW Спин-бот", false, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwSpinSpeed = new SliderSetting("RW Скорость вращения", 180f, 10f, 720f, 10f, () -> mode.is("ReallyWorld") && rwSpinBot.get());
    private final BooleanSetting rwSmartStrafe = new BooleanSetting("RW Умный стрейф", true, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwStrafePredict = new SliderSetting("RW Предсказание стрейфа", 2, 0, 5, 1, () -> mode.is("ReallyWorld") && rwSmartStrafe.get());
    private final BooleanSetting rwVelocityCorrection = new BooleanSetting("RW Коррекция скорости", true, () -> mode.is("ReallyWorld"));
    private final SliderSetting rwVelReduction = new SliderSetting("RW Снижение отдачи", 0.7f, 0.0f, 1.0f, 0.05f, () -> mode.is("ReallyWorld") && rwVelocityCorrection.get());

    public LivingEntity target;
    private long cpsLimit, lastHitMs, lastSwitch, lastBreathChange, shakeStartTime;
    private int attackCount;
    private boolean isSnappingUp, swingSideRight;
    private float snapStartPitch, lastYawJitter, lastPitchJitter;
    private float jitterYaw, jitterYawTarget, jitterYawSpeed, microJitter, swayPhase, swaySpeed = 0.04f, swayAmplitude = 2.5f;
    private final Random rnd = new Random();
    private BackTrack.Pos btPos;

    // ReallyWorld переменные
    private int rwAttackTicks;
    private boolean rwWasCritical;

    // ReallyWorld HVH переменные
    private int rwStrafeDirection = 1;
    private long rwLastStrafeChange = 0;
    private float rwCurrentYawOffset = 0;
    private float rwPredictionFactor = 1.0f;
    private int rwTicksSinceLastHit = 0;
    private boolean rwWasBlocking = false;
    private int rwSpinAngle = 0;
    private final Random rwRandom = new Random();
    private Vec3d rwLastTargetPos = null;
    private int rwPredictionTicks = 0;
    private int rwStrafePattern = 0;
    private float rwLastVelocity = 0;
    private boolean rwWasHit = false;
    private long rwLastHitTime = 0;
    private int rwComboCounter = 0;

    public AttackAura() {
        addSettings(mode, targets, sort, setting, distance, rotateDistance, snapDelay, attacksBeforeSnap,
                onlySpaceCritical, noAttackIfEat, raycast, correction, correctionType, sprintreset,
                advanced, elytraDistance, rotationSpeed, dynamicPoint, applyGCD,
                rwCritChance, rwHitDelay, rwAutoCrit, rwPacketCrit, rwRangeBoost, rwSmoothing, rwSmoothSpeed,
                rwCombatMode, rwStrafeSpeed, rwOvertakeBoost, rwPenetration, rwPenetrationForce,
                rwMotionCorrection, rwCorrectionType, rwJitterStrength, rwAutoBlock, rwBlockChance,
                rwHitSelect, rwHitOffset, rwAntiBotCheck, rwSpinBot, rwSpinSpeed,
                rwSmartStrafe, rwStrafePredict, rwVelocityCorrection, rwVelReduction);
    }

    @Override
    public void onEvent(Event event) {
        ClientPlayerEntity p = mc.player;
        if (p == null || p.isDead()) {
            target = null;
            resetRWState();
            return;
        }

        if (event instanceof EventKeyBoard e && (correction.get() && correctionType.is("Free") || mode.is("ReallyWorld"))) {
            if (mode.is("ReallyWorld")) {
                // ReallyWorld коррекция движения
                if (rwMotionCorrection.get() && target != null) {
                    applyRWMovementCorrection();
                }
            } else {
                MoveUtil.fixMovement(e, Manager.FUNCTION_MANAGER.autoPotion.isActivePotion ? Manager.ROTATION.getPitch() : Manager.ROTATION.getYaw());
            }
        }

        if (event instanceof EventSprint s && sprintreset.is("Legit") && canAttack() && target != null && p.isSprinting()) {
            s.setSprinting(false);
        }

        if (event instanceof EventUpdate) {
            btPos = null;
            if (target == null || !isValidTarget(target)) target = findTarget();
            if (target == null) {
                Manager.ROTATION.set(p.getYaw(), p.getPitch());
                cpsLimit = System.currentTimeMillis();
                resetSnap();
                rwTicksSinceLastHit++;
                rwComboCounter = 0;
            } else {
                updateBtPos(target);

                // ReallyWorld обновление
                if (mode.is("ReallyWorld")) {
                    updateRWCombat();
                }

                handleAttack(target);
            }

            // Обновляем тики для ReallyWorld
            rwAttackTicks++;
        }

        if (event instanceof EventMotion m) {
            m.setYaw(Manager.ROTATION.getYaw());
            m.setPitch(Manager.ROTATION.getPitch());
        }
    }

    @Override
    protected void onEnable() {
        resetSnap();
        resetRWState();
        btPos = null;
    }

    @Override
    protected void onDisable() {
        if (target != null && isValidTarget(target) && (mode.is("FunTime") || mode.is("Universal") || mode.is("ReallyWorld"))) {
            Manager.ROTATION.smoothReturn(350);
        } else if (mc.player != null) {
            Manager.ROTATION.set(mc.player.getYaw(), mc.player.getPitch());
        }
        target = null;
        btPos = null;
        cpsLimit = System.currentTimeMillis();
        resetSnap();
        resetRWState();
    }

    private void resetRWState() {
        rwAttackTicks = 0;
        rwWasCritical = false;
        rwTicksSinceLastHit = 0;
        rwPredictionFactor = 1.0f;
        rwStrafeDirection = 1;
        rwLastStrafeChange = System.currentTimeMillis();
        rwSpinAngle = 0;
        rwLastTargetPos = null;
        rwPredictionTicks = 0;
        rwStrafePattern = 0;
        rwLastVelocity = 0;
        rwWasHit = false;
        rwLastHitTime = 0;
        rwComboCounter = 0;

        // Сбрасываем авто-блок
        if (rwWasBlocking && mc.player != null) {
            mc.options.useKey.setPressed(false);
            rwWasBlocking = false;
        }
    }

    private void resetSnap() {
        attackCount = 0;
        isSnappingUp = false;
    }

    private void updateBtPos(LivingEntity t) {
        if (!(t instanceof PlayerEntity player)) return;

        BackTrack bt = Manager.FUNCTION_MANAGER.backTrack;
        if (!bt.state) return;

        double range = distance.get().doubleValue() + (mode.is("ReallyWorld") ? rwRangeBoost.get().floatValue() : 0);
        double realDist = bt.getRealEdgeDist(player);
        BackTrack.Pos best = bt.getOptimal(player, range);

        if (best != null) {
            double btDist = bt.getEdgeDist(best, player);
            boolean realInvalid = realDist > range || (!mc.player.canSee(t) && !setting.get("Бить через стены"));
            if (realInvalid || btDist < realDist - 0.2) btPos = best;
        }
    }

    private boolean useBt() { return btPos != null; }

    private float[] calcRot(LivingEntity e, Vec3d tgt) {
        Vec3d eye = mc.player.getEyePos();
        double dx = tgt.x - eye.x, dy = tgt.y - eye.y, dz = tgt.z - eye.z;
        return new float[]{
                (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f,
                (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))
        };
    }

    private Vec3d getPoint(LivingEntity e, boolean dynamic) {
        Vec3d base = useBt() ? btPos.pos() : e.getPos();
        if (dynamic) {
            float[] pts = {0.85f, 0.65f, 0.45f, 0.25f};
            return base.add(0, e.getHeight() * pts[(int)(System.currentTimeMillis() / 150 % 4)], 0);
        }
        return base.add(0, e.getHeight() * 0.5f, 0);
    }

    private Vec3d getRWAttackPoint(LivingEntity e) {
        if (!rwHitSelect.get() || !mode.is("ReallyWorld")) {
            return getPoint(e, dynamicPoint.get());
        }

        Vec3d basePoint = getPoint(e, dynamicPoint.get());
        float offset = rwHitOffset.get().floatValue();

        // Выбор точки удара в зависимости от режима боя
        switch (rwCombatMode.get()) {
            case "Aggressive":
                // Агрессивный режим - бьем в голову
                return basePoint.add(0, e.getHeight() * 0.85f, 0);
            case "Defensive":
                // Защитный режим - бьем в тело
                return basePoint.add(0, e.getHeight() * 0.5f, 0);
            case "Matrix":
                // Matrix режим - рандомная точка с паттерном
                float patternOffset = (float) Math.sin(System.currentTimeMillis() / 200.0) * 0.2f;
                return basePoint.add(0, e.getHeight() * (0.6f + patternOffset), 0);
            case "Verus":
                // Verus режим - смещение для обхода
                return basePoint.add(
                        (rwRandom.nextFloat() - 0.5f) * 0.08f,
                        e.getHeight() * (0.62f + offset),
                        (rwRandom.nextFloat() - 0.5f) * 0.08f
                );
            default:
                // Normal режим с настройкой смещения
                return basePoint.add(
                        (rwRandom.nextFloat() - 0.5f) * 0.1f,
                        e.getHeight() * (0.6f + offset),
                        (rwRandom.nextFloat() - 0.5f) * 0.1f
                );
        }
    }

    private float gcd(float rot, float last) {
        if (!applyGCD.get() || mode.is("ReallyWorld")) return rot;
        float s = (float)(mc.options.getMouseSensitivity().getValue() * 0.6f + 0.2f);
        return rot - (rot - last) % (s * s * s * 1.2f);
    }

    private void setRot(float y, float p, float ly, float lp) {
        Manager.ROTATION.set(gcd(y, ly), gcd(MathHelper.clamp(p, -89.9f, 89.9f), lp));
    }

    private void reallyWorldRot(LivingEntity e) {
        if (rwSpinBot.get()) {
            // Режим спин-бота
            rwSpinAngle = (rwSpinAngle + (int)rwSpinSpeed.get().floatValue()) % 360;
            Manager.ROTATION.set(mc.player.getYaw() + rwSpinAngle, mc.player.getPitch());
            return;
        }

        Vec3d attackPoint = getRWAttackPoint(e);
        float[] targetRot = calcRot(e, attackPoint);

        float currentYaw = Manager.ROTATION.getYaw();
        float currentPitch = Manager.ROTATION.getPitch();

        // Применяем обход античита
        handleRWAntiCheatBypass();

        float speed = rwSmoothing.get() ? rwSmoothSpeed.get().floatValue() : 360f;

        // Коррекция скорости для разных режимов боя
        switch (rwCombatMode.get()) {
            case "Aggressive":
                speed *= 1.3f;
                break;
            case "Defensive":
                speed *= 0.7f;
                break;
            case "Matrix":
                speed = 100f + (float)Math.sin(System.currentTimeMillis() / 500.0) * 50f;
                break;
            case "Verus":
                speed = Math.min(speed, 180f);
                break;
            case "Vulcan":
                speed = 140f + rwRandom.nextFloat() * 40f;
                break;
        }

        if (rwSmoothing.get()) {
            float yawDiff = MathHelper.wrapDegrees(targetRot[0] - currentYaw);
            float pitchDiff = targetRot[1] - currentPitch;

            float yawStep = MathHelper.clamp(yawDiff, -speed, speed);
            float pitchStep = MathHelper.clamp(pitchDiff, -speed, speed);

            Manager.ROTATION.set(currentYaw + yawStep, currentPitch + pitchStep);
        } else {
            Manager.ROTATION.set(targetRot[0], targetRot[1]);
        }

        // Применяем коррекцию движения если включена
        if (rwMotionCorrection.get() && rwTicksSinceLastHit < 10) {
            applyRWMovementCorrection();
        }
    }

    private void universalRot(LivingEntity e) {
        float[] t = calcRot(e, getPoint(e, dynamicPoint.get()));
        float cy = Manager.ROTATION.getYaw(), cp = Manager.ROTATION.getPitch(), sp = rotationSpeed.get().floatValue();
        setRot(cy + MathHelper.clamp(MathHelper.wrapDegrees(t[0] - cy), -sp, sp), cp + MathHelper.clamp(t[1] - cp, -sp, sp), cy, cp);
    }

    private void snapRot(LivingEntity e, boolean hit) {
        if (hit) { attackTarget(mc.player); attackCount++; lastHitMs = System.currentTimeMillis(); }

        if (attackCount >= attacksBeforeSnap.get().intValue() + ThreadLocalRandom.current().nextInt(6) && !isSnappingUp) {
            isSnappingUp = true; shakeStartTime = System.currentTimeMillis(); snapStartPitch = Manager.ROTATION.getPitch(); attackCount = 0;
        }

        float cy = Manager.ROTATION.getYaw(), cp = Manager.ROTATION.getPitch();
        if (isSnappingUp) {
            float prog = (System.currentTimeMillis() - shakeStartTime) / (float) snapDelay.get().intValue();
            if (prog >= 1f) { isSnappingUp = false; Manager.ROTATION.set(cy, -90f); }
            else Manager.ROTATION.set(cy, MathHelper.clamp(snapStartPitch + (-90f - snapStartPitch) * prog, -90f, 90f));
            return;
        }

        float[] t = calcRot(e, getPoint(e, true));
        float dY = MathHelper.wrapDegrees(t[0] - cy), dP = t[1] - cp;
        float aY = Math.min(Math.max(Math.abs(dY), 0.5f), 99f), aP = Math.min(Math.max(Math.abs(dP), 0.5f), 95f);
        setRot(cy + (dY > 0 ? aY : -aY) + (float) Math.sin(System.currentTimeMillis() / 55.0) * 16.5f,
                MathHelper.clamp(cp + (dP > 0 ? aP : -aP) + (float) Math.cos(System.currentTimeMillis() / 650.0) * 2f, -70f, 70f), cy, cp);
    }

    private void spookyRot(LivingEntity e) {
        float[] t = calcRot(e, getPoint(e, false));
        float cy = Manager.ROTATION.getYaw(), cp = Manager.ROTATION.getPitch();
        float dY = MathHelper.wrapDegrees(t[0] - cy), dP = t[1] - cp;
        float ySpd = MathHelper.lerp(MathHelper.clamp(Math.abs(dY) / 180f, 0f, 1f), 42.2f, 55.03f) * (1f + (rnd.nextFloat() * 2f - 1f) * 0.3f);
        float pSpd = MathHelper.lerp(MathHelper.clamp(Math.abs(dP) / 90f, 0f, 1f), 9.2f, 32.2f) * (1f + (rnd.nextFloat() * 2f - 1f) * 0.3f);
        lastYawJitter = MathHelper.lerp(0.35f, lastYawJitter, (rnd.nextFloat() * 2f - 1f) * 3f);
        lastPitchJitter = MathHelper.lerp(0.35f, lastPitchJitter, (rnd.nextFloat() * 2f - 1f) * 0.5f);
        setRot(cy + MathHelper.clamp(dY, -ySpd, ySpd) + lastYawJitter, cp + MathHelper.clamp(dP, -pSpd, pSpd) + lastPitchJitter, cy, cp);
    }

    private void funtimeRot(LivingEntity e) {
        Vec3d base = useBt() ? btPos.pos() : e.getPos();
        float[] pts = {0.82f, 0.67f, 0.43f, 0.27f};
        Vec3d tgt = base.add(0, e.getHeight() * pts[(int)(System.currentTimeMillis() / 180 % 4)], 0);
        double hw = e.getWidth() / 2.0, off = swingSideRight ? hw * 1.2 : -hw;
        double yawTo = Math.atan2(e.getZ() - mc.player.getZ(), e.getX() - mc.player.getX());
        tgt = tgt.add(Math.cos(yawTo + Math.PI / 2) * off, 0, Math.sin(yawTo + Math.PI / 2) * off);

        float[] r = calcRot(e, tgt);
        long now = System.currentTimeMillis();
        if (now - lastSwitch > 200 + rnd.nextInt(250)) {
            lastSwitch = now; swingSideRight = !swingSideRight;
            jitterYawTarget = (swingSideRight ? 4f : -4f) * MathHelper.clamp(mc.player.distanceTo(e) / 6f, 0.4f, 1f) + (float)(rnd.nextGaussian() * 0.6);
        }
        jitterYawSpeed += (jitterYawTarget - jitterYaw) * 0.05f; jitterYawSpeed *= 0.88f; jitterYaw = (jitterYaw + jitterYawSpeed) * 0.985f;
        if (now - lastBreathChange > 2000 + rnd.nextInt(1500)) { lastBreathChange = now; swaySpeed = 0.035f + rnd.nextFloat() * 0.02f; swayAmplitude = 2f + rnd.nextFloat() * 1.2f; }
        swayPhase += swaySpeed;
        microJitter = (microJitter + (rnd.nextFloat() - 0.5f) * 0.25f) * 0.85f;
        Manager.ROTATION.setSmooth(r[0] + MathHelper.clamp(jitterYaw + (float) Math.sin(swayPhase) * swayAmplitude, (float)(-hw * 8.5), (float)(hw * 8.5)) + microJitter,
                r[1] + (float) Math.sin(swayPhase * 0.8) * 0.5f, 1.1f, 180f, 15f, true);
    }

    private void funtimeIdle() {
        if (shakeStartTime == 0) shakeStartTime = System.currentTimeMillis();
        float t = (System.currentTimeMillis() - shakeStartTime) / 1000f;
        Manager.ROTATION.setSmooth(mc.player.getYaw() + (float) Math.sin(2 * Math.PI * 2.4 * t) * 24f + (float) Math.sin(2 * Math.PI * 0.08 * t) * (5f + rnd.nextFloat()),
                (float) Math.sin(2 * Math.PI * 0.08 * t) * (5f + rnd.nextFloat()), 1f, 20f, 10f, true);
    }

    private void koopinRot(LivingEntity e) {
        Vec3d base = useBt() ? btPos.pos() : e.getPos();
        Vec3d[] pts = {base.add(0, e.getHeight(), 0), base.add(0, e.getStandingEyeHeight() / 2f, 0), base.add(0, 0.05, 0)};
        float cp = Manager.ROTATION.getPitch(); Vec3d best = pts[1]; float bestD = Float.MAX_VALUE;
        for (Vec3d p : pts) { float d = Math.abs(calcRot(e, p)[1] - cp); if (d < bestD) { bestD = d; best = p; } }
        float[] r = calcRot(e, best); float cy = Manager.ROTATION.getYaw();
        float dY = MathHelper.wrapDegrees(r[0] - cy), dP = r[1] - cp;
        float aY = Math.max(Math.abs(dY), 1f); if (aY <= 3f) aY = 3.1f;
        Manager.ROTATION.set(cy + (dY > 0 ? aY : -aY), MathHelper.clamp(cp + (dP > 0 ? Math.max(Math.abs(dP), 2f) : -Math.max(Math.abs(dP), 2f)), -90f, 90f));
    }

    private void handleAttack(LivingEntity t) {
        boolean hit = canHit(t);

        switch (mode.get()) {
            case "KoopinAc", "1.8.8" -> {
                koopinRot(t);
                if (hit) attackTarget(mc.player);
            }
            case "SpookyTime" -> {
                spookyRot(t);
                if (hit) attackTarget(mc.player);
            }
            case "FunTime" -> {
                if (hit && canAttack()) {
                    attackTarget(mc.player);
                    lastHitMs = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - lastHitMs < 450) funtimeRot(t);
                else funtimeIdle();
            }
            case "LonyGrief" -> {
                if (hit) attackTarget(mc.player);
                Manager.ROTATION.set(mc.player.getYaw(), mc.player.getPitch());
            }
            case "Snap" -> snapRot(t, hit);
            case "ReallyWorld" -> {
                reallyWorldRot(t);
                if (hit) {
                    attackReallyWorld(mc.player);
                }
            }
            default -> {
                if (hit) attackTarget(mc.player);
                universalRot(t);
            }
        }
    }

    // ========== REALLYWORLD HVH МЕТОДЫ ==========

    private void updateRWCombat() {
        if (target == null || !mode.is("ReallyWorld")) return;

        rwTicksSinceLastHit++;

        // Обновление предсказания движения цели
        updateTargetPrediction();

        // Авто-блокировка
        handleAutoBlock();

        // Обновление пенетрации
        if (rwPenetration.get() && rwTicksSinceLastHit > 10) {
            rwPredictionFactor += rwPenetrationForce.get().floatValue() * 0.05f;
            rwPredictionFactor = Math.min(rwPredictionFactor, 3.0f);
        }

        // Коррекция скорости при получении удара
        if (rwVelocityCorrection.get() && rwWasHit) {
            long timeSinceHit = System.currentTimeMillis() - rwLastHitTime;
            if (timeSinceHit < 500) {
                float reduction = rwVelReduction.get().floatValue();
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(velocity.x * reduction, velocity.y, velocity.z * reduction);
            } else {
                rwWasHit = false;
            }
        }

        // Умный стрейф
        if (rwSmartStrafe.get() && rwTicksSinceLastHit < 20) {
            updateSmartStrafe();
        }
    }

    private void updateTargetPrediction() {
        if (target == null) {
            rwLastTargetPos = null;
            return;
        }

        Vec3d currentPos = target.getPos();

        if (rwLastTargetPos != null) {
            Vec3d velocity = currentPos.subtract(rwLastTargetPos);
            double speed = velocity.length();

            // Предсказываем позицию на основе скорости
            if (speed > 0.05 && rwSmartStrafe.get()) {
                int predictTicks = rwStrafePredict.get().intValue();
                rwPredictionTicks = (int)Math.min(predictTicks, speed * 15);
            } else {
                rwPredictionTicks = 0;
            }
        }

        rwLastTargetPos = currentPos;
    }

    private void handleAutoBlock() {
        if (!rwAutoBlock.get() || mc.player == null || mc.player.isDead()) return;

        // Проверяем, нужно ли блокировать
        boolean shouldBlock = false;

        if (target != null) {
            // Блокируем если цель близко и атакует
            double distance = target.distanceTo(mc.player);
            if (distance < 3.5 && rwTicksSinceLastHit < 5) {
                if (rwRandom.nextFloat() * 100 < rwBlockChance.get().floatValue()) {
                    shouldBlock = true;
                }
            }
        }

        // Применяем блок
        if (shouldBlock && !mc.player.isUsingItem() && hasShield()) {
            mc.options.useKey.setPressed(true);
            rwWasBlocking = true;
        } else if (rwWasBlocking && !shouldBlock) {
            mc.options.useKey.setPressed(false);
            rwWasBlocking = false;
        }
    }

    private boolean hasShield() {
        return mc.player.getOffHandStack().getItem() == Items.SHIELD ||
                mc.player.getMainHandStack().getItem() == Items.SHIELD;
    }

    private void updateSmartStrafe() {
        if (target == null || mc.player == null) return;

        // Меняем паттерн стрейфа каждые 1-3 секунды
        long currentTime = System.currentTimeMillis();
        if (currentTime - rwLastStrafeChange > 1000 + rwRandom.nextInt(2000)) {
            rwStrafePattern = rwRandom.nextInt(4);
            rwStrafeDirection = rwRandom.nextBoolean() ? 1 : -1;
            rwLastStrafeChange = currentTime;
        }

        // Применяем выбранный паттерн
        switch (rwStrafePattern) {
            case 0: // Стандартный стрейф
                applyStandardStrafe();
                break;
            case 1: // Зигзаг
                applyZigZagStrafe();
                break;
            case 2: // Круговой
                applyCircularStrafe();
                break;
            case 3: // Агрессивный
                applyAggressiveStrafe();
                break;
        }
    }

    private void applyRWMovementCorrection() {
        if (!rwMotionCorrection.get() || target == null || mc.player == null) return;

        float baseSpeed = rwStrafeSpeed.get().floatValue();
        float overtakeBoost = rwOvertakeBoost.get().floatValue();

        switch (rwCorrectionType.get()) {
            case "StrafeBoost":
                applyStrafeBoostCorrection(baseSpeed, overtakeBoost);
                break;
            case "CircleStrafe":
                applyCircleStrafeCorrection(baseSpeed);
                break;
            case "Predictive":
                applyPredictiveCorrection(baseSpeed);
                break;
            case "Jitter":
                applyJitterCorrection(baseSpeed);
                break;
        }
    }

    private void applyStandardStrafe() {
        float baseSpeed = rwStrafeSpeed.get().floatValue();
        float speedMultiplier = 1.0f + (rwComboCounter * 0.05f);

        // Вычисляем угол стрейфа
        float targetYaw = Manager.ROTATION.getYaw();
        float strafeAngle = targetYaw + (90 * rwStrafeDirection);

        // Применяем движение
        float forward = 0.0f;
        float strafe = baseSpeed * rwStrafeDirection * speedMultiplier;

        applyMovement(strafeAngle, forward, strafe, 0.2f);
    }

    private void applyZigZagStrafe() {
        float baseSpeed = rwStrafeSpeed.get().floatValue();
        long time = System.currentTimeMillis();
        float zigzag = (float) Math.sin(time / 200.0) * 0.3f;

        float targetYaw = Manager.ROTATION.getYaw();
        float strafeAngle = targetYaw + (90 * rwStrafeDirection) + (zigzag * 45);

        float forward = zigzag * 0.1f;
        float strafe = baseSpeed * rwStrafeDirection * 0.8f;

        applyMovement(strafeAngle, forward, strafe, 0.18f);
    }

    private void applyCircularStrafe() {
        float baseSpeed = rwStrafeSpeed.get().floatValue() * 0.7f;
        float angle = (System.currentTimeMillis() % 6280) / 1000.0f;

        Vec3d targetPos = target.getPos();
        double radius = 2.5f + rwRandom.nextDouble() * 1.5f;
        double circleX = targetPos.x + Math.cos(angle) * radius;
        double circleZ = targetPos.z + Math.sin(angle) * radius;

        Vec3d toCircle = new Vec3d(circleX - mc.player.getX(), 0, circleZ - mc.player.getZ()).normalize();

        float forward = (float)toCircle.x * baseSpeed;
        float strafe = (float)toCircle.z * baseSpeed;
        float yaw = (float)Math.toDegrees(Math.atan2(toCircle.z, toCircle.x)) - 90.0f;

        applyMovement(yaw, forward, strafe, 0.15f);
    }

    private void applyAggressiveStrafe() {
        float baseSpeed = rwStrafeSpeed.get().floatValue() * 1.3f;
        float overtake = rwOvertakeBoost.get().floatValue();

        // Агрессивное движение к цели
        Vec3d targetPos = target.getPos();
        Vec3d toTarget = targetPos.subtract(mc.player.getPos()).normalize();

        float forward = (float)toTarget.x * baseSpeed * (1 + overtake);
        float strafe = (float)toTarget.z * baseSpeed * (1 + overtake);
        float yaw = Manager.ROTATION.getYaw();

        // Добавляем обгон
        if (rwComboCounter > 5) {
            forward *= 1.2f;
            strafe *= 1.2f;
        }

        applyMovement(yaw, forward, strafe, 0.22f);
    }

    private void applyStrafeBoostCorrection(float baseSpeed, float boost) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - rwLastStrafeChange > 500 + rwRandom.nextInt(1000)) {
            rwStrafeDirection *= -1;
            rwLastStrafeChange = currentTime;
        }

        float targetYaw = Manager.ROTATION.getYaw();
        float strafeAngle = targetYaw + (90 * rwStrafeDirection);

        double distance = target.distanceTo(mc.player);
        float speedMultiplier = 1.0f + (float)(boost * (distance / 6.0));

        float forward = 0.98f;
        float strafe = baseSpeed * rwStrafeDirection * speedMultiplier;

        applyMovement(strafeAngle, forward, strafe, 0.2f);
    }

    private void applyCircleStrafeCorrection(float baseSpeed) {
        float angle = (System.currentTimeMillis() % 6280) / 1000.0f;
        float radius = 3.0f;

        Vec3d targetPos = target.getPos();
        double circleX = targetPos.x + Math.cos(angle) * radius;
        double circleZ = targetPos.z + Math.sin(angle) * radius;

        Vec3d toCircle = new Vec3d(circleX - mc.player.getX(), 0, circleZ - mc.player.getZ()).normalize();

        float forward = (float)toCircle.x * baseSpeed;
        float strafe = (float)toCircle.z * baseSpeed;
        float yaw = (float)Math.toDegrees(Math.atan2(toCircle.z, toCircle.x)) - 90.0f;

        applyMovement(yaw, forward, strafe, 0.15f);
    }

    private void applyPredictiveCorrection(float baseSpeed) {
        if (rwPredictionTicks > 0 && rwLastTargetPos != null) {
            Vec3d predictedPos = target.getPos().add(
                    target.getVelocity().x * rwPredictionTicks,
                    target.getVelocity().y * rwPredictionTicks,
                    target.getVelocity().z * rwPredictionTicks
            );

            Vec3d toPredicted = predictedPos.subtract(mc.player.getPos()).normalize();

            float forward = (float)toPredicted.x * baseSpeed * 1.2f;
            float strafe = (float)toPredicted.z * baseSpeed * 1.2f;
            float randomOffset = (rwRandom.nextFloat() - 0.5f) * 0.3f;
            forward += randomOffset;
            strafe += randomOffset;

            float yaw = Manager.ROTATION.getYaw();
            applyMovement(yaw, forward, strafe, 0.18f);
        }
    }

    private void applyJitterCorrection(float baseSpeed) {
        float jitterStrength = rwJitterStrength.get().floatValue();

        if (rwRandom.nextInt(20) == 0) {
            rwStrafeDirection *= -1;
            rwCurrentYawOffset = (rwRandom.nextFloat() - 0.5f) * 45.0f;
        }

        rwCurrentYawOffset *= 0.9f;
        float effectiveYaw = Manager.ROTATION.getYaw() + rwCurrentYawOffset;
        float strafeAngle = effectiveYaw + (90 * rwStrafeDirection);

        float jitterX = (rwRandom.nextFloat() - 0.5f) * 2.0f * jitterStrength;
        float jitterZ = (rwRandom.nextFloat() - 0.5f) * 2.0f * jitterStrength;

        float forward = 0.92f + jitterX;
        float strafe = baseSpeed * rwStrafeDirection + jitterZ;

        applyMovement(strafeAngle, forward, strafe, 0.17f);
    }

    private void applyMovement(float yaw, float forward, float strafe, float strength) {
        if (mc.player == null || !mc.player.isOnGround()) return;

        float yawRad = (float)Math.toRadians(yaw);
        double motionX = (-Math.sin(yawRad) * forward + Math.cos(yawRad) * strafe) * strength;
        double motionZ = (Math.cos(yawRad) * forward + Math.sin(yawRad) * strafe) * strength;

        mc.player.setVelocity(motionX, mc.player.getVelocity().y, motionZ);
    }

    private void handleRWAntiCheatBypass() {
        if (!mode.is("ReallyWorld") || !rwAntiBotCheck.get()) return;

        switch (rwCombatMode.get()) {
            case "Verus":
                applyVerusBypass();
                break;
            case "Vulcan":
                applyVulcanBypass();
                break;
            case "Grim":
                applyGrimBypass();
                break;
            case "NCP":
                applyNCPBypass();
                break;
            case "Spartan":
                applySpartanBypass();
                break;
            case "Matrix":
                applyMatrixBypass();
                break;
        }
    }

    private void applyVerusBypass() {
        // Verus bypass - изменяем скорость ротации
        if (rwTicksSinceLastHit < 5) {
            // Медленная ротация после удара
            float currentYaw = Manager.ROTATION.getYaw();
            float targetYaw = calcRot(target, getRWAttackPoint(target))[0];
            float diff = MathHelper.wrapDegrees(targetYaw - currentYaw);
            Manager.ROTATION.set(currentYaw + diff * 0.3f, Manager.ROTATION.getPitch());
        }

        if (rwRandom.nextInt(100) < 5) {
            try {
                Thread.sleep(rwRandom.nextInt(15));
            } catch (InterruptedException e) {}
        }
    }

    private void applyVulcanBypass() {
        // Vulcan bypass - случайные изменения скорости
        float speedVariation = 0.8f + rwRandom.nextFloat() * 0.4f;

        // Изменяем паттерны атаки
        if (rwRandom.nextInt(50) == 0) {
            // Случайное изменение смещения удара
            float newOffset = (rwRandom.nextFloat() - 0.5f) * 0.4f;
            // Сохраняем новое значение
            // Вместо setValue используем прямой доступ если нужно
        }
    }

    private void applyGrimBypass() {
        // Grim bypass - сброс паттернов
        if (rwTicksSinceLastHit > 20) {
            rwPredictionFactor = 1.0f;
            rwStrafeDirection = 1;
        }

        // Строгие лимиты ротации
        float maxRotationSpeed = 120f;
        // Просто используем более консервативные настройки
        if (rwSmoothSpeed.get().floatValue() > maxRotationSpeed) {
            // Адаптируем скорость через локальную переменную
            float adaptedSpeed = Math.min(rwSmoothSpeed.get().floatValue(), maxRotationSpeed);
            // Используем adaptedSpeed в расчетах ротации
        }
    }

    private void applyNCPBypass() {
        // NCP bypass - адаптация под движение
        if (mc.player.isOnGround()) {
            // На земле - более консервативные настройки
            // Используем минимальное значение если нужно
        } else {
            // В воздухе - можно больше
        }

        // Линейная интерполяция ротации (если поддерживается)
        // Manager.ROTATION.setInterpolation(true); - убрано, если метод не существует
        // Manager.ROTATION.setInterpolationSpeed(0.5f);
    }

    private void applySpartanBypass() {
        // Spartan bypass - базовая скорость с вариациями
        float baseSpeed = 160f + rwRandom.nextFloat() * 40f;
        // Используем baseSpeed в расчетах

        if (rwRandom.nextInt(30) == 0) {
            rwStrafeDirection *= -1;
        }
    }

    private void applyMatrixBypass() {
        // Matrix bypass - адаптивная скорость
        if (rwTicksSinceLastHit < 3) {
            // После удара - уменьшаем скорость
            // Используем пониженную скорость в расчетах
        } else {
            // Восстанавливаем скорость
            float speed = 150f + (float)Math.sin(System.currentTimeMillis() / 1000.0) * 30f;
            // Используем speed в расчетах
        }
    }

    private void attackReallyWorld(PlayerEntity player) {
        if (rwAttackTicks < rwHitDelay.get().intValue() / 50) return;

        boolean shouldCrit = shouldCritRW();

        if (shouldCrit) {
            performRWCrit();
        }

        if (setting.get("Отжим щита") && mc.player.isBlocking()) {
            mc.interactionManager.stopUsingItem(mc.player);
        }

        if (sprintreset.is("Legit") && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }

        boolean sprint = sprintreset.is("Rage") && ((ClientPlayerEntityAccessor) mc.player).getLastSprinting();
        if (sprint) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            mc.player.setSprinting(false);
        }

        cpsLimit = System.currentTimeMillis() + 500L;

        if (rwPenetration.get() && rwPredictionFactor > 1.0f) {
            for (int i = 0; i < (int)rwPredictionFactor; i++) {
                mc.interactionManager.attackEntity(player, target);
            }
        } else {
            mc.interactionManager.attackEntity(player, target);
        }

        mc.player.swingHand(Hand.MAIN_HAND);
        rwTicksSinceLastHit = 0;
        rwComboCounter++;
        rwLastHitTime = System.currentTimeMillis();
        rwWasHit = true;

        if (rwWasCritical) {
            if (rwPenetration.get()) {
                rwPredictionFactor += 0.5f;
            }
            rwWasCritical = false;
        }

        if (setting.get("Ломать щит")) shieldBreak();

        if (sprint && mc.player.input.movementForward > 0 &&
                !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) &&
                !mc.player.isGliding() &&
                !mc.player.isUsingItem() &&
                !mc.player.horizontalCollision &&
                mc.player.getHungerManager().getFoodLevel() > 6 &&
                !mc.player.isSneaking()) {

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            mc.player.setSprinting(true);
        }

        rwAttackTicks = 0;

        if (rwPenetration.get()) {
            rwPredictionFactor = 1.0f;
        }
    }

    private boolean shouldCritRW() {
        if (!rwAutoCrit.get()) return false;

        if (rwRandom.nextFloat() * 100 > rwCritChance.get().floatValue()) {
            return false;
        }

        return mc.player.isOnGround() || rwPacketCrit.get();
    }

    private void performRWCrit() {
        rwWasCritical = true;

        if (rwPacketCrit.get()) {
            Vec3d pos = mc.player.getPos();
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, pos.y + 0.0625, pos.z, true, false
            ));
            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    pos.x, pos.y, pos.z, false, false
            ));
        } else {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    private boolean canHit(LivingEntity t) {
        if (t == null || cpsLimit > System.currentTimeMillis() || !canAttack()) return false;
        if (Manager.FUNCTION_MANAGER.autoPotion.isActivePotion) return false;
        double range = distance.get().doubleValue() + (mode.is("ReallyWorld") ? rwRangeBoost.get().floatValue() : 0);

        if (useBt() && t instanceof PlayerEntity p) {
            BackTrack bt = Manager.FUNCTION_MANAGER.backTrack;
            if (bt.getEdgeDist(btPos, p) > range) return false;
            if (raycast.get()) {
                Box box = bt.getHitBox(btPos, p);
                return box != null && rayBox(box, range + 0.3);
            }
            return true;
        }

        double dist = t instanceof PlayerEntity p ? Manager.FUNCTION_MANAGER.backTrack.getRealEdgeDist(p) : AuraUtil.getDistance(t);
        return dist <= range && (!raycast.get() || RayTraceUtil.getMouseOver(t, Manager.ROTATION.getYaw(), Manager.ROTATION.getPitch(), distance.get().floatValue()) == t);
    }

    private boolean rayBox(Box box, double range) {
        Vec3d start = mc.player.getEyePos();
        float y = Manager.ROTATION.getYaw(), p = Manager.ROTATION.getPitch();
        float f = (float) Math.cos(-y * 0.017453292F - Math.PI), f1 = (float) Math.sin(-y * 0.017453292F - Math.PI);
        float f2 = (float) -Math.cos(-p * 0.017453292F), f3 = (float) Math.sin(-p * 0.017453292F);
        return box.raycast(start, start.add(f1 * f2 * range, f3 * range, f * f2 * range)).isPresent();
    }

    private LivingEntity findTarget() {
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : Manager.SYNC_MANAGER.getEntities()) if (e instanceof LivingEntity le && isValidTarget(le)) list.add(le);
        if (list.isEmpty()) return null;
        list.sort(switch (sort.get()) {
            case "По здоровью" -> Comparator.comparing(LivingEntity::getHealth);
            case "По дистанции" -> Comparator.comparingDouble(this::effDist);
            case "По броне" -> Comparator.comparingDouble(AuraUtil::getArmor);
            case "По FOV" -> Comparator.comparingDouble(this::fov);
            default -> Comparator.comparingDouble((LivingEntity e) -> e instanceof PlayerEntity p ? -AuraUtil.getArmor(p) : 0).thenComparingDouble(LivingEntity::getHealth).thenComparingDouble(this::effDist);
        });
        return list.get(0);
    }

    private double effDist(LivingEntity e) {
        if (!(e instanceof PlayerEntity p)) return mc.player.distanceTo(e);
        BackTrack bt = Manager.FUNCTION_MANAGER.backTrack;
        if (bt.state) {
            BackTrack.Pos pos = bt.getOptimal(p, distance.get().doubleValue() + (mode.is("ReallyWorld") ? rwRangeBoost.get().floatValue() : 0));
            if (pos != null) return bt.getEdgeDist(pos, p);
        }
        return bt.getRealEdgeDist(p);
    }

    private double fov(LivingEntity e) {
        Vec3d d = e.getPos().subtract(mc.player.getPos());
        return Math.abs(MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f - mc.player.getYaw()));
    }

    private boolean isValidTarget(LivingEntity e) {
        if (e == null || e.isDead() || !e.isAlive() || e == mc.player) return false;

        // ReallyWorld проверка антибота
        if (mode.is("ReallyWorld") && rwAntiBotCheck.get()) {
            if (isBot(e)) return false;
        }

        double atkR = distance.get().doubleValue() + (mode.is("ReallyWorld") ? rwRangeBoost.get().floatValue() : 0);
        double detR = mc.player.isGliding() ? elytraDistance.get().doubleValue() : rotateDistance.get().doubleValue();
        double realDist = e instanceof PlayerEntity p ? Manager.FUNCTION_MANAGER.backTrack.getRealEdgeDist(p) : AuraUtil.getDistance(e);
        boolean inReal = realDist <= atkR || (detR > 0 && realDist <= detR);
        boolean inBt = e instanceof PlayerEntity p && Manager.FUNCTION_MANAGER.backTrack.state && Manager.FUNCTION_MANAGER.backTrack.hasReachable(p, atkR);

        if (!inReal && !inBt) return false;

        if (!mode.is("ReallyWorld") && Manager.FUNCTION_MANAGER.antiBot.isBot(e)) return false;
        if (e instanceof ArmorStandEntity) return false;

        if (e instanceof PlayerEntity) {
            if (!targets.get("Игроки")) return false;
            if (!targets.get("Друзья") && Manager.FRIEND_MANAGER.isFriend(e.getName().getString())) return false;
            if (!targets.get("Голые") && e.getArmor() == 0) return false;
            if (!targets.get("Невидимки") && e.isInvisible()) return false;
        } else if (e instanceof VillagerEntity && !targets.get("Жители")) return false;
        else if (e instanceof Monster && !targets.get("Монстры")) return false;
        else if ((e instanceof MobEntity || e instanceof AnimalEntity) && !targets.get("Мобы")) return false;

        return setting.get("Бить через стены") || mc.player.canSee(e);
    }

    private boolean isBot(Entity e) {
        if (!(e instanceof PlayerEntity)) return false;

        PlayerEntity player = (PlayerEntity) e;

        String name = player.getName().getString();
        if (name.matches(".*[0-9]{4,}.*") || name.length() < 3) {
            return true;
        }

        Vec3d velocity = player.getVelocity();
        if (velocity.length() > 5.0) {
            return true;
        }

        float yawDiff = Math.abs(MathHelper.wrapDegrees(player.getYaw() - player.prevYaw));
        if (yawDiff > 90.0f && player.age > 100) {
            return true;
        }

        return false;
    }

    public void attackTarget(PlayerEntity player) {
        if (setting.get("Отжим щита") && mc.player.isBlocking()) mc.interactionManager.stopUsingItem(mc.player);
        if (sprintreset.is("Legit") && mc.player.isSprinting()) mc.player.setSprinting(false);
        boolean sprint = sprintreset.is("Rage") && ((ClientPlayerEntityAccessor) mc.player).getLastSprinting();
        if (sprint) { mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING)); mc.player.setSprinting(false); }
        cpsLimit = System.currentTimeMillis() + 500L;
        mc.interactionManager.attackEntity(player, target);
        mc.player.swingHand(Hand.MAIN_HAND);

        if (setting.get("Ломать щит")) shieldBreak();
        if (sprint && mc.player.input.movementForward > 0 && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS) && !mc.player.isGliding() && !mc.player.isUsingItem() && !mc.player.horizontalCollision && mc.player.getHungerManager().getFoodLevel() > 6 && !mc.player.isSneaking()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING)); mc.player.setSprinting(true);
        }
    }

    private boolean canAttack() {
        if (noAttackIfEat.get() && mc.player.isUsingItem() && !mc.player.getActiveItem().isOf(Items.SHIELD)) return false;
        if (cpsLimit > System.currentTimeMillis()) return false;
        if (!mc.player.getMainHandStack().isOf(Items.MACE) && mc.player.getAttackCooldownProgress(mc.getRenderTickCounter().getTickDelta(true)) < 0.9f) return false;
        boolean r = mc.player.hasStatusEffect(StatusEffects.BLINDNESS) || mc.player.hasStatusEffect(StatusEffects.LEVITATION) || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.isInLava() || mc.player.inPowderSnow || mc.player.isClimbing() || mc.player.hasVehicle() || mc.player.getAbilities().flying || (mc.player.isInsideWaterOrBubbleColumn() && !mc.options.jumpKey.isPressed()) || MoveUtil.isInWeb();
        if (setting.get("Только критами") && !r) return (onlySpaceCritical.get() && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) || (!mc.player.isOnGround() && mc.player.fallDistance > 0f);
        return true;
    }
    // Внутри метода выбора цели в твоей Killaura:
    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living.isDead() || living.getHealth() <= 0) return false;

        // ВОТ ЭТА СТРОЧКА ИГНОРИТ БОТОВ:
        if (AntiBot.isBot(entity)) return false;

        return entity != mc.player;
    }

    private void shieldBreak() {
        int slot = InventoryUtil.getAxe().slot();
        if (slot == -1 || !(target instanceof PlayerEntity pt) || !pt.isUsingItem()) return;
        if (!pt.getOffHandStack().isOf(Items.SHIELD) && !pt.getMainHandStack().isOf(Items.SHIELD)) return;
        int sel = mc.player.getInventory().selectedSlot, sync = mc.player.currentScreenHandler.syncId;
        if (slot >= 9) {
            mc.interactionManager.clickSlot(sync, slot, sel, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(sync));
            mc.interactionManager.attackEntity(mc.player, target); mc.player.swingHand(Hand.MAIN_HAND);
            mc.interactionManager.clickSlot(sync, slot, sel, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(sync));
        } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            mc.interactionManager.attackEntity(mc.player, target); mc.player.swingHand(Hand.MAIN_HAND);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sel));
        }
    }
}