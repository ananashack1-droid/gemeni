package sky.client.modules.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.events.impl.player.EventAttack;
import sky.client.manager.Manager;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;
import sky.client.modules.combat.AttackAura;
import sky.client.modules.setting.ModeSetting;
import sky.client.modules.setting.SliderSetting;
import sky.client.modules.setting.TextSetting;
import sky.client.util.player.TimerUtil;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@FunctionAnnotation(name = "AutoMessage", type = Type.Player, desc = "Автоматическое сообщение в чат или ЛС")
public class AutoMessage extends Function {

    private final ModeSetting mode = new ModeSetting(
            "Отправлять",
            "После убийства",
            "После убийства",
            "Во время таргета",
            "По задержке"
    );
    private final SliderSetting timer = new SliderSetting("Задержка", 5000f, 0f, 35000f, 1000f,
            () -> mode.is("По задержке") || mode.is("Во время таргета") || mode.is("msg(Test)"));
    private final TextSetting text = new TextSetting("Сообщение", "Привет %target%!");

    private final TimerUtil delayTimer = new TimerUtil();
    private LivingEntity lastTarget;
    private boolean waitingForDeath = false;


    private final Pattern pattern = Pattern.compile(".*");

    public AutoMessage() {
        addSettings(mode, timer, text);
    }

    @Override
    public void onEvent(Event event) {
        if (mc.player == null || mc.world == null) return;
        AttackAura aura = Manager.FUNCTION_MANAGER.attackAura;

        if (event instanceof EventAttack attackEvent && attackEvent.getTarget() instanceof LivingEntity entity) {
            if (mode.is("После убийства")) {
                lastTarget = entity;
                waitingForDeath = true;
            }
        }

        if (event instanceof EventUpdate) {
            if (mode.is("По задержке")) {
                if (delayTimer.hasTimeElapsed(timer.get().longValue())) {
                    sendMessage(replaceTarget(text.getValue(), null));
                    delayTimer.reset();
                }
                return;
            }

            if (mode.is("Во время таргета")) {
                if (aura.target != null && delayTimer.hasTimeElapsed(timer.get().longValue())) {
                    sendMessage(replaceTarget(text.getValue(), aura.target));
                    delayTimer.reset();
                }
                return;
            }

            if (mode.is("После убийства") && waitingForDeath && lastTarget != null) {
                boolean dead = lastTarget.isDead() || lastTarget.getHealth() <= 0.0F;
                boolean unloaded = mc.world.getEntityById(lastTarget.getId()) == null;

                if (dead || unloaded) {
                    sendMessage(replaceTarget(text.getValue(), lastTarget));
                    waitingForDeath = false;
                    lastTarget = null;
                    delayTimer.reset();
                }
            }
        }
    }

    private void sendMessage(String msg) {
        if (msg == null || msg.trim().isEmpty()) return;
        mc.player.networkHandler.sendChatMessage(msg);
    }

    private String replaceTarget(String msg, LivingEntity target) {
        return msg.replace("%target%", target != null ? target.getName().getString() : "хряк");
    }

    private List<String> getOnlinePlayers() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .filter(profileName -> pattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }
}
