package sky.client.modules.combat;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import sky.client.events.Event;
import sky.client.events.impl.EventUpdate;
import sky.client.modules.Function;
import sky.client.modules.FunctionAnnotation;
import sky.client.modules.Type;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@FunctionAnnotation(name = "AntiBot", type = Type.Combat, desc = "Advanced ReallyWorld Bot Check")
public class AntiBot extends Function {

    // Используем Set для быстрого поиска по UUID
    public static final Set<UUID> botUuids = new HashSet<>();

    public AntiBot() {
        super();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (mc.player == null || mc.world == null) return;

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;

                if (isBotCheck(player)) {
                    botUuids.add(player.getUuid());
                } else {
                    botUuids.remove(player.getUuid());
                }
            }
        }
    }

    private boolean isBotCheck(PlayerEntity entity) {
        // 1. САМОЕ ВАЖНОЕ: Если игрок живет в мире долго, это НЕ бот античита.
        // Боты Grim/Polar обычно исчезают через 20-40 тиков (1-2 секунды).
        if (entity.age > 100) return false;

        // 2. Проверка на TAB-лист.
        // Если игрока нет в табе ВООБЩЕ - это подозрительно.
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(entity.getUuid());
        if (entry == null) return true;

        // 3. Смягчаем проверку пинга.
        // Убираем отсечку по 0мс, так как у некоторых игроков реально может быть такой пинг в табе.
        // Оставляем только проверку на "пустой" пинг.
        if (entry.getLatency() < 0) return true;

        // 4. Проверка на ник (убираем слишком строгие фильтры).
        String name = entity.getGameProfile().getName();
        if (name.isEmpty() || name.startsWith("NPC")) return true;

        // 5. Если сущность невидима И находится очень близко/высоко над головой — это бот.
        if (entity.isInvisible() && entity.getY() > mc.player.getY() + 2.0) return true;

        return false;
    }

    // Эти методы будет вызывать твоя Киллаура
    public static boolean isBot(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        return botUuids.contains(entity.getUuid());
    }

    @Override
    public void onDisable() {
        botUuids.clear();
        super.onDisable();
    }
}